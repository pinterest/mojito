package com.box.l10n.mojito.service.scheduledjob.jobs;

import static com.box.l10n.mojito.slack.SlackClient.COLOR_DANGER;

import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskBlobStorage;
import com.box.l10n.mojito.service.scheduledjob.IScheduledJob;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobRepository;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJob;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobInput;
import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import com.box.l10n.mojito.slack.SlackClients;
import com.box.l10n.mojito.slack.request.Attachment;
import com.box.l10n.mojito.slack.request.Field;
import com.box.l10n.mojito.slack.request.Message;
import java.util.ArrayList;
import java.util.List;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@DisallowConcurrentExecution
public class ScheduledThirdPartySync implements IScheduledJob {

  static Logger logger = LoggerFactory.getLogger(ScheduledThirdPartySync.class);

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;
  @Autowired private ScheduledJobRepository scheduledJobRepository;
  @Autowired SlackClients slackClients;

  @Value("${l10n.scheduledJobs.slack.clientId:}")
  private String slackClientId;

  @Value("${l10n.scheduledJobs.slack.channel:}")
  private String slackChannel;

  public static int runAmount = 0;

  @Autowired PollableTaskBlobStorage pollableTaskBlobStorage;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    if (runAmount == 1) {
      return;
    }

    runAmount++;

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
      PollableFuture<Void> task =
          quartzPollableTaskScheduler.scheduleJobWithCustomTimeout(
              ThirdPartySyncJob.class, thirdPartySync, "thirdPartySync", 3600L);
      jobExecutionContext.put("PollableTaskId", task.getPollableTask().getId());
      task.get();
    } catch (Exception e) {
      throw new JobExecutionException(e);
    }
  }

  @Override
  public void onSuccess(JobExecutionContext context) {
    ScheduledJob job = scheduledJobRepository.findByJobKey(context.getJobDetail().getKey());
    logger.info("Third-Party Sync succeeded for repository {}.", job.getRepository().getName());
  }

  @Override
  public void onFailure(JobExecutionContext context, JobExecutionException jobException) {
    Long polladbleTaskId = (Long) context.get("PollableTaskId");
    ScheduledJob job = scheduledJobRepository.findByJobKey(context.getJobDetail().getKey());
    ScheduledThirdPartySyncProperties properties =
        (ScheduledThirdPartySyncProperties) job.getProperties();

    logger.error(
        "Third-Party Sync for repository {} has failed. Pollable Task ID: {}",
        job.getRepository().getName(),
        polladbleTaskId);

    // TODO: Notifications
    if (slackClientId.isEmpty() || slackChannel.isEmpty()) return;
    Message warning = buildSlackMessage(job, properties, jobException, polladbleTaskId);

    SlackClient slackClient = slackClients.getById(slackClientId);
    try {
      slackClient.sendInstantMessage(warning);
    } catch (SlackClientException e) {
      logger.error(
          "Slack client failed to send warning message with id '{}' and channel '{}'",
          slackClientId,
          slackChannel,
          e);
    }
  }

  private Message buildSlackMessage(
      ScheduledJob job,
      ScheduledThirdPartySyncProperties properties,
      JobExecutionException jobException,
      Long pollableId) {
    Message warning = new Message();
    warning.setText("");
    warning.setChannel(slackChannel);

    Attachment attachment = new Attachment();
    attachment.setTitle("Mojito | Third Party Sync failed for " + job.getRepository().getName());
    attachment.setText("This scheduled job has failed, visit /api/pollableTasks/" + pollableId);

    List<Field> fields = new ArrayList<>();
    fields.add(createField("Repository", job.getRepository().getName()));
    fields.add(createField("Third Party ID", properties.getThirdPartyProjectId()));

    fields.add(createField("Error", jobException.getMessage()));

    attachment.setFields(fields);
    attachment.setColor(COLOR_DANGER);
    warning.getAttachments().add(attachment);
    return warning;
  }

  private Field createField(String title, String value) {
    Field field = new Field();
    field.setTitle(title);
    field.setValue(value);
    return field;
  }
}
