package com.box.l10n.mojito.service.blobstorage;

import static java.util.Optional.ofNullable;

import com.box.l10n.mojito.service.blobstorage.redis.RedisClient;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RedisStructuredBlobStorageProxy {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(RedisStructuredBlobStorageProxy.class);

  private final BlobStorage blobStorage;

  private final Optional<RedisClient> redisClientOptional;

  private final ExecutorService executorService;

  public RedisStructuredBlobStorageProxy(
      BlobStorage blobStorage,
      @Autowired(required = false) RedisClient redisClient,
      @Value("${l10n.redis.redundancy-thread-pool.num-threads:4}") int numberOfThreads) {
    this.blobStorage = blobStorage;
    this.redisClientOptional = ofNullable(redisClient);
    this.executorService = Executors.newFixedThreadPool(numberOfThreads);
  }

  private String getKey(StructuredBlobStorage.Prefix prefix, String name) {
    return prefix.toString().toLowerCase() + "/" + name;
  }

  public Optional<String> getString(StructuredBlobStorage.Prefix prefix, String name) {
    String key = this.getKey(prefix, name);
    Optional<String> redisValue =
        this.redisClientOptional.flatMap(
            redisClient -> {
              LOGGER.debug(
                  "RedisStructuredBlobStorageProxy: Retrieve string from Redis for key: {}", key);
              return redisClient.get(key);
            });
    if (redisValue.isPresent()) {
      return redisValue;
    } else {
      LOGGER.debug(
          "RedisStructuredBlobStorageProxy: Retrieve string from BlobStorage for key: {}", key);
      Optional<String> result = this.blobStorage.getString(key);
      if (result.isPresent() && this.redisClientOptional.isPresent()) {
        RedisClient redisClient = this.redisClientOptional.get();
        String value = result.get();
        CompletableFuture.runAsync(
            () -> redisClient.put(key, value, this.blobStorage.getRetention(key)),
            this.executorService);
      }
      return result;
    }
  }

  public void put(
      StructuredBlobStorage.Prefix prefix, String name, String value, Retention retention) {
    String key = this.getKey(prefix, name);
    if (this.redisClientOptional.isPresent()) {
      LOGGER.debug("RedisStructuredBlobStorageProxy: Store string in Redis for key: {}", key);
      RedisClient redisClient = this.redisClientOptional.get();
      redisClient.put(key, value, retention);
      CompletableFuture.runAsync(
          () -> this.blobStorage.put(key, value, retention), this.executorService);
    } else {
      LOGGER.debug("RedisStructuredBlobStorageProxy: Store string in BlobStorage for key: {}", key);
      this.blobStorage.put(key, value, retention);
    }
  }

  public void delete(StructuredBlobStorage.Prefix prefix, String name) {
    String key = this.getKey(prefix, name);
    if (this.redisClientOptional.isPresent()) {
      LOGGER.debug("RedisStructuredBlobStorageProxy: Deleting string from Redis for key: {}", key);
      RedisClient redisClient = this.redisClientOptional.get();
      redisClient.delete(key);
      CompletableFuture.runAsync(() -> this.blobStorage.delete(key), this.executorService);
    } else {
      LOGGER.debug(
          "RedisStructuredBlobStorageProxy: Deleting string from BlobStorage for key: {}", key);
      this.blobStorage.delete(key);
    }
  }

  public Optional<byte[]> getBytes(StructuredBlobStorage.Prefix prefix, String name) {
    String key = this.getKey(prefix, name);
    Optional<byte[]> redisValue =
        this.redisClientOptional.flatMap(
            redisClient -> {
              LOGGER.debug(
                  "RedisStructuredBlobStorageProxy: Retrieve binary object from Redis for key: {}",
                  key);
              return redisClient.getBytes(key);
            });
    if (redisValue.isPresent()) {
      return redisValue;
    } else {
      LOGGER.debug(
          "RedisStructuredBlobStorageProxy: Retrieve binary object from BlobStorage for key: {}",
          key);
      Optional<byte[]> result = this.blobStorage.getBytes(key);
      if (result.isPresent() && this.redisClientOptional.isPresent()) {
        RedisClient redisClient = this.redisClientOptional.get();
        byte[] value = result.get();
        CompletableFuture.runAsync(
            () -> redisClient.put(key, value, this.blobStorage.getRetention(key)),
            this.executorService);
      }
      return result;
    }
  }

  public void putBytes(
      StructuredBlobStorage.Prefix prefix, String name, byte[] value, Retention retention) {
    String key = this.getKey(prefix, name);
    if (this.redisClientOptional.isPresent()) {
      LOGGER.debug(
          "RedisStructuredBlobStorageProxy: Store binary object in Redis for key: {}", key);
      RedisClient redisClient = this.redisClientOptional.get();
      redisClient.put(key, value, retention);
      CompletableFuture.runAsync(
          () -> this.blobStorage.put(key, value, retention), this.executorService);
    } else {
      LOGGER.debug(
          "RedisStructuredBlobStorageProxy: Store binary object in BlobStorage for key: {}", key);
      this.blobStorage.put(key, value, retention);
    }
  }
}
