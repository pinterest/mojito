package com.box.l10n.mojito.service.ai.translation;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PromptType;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TmTextUnitPendingMT;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.rest.ai.AIException;
import com.box.l10n.mojito.service.ai.RepositoryLocaleAIPromptRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import java.util.List;
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

  @Autowired TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

  @Autowired RepositoryRepository repositoryRepository;

  @Value("${l10n.ai.translation.quartz.schedulerName:" + DEFAULT_SCHEDULER_NAME + "}")
  String quartzSchedulerName;

  public void scheduleAITranslationJob(AITranslateJobInput input) {
    if (repositoryLocaleAIPromptRepository.findCountOfActiveRepositoryPromptsByType(
            input.getRepositoryId(), PromptType.TRANSLATION.toString())
        > 0) {
      logger.debug("Scheduling AI translation for tmTextUnitId: {}", input.getTmTextUnitId());
      if (input.getLocales() == null) {
        // If no locales provided, get locales without existing variant
        input.setLocales(getLocalesWithoutExistingVariant(input));
      }
      persistToPendingMTTable(input);
      scheduleQuartzJob(input);
    }
  }

  private List<String> getLocalesWithoutExistingVariant(AITranslateJobInput input) {
    logger.debug(
        "Get locales without existing variant for tmTextUnitId: {}", input.getTmTextUnitId());
    List<Locale> localesWithExistingVariant =
        tmTextUnitCurrentVariantRepository.findByTmTextUnit_Id(input.getTmTextUnitId()).stream()
            .map(TMTextUnitCurrentVariant::getLocale)
            .toList();
    Repository repository =
        repositoryRepository
            .findById(input.getRepositoryId())
            .orElseThrow(
                () -> new AIException("Repository not found for id: " + input.getRepositoryId()));

    return repository.getRepositoryLocales().stream()
        .filter(
            repositoryLocale -> repositoryLocale.getParentLocale() != null) // remove source locale
        .filter(
            repositoryLocale -> !localesWithExistingVariant.contains(repositoryLocale.getLocale()))
        .map(repositoryLocale -> repositoryLocale.getLocale().getBcp47Tag())
        .toList();
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
