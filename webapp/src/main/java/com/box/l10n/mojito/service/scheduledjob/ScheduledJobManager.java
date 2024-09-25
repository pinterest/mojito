package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.quartz.QuartzSchedulerManager;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobConfig;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobsConfig;
import jakarta.annotation.PostConstruct;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@Component
public class ScheduledJobManager {
  @Autowired ThirdPartySyncJobsConfig thirdPartySyncJobsConfig;
  @Autowired QuartzSchedulerManager schedulerManager;
  @Autowired ScheduledJobsRepository scheduledJobsRepository;

  private static final String QUARTZ_SCHEDULER_NAME = "scheduledJobs";

  @PostConstruct
  public void init() throws ClassNotFoundException, SchedulerException {
    // Loop through jobs and schedule them
    for (ThirdPartySyncJobConfig syncJobConfig :
        thirdPartySyncJobsConfig.getThirdPartySyncJobs().values()) {
      scheduleJobFromConfig(syncJobConfig);
    }
  }

  public void scheduleJobFromConfig(ThirdPartySyncJobConfig jobConfig)
      throws ClassNotFoundException, SchedulerException {
    // For v1 these are all third party sync jobs
    ScheduledJobTypes scheduledJobType = ScheduledJobTypes.THIRD_PARTY_SYNC;
    Class<? extends Job> jobType = loadJobClass(scheduledJobType.getClassName());

    // Create the job with identity repository name and job type to easily find the job using the
    // scheduler
    JobDetail job =
        JobBuilder.newJob(jobType)
            .withIdentity(jobConfig.getRepository(), scheduledJobType.toString())
            .build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withSchedule(CronScheduleBuilder.cronSchedule(jobConfig.getCron()))
            .forJob(job)
            .build();

    getScheduler().scheduleJob(job, trigger);
    scheduledJobsRepository.addJobFromConfig(jobConfig, ScheduledJobTypes.THIRD_PARTY_SYNC);
  }

  public Scheduler getScheduler() {
    return schedulerManager.getScheduler(QUARTZ_SCHEDULER_NAME);
  }

  public boolean triggerJob(String jobName, String jobGroup) throws SchedulerException {
    JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
    Scheduler scheduler = getScheduler();

    try {
      if (!scheduler.checkExists(jobKey)) return false;
    } catch (SchedulerException e) {
      return false;
    }

    scheduler.triggerJob(jobKey);
    return true;
  }

  public Class<? extends Job> loadJobClass(String className) throws ClassNotFoundException {
    Class<?> clazz = Class.forName(className);
    // Check if the class implements Job interface
    if (!Job.class.isAssignableFrom(clazz)) {
      throw new IllegalArgumentException(
          "Class " + className + " does not implement Job interface.");
    }
    return clazz.asSubclass(Job.class);
  }
}
