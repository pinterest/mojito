package com.box.l10n.mojito.service.assetcontent;

import org.quartz.JobDetail;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;

@Profile("!disablescheduling")
@Configuration
@ConditionalOnProperty(value = "l10n.asset-content.cleanup-job.enabled", havingValue = "true")
public class AssetContentCleanupJobConfig {
  @Bean(name = "jobDetailAssetContentCleanup")
  public JobDetailFactoryBean jobDetailAssetContentCleanup() {
    JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
    jobDetailFactory.setJobClass(AssetContentCleanupJob.class);
    jobDetailFactory.setDescription("Delete old Asset Content data");
    jobDetailFactory.setDurability(true);
    return jobDetailFactory;
  }

  @Bean
  public CronTriggerFactoryBean triggerAssetContentCleanup(
      @Qualifier("jobDetailAssetContentCleanup") JobDetail job,
      AssetContentCleanupConfigurationProperties configurationProperties) {
    CronTriggerFactoryBean trigger = new CronTriggerFactoryBean();
    trigger.setJobDetail(job);
    trigger.setCronExpression(configurationProperties.getCron());
    return trigger;
  }
}
