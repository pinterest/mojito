package com.box.l10n.mojito.service.scheduledjob;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "l10n.scheduledjobs.listener.retry")
public class ScheduledJobListenerRetryConfiguration {
  private int maxAttempts = 5;
  private long initialInterval = 10000;
  private double multiplier = 2;

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public long getInitialInterval() {
    return initialInterval;
  }

  public void setInitialInterval(long initialInterval) {
    this.initialInterval = initialInterval;
  }

  public double getMultiplier() {
    return multiplier;
  }

  public void setMultiplier(double multiplier) {
    this.multiplier = multiplier;
  }
}
