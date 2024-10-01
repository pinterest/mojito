package com.box.l10n.mojito.service.scheduledjob.jobs;

import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.scheduledjob.IScheduledJob;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobRepository;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJob;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobInput;
import java.util.ArrayList;
import java.util.List;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ScheduledThirdPartySync implements IScheduledJob {

  static Logger logger = LoggerFactory.getLogger(ScheduledThirdPartySync.class);

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;
  @Autowired private ScheduledJobRepository scheduledJobRepository;

  @SuppressWarnings("unchecked")
  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    ScheduledJob job =
        scheduledJobRepository.findByJobKey(jobExecutionContext.getJobDetail().getKey());
    ScheduledThirdPartySyncProperties properties =
        (ScheduledThirdPartySyncProperties) job.getProperties();

    logger.info("Third party sync for repository {} has started.", job.getRepository().getName());

    List<String> options = new ArrayList<>();
    ThirdPartySyncJobInput thirdPartySync = new ThirdPartySyncJobInput();
    thirdPartySync.setRepositoryId(job.getRepository().getId());
    thirdPartySync.setThirdPartyProjectId(properties.getThirdPartyProjectId());
    thirdPartySync.setActions(properties.getActions());
    thirdPartySync.setPluralSeparator(properties.getPluralSeparator());
    thirdPartySync.setLocaleMapping(properties.getLocaleMapping());

    thirdPartySync.setSkipTextUnitsWithPattern(
        properties.getSkipTextUnitsWithPattern().isEmpty()
            ? null
            : properties.getSkipTextUnitsWithPattern());
    thirdPartySync.setSkipAssetsWithPathPattern(
        properties.getSkipAssetsWithPathPattern().isEmpty()
            ? null
            : properties.getSkipAssetsWithPathPattern());
    thirdPartySync.setIncludeTextUnitsWithPattern(
        properties.getIncludeTextUnitsWithPattern().isEmpty()
            ? null
            : properties.getIncludeTextUnitsWithPattern());

    thirdPartySync.setOptions(options);

    try {
      quartzPollableTaskScheduler
          .scheduleJobWithCustomTimeout(
              ThirdPartySyncJob.class, thirdPartySync, "thirdPartySync", 3600L)
          .get();
    } catch (Exception e) {
      throw new JobExecutionException(e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void onSuccess(JobExecutionContext context) {
    ScheduledJob job = scheduledJobRepository.findByJobKey(context.getJobDetail().getKey());
    logger.info("Third-Party Sync succeeded for repository {}.", job.getRepository().getName());
  }

  @SuppressWarnings("unchecked")
  @Override
  public void onFailure(JobExecutionContext context, JobExecutionException jobException) {
    ScheduledJob job = scheduledJobRepository.findByJobKey(context.getJobDetail().getKey());
    logger.error(
        "Third-Party Sync for repository {} has failed:",
        job.getRepository().getName(),
        jobException);
    // TODO: Notifications
  }
}
