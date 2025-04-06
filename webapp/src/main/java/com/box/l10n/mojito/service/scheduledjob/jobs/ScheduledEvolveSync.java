package com.box.l10n.mojito.service.scheduledjob.jobs;

import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.pagerduty.PagerDutyException;
import com.box.l10n.mojito.pagerduty.PagerDutyIntegrationService;
import com.box.l10n.mojito.pagerduty.PagerDutyPayload;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.evolve.EvolveConfigurationProperties;
import com.box.l10n.mojito.service.evolve.EvolveSyncJob;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.scheduledjob.IScheduledJob;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobRepository;
import com.box.l10n.mojito.utils.ServerConfig;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.text.StrSubstitutor;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@DisallowConcurrentExecution
public class ScheduledEvolveSync implements IScheduledJob {
  static Logger logger = LoggerFactory.getLogger(ScheduledThirdPartySync.class);

  @Autowired private PollableTaskService pollableTaskService;

  @Autowired private QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Autowired private ScheduledJobRepository scheduledJobRepository;

  @Autowired private PagerDutyIntegrationService pagerDutyIntegrationService;

  @Autowired private ServerConfig serverConfig;

  @Autowired private EvolveConfigurationProperties evolveConfigurationProperties;

  private ScheduledJob scheduledJob;

  private Long pollableTaskId;

  @Override
  public void onSuccess(JobExecutionContext context) {
    pagerDutyIntegrationService
        .getDefaultPagerDutyClient()
        .ifPresent(
            pd -> {
              try {
                pd.resolveIncident(scheduledJob.getUuid());
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
                      .path("api/jobs/" + scheduledJob.getUuid())
                      .build()
                      .toUriString();

              String title =
                  StrSubstitutor.replace(
                      this.evolveConfigurationProperties.getJobFailureNotificationTitle(),
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
                pd.triggerIncident(scheduledJob.getUuid(), payload);
              } catch (PagerDutyException e) {
                logger.error(
                    "Couldn't send PagerDuty notification for failed Evolve sync, Pollable Task URL: '{}', Scheduled Job: '{}'",
                    pollableTaskUrl,
                    scheduledJobUrl,
                    e);
              }
            });
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    scheduledJob = scheduledJobRepository.findByJobKey(context.getJobDetail().getKey());
    try {
      PollableFuture<Void> task =
          quartzPollableTaskScheduler.scheduleJobWithCustomTimeout(
              EvolveSyncJob.class,
              null,
              "evolveSync",
              evolveConfigurationProperties.getTaskTimeout());
      pollableTaskId = task.getPollableTask().getId();
      // Wait for sync to complete
      pollableTaskService.waitForPollableTask(
          pollableTaskId, evolveConfigurationProperties.getTaskTimeout() * 1000, 10000);
    } catch (Exception e) {
      throw new JobExecutionException(e);
    }
  }
}
