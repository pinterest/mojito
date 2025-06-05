package com.box.l10n.mojito.service.blobstorage;

import static com.box.l10n.mojito.service.blobstorage.Retention.PERMANENT;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import com.box.l10n.mojito.service.blobstorage.redis.RedisPoolManager;
import com.google.common.annotations.VisibleForTesting;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

@Component
public class RedisStructuredBlobStorage {
  private static final Logger LOGGER = LoggerFactory.getLogger(RedisStructuredBlobStorage.class);

  @VisibleForTesting static final int ONE_DAY_IN_SECONDS = 24 * 60 * 60;

  private final BlobStorage blobStorage;

  private final Optional<RedisPoolManager> redisPoolManager;

  private final ExecutorService executorService;

  public RedisStructuredBlobStorage(
      BlobStorage blobStorage,
      @Autowired(required = false) RedisPoolManager redisPoolManager,
      @Value("${l10n.redis.redundancy-thread-pool.num-threads:4}") int numberOfThreads) {
    this.blobStorage = blobStorage;
    this.redisPoolManager = ofNullable(redisPoolManager);
    this.executorService = Executors.newFixedThreadPool(numberOfThreads);
  }

  private String getKey(StructuredBlobStorage.Prefix prefix, String name) {
    return prefix.toString().toLowerCase() + "/" + name;
  }

  public Optional<String> getString(StructuredBlobStorage.Prefix prefix, String name) {
    String key = this.getKey(prefix, name);
    try (Jedis redisClient = this.redisPoolManager.map(RedisPoolManager::getJedis).orElse(null)) {
      if (redisClient != null && redisClient.exists(key)) {
        LOGGER.debug("RedisStructuredBlobStorage: Retrieve string from Redis for key: {}", key);
        return of(redisClient.get(key));
      } else {
        LOGGER.debug(
            "RedisStructuredBlobStorage: Retrieve string from BlobStorage for key: {}", key);
        Optional<String> result = this.blobStorage.getString(key);
        if (result.isPresent() && redisClient != null) {
          Optional<Retention> retention = this.blobStorage.getRetention(key);
          CompletableFuture.runAsync(
              () -> {
                try (Jedis asyncRedisClient =
                    this.redisPoolManager.map(RedisPoolManager::getJedis).orElse(null)) {
                  if (retention.isPresent() && retention.get() != PERMANENT) {
                    asyncRedisClient.setex(key, ONE_DAY_IN_SECONDS, result.get());
                  } else {
                    asyncRedisClient.set(key, result.get());
                  }
                }
              },
              this.executorService);
        }
        return result;
      }
    }
  }

  public void put(
      StructuredBlobStorage.Prefix prefix, String name, String content, Retention retention) {
    String key = this.getKey(prefix, name);
    try (Jedis redisClient = this.redisPoolManager.map(RedisPoolManager::getJedis).orElse(null)) {
      if (redisClient != null) {
        LOGGER.debug("RedisStructuredBlobStorage: Store string in Redis for key: {}", key);
        if (retention == PERMANENT) {
          redisClient.set(key, content);
        } else {
          redisClient.setex(key, ONE_DAY_IN_SECONDS, content);
        }
        CompletableFuture.runAsync(
            () -> this.blobStorage.put(key, content, retention), this.executorService);
      } else {
        LOGGER.debug(
            "RedisStructuredBlobStorage: Store string in BlobStorage with retention for key: {}",
            key);
        this.blobStorage.put(key, content, retention);
      }
    }
  }

  public void delete(StructuredBlobStorage.Prefix prefix, String name) {
    String key = this.getKey(prefix, name);
    LOGGER.debug("RedisStructuredBlobStorage: Deleting string from BlobStorage for key: {}", key);
    try (Jedis redisClient = this.redisPoolManager.map(RedisPoolManager::getJedis).orElse(null)) {
      if (redisClient != null) {
        redisClient.del(key);
        CompletableFuture.runAsync(() -> this.blobStorage.delete(key), this.executorService);
      } else {
        this.blobStorage.delete(key);
      }
    }
  }

  public Optional<byte[]> getBytes(StructuredBlobStorage.Prefix prefix, String name) {
    String key = this.getKey(prefix, name);
    try (Jedis redisClient = this.redisPoolManager.map(RedisPoolManager::getJedis).orElse(null)) {
      if (redisClient != null && redisClient.exists(key)) {
        LOGGER.debug(
            "RedisStructuredBlobStorage: Retrieve binary object from Redis for key: {}", key);
        return of(redisClient.get(key.getBytes(StandardCharsets.UTF_8)));
      } else {
        LOGGER.debug(
            "RedisStructuredBlobStorage: Retrieve binary object from BlobStorage for key: {}", key);
        Optional<byte[]> result = this.blobStorage.getBytes(key);
        if (result.isPresent() && redisClient != null) {
          Optional<Retention> retention = this.blobStorage.getRetention(key);
          CompletableFuture.runAsync(
              () -> {
                try (Jedis asyncRedisClient =
                    this.redisPoolManager.map(RedisPoolManager::getJedis).orElse(null)) {
                  if (retention.isPresent() && retention.get() != PERMANENT) {
                    asyncRedisClient.setex(
                        key.getBytes(StandardCharsets.UTF_8), ONE_DAY_IN_SECONDS, result.get());
                  } else {
                    asyncRedisClient.set(key.getBytes(StandardCharsets.UTF_8), result.get());
                  }
                }
              },
              this.executorService);
        }
        return result;
      }
    }
  }

  public void putBytes(
      StructuredBlobStorage.Prefix prefix, String name, byte[] content, Retention retention) {
    String key = this.getKey(prefix, name);
    try (Jedis redisClient = this.redisPoolManager.map(RedisPoolManager::getJedis).orElse(null)) {
      if (redisClient != null) {
        LOGGER.debug("RedisStructuredBlobStorage: Store binary object in Redis for key: {}", key);
        if (retention == PERMANENT) {
          redisClient.set(key.getBytes(StandardCharsets.UTF_8), content);
        } else {
          redisClient.setex(key.getBytes(StandardCharsets.UTF_8), ONE_DAY_IN_SECONDS, content);
        }
        CompletableFuture.runAsync(
            () -> this.blobStorage.put(key, content, retention), this.executorService);
      } else {
        LOGGER.debug(
            "RedisStructuredBlobStorage: Store binary object in BlobStorage for key: {}", key);
        this.blobStorage.put(key, content, retention);
      }
    }
  }
}
