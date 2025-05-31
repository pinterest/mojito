package com.box.l10n.mojito.service.blobstorage.redis;

import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

@Component
@ConditionalOnProperty("l10n.redis.connection.redisEndpoint")
public class RedisPoolManager {
  private static final Logger LOG = LoggerFactory.getLogger(RedisPoolManager.class);

  private JedisPool jedisPool;

  private final RedisConfigurationProperties redisConfigurationProperties;

  private final RedisPoolConfigurationProperties redisPoolConfigurationProperties;

  private final ScheduledThreadPoolConfigProperties scheduledThreadPoolConfigProperties;

  private final ScheduledExecutorService scheduler;

  public RedisPoolManager(
      RedisConfigurationProperties redisConfigurationProperties,
      RedisPoolConfigurationProperties redisPoolConfigurationProperties,
      ScheduledThreadPoolConfigProperties scheduledThreadPoolConfigProperties) {
    this.redisConfigurationProperties = redisConfigurationProperties;
    this.redisPoolConfigurationProperties = redisPoolConfigurationProperties;
    this.scheduledThreadPoolConfigProperties = scheduledThreadPoolConfigProperties;
    this.scheduler =
        Executors.newScheduledThreadPool(
            this.scheduledThreadPoolConfigProperties.getScheduledThreadPoolSize());
  }

  @VisibleForTesting
  AwsCredentials getAwsCredentials() {
    if (this.redisConfigurationProperties.getAccessKey() != null
        && this.redisConfigurationProperties.getSecretKey() != null) {
      LOG.info("Using AWS credentials from the properties file");
      return AwsBasicCredentials.create(
          this.redisConfigurationProperties.getAccessKey(),
          this.redisConfigurationProperties.getSecretKey());
    }
    try (DefaultCredentialsProvider defaultCredentialsProvider =
        DefaultCredentialsProvider.create()) {
      LOG.info("Using default AWS credentials");
      return defaultCredentialsProvider.resolveCredentials();
    }
  }

  @VisibleForTesting
  void refreshJedisPool() {
    if (this.jedisPool != null) {
      this.jedisPool.close();
    }
    IAMAuthTokenRequest iamAuthTokenRequest =
        new IAMAuthTokenRequest(
            this.redisConfigurationProperties.getRedisUserId(),
            this.redisConfigurationProperties.getReplicationGroupId(),
            this.redisConfigurationProperties.getRegion());
    String authToken = iamAuthTokenRequest.toSignedRequestUri(this.getAwsCredentials());
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(this.redisPoolConfigurationProperties.getRedisPoolMaxTotal());
    poolConfig.setMaxIdle(this.redisPoolConfigurationProperties.getRedisPoolMaxIdle());
    poolConfig.setMinIdle(this.redisPoolConfigurationProperties.getRedisPoolMinIdle());
    DefaultJedisClientConfig clientConfig =
        DefaultJedisClientConfig.builder()
            .user(this.redisConfigurationProperties.getRedisUserId())
            .password(authToken)
            .ssl(true)
            .timeoutMillis(this.redisPoolConfigurationProperties.getRedisTimeoutMillis())
            .build();
    this.jedisPool =
        new JedisPool(
            poolConfig,
            new HostAndPort(
                this.redisConfigurationProperties.getRedisEndpoint(),
                this.redisConfigurationProperties.getRedisPort()),
            clientConfig);
  }

  @PostConstruct
  public void init() {
    this.refreshJedisPool();
    this.scheduler.scheduleAtFixedRate(
        this::refreshJedisPool,
        this.scheduledThreadPoolConfigProperties.getScheduledThreadPeriodInSeconds(),
        this.scheduledThreadPoolConfigProperties.getScheduledThreadPeriodInSeconds(),
        TimeUnit.MINUTES);
  }

  public Jedis getJedis() {
    return this.jedisPool.getResource();
  }

  @PreDestroy
  public void shutdown() {
    if (this.jedisPool != null) {
      this.jedisPool.close();
    }
    this.scheduler.shutdown();
  }

  JedisPool getJedisPool() {
    return this.jedisPool;
  }
}
