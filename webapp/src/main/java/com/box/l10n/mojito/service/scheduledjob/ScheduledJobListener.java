package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.entity.ScheduledJob;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Listener that listens for Quartz job events, this listener is attached to the 'scheduledJobs'
 * scheduler and handles setting the job status, start date and end date for pre- and post-execution
 * of the job. Scheduled jobs implement the IScheduledJob interface which allows the job to receive
 * failure and success events from the listener.
 *
 * @author mattwilshire
 */
public class ScheduledJobListener extends JobListenerSupport {

  static Logger logger = LoggerFactory.getLogger(ScheduledJobListener.class);

  private final ScheduledJobRepository scheduledJobRepository;
  private final ScheduledJobStatusRepository scheduledJobStatusRepository;
  private final RetryTemplate retryTemplate;

  public ScheduledJobListener(
      ScheduledJobRepository scheduledJobRepository,
      ScheduledJobStatusRepository scheduledJobStatusRepository,
      ScheduledJobListenerRetryConfiguration retryConfiguration) {
    this.scheduledJobRepository = scheduledJobRepository;
    this.scheduledJobStatusRepository = scheduledJobStatusRepository;
    this.retryTemplate = buildRetryTemplate(retryConfiguration);
  }

  private static RetryTemplate buildRetryTemplate(
      ScheduledJobListenerRetryConfiguration retryConfiguration) {
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(retryConfiguration.getMaxAttempts());

    ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
    backOffPolicy.setInitialInterval(retryConfiguration.getInitialInterval());
    backOffPolicy.setMultiplier(retryConfiguration.getMultiplier());

    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setRetryPolicy(retryPolicy);
    retryTemplate.setBackOffPolicy(backOffPolicy);
    return retryTemplate;
  }

  @Override
  public String getName() {
    return "ScheduledJobListener";
  }

  /** The job is about to be executed, set the status and start date. */
  @Override
  public void jobToBeExecuted(JobExecutionContext context) {
    /* If multi quartz scheduler is not being used, the listener will
    be attached to default jobs, ignore if it's not a scheduled job */
    Optional<ScheduledJob> optScheduledJob =
        scheduledJobRepository.findByUuid(context.getJobDetail().getKey().getName());

    if (optScheduledJob.isEmpty()) return;
    ScheduledJob scheduledJob = optScheduledJob.get();

    logger.debug(
        "Preparing to execute job {} for repository {}",
        scheduledJob.getJobType().getEnum(),
        scheduledJob.getRepository().getName());

    scheduledJob.setJobStatus(
        scheduledJobStatusRepository.findByEnum(ScheduledJobStatus.IN_PROGRESS));
    scheduledJob.setStartDate(ZonedDateTime.now());
    scheduledJob.setEndDate(null);

    scheduledJobRepository.save(scheduledJob);

    logger.debug(
        "Job {} for repository {} is now in progress.",
        scheduledJob.getJobType().getEnum(),
        scheduledJob.getRepository().getName());
  }

  /** The job finished execution, if an error occurred jobException will not be null. */
  @Override
  public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
    // This method must never throw an exception. If it does, Quartz will abort the remaining
    // listener notifications - including the one that informs the DB that the job completed
    // successfully. Skipping that notification will leave the job stuck in a BLOCKED state.
    try {
      retryTemplate.execute(
          retryContext -> {
            handleJobWasExecuted(context, jobException);
            return null;
          });
    } catch (Exception e) {
      logger.error(
          "Failed to handle post execution for job {} after retries",
          context.getJobDetail().getKey().getName(),
          e);
    }
  }

  private void handleJobWasExecuted(
      JobExecutionContext context, JobExecutionException jobException) {

    Optional<ScheduledJob> optScheduledJob =
        scheduledJobRepository.findByUuid(context.getJobDetail().getKey().getName());

    if (optScheduledJob.isEmpty()) return;
    ScheduledJob scheduledJob = optScheduledJob.get();

    logger.debug(
        "Handling post execution for job {} for repository {}",
        scheduledJob.getJobType().getEnum(),
        scheduledJob.getRepository().getName());

    scheduledJob.setEndDate(ZonedDateTime.now());
    IScheduledJob jobInstance = (IScheduledJob) context.getJobInstance();

    scheduledJob.setJobStatus(
        scheduledJobStatusRepository.findByEnum(
            jobException == null ? ScheduledJobStatus.SUCCEEDED : ScheduledJobStatus.FAILED));

    // Notify the job instance of the status
    if (jobException == null) {
      jobInstance.onSuccess(context);
    } else {
      jobInstance.onFailure(context, jobException);
    }

    scheduledJobRepository.save(scheduledJob);

    logger.debug(
        "Saved results for job {} for repository {}",
        scheduledJob.getJobType().getEnum(),
        scheduledJob.getRepository().getName());
  }
}
