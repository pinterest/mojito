package com.box.l10n.mojito.service.pollableTask;

import java.time.Period;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.pollable-task.cleanup-job")
public class OldPollableTaskCleanupConfiguration {
  private String cron = "0 0 0 * * ?";

  private Period retentionPeriod = Period.ofMonths(6);

  private int batchSize = 100000;

  public String getCron() {
    return cron;
  }

  public void setCron(String cron) {
    this.cron = cron;
  }

  public Period getRetentionPeriod() {
    return retentionPeriod;
  }

  public void setRetentionPeriod(Period retentionPeriod) {
    this.retentionPeriod = retentionPeriod;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }
}
