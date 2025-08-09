package com.box.l10n.mojito.service.delta;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "l10n.push-pull-run.cleanup-job")
public class PushPullRunCleanupConfigurationProperties {
  /** Runs once a day by default. */
  private String cron = "0 0 0 * * ?";

  private Duration retentionDuration = Duration.ofDays(30);

  private int extraNumberOfWeeksToRetain = 2;

  private int deleteBatchSize = 100000;

  private int maxNumberOfBatches = 100;

  public String getCron() {
    return cron;
  }

  public void setCron(String cron) {
    this.cron = cron;
  }

  public Duration getRetentionDuration() {
    return retentionDuration;
  }

  public void setRetentionDuration(Duration retentionDuration) {
    this.retentionDuration = retentionDuration;
  }

  public int getExtraNumberOfWeeksToRetain() {
    return extraNumberOfWeeksToRetain;
  }

  public void setExtraNumberOfWeeksToRetain(int extraNumberOfWeeksToRetain) {
    this.extraNumberOfWeeksToRetain = extraNumberOfWeeksToRetain;
  }

  public int getDeleteBatchSize() {
    return deleteBatchSize;
  }

  public void setDeleteBatchSize(int deleteBatchSize) {
    this.deleteBatchSize = deleteBatchSize;
  }

  public int getMaxNumberOfBatches() {
    return maxNumberOfBatches;
  }

  public void setMaxNumberOfBatches(int maxNumberOfBatches) {
    this.maxNumberOfBatches = maxNumberOfBatches;
  }
}
