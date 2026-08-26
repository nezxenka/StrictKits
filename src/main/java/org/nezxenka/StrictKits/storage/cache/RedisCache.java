package org.nezxenka.StrictKits.storage.cache;

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

public final class RedisCache implements CacheProvider {

    private static final int SUBSCRIBER_SO_TIMEOUT = 0;

    private final DatabaseConfig config;
    private final Logger logger;
    private final String nodeId = UUID.randomUUID().toString();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private JedisPool pool;
    private String recordPrefix;
    private String generationKey;
    private volatile long generation = 1L;
    private volatile Consumer<String> kitListener;
    private Thread subscriber;
    private JedisPubSub pubSub;

    public RedisCache(DatabaseConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    @Override
    public String name() {
        return "Redis";
    }

    @Override
    public void initialize() throws Exception {
        this.recordPrefix = config.getRedisKeyPrefix() + "pd:";
        this.generationKey = config.getRedisKeyPrefix() + "gen";

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(config.getRedisMaxTotal());
        poolConfig.setMaxIdle(config.getRedisMaxIdle());
        poolConfig.setMinIdle(config.getRedisMinIdle());
        poolConfig.setMaxWaitMillis(config.getRedisMaxWaitMillis());
        poolConfig.setTestOnBorrow(false);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setBlockWhenExhausted(true);

        this.pool = new JedisPool(poolConfig, config.getRedisHost(), config.getRedisPort(),
                config.getRedisTimeout(), emptyToNull(config.getRedisUsername()),
                emptyToNull(config.getRedisPassword()), config.getRedisDatabase(), null, config.isRedisSsl());

        try (Jedis jedis = pool.getResource()) {
            String stored = jedis.get(generationKey);
            if (stored == null) {
                jedis.set(generationKey, "1");
                generation = 1L;
            } else {
                generation = parseLong(stored, 1L);
            }
        }
        running.set(true);
        startSubscriber();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static long parseLong(String raw, long fallback) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String key(UUID uuid) {
        return recordPrefix + generation + ':' + uuid;
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
            jedis.publish(config.getRedisChannel(), "k:" + nodeId + ':' + next + ':' + kit);
        } catch (JedisException e) {
            logger.log(Level.WARNING, "Redis: не удалось сбросить кэш кита " + kit, e);
        }
    }

    @Override
    public void setKitInvalidationListener(Consumer<String> kitListener) {
        this.kitListener = kitListener;
    }

    private void startSubscriber() {
        this.pubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                handleMessage(message);
            }
        };
        this.subscriber = new Thread(this::subscribeLoop, "StrictKits-Redis-Sub");
        this.subscriber.setDaemon(true);
        this.subscriber.start();
    }

    private Jedis openSubscriberConnection() {
        Jedis jedis = new Jedis(config.getRedisHost(), config.getRedisPort(),
                config.getRedisTimeout(), SUBSCRIBER_SO_TIMEOUT, config.isRedisSsl());
        String password = emptyToNull(config.getRedisPassword());
        if (password != null) {
            String user = emptyToNull(config.getRedisUsername());
            if (user != null) {
                jedis.auth(user, password);
            } else {
                jedis.auth(password);
            }
        }
        if (config.getRedisDatabase() > 0) {
            jedis.select(config.getRedisDatabase());
        }
        return jedis;
    }

    private void subscribeLoop() {
        long backoff = 1000L;
        while (running.get()) {
            try (Jedis jedis = openSubscriberConnection()) {
                backoff = 1000L;
                jedis.subscribe(pubSub, config.getRedisChannel());
            } catch (Exception e) {
                if (!running.get()) {
                    return;
                }
                logger.log(Level.WARNING, "Redis: подписка разорвана, повтор через " + backoff + "ms", e);
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
            backoff = Math.min(backoff * 2L, 30000L);
        }
    }

    private void handleMessage(String message) {
        if (message.isEmpty() || message.charAt(0) != 'k') {
            return;
        }
        int first = message.indexOf(':');
        if (first < 0) {
            return;
        }
        int second = message.indexOf(':', first + 1);
        if (second < 0) {
            return;
        }
        if (nodeId.equals(message.substring(first + 1, second))) {
            return;
        }
        String payload = message.substring(second + 1);
        int split = payload.indexOf(':');
        if (split < 0) {
            return;
        }
        generation = parseLong(payload.substring(0, split), generation);
        Consumer<String> listener = kitListener;
        if (listener != null) {
            listener.accept(payload.substring(split + 1));
        }
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
}
