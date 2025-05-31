package com.box.l10n.mojito.service.blobstorage.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.redis.client-pool")
public class RedisPoolConfigurationProperties {
  private int redisPoolMaxTotal = 10;

  private int redisPoolMaxIdle = 5;

  private int redisPoolMinIdle = 1;

  private int redisTimeoutMillis = 2000;

  public int getRedisPoolMaxTotal() {
    return redisPoolMaxTotal;
  }

  public void setRedisPoolMaxTotal(int redisPoolMaxTotal) {
    this.redisPoolMaxTotal = redisPoolMaxTotal;
  }

  public int getRedisPoolMaxIdle() {
    return redisPoolMaxIdle;
  }

  public void setRedisPoolMaxIdle(int redisPoolMaxIdle) {
    this.redisPoolMaxIdle = redisPoolMaxIdle;
  }

  public int getRedisPoolMinIdle() {
    return redisPoolMinIdle;
  }

  public void setRedisPoolMinIdle(int redisPoolMinIdle) {
    this.redisPoolMinIdle = redisPoolMinIdle;
  }

  public int getRedisTimeoutMillis() {
    return redisTimeoutMillis;
  }

  public void setRedisTimeoutMillis(int redisTimeoutMillis) {
    this.redisTimeoutMillis = redisTimeoutMillis;
  }
}
