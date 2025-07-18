package com.box.l10n.mojito.service.pollableTask;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.stereotype.Component;

@Profile("!disablescheduling")
@Configuration
@Component
@DisallowConcurrentExecution
@ConditionalOnProperty(value = "l10n.pollable-task.enabled", havingValue = "true")
public class OldPollableTaskCleanupJob implements Job {
  private static final Logger LOGGER = LoggerFactory.getLogger(OldPollableTaskCleanupJob.class);

  @Autowired private PollableTaskCleanupService pollableTaskCleanupService;

  @Autowired private OldPollableTaskCleanupConfiguration oldPollableTaskCleanupConfiguration;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    LOGGER.debug(
        "Cleaning up pollable tasks older than {}",
        this.oldPollableTaskCleanupConfiguration.getRetentionPeriod());
    this.pollableTaskCleanupService.cleanOldPollableTaskData(
        this.oldPollableTaskCleanupConfiguration.getRetentionPeriod(),
        this.oldPollableTaskCleanupConfiguration.getBatchSize());
  }

  @Bean(name = "jobDetailOldPollableTaskCleanup")
  public JobDetailFactoryBean jobDetailOldPollableTaskCleanup() {
    JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
    jobDetailFactory.setJobClass(OldPollableTaskCleanupJob.class);
    jobDetailFactory.setDescription("Delete old Pollable Task data");
    jobDetailFactory.setDurability(true);
    return jobDetailFactory;
  }

  @Bean
  public CronTriggerFactoryBean triggerOldPollableTaskCleanup(
      @Qualifier("jobDetailOldPollableTaskCleanup") JobDetail job) {
    CronTriggerFactoryBean trigger = new CronTriggerFactoryBean();
    trigger.setJobDetail(job);
    trigger.setCronExpression(this.oldPollableTaskCleanupConfiguration.getCron());
    return trigger;
  }
}
