package com.box.l10n.mojito.service.blobstorage.redis;

import static com.box.l10n.mojito.service.blobstorage.Retention.PERMANENT;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import com.box.l10n.mojito.service.blobstorage.Retention;
import com.google.common.annotations.VisibleForTesting;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisAccessControlException;
import redis.clients.jedis.exceptions.JedisException;

@Component
@ConditionalOnProperty("l10n.redis.connection.endpoint")
public class RedisClient {
  private static final Logger LOG = LoggerFactory.getLogger(RedisClient.class);

  @VisibleForTesting static final int ONE_DAY_IN_SECONDS = 24 * 60 * 60;

  private final RedisPoolManager redisPoolManager;
  private final RedisScriptManager redisScriptManager;

  private final int maxRetries;

  private final Duration retryMinBackoff;

  private final Duration retryMaxBackoff;

  public RedisClient(
      RedisPoolManager redisPoolManager,
      RedisScriptManager redisScriptManager,
      RedisConfigurationProperties redisConfigurationProperties) {
    this.redisPoolManager = redisPoolManager;
    this.redisScriptManager = redisScriptManager;
    this.maxRetries = redisConfigurationProperties.getMaxRetries();
    this.retryMinBackoff =
        Duration.ofMillis(redisConfigurationProperties.getRetryMinBackoffMillis());
    this.retryMaxBackoff =
        Duration.ofMillis(redisConfigurationProperties.getRetryMaxBackoffMillis());
  }

  public Optional<String> get(String key) {
    return Mono.fromCallable(
            () -> {
              try (Jedis redisClient = this.redisPoolManager.getJedis()) {
                return ofNullable(redisClient.get(key));
              } catch (JedisException e) {
                LOG.info("Unable to retrieve key from Redis: {}", key, e);
                throw e;
              }
            })
        .retryWhen(
            Retry.backoff(this.maxRetries, (this.retryMinBackoff))
                .maxBackoff(this.retryMaxBackoff)
                .filter(e -> e instanceof JedisAccessControlException))
        .doOnError(e -> LOG.error("Unable to retrieve key from Redis: {}", key, e))
        .onErrorReturn(empty())
        .block();
  }

  public void put(String key, String value, Retention retention) {
    Mono.fromRunnable(
            () -> {
              try (Jedis redisClient = this.redisPoolManager.getJedis()) {
                if (retention == PERMANENT) {
                  redisClient.set(key, value);
                } else {
                  redisClient.setex(key, ONE_DAY_IN_SECONDS, value);
                }
              } catch (JedisException e) {
                LOG.info("Unable to save key to Redis: {}", key, e);
                throw e;
              }
            })
        .retryWhen(
            Retry.backoff(this.maxRetries, (this.retryMinBackoff))
                .maxBackoff(this.retryMaxBackoff)
                .filter(e -> e instanceof JedisAccessControlException))
        .doOnError(e -> LOG.error("Unable to save key to Redis: {}", key, e))
        .onErrorComplete()
        .block();
  }

  public void delete(String key) {
    Mono.fromRunnable(
            () -> {
              try (Jedis redisClient = this.redisPoolManager.getJedis()) {
                redisClient.del(key);
              } catch (JedisException e) {
                LOG.info("Unable to delete key from Redis: {}", key, e);
                throw e;
              }
            })
        .retryWhen(
            Retry.backoff(this.maxRetries, (this.retryMinBackoff))
                .maxBackoff(this.retryMaxBackoff)
                .filter(e -> e instanceof JedisAccessControlException))
        .doOnError(e -> LOG.error("Unable to delete key from Redis: {}", key, e))
        .onErrorComplete()
        .block();
  }

  public Optional<byte[]> getBytes(String key) {
    return Mono.fromCallable(
            () -> {
              try (Jedis redisClient = this.redisPoolManager.getJedis()) {
                return ofNullable(redisClient.get(key.getBytes(StandardCharsets.UTF_8)));
              } catch (JedisException e) {
                LOG.info("Unable to retrieve binary key from Redis: {}", key, e);
                throw e;
              }
            })
        .retryWhen(
            Retry.backoff(this.maxRetries, (this.retryMinBackoff))
                .maxBackoff(this.retryMaxBackoff)
                .filter(e -> e instanceof JedisAccessControlException))
        .doOnError(e -> LOG.error("Unable to retrieve binary key from Redis: {}", key, e))
        .onErrorReturn(empty())
        .block();
  }

  public void put(String key, byte[] value, Retention retention) {
    Mono.fromRunnable(
            () -> {
              try (Jedis redisClient = this.redisPoolManager.getJedis()) {
                byte[] binaryKey = key.getBytes(StandardCharsets.UTF_8);
                if (retention == PERMANENT) {
                  redisClient.set(binaryKey, value);
                } else {
                  redisClient.setex(binaryKey, ONE_DAY_IN_SECONDS, value);
                }
              } catch (JedisException e) {
                LOG.info("Unable to save key to Redis: {}", key, e);
                throw e;
              }
            })
        .retryWhen(
            Retry.backoff(this.maxRetries, (this.retryMinBackoff))
                .maxBackoff(this.retryMaxBackoff)
                .filter(e -> e instanceof JedisAccessControlException))
        .doOnError(e -> LOG.error("Unable to save key to Redis: {}", key, e))
        .onErrorComplete()
        .block();
  }

  public Long executeRateLimitScript(RedisScript script, List<String> keys, List<String> args)
      throws JedisException {
    return Mono.fromCallable(
            () -> {
              try (Jedis redisClient = this.redisPoolManager.getJedis()) {
                if (!redisScriptManager.isScriptLoaded(script)) {
                  // The script may not have loaded on startup if Redis was down, try load it now
                  // before
                  // executing the script.
                  LOG.warn(
                      "RateLimitScript '{}' not loaded in Redis, loading now.",
                      script.getScriptName());
                  redisScriptManager.loadScript(script);
                }
                return (Long)
                    redisClient.evalsha(redisScriptManager.getScriptSHA(script), keys, args);
              }
            })
        .retryWhen(
            Retry.backoff(this.maxRetries, (this.retryMinBackoff))
                .maxBackoff(this.retryMaxBackoff)
                .filter(e -> e instanceof JedisAccessControlException))
        .block();
  }
}
