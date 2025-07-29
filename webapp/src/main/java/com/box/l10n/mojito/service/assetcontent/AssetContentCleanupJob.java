package com.box.l10n.mojito.service.assetcontent;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("!disablescheduling")
@Component
@DisallowConcurrentExecution
@ConditionalOnProperty(value = "l10n.asset-content.cleanup-job.enabled", havingValue = "true")
public class AssetContentCleanupJob implements Job {
  private static final Logger LOGGER = LoggerFactory.getLogger(AssetContentCleanupJob.class);

  @Autowired private AssetContentService assetContentService;

  @Autowired private AssetContentCleanupConfigurationProperties configurationProperties;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    LOGGER.debug("Cleaning up asset content");
    this.assetContentService.cleanAssetContentData(
        this.configurationProperties.getRetentionPeriod(),
        this.configurationProperties.getBatchSize());
  }
}
