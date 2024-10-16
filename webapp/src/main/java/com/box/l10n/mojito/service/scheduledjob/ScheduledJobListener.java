package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.entity.ScheduledJob;
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

  public ScheduledJobListener(
      ScheduledJobRepository scheduledJobRepository,
      ScheduledJobStatusRepository scheduledJobStatusRepository) {
    this.scheduledJobRepository = scheduledJobRepository;
    this.scheduledJobStatusRepository = scheduledJobStatusRepository;
  }

  @Override
  public String getName() {
    return "ScheduledJobListener";
  }

  @Override
  public void jobToBeExecuted(JobExecutionContext context) {
    ScheduledJob scheduledJob =
        scheduledJobRepository.findByJobKey(context.getJobDetail().getKey());

    scheduledJob.setJobStatus(
        scheduledJobStatusRepository.findByEnum(ScheduledJobStatus.IN_PROGRESS));
    scheduledJob.setStartDate(new Date());
    scheduledJob.setEndDate(null);

    scheduledJobRepository.save(scheduledJob);
  }

  @Override
  public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
    ScheduledJob scheduledJob =
        scheduledJobRepository.findByJobKey(context.getJobDetail().getKey());
    scheduledJob.setEndDate(new Date());

    IScheduledJob jobInstance = (IScheduledJob) context.getJobInstance();

    if (jobException == null) {
      scheduledJob.setJobStatus(
          scheduledJobStatusRepository.findByEnum(ScheduledJobStatus.SUCCEEDED));
      jobInstance.onSuccess(context);
    } else {
      scheduledJob.setJobStatus(scheduledJobStatusRepository.findByEnum(ScheduledJobStatus.FAILED));
      jobInstance.onFailure(context, jobException);
    }

    scheduledJobRepository.save(scheduledJob);
  }
}
