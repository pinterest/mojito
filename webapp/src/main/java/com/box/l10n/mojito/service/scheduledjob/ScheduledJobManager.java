package com.box.l10n.mojito.service.scheduledjob;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;

import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.quartz.QuartzSchedulerManager;
import com.box.l10n.mojito.retry.DeadLockLoserExceptionRetryTemplate;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.scheduledjob.jobs.ScheduledThirdPartySyncProperties;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobConfig;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobsConfig;
import com.google.common.base.Strings;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
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

/**
 * Component for scheduling jobs inside the scheduled jobs table. Currently, jobs are pulled from
 * the application.properties and pushed to the scheduled_job table.
 *
 * @author mattwilshire
 */
@Configuration
@Component
@ConditionalOnProperty(value = "l10n.scheduledJobs.enabled", havingValue = "true")
public class ScheduledJobManager {
  static Logger logger = LoggerFactory.getLogger(ScheduledJobManager.class);

  @Autowired ThirdPartySyncJobsConfig thirdPartySyncJobsConfig;
  @Autowired QuartzSchedulerManager schedulerManager;
  @Autowired ScheduledJobRepository scheduledJobRepository;
  @Autowired ScheduledJobStatusRepository scheduledJobStatusRepository;
  @Autowired ScheduledJobTypeRepository scheduledJobTypeRepository;
  @Autowired RepositoryRepository repositoryRepository;
  @Autowired DeadLockLoserExceptionRetryTemplate deadlockRetryTemplate;

  private final List<String> uuidPool = new ArrayList<>();

  /* Quartz scheduler dedicated to scheduled jobs using in memory data source */
  @Value("${l10n.scheduledJobs.quartz.schedulerName:" + DEFAULT_SCHEDULER_NAME + "}")
  private String schedulerName;

  @PostConstruct
  public void init() throws ClassNotFoundException, SchedulerException {
    logger.info("Scheduled Job Manager started.");
    // Add Job Listener
    getScheduler()
        .getListenerManager()
        .addJobListener(
            new ScheduledJobListener(
                scheduledJobRepository, scheduledJobStatusRepository, deadlockRetryTemplate));
    // Add Trigger Listener
    getScheduler()
        .getListenerManager()
        .addTriggerListener(new ScheduledJobTriggerListener(scheduledJobRepository));

    pushJobsToDB();
    cleanQuartzJobs();
    scheduleAllJobs();
  }

  /** Schedule all the jobs in the scheduled_job table with their cron expression. */
  public void scheduleAllJobs() throws ClassNotFoundException, SchedulerException {
    List<ScheduledJob> scheduledJobs = scheduledJobRepository.findAll();

    for (ScheduledJob scheduledJob : scheduledJobs) {
      JobKey jobKey = getJobKey(scheduledJob);
      TriggerKey triggerKey = getTriggerKey(scheduledJob);

      // Retrieve job class from enum value e.g. THIRD_PARTY_SYNC -> ScheduledThirdPartySync
      Class<? extends IScheduledJob> jobType =
          loadJobClass(scheduledJob.getJobType().getEnum().getJobClassName());

      JobDetail job = JobBuilder.newJob(jobType).withIdentity(jobKey).build();
      Trigger trigger = buildTrigger(jobKey, scheduledJob.getCron(), triggerKey);

      if (getScheduler().checkExists(jobKey)) {
        // The cron could have changed, reschedule the job
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

  /** Push the jobs defined in the application.properties to the jobs table. */
  public void pushJobsToDB() {
    for (ThirdPartySyncJobConfig syncJobConfig :
        thirdPartySyncJobsConfig.getThirdPartySyncJobs().values()) {
      if (Strings.isNullOrEmpty(syncJobConfig.getUuid())
          || Strings.isNullOrEmpty(syncJobConfig.getCron())) {
        logger.debug(
            "UUID or cron expression not defined for repository {}, skipping this third party sync job.",
            syncJobConfig.getRepository());
        continue;
      }
      uuidPool.add(syncJobConfig.getUuid());

      ScheduledJob scheduledJob =
          scheduledJobRepository.findByIdAndJobType(
              syncJobConfig.getUuid(), ScheduledJobType.THIRD_PARTY_SYNC);

      if (scheduledJob == null) {
        scheduledJob = new ScheduledJob();
      }

      scheduledJob.setId(syncJobConfig.getUuid());
      scheduledJob.setCron(syncJobConfig.getCron());
      scheduledJob.setRepository(repositoryRepository.findByName(syncJobConfig.getRepository()));
      scheduledJob.setJobStatus(
          scheduledJobStatusRepository.findByEnum(ScheduledJobStatus.SCHEDULED));
      scheduledJob.setJobType(
          scheduledJobTypeRepository.findByEnum(ScheduledJobType.THIRD_PARTY_SYNC));
      scheduledJob.setProperties(getScheduledThirdPartySyncProperties(syncJobConfig));

      try {
        scheduledJobRepository.save(scheduledJob);
      } catch (DataIntegrityViolationException e) {
        // Attempted to insert another scheduled job into the table with the same repo and job type,
        // this can happen in a clustered quartz environment, don't need to display the error.
      }
    }
  }

  /**
   * Remove jobs defined under this custom scheduler that are not listed in the application
   * properties but are present in the DB table.
   */
  public void cleanQuartzJobs() throws SchedulerException {
    // Clean Quartz jobs that don't exist in the UUID pool
    logger.info("Performing Quartz scheduled jobs clean up");
    for (JobKey jobKey : getScheduler().getJobKeys(GroupMatcher.anyGroup())) {
      if (!uuidPool.contains(jobKey.getName())) {
        if (getScheduler().checkExists(jobKey)) {
          getScheduler().deleteJob(jobKey);
        }

        scheduledJobRepository
            .findById(jobKey.getName())
            .ifPresent(
                job -> {
                  scheduledJobRepository.delete(job);
                });

        logger.info(
            "Removed job with id: '{}' as it is no longer in the data source.", jobKey.getName());
      }
    }
  }

  // v1
  private ScheduledThirdPartySyncProperties getScheduledThirdPartySyncProperties(
      ThirdPartySyncJobConfig jobConfig) {
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
    return thirdPartySyncProperties;
  }

  public Trigger buildTrigger(JobKey jobKey, String cronExpression, TriggerKey triggerKey) {
    // Misfires will only happen if the job was triggered manually and the cron schedule was missed
    // The misfire policy is defaulted to 60s, meaning it is only a misfire if it
    // misses its schedule by 60s, we shouldn't have this happen normally
    return TriggerBuilder.newTrigger()
        .withSchedule(
            CronScheduleBuilder.cronSchedule(cronExpression)
                .withMisfireHandlingInstructionDoNothing())
        .withIdentity(triggerKey)
        .forJob(jobKey)
        .build();
  }

  public JobKey getJobKey(ScheduledJob scheduledJob) {
    // name = UUID
    // group = THIRD_PARTY_SYNC
    return new JobKey(scheduledJob.getId(), scheduledJob.getJobType().getEnum().toString());
  }

  private TriggerKey getTriggerKey(ScheduledJob scheduledJob) {
    // name = trigger_UUID
    // group = THIRD_PARTY_SYNC
    return new TriggerKey(
        "trigger_" + scheduledJob.getId(), scheduledJob.getJobType().getEnum().toString());
  }

  public Scheduler getScheduler() {
    return schedulerManager.getScheduler(schedulerName);
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
