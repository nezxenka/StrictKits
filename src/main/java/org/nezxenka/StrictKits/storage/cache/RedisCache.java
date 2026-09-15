package org.nezxenka.StrictKits.storage.cache;

import lombok.RequiredArgsConstructor;
import org.nezxenka.StrictKits.storage.DatabaseConfig;
import org.nezxenka.StrictKits.storage.PlayerRecord;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public final class RedisCache implements CacheProvider {

    private static final int SUBSCRIBER_SO_TIMEOUT = 0;
    private static final long MIN_BACKOFF_MILLIS = 1000L;
    private static final long MAX_BACKOFF_MILLIS = 30000L;
    private static final String KIT_INVALIDATION = "k";

    private final DatabaseConfig config;
    private final Logger logger;
    private final String nodeId = UUID.randomUUID().toString();
    private final AtomicBoolean running = new AtomicBoolean();

    private JedisPool pool;
    private String recordPrefix;
    private String generationKey;
    private volatile long generation = 1L;
    private volatile Consumer<String> kitListener;
    private Thread subscriber;
    private JedisPubSub pubSub;

    @Override
    public String name() {
        return "Redis";
    }

    @Override
    public void initialize() {
        recordPrefix = config.getRedisKeyPrefix() + "pd:";
        generationKey = config.getRedisKeyPrefix() + "gen";

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(config.getRedisMaxTotal());
        poolConfig.setMaxIdle(config.getRedisMaxIdle());
        poolConfig.setMinIdle(config.getRedisMinIdle());
        poolConfig.setMaxWaitMillis(config.getRedisMaxWaitMillis());
        poolConfig.setTestOnBorrow(false);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setBlockWhenExhausted(true);

        pool = new JedisPool(poolConfig, config.getRedisHost(), config.getRedisPort(),
                config.getRedisTimeout(), emptyToNull(config.getRedisUsername()),
                emptyToNull(config.getRedisPassword()), config.getRedisDatabase(), null, config.isRedisSsl());

        try (Jedis jedis = pool.getResource()) {
            jedis.setnx(generationKey, "1");
            generation = parseLong(jedis.get(generationKey), 1L);
        }
        running.set(true);
        startSubscriber();
    }

    @Override
    public PlayerRecord get(UUID uuid) {
        try (Jedis jedis = pool.getResource()) {
            return RecordCodec.decode(uuid, jedis.get(key(uuid)));
        } catch (JedisException e) {
            logger.log(Level.WARNING, "Redis: не удалось прочитать данные " + uuid, e);
            return null;
        }
    }

    @Override
    public void put(PlayerRecord record) {
        String payload = RecordCodec.encode(record);
        if (payload == null) {
            return;
        }
        try (Jedis jedis = pool.getResource()) {
            jedis.setex(key(record.getUuid()), config.getRedisEntryTtlSeconds(), payload);
        } catch (JedisException e) {
            logger.log(Level.WARNING, "Redis: не удалось записать данные " + record.getUuid(), e);
        }
    }

    @Override
    public void invalidateKit(String kit) {
        try (Jedis jedis = pool.getResource()) {
            long next = jedis.incr(generationKey);
            generation = next;
            jedis.publish(config.getRedisChannel(), String.join(":", KIT_INVALIDATION, nodeId, Long.toString(next), kit));
        } catch (JedisException e) {
            logger.log(Level.WARNING, "Redis: не удалось сбросить кэш кита " + kit, e);
        }
    }

    @Override
    public void setKitInvalidationListener(Consumer<String> listener) {
        this.kitListener = listener;
    }

    @Override
    public void shutdown() {
        running.set(false);
        if (pubSub != null) {
            try {
                pubSub.unsubscribe();
            } catch (Exception ignored) {
            }
        }
        if (subscriber != null) {
            subscriber.interrupt();
        }
        if (pool != null) {
            pool.close();
        }
    }

    private String key(UUID uuid) {
        return recordPrefix + generation + ':' + uuid;
    }

    private void startSubscriber() {
        pubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                handleMessage(message);
            }
        };
        subscriber = new Thread(this::subscribeLoop, "StrictKits-Redis-Sub");
        subscriber.setDaemon(true);
        subscriber.start();
    }

    private Jedis openSubscriberConnection() {
        Jedis jedis = new Jedis(config.getRedisHost(), config.getRedisPort(),
                config.getRedisTimeout(), SUBSCRIBER_SO_TIMEOUT, config.isRedisSsl());
        String password = emptyToNull(config.getRedisPassword());
        String user = emptyToNull(config.getRedisUsername());
        if (password != null && user != null) {
            jedis.auth(user, password);
        } else if (password != null) {
            jedis.auth(password);
        }
        if (config.getRedisDatabase() > 0) {
            jedis.select(config.getRedisDatabase());
        }
        return jedis;
    }

    private void subscribeLoop() {
        long backoff = MIN_BACKOFF_MILLIS;
        while (running.get()) {
            try (Jedis jedis = openSubscriberConnection()) {
                backoff = MIN_BACKOFF_MILLIS;
                jedis.subscribe(pubSub, config.getRedisChannel());
            } catch (Exception e) {
                if (running.get()) {
                    logger.log(Level.WARNING, "Redis: подписка разорвана, повтор через " + backoff + "ms", e);
                }
            }
            if (!running.get()) {
                return;
            }
            try {
                Thread.sleep(backoff);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
            backoff = Math.min(backoff * 2L, MAX_BACKOFF_MILLIS);
        }
    }

    private void handleMessage(String message) {
        String[] parts = message.split(":", 4);
        if (parts.length != 4 || !KIT_INVALIDATION.equals(parts[0]) || nodeId.equals(parts[1])) {
            return;
        }
        generation = parseLong(parts[2], generation);
        Consumer<String> listener = kitListener;
        if (listener != null) {
            listener.accept(parts[3]);
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static long parseLong(String raw, long fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
