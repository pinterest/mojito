package com.box.l10n.mojito.service.scheduledjob;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;

import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.quartz.QuartzSchedulerManager;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.scheduledjob.jobs.ScheduledThirdPartySyncProperties;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobConfig;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobsConfig;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Configuration
@Component
@ConditionalOnProperty(value = "l10n.org.quartz.scheduler.enabled", havingValue = "true")
public class ScheduledJobManager {
  static Logger logger = LoggerFactory.getLogger(ScheduledJobManager.class);

  @Autowired ThirdPartySyncJobsConfig thirdPartySyncJobsConfig;
  @Autowired QuartzSchedulerManager schedulerManager;
  @Autowired ScheduledJobRepository scheduledJobRepository;
  @Autowired ScheduledJobStatusRepository scheduledJobStatusRepository;
  @Autowired ScheduledJobTypeRepository scheduledJobTypeRepository;
  @Autowired RepositoryRepository repositoryRepository;

  /* Quartz scheduler dedicated to scheduled jobs using in memory data source */
  @Value("${l10n.scheduledJobs.quartz.schedulerName:" + DEFAULT_SCHEDULER_NAME + "}")
  private String schedulerName;

  @PostConstruct
  public void init() throws ClassNotFoundException, SchedulerException {
    logger.info("Scheduled Job Manager started.");

    // Attach Job and Trigger listeners on the 'scheduledJobs' scheduler
    getScheduler()
        .getListenerManager()
        .addJobListener(
            new ScheduledJobListener(scheduledJobRepository, scheduledJobStatusRepository));
    getScheduler()
        .getListenerManager()
        .addTriggerListener(new ScheduledJobTriggerListener(scheduledJobRepository));

    // Loop through app properties jobs and push them to DB
    for (ThirdPartySyncJobConfig syncJobConfig :
        thirdPartySyncJobsConfig.getThirdPartySyncJobs().values()) {
      if (syncJobConfig.getCron().isEmpty()) {
        logger.debug(
            "Cron expression not defined for repository {}, skipping this third party sync job.",
            syncJobConfig.getRepository());
        continue;
      }
      pushJobToDB(syncJobConfig);
    }

    // Clean up quartz jobs for this scheduler that are not in the DB.
    cleanupDB();
    scheduleJobs();
  }

  public void cleanupDB() throws SchedulerException {
    for (JobKey jobKey : getScheduler().getJobKeys(GroupMatcher.anyGroup())) {
      if (scheduledJobRepository.findByJobKey(jobKey) == null) {
        logger.info(
            "Removing {} job for repo ID: {} as it's no longer in the 'scheduled_job' table.",
            jobKey.getGroup(),
            jobKey.getName());
        getScheduler().deleteJob(jobKey);
      }
    }
  }

  public void pushJobToDB(ThirdPartySyncJobConfig jobConfig) {
    Long id = repositoryRepository.findByName(jobConfig.getRepository()).getId();
    ScheduledJob result =
        scheduledJobRepository.findByRepositoryIdAndJobType(id, ScheduledJobType.THIRD_PARTY_SYNC);
    if (result != null) return;

    ScheduledJob scheduledJob = new ScheduledJob();
    scheduledJob.setCron(jobConfig.getCron());
    scheduledJob.setRepository(repositoryRepository.findByName(jobConfig.getRepository()));
    scheduledJob.setJobStatus(
        scheduledJobStatusRepository.findByEnum(ScheduledJobStatus.IN_PROGRESS));

    scheduledJob.setJobType(
        scheduledJobTypeRepository.findByEnum(ScheduledJobType.THIRD_PARTY_SYNC));

    ScheduledThirdPartySyncProperties thirdPartySyncProperties =
        new ScheduledThirdPartySyncProperties();
    thirdPartySyncProperties.setThirdPartyProjectId(jobConfig.getThirdPartyProjectId());
    thirdPartySyncProperties.setActions(jobConfig.getActions());
    thirdPartySyncProperties.setPluralSeparator(jobConfig.getPluralSeparator());
    thirdPartySyncProperties.setLocaleMapping(jobConfig.getLocaleMapping());
    thirdPartySyncProperties.setSkipTextUnitsWithPattern(jobConfig.getSkipTextUnitsWithPattern());
    thirdPartySyncProperties.setSkipAssetsWithPathPattern(jobConfig.getSkipAssetsWithPathPattern());
    thirdPartySyncProperties.setIncludeTextUnitsWithPattern(
        jobConfig.getIncludeTextUnitsWithPattern());
    thirdPartySyncProperties.setOptions(jobConfig.getOptions());

    scheduledJob.setProperties(thirdPartySyncProperties);

    try {
      scheduledJobRepository.save(scheduledJob);
    } catch (DataIntegrityViolationException e) {
      // Attempted to insert another scheduled job into the table with the same repo and job type,
      // this can happen in a clustered quartz environment, don't need to display the error.
    }
  }

  public void scheduleJobs() throws ClassNotFoundException, SchedulerException {
    List<ScheduledJob> scheduledJobs = scheduledJobRepository.findAll();

    for (ScheduledJob scheduledJob : scheduledJobs) {
      JobKey jobKey = getJobKey(scheduledJob);
      TriggerKey triggerKey = getTriggerKey(scheduledJob);

      // Retrieve job class from enum value e.g. THIRD_PARTY_SYNC -> ScheduledThirdPartySync
      Class<? extends IScheduledJob> jobType =
          loadJobClass(scheduledJob.getJobType().getEnum().getJobClassName());

      JobDetail job = JobBuilder.newJob(jobType).withIdentity(jobKey).build();

      Trigger trigger = buildTrigger(jobKey, scheduledJob.getCron(), triggerKey);

      if (!scheduledJob.getEnabled()) {
        // The job is disabled and exists in Quartz, pause it
        if (getScheduler().checkExists(jobKey)) {
          getScheduler().pauseJob(jobKey);
        }
        return;
      }

      if (getScheduler().checkExists(jobKey)) {
        getScheduler().rescheduleJob(triggerKey, trigger);
      } else {
        getScheduler().scheduleJob(job, trigger);
      }

      scheduledJob.setJobStatus(
          scheduledJobStatusRepository.findByEnum(ScheduledJobStatus.SCHEDULED));
      scheduledJob.setStartDate(null);
      scheduledJob.setEndDate(null);
      scheduledJobRepository.save(scheduledJob);
    }
  }

  public Trigger buildTrigger(JobKey jobKey, String cronExpression, TriggerKey triggerKey) {
    return TriggerBuilder.newTrigger()
        .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
        .withIdentity(triggerKey)
        .forJob(jobKey)
        .build();
  }

  private JobKey getJobKey(ScheduledJob scheduledJob) {
    return new JobKey(
        scheduledJob.getRepository().getId().toString(),
        scheduledJob.getJobType().getEnum().toString());
  }

  private TriggerKey getTriggerKey(ScheduledJob scheduledJob) {
    return new TriggerKey(
        "trigger_" + scheduledJob.getRepository().getId().toString(),
        scheduledJob.getJobType().getEnum().toString());
  }

  public Scheduler getScheduler() {
    return schedulerManager.getScheduler(schedulerName);
  }

  public boolean triggerJob(String jobName, String jobGroup) throws SchedulerException {
    JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
    try {
      if (!getScheduler().checkExists(jobKey)) return false;
      getScheduler().triggerJob(jobKey);
      return true;
    } catch (SchedulerException e) {
      return false;
    }
  }

  public Class<? extends IScheduledJob> loadJobClass(String className)
      throws ClassNotFoundException {
    Class<?> clazz = Class.forName(className);
    // Reflection to check if the class implements the IScheduledJob interface
    if (!IScheduledJob.class.isAssignableFrom(clazz)) {
      throw new IllegalArgumentException(
          "Class " + className + " does not implement IScheduledJob interface.");
    }
    return clazz.asSubclass(IScheduledJob.class);
  }
}
