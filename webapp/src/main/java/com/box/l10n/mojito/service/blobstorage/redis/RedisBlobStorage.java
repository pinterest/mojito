package com.box.l10n.mojito.service.blobstorage.redis;

import com.box.l10n.mojito.service.blobstorage.BlobStorage;
import com.box.l10n.mojito.service.blobstorage.Retention;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.Optional;

/**
 * Implementation that uses Redis to store the blobs.
 */
public class RedisBlobStorage implements BlobStorage {

    private final JedisPool redisClientPool;

    public RedisBlobStorage(JedisPool redisClientPool) {
        this.redisClientPool = redisClientPool;
    }


    @Override
    public Optional<byte[]> getBytes(String name) {
        return Optional.ofNullable(redisClientPool.getResource().get(name.getBytes()));
    }

    @Override
    public void put(String name, byte[] content, Retention retention) {
        redisClientPool.getResource().set(name.getBytes(), content, (new SetParams()).ex(retentionToSecondsMapper(retention)));
    }

    @Override
    public void delete(String name) {
        redisClientPool.getResource().del(name.getBytes());
    }

    @Override
    public boolean exists(String name) {
        return redisClientPool.getResource().exists(name);
    }

    private static int retentionToSecondsMapper(Retention retention) {
        switch (retention) {
            case PERMANENT:
                return 60 * 60 * 24 * 7; // One week
            case MIN_1_DAY:
                return 60 * 60 * 24; // One day
            default:
                throw new IllegalArgumentException("Retention not supported: " + retention);
        }
    }
}
