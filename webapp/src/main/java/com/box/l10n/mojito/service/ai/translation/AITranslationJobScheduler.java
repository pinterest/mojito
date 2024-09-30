package com.box.l10n.mojito.service.ai.translation;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.TmTextUnitPendingMT;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.ai.RepositoryLocaleAIPromptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "l10n.ai.translation.enabled", havingValue = "true")
public class AITranslationJobScheduler {

  private static Logger logger = LoggerFactory.getLogger(AITranslationJobScheduler.class);

  @Autowired RepositoryLocaleAIPromptRepository repositoryLocaleAIPromptRepository;

  @Autowired TmTextUnitPendingMTRepository tmTextUnitPendingMTRepository;

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Value("${l10n.ai.translation.quartz.schedulerName:" + DEFAULT_SCHEDULER_NAME + "}")
  String quartzSchedulerName;

  public void scheduleAITranslationJob(AITranslateJobInput input) {
    if (repositoryLocaleAIPromptRepository.findCountOfActiveRepositoryTranslationPrompts(
            input.getRepositoryId())
        > 0) {
      logger.debug("Scheduling AI translation for tmTextUnitId: {}", input.getTmTextUnitId());
      persistToPendingMTTable(input);
      scheduleQuartzJob(input);
    }
  }

  private void scheduleQuartzJob(AITranslateJobInput input) {
    QuartzJobInfo<AITranslateJobInput, Void> quartzJobInfo =
        QuartzJobInfo.newBuilder(AITranslateJob.class)
            .withInlineInput(false)
            .withUniqueId("AITranslateJob_" + input.getTmTextUnitId())
            .withInput(input)
            .withScheduler(quartzSchedulerName)
            .build();

    quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
  }

  private void persistToPendingMTTable(AITranslateJobInput input) {
    TmTextUnitPendingMT tmTextUnitPendingMT = new TmTextUnitPendingMT();
    tmTextUnitPendingMT.setTmTextUnitId(input.getTmTextUnitId());
    tmTextUnitPendingMT.setCreatedDate(JSR310Migration.newDateTimeEmptyCtor());
    tmTextUnitPendingMTRepository.save(tmTextUnitPendingMT);
  }
}
