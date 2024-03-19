package com.box.l10n.mojito.service.blobstorage.s3;

import com.box.l10n.mojito.service.blobstorage.BlobStorage;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.box.l10n.mojito.service.blobstorage.redis.RedisBlobStorage;
import org.springframework.scheduling.annotation.Async;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class S3WithRedisCacheBlobStorage implements BlobStorage {

    S3BlobStorage s3BlobStorage;

    RedisBlobStorage redisBlobStorage;

    Set<String> redisCacheKeyPrefixes;

    public S3WithRedisCacheBlobStorage(S3BlobStorage s3BlobStorage, RedisBlobStorage redisBlobStorage, Set<String> redisCacheKeyPrefixes) {
        this.s3BlobStorage = s3BlobStorage;
        this.redisBlobStorage = redisBlobStorage;
        this.redisCacheKeyPrefixes = redisCacheKeyPrefixes;
    }

    @Override
    public Optional<byte[]> getBytes(String name) {
        Optional<byte[]> bytes;
        long start = System.currentTimeMillis();
        if (isACachedPrefix(name) && redisBlobStorage.exists(name)) {
            bytes = redisBlobStorage.getBytes(name);
            long end = System.currentTimeMillis();
            System.out.println("Redis get took: " + (end - start) + "ms");
        } else {
            Optional<byte[]> s3Bytes = s3BlobStorage.getBytes(name);
            if (isACachedPrefix(name) && s3Bytes.isPresent()) {
                s3Bytes.ifPresent(b -> putRedisAsync(name, b, Retention.PERMANENT));
            }
            bytes = s3Bytes;
        }

        return bytes;
    }

    @Override
    public void put(String name, byte[] content, Retention retention) {
        if (isACachedPrefix(name)) {
            redisBlobStorage.put(name, content, retention);
        }
        putS3Async(name, content, retention);
    }

    @Override
    public void delete(String name) {
        if (isACachedPrefix(name)) {
            redisBlobStorage.delete(name);
        }
        deleteS3Async(name);
    }

    @Override
    public boolean exists(String name) {
        return (isACachedPrefix(name) && redisBlobStorage.exists(name)) || s3BlobStorage.exists(name);
    }

    @Async
    public void putS3Async(String name, byte[] content, Retention retention) {
        s3BlobStorage.put(name, content, retention);
    }

    @Async
    public void putRedisAsync(String name, byte[] content, Retention retention) {
        redisBlobStorage.put(name, content, retention);
    }

    @Async
    public void deleteS3Async(String name) {
        s3BlobStorage.delete(name);
    }

    private boolean isACachedPrefix(String name) {
        // if no cache key prefixes are configured then all prefixes are cached
        return redisCacheKeyPrefixes.isEmpty() || redisCacheKeyPrefixes.stream().anyMatch(name::startsWith);
    }
}
