package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.retry.DeadLockLoserExceptionRetryTemplate;
import jakarta.transaction.Transactional;
import java.util.Date;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  @Override
  @Transactional
  public void jobToBeExecuted(JobExecutionContext context) {
    ScheduledJob scheduledJob =
        scheduledJobRepository.findByJobKey(context.getJobDetail().getKey());

    scheduledJob.setJobStatus(
        scheduledJobStatusRepository.findByEnum(ScheduledJobStatus.IN_PROGRESS));
    scheduledJob.setStartDate(new Date());
    scheduledJob.setEndDate(null);

    // TODO: Try catch, PD NOTIFICATION
    deadlockRetryTemplate.execute(
        c -> {
          scheduledJobRepository.save(scheduledJob);
          return null;
        });
  }

  @Override
  @Transactional
  public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
    ScheduledJob scheduledJob =
        scheduledJobRepository.findByJobKey(context.getJobDetail().getKey());
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

    // TODO: Try catch, PD NOTIFICATION
    deadlockRetryTemplate.execute(
        c -> {
          scheduledJobRepository.save(scheduledJob);
          return null;
        });
  }
}
