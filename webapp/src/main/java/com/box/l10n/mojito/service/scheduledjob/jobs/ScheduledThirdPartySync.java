package com.box.l10n.mojito.service.scheduledjob.jobs;

import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.pagerduty.PagerDutyException;
import com.box.l10n.mojito.pagerduty.PagerDutyIntegrationService;
import com.box.l10n.mojito.pagerduty.PagerDutyPayload;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.scheduledjob.IScheduledJob;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobRepository;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJob;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobInput;
import com.box.l10n.mojito.utils.ServerConfig;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.text.StrSubstitutor;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Scheduled job that performs the third party sync for a repository off a cron schedule.
 *
 * @author mattwilshire
 */
@Component
@DisallowConcurrentExecution
public class ScheduledThirdPartySync implements IScheduledJob {

  static Logger logger = LoggerFactory.getLogger(ScheduledThirdPartySync.class);

  @Autowired private QuartzPollableTaskScheduler quartzPollableTaskScheduler;
  @Autowired private ScheduledJobRepository scheduledJobRepository;
  @Autowired private PagerDutyIntegrationService pagerDutyIntegrationService;

  @Autowired ServerConfig serverConfig;

  @Value(
      "${l10n.scheduledJobs.thirdPartySync.notifications.title:MOJITO | Third party sync failed for {repository}}")
  String notificationTitle;

  private ScheduledJob scheduledJob;
  private Long pollableTaskId;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    // Fetch the scheduled job and cast the properties
    scheduledJob = scheduledJobRepository.findByJobKey(jobExecutionContext.getJobDetail().getKey());
    ScheduledThirdPartySyncProperties scheduledJobProperties =
        (ScheduledThirdPartySyncProperties) scheduledJob.getProperties();

    logger.info(
        "Third party sync for repository {} has started.", scheduledJob.getRepository().getName());

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

    // Resolve PD incident
    pagerDutyIntegrationService
        .getDefaultPagerDutyClient()
        .ifPresent(
            pd -> {
              try {
                pd.resolveIncident(scheduledJob.getId());
              } catch (PagerDutyException e) {
                logger.error(
                    "Couldn't send resolve PagerDuty notification for successful third party sync of repository: '{}' -",
                    scheduledJob.getRepository().getName(),
                    e);
              }
            });
  }

  @Override
  public void onFailure(JobExecutionContext context, JobExecutionException jobException) {
    logger.error(
        "Third-Party Sync for repository {} has failed. Pollable Task ID: {}",
        scheduledJob.getRepository().getName(),
        pollableTaskId);

    // Trigger PD incident
    pagerDutyIntegrationService
        .getDefaultPagerDutyClient()
        .ifPresent(
            pd -> {
              String pollableTaskUrl =
                  UriComponentsBuilder.fromHttpUrl(serverConfig.getUrl())
                      .path("api/pollableTasks/" + pollableTaskId.toString())
                      .build()
                      .toUriString();

              String scheduledJobUrl =
                  UriComponentsBuilder.fromHttpUrl(serverConfig.getUrl())
                      .path("api/jobs/" + scheduledJob.getId())
                      .build()
                      .toUriString();

              String title =
                  StrSubstitutor.replace(
                      notificationTitle,
                      ImmutableMap.of("repository", scheduledJob.getRepository().getName()),
                      "{",
                      "}");

              PagerDutyPayload payload =
                  new PagerDutyPayload(
                      title,
                      serverConfig.getUrl(),
                      PagerDutyPayload.Severity.CRITICAL,
                      ImmutableMap.of(
                          "Pollable Task", pollableTaskUrl, "Scheduled Job", scheduledJobUrl));

              try {
                pd.triggerIncident(scheduledJob.getId(), payload);
              } catch (PagerDutyException e) {
                logger.error(
                    "Couldn't send PagerDuty notification for failed third party sync of repository: '{}' -",
                    scheduledJob.getRepository().getName(),
                    e);
              }
            });
  }
}
