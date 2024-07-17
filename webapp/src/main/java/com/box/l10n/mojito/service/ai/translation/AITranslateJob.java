package com.box.l10n.mojito.service.ai.translation;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.RepositoryLocaleAIPrompt;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TmTextUnitPendingMT;
import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.service.ai.LLMService;
import com.box.l10n.mojito.service.ai.RepositoryLocaleAIPromptRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AITranslateJob extends QuartzPollableJob<AITranslateJobInput, Void> {

  static Logger logger = LoggerFactory.getLogger(AITranslateJob.class);

  @Autowired TmTextUnitPendingMTRepository tmTextUnitPendingMTRepository;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired TMService tmService;

  @Autowired LLMService llmService;

  @Autowired MeterRegistry meterRegistry;

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired RepositoryLocaleAIPromptRepository repositoryLocaleAIPromptRepository;

  @Autowired AITranslationTextUnitFilterService aiTranslationTextUnitFilterService;

  @Value("${l10n.ai.translation.pendingMT.expiryDuration:PT3H}")
  Duration expiryDuration;

  @Override
  @Timed("AITranslateJob.call")
  public Void call(AITranslateJobInput input) throws Exception {

    try {
      Repository repository =
          repositoryRepository
              .findById(input.getRepositoryId())
              .orElseThrow(
                  () ->
                      new AITranslateJobException(
                          "Repository with id " + input.getRepositoryId() + " not found."));

      TmTextUnitPendingMT pendingMT =
          tmTextUnitPendingMTRepository.findByTmTextUnitId(input.getTmTextUnitId());

      if (pendingMT != null) {
        if (!isExpired(pendingMT)) {
          tmTextUnitRepository
              .findById(input.getTmTextUnitId())
              .ifPresent(
                  tmTextUnit -> {
                    if (aiTranslationTextUnitFilterService.isTranslatable(tmTextUnit)) {
                      translateLocales(input, tmTextUnit, repository);
                    } else {
                      logger.debug(
                          "Text unit with name: {} should not be translated, skipping AI translation.",
                          tmTextUnit.getName());
                      meterRegistry.counter(
                          "AITranslateJob.translate.notTranslatable",
                          Tags.of("repository", repository.getName()));
                    }
                  });
        } else {
          // If the pending MT is expired, log an error and delete it
          logger.error("Pending MT for tmTextUnitId: {} is expired", input.getTmTextUnitId());
          meterRegistry.counter(
              "AITranslateJob.expired", Tags.of("repository", repository.getName()));
        }
        tmTextUnitPendingMTRepository.delete(pendingMT);
      } else {
        logger.warn("No pending MT found for tmTextUnitId: {}", input.getTmTextUnitId());
        meterRegistry.counter(
            "AITranslateJob.noPendingMTFound", Tags.of("repository", repository.getName()));
      }

    } catch (Exception e) {
      logger.error("Error running job for text unit id {}", input.getTmTextUnitId(), e);
      meterRegistry.counter(
          "AITranslateJob.error", Tags.of("repository", input.getRepositoryId().toString()));
    }
    return null;
  }

  private void translateLocales(
      AITranslateJobInput input, TMTextUnit tmTextUnit, Repository repository) {
    Map<Locale, RepositoryLocaleAIPrompt> repositoryLocaleAIPrompts =
        repositoryLocaleAIPromptRepository
            .getActiveTranslationPromptsByRepository(input.getRepositoryId())
            .stream()
            .collect(Collectors.toMap(RepositoryLocaleAIPrompt::getLocale, Function.identity()));
    repository.getRepositoryLocales().stream()
        .filter(rl -> rl.getParentLocale() != null)
        .map(RepositoryLocale::getLocale)
        .forEach(
            targetLocale -> {
              try {
                // Get the prompt override for this locale if it exists, otherwise use the
                // repository default
                RepositoryLocaleAIPrompt repositoryLocaleAIPrompt =
                    repositoryLocaleAIPrompts.get(targetLocale) != null
                        ? repositoryLocaleAIPrompts.get(targetLocale)
                        : repositoryLocaleAIPrompts.get(null);
                if (repositoryLocaleAIPrompt != null && !repositoryLocaleAIPrompt.isDisabled()) {
                  executeTranslationPrompt(
                      tmTextUnit, repository, targetLocale, repositoryLocaleAIPrompt);
                } else {
                  logger.debug(
                      "No active translation prompt found for locale: {}, skipping AI translation.",
                      targetLocale.getBcp47Tag());
                  meterRegistry.counter(
                      "AITranslateJob.translate.noActivePrompt",
                      Tags.of(
                          "repository",
                          repository.getName(),
                          "locale",
                          targetLocale.getBcp47Tag()));
                }
              } catch (Exception e) {
                logger.error(
                    "Error translating text unit id {} for locale: {}",
                    tmTextUnit.getId(),
                    targetLocale.getBcp47Tag(),
                    e);
                meterRegistry.counter(
                    "AITranslateJob.translate.error",
                    Tags.of(
                        "repository", repository.getName(), "locale", targetLocale.getBcp47Tag()));
              }
            });
  }

  private void executeTranslationPrompt(
      TMTextUnit tmTextUnit,
      Repository repository,
      Locale targetLocale,
      RepositoryLocaleAIPrompt repositoryLocaleAIPrompt) {
    String translation =
        llmService.translate(
            tmTextUnit,
            repository.getSourceLocale().getBcp47Tag(),
            targetLocale.getBcp47Tag(),
            repositoryLocaleAIPrompt.getAiPrompt());
    tmService.addTMTextUnitVariant(
        tmTextUnit.getId(),
        targetLocale.getId(),
        translation,
        tmTextUnit.getComment(),
        TMTextUnitVariant.Status.MT_TRANSLATED,
        false,
        JSR310Migration.dateTimeNow());
    meterRegistry.counter(
        "AITranslateJob.translate.success",
        Tags.of("repository", repository.getName(), "locale", targetLocale.getBcp47Tag()));
  }

  private boolean isExpired(TmTextUnitPendingMT pendingMT) {
    return pendingMT
        .getCreatedDate()
        .isBefore(JSR310Migration.newDateTimeEmptyCtor().minus(expiryDuration));
  }
}
