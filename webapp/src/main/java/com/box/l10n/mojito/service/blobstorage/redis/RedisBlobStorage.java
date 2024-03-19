package com.box.l10n.mojito.service.blobstorage.redis;

import com.box.l10n.mojito.service.blobstorage.BlobStorage;
import com.box.l10n.mojito.service.blobstorage.Retention;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.Optional;

/**
 * Implementation that uses Redis to store the blobs.
 */
public class RedisBlobStorage implements BlobStorage {

    private static final Logger logger = LoggerFactory.getLogger(RedisBlobStorage.class);

    private final JedisPool redisClientPool;

    public RedisBlobStorage(JedisPool redisClientPool) {
        this.redisClientPool = redisClientPool;
    }


    @Override
    public Optional<byte[]> getBytes(String name) {
        Optional<byte[]> bytes;
        try (Jedis jedis = redisClientPool.getResource()) {
            long startTime = System.currentTimeMillis();
            bytes = Optional.ofNullable(jedis.get(name.getBytes()));
            long endTime = System.currentTimeMillis();
            System.out.println("Redis get took: " + (endTime - startTime) + "ms");
        }
        return bytes;
    }

    @Override
    public void put(String name, byte[] content, Retention retention) {
        try (Jedis jedis = redisClientPool.getResource()) {
            jedis.set(name.getBytes(), content, (new SetParams()).ex(retentionToSecondsMapper(retention)));
        }
    }

    @Override
    public void delete(String name) {
        try (Jedis jedis = redisClientPool.getResource()) {
            jedis.del(name.getBytes());
        }
    }

    @Override
    public boolean exists(String name) {
        try (Jedis jedis = redisClientPool.getResource()) {
            return jedis.exists(name);
        }
    }

        private static int retentionToSecondsMapper(Retention retention) {
        switch (retention) {
            case PERMANENT:
                return 60 * 60 * 24 * 7; // One week
            case MIN_1_DAY:
                return 60 * 60 * 24; // One day
            default:
                logger.warn(String.format("Retention {} not supported, using default of one day", retention.name()));
                return 60 * 60 * 24; // One day
        }
    }
}
