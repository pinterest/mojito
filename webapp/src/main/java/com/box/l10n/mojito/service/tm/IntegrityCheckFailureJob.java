package com.box.l10n.mojito.service.tm;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.stereotype.Component;

@Component
@Configuration
@ConditionalOnProperty(value = "l10n.integrity-check-failure.enabled", havingValue = "true")
public class IntegrityCheckFailureJob implements Job {
  @Autowired private IntegrityCheckFailureService integrityCheckFailureService;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    this.integrityCheckFailureService.cleanUpAndAlertIntegrityCheckFailures();
  }

  @Bean(name = "integrityCheckFailureCron")
  public JobDetailFactoryBean jobIntegrityCheckFailureCronJob() {
    JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
    jobDetailFactory.setJobClass(IntegrityCheckFailureJob.class);
    jobDetailFactory.setDescription("Clean up and open an incident for Integrity Check Failures");
    jobDetailFactory.setDurability(true);
    jobDetailFactory.setName("integrityCheckFailureCron");
    return jobDetailFactory;
  }

  @Bean
  public CronTriggerFactoryBean triggerIntegrityCheckFailureCronJob(
      @Qualifier("integrityCheckFailureCron") JobDetail job,
      IntegrityCheckFailureConfigurationProperties configurationProperties) {
    CronTriggerFactoryBean trigger = new CronTriggerFactoryBean();
    trigger.setJobDetail(job);
    trigger.setCronExpression(configurationProperties.getJobCronExpression());
    return trigger;
  }
}
