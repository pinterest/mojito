package com.box.l10n.mojito.service.blobstorage.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.redis")
public class RedisConfigurationProperties {
  private String redisEndpoint;

  private int redisPort = 6379;

  private String accessKey;

  private String secretKey;

  private String region;

  private String redisUserId;

  private String replicationGroupId;

  public String getRedisEndpoint() {
    return redisEndpoint;
  }

  public void setRedisEndpoint(String redisEndpoint) {
    this.redisEndpoint = redisEndpoint;
  }

  public int getRedisPort() {
    return redisPort;
  }

  public void setRedisPort(int redisPort) {
    this.redisPort = redisPort;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getRedisUserId() {
    return redisUserId;
  }

  public void setRedisUserId(String redisUserId) {
    this.redisUserId = redisUserId;
  }

  public String getReplicationGroupId() {
    return replicationGroupId;
  }

  public void setReplicationGroupId(String replicationGroupId) {
    this.replicationGroupId = replicationGroupId;
  }
}
