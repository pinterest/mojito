package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.entity.ScheduledJob;
import java.util.Date;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduledJobListener extends JobListenerSupport {

  static Logger logger = LoggerFactory.getLogger(ScheduledJobListener.class);

  private final ScheduledJobRepository scheduledJobRepository;

  ScheduledJobListener(ScheduledJobRepository scheduledJobRepository) {
    this.scheduledJobRepository = scheduledJobRepository;
  }

  @Override
  public String getName() {
    return "ScheduledJobListener";
  }

  @Override
  public void jobToBeExecuted(JobExecutionContext context) {
    JobKey jobKey = context.getJobDetail().getKey();
    ScheduledJob scheduledJob = scheduledJobRepository.findByJobKey(jobKey);

    scheduledJob.setJobStatus(ScheduledJobStatus.IN_PROGRESS);
    scheduledJob.setStartDate(new Date());

    scheduledJobRepository.save(scheduledJob);
  }

  @Override
  public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
    ScheduledJob scheduledJob =
        scheduledJobRepository.findByJobKey(context.getJobDetail().getKey());
    scheduledJob.setEndDate(new Date());

    IScheduledJob sJobInstance = (IScheduledJob) context.getJobInstance();

    if (jobException == null) {
      scheduledJob.setJobStatus(ScheduledJobStatus.SUCCEEDED);
      sJobInstance.onSuccess(context);
    } else {
      scheduledJob.setJobStatus(ScheduledJobStatus.FAILED);
      sJobInstance.onFailure(context, jobException);
    }

    scheduledJobRepository.save(scheduledJob);
  }
}
