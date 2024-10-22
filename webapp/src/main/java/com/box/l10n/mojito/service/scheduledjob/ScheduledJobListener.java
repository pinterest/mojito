package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.retry.DeadLockLoserExceptionRetryTemplate;
import java.util.Date;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener that listens for Quartz job events, this listener is attached to the 'scheduledJobs'
 * scheduler and handles setting the job status, start date and end date for pre- and post-execution
 * of the job. Scheduled jobs implement the IScheduledJob interface which allows the job to receive
 * failure and success notifications from the listener.
 *
 * @author mattwilshire
 */
public class ScheduledJobListener extends JobListenerSupport {

  static Logger logger = LoggerFactory.getLogger(ScheduledJobListener.class);

  private final ScheduledJobRepository scheduledJobRepository;
  private final ScheduledJobStatusRepository scheduledJobStatusRepository;
  private final DeadLockLoserExceptionRetryTemplate deadlockRetryTemplate;

  public ScheduledJobListener(
      ScheduledJobRepository scheduledJobRepository,
      ScheduledJobStatusRepository scheduledJobStatusRepository,
      DeadLockLoserExceptionRetryTemplate deadlockRetryTemplate) {
    this.scheduledJobRepository = scheduledJobRepository;
    this.scheduledJobStatusRepository = scheduledJobStatusRepository;
    this.deadlockRetryTemplate = deadlockRetryTemplate;
  }

  @Override
  public String getName() {
    return "ScheduledJobListener";
  }

  /**
   * The job is about to be executed, set the status and start date. Do not mark as @Transactional
   * otherwise a deadlock will occur at the method level not at the save().
   */
  @Override
  public void jobToBeExecuted(JobExecutionContext context) {
    ScheduledJob scheduledJob =
        scheduledJobRepository.findByJobKey(context.getJobDetail().getKey());

    logger.debug(
        "Preparing to execute job {} for repository {}",
        scheduledJob.getJobType().getEnum(),
        scheduledJob.getRepository().getName());

    scheduledJob.setJobStatus(
        scheduledJobStatusRepository.findByEnum(ScheduledJobStatus.IN_PROGRESS));
    scheduledJob.setStartDate(new Date());
    scheduledJob.setEndDate(null);

    // This had a deadlock due to the audited table being updated by other jobs are the same time,
    // the rev and revend columns are incremental meaning a lock is needed to increment the next
    // row.
    deadlockRetryTemplate.execute(
        c -> {
          scheduledJobRepository.save(scheduledJob);
          return null;
        });

    logger.debug(
        "Job {} for repository {} is now in progress.",
        scheduledJob.getJobType().getEnum(),
        scheduledJob.getRepository().getName());
  }

  /** The job finished execution, if an error occurred jobException will not be null */
  @Override
  public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
    ScheduledJob scheduledJob =
        scheduledJobRepository.findByJobKey(context.getJobDetail().getKey());
    logger.debug(
        "Handling post execution for job {} for repository {}",
        scheduledJob.getJobType().getEnum(),
        scheduledJob.getRepository().getName());

    scheduledJob.setEndDate(new Date());
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

    deadlockRetryTemplate.execute(
        c -> {
          scheduledJobRepository.save(scheduledJob);
          return null;
        });

    logger.debug(
        "Saved results for job {} for repository {}",
        scheduledJob.getJobType().getEnum(),
        scheduledJob.getRepository().getName());
  }
}
