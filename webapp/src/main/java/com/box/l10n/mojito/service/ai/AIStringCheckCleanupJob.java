package com.box.l10n.mojito.service.ai;

import java.time.Period;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
@ConditionalOnProperty(value = "l10n.ai-string-check.cleanup-job.enabled", havingValue = "true")
public class AIStringCheckCleanupJob implements Job {
  @Value("${l10n.ai-string-check.cleanup-job.retention-period:P6M}")
  private Period retentionPeriod;

  @Value("${l10n.ai-string-check.cleanup-job.cron-expression:0 0 2 * * ?}")
  private String cronExpression;

  @Autowired private AIStringCheckCleanupService aiStringCheckCleanupService;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    this.aiStringCheckCleanupService.cleanup(this.retentionPeriod);
  }

  @Bean(name = "jobDetailAIStringCheckCleanupJob")
  public JobDetailFactoryBean jobDetailAIStringCheckCleanupJob() {
    JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
    jobDetailFactory.setJobClass(AIStringCheckCleanupJob.class);
    jobDetailFactory.setDescription("Delete stale AI String Check data.");
    jobDetailFactory.setDurability(true);
    return jobDetailFactory;
  }

  @Bean
  public CronTriggerFactoryBean triggerAIStringCheckCleanupJob(
      @Qualifier("jobDetailAIStringCheckCleanupJob") JobDetail job) {
    CronTriggerFactoryBean trigger = new CronTriggerFactoryBean();
    trigger.setJobDetail(job);
    trigger.setCronExpression(this.cronExpression);
    return trigger;
  }
}
