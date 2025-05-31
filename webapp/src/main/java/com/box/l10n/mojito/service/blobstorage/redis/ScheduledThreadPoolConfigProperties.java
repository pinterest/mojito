package com.box.l10n.mojito.service.blobstorage.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.redis.client-refresher-pool")
public class ScheduledThreadPoolConfigProperties {
  private int scheduledThreadPoolSize = 1;

  private int scheduledThreadPeriodInSeconds = 10;

  public int getScheduledThreadPoolSize() {
    return scheduledThreadPoolSize;
  }

  public void setScheduledThreadPoolSize(int scheduledThreadPoolSize) {
    this.scheduledThreadPoolSize = scheduledThreadPoolSize;
  }

  public int getScheduledThreadPeriodInSeconds() {
    return scheduledThreadPeriodInSeconds;
  }

  public void setScheduledThreadPeriodInSeconds(int scheduledThreadPeriodInSeconds) {
    this.scheduledThreadPeriodInSeconds = scheduledThreadPeriodInSeconds;
  }
}
