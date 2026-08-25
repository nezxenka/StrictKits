package org.nezxenka.StrictKits.storage.cache;

import org.nezxenka.StrictKits.storage.DatabaseConfig;
import org.nezxenka.StrictKits.storage.PlayerRecord;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RedisCache implements CacheProvider {

    private final DatabaseConfig config;
    private final Logger logger;
    private final String nodeId = UUID.randomUUID().toString();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private JedisPool pool;
    private URI uri;
    private String recordPrefix;
    private String generationKey;
    private volatile long generation = 1L;
    private volatile Consumer<UUID> playerListener;
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
        this.uri = buildUri();
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
        this.pool = new JedisPool(poolConfig, uri, config.getRedisTimeout());

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

    private URI buildUri() {
        StringBuilder builder = new StringBuilder(64);
        builder.append(config.isRedisSsl() ? "rediss://" : "redis://");
        String user = config.getRedisUsername();
        String password = config.getRedisPassword();
        if (password != null && !password.isEmpty()) {
            builder.append(user == null ? "" : user).append(':').append(password).append('@');
        }
        builder.append(config.getRedisHost()).append(':').append(config.getRedisPort());
        builder.append('/').append(config.getRedisDatabase());
        return URI.create(builder.toString());
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
            logger.log(Level.WARNING, "Redis get failed for " + uuid, e);
            return null;
        }
    }

    @Override
    public void put(PlayerRecord record) {
        try (Jedis jedis = pool.getResource()) {
            jedis.setex(key(record.getUuid()), config.getRedisEntryTtlSeconds(), RecordCodec.encode(record));
        } catch (JedisException e) {
            logger.log(Level.WARNING, "Redis put failed for " + record.getUuid(), e);
        }
    }

    @Override
    public void invalidate(UUID uuid) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(key(uuid));
            jedis.publish(config.getRedisChannel(), "p:" + nodeId + ':' + uuid);
        } catch (JedisException e) {
            logger.log(Level.WARNING, "Redis invalidate failed for " + uuid, e);
        }
    }

    @Override
    public void invalidateKit(String kit) {
        try (Jedis jedis = pool.getResource()) {
            long next = jedis.incr(generationKey);
            generation = next;
            jedis.publish(config.getRedisChannel(), "k:" + nodeId + ':' + next + ':' + kit);
        } catch (JedisException e) {
            logger.log(Level.WARNING, "Redis kit invalidate failed for " + kit, e);
        }
    }

    @Override
    public void setRemoteInvalidationListener(Consumer<UUID> playerListener, Consumer<String> kitListener) {
        this.playerListener = playerListener;
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

    private void subscribeLoop() {
        long backoff = 1000L;
        while (running.get()) {
            try (Jedis jedis = new Jedis(uri, config.getRedisTimeout())) {
                backoff = 1000L;
                jedis.subscribe(pubSub, config.getRedisChannel());
            } catch (Exception e) {
                if (!running.get()) {
                    return;
                }
                logger.log(Level.WARNING, "Redis subscriber disconnected, retry in " + backoff + "ms", e);
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
                backoff = Math.min(backoff * 2L, 30000L);
            }
        }
    }

    private void handleMessage(String message) {
        int first = message.indexOf(':');
        if (first < 0) {
            return;
        }
        char type = message.charAt(0);
        int second = message.indexOf(':', first + 1);
        if (second < 0) {
            return;
        }
        String origin = message.substring(first + 1, second);
        if (nodeId.equals(origin)) {
            return;
        }
        String payload = message.substring(second + 1);
        if (type == 'p') {
            Consumer<UUID> listener = playerListener;
            if (listener != null) {
                try {
                    listener.accept(UUID.fromString(payload));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } else if (type == 'k') {
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
