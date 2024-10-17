package com.box.l10n.mojito.service.scheduledjob.jobs;

import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.scheduledjob.IScheduledJob;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobRepository;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJob;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobInput;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@DisallowConcurrentExecution
public class ScheduledThirdPartySync implements IScheduledJob {

  static Logger logger = LoggerFactory.getLogger(ScheduledThirdPartySync.class);

  @Autowired private QuartzPollableTaskScheduler quartzPollableTaskScheduler;
  @Autowired private ScheduledJobRepository scheduledJobRepository;

  public static int runAmount = 1;

  private ScheduledJob scheduledJob;
  private ScheduledThirdPartySyncProperties scheduledJobProperties;
  private Long pollableTaskId;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    // Fetch the scheduled job and cast the properties
    scheduledJob = scheduledJobRepository.findByJobKey(jobExecutionContext.getJobDetail().getKey());
    scheduledJobProperties = (ScheduledThirdPartySyncProperties) scheduledJob.getProperties();

    logger.info(
        "Third party sync for repository {} has started.", scheduledJob.getRepository().getName());

    if (runAmount == 1) {
      return;
    }

    runAmount++;

    // Create ThirdPartySyncInput from scheduled job and properties
    ThirdPartySyncJobInput thirdPartySyncJobInput =
        new ThirdPartySyncJobInput(scheduledJob, scheduledJobProperties);

    try {
      PollableFuture<Void> task =
          quartzPollableTaskScheduler.scheduleJobWithCustomTimeout(
              ThirdPartySyncJob.class, thirdPartySyncJobInput, "thirdPartySync", 3600L);
      pollableTaskId = task.getPollableTask().getId();
      // Wait for sync to complete
      task.get();
    } catch (Exception e) {
      throw new JobExecutionException(e);
    }
  }

  @Override
  public void onSuccess(JobExecutionContext context) {
    logger.info(
        "Third-Party Sync succeeded for repository {}.", scheduledJob.getRepository().getName());
  }

  @Override
  public void onFailure(JobExecutionContext context, JobExecutionException jobException) {
    logger.error(
        "Third-Party Sync for repository {} has failed. Pollable Task ID: {}",
        scheduledJob.getRepository().getName(),
        pollableTaskId);
  }
}
