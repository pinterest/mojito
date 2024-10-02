package com.box.l10n.mojito.service.ai.translation;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PromptType;
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
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "l10n.ai.translation.enabled", havingValue = "true")
public class AITranslateJob extends QuartzPollableJob<AITranslateJobInput, Void> {

  static Logger logger = LoggerFactory.getLogger(AITranslateJob.class);

  private static final String REPOSITORY_DEFAULT_PROMPT = "repository_default_prompt";

  @Autowired TmTextUnitPendingMTRepository tmTextUnitPendingMTRepository;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

  @Autowired TMService tmService;

  @Autowired LLMService llmService;

  @Autowired MeterRegistry meterRegistry;

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired RepositoryLocaleAIPromptRepository repositoryLocaleAIPromptRepository;

  @Autowired AITranslationTextUnitFilterService aiTranslationTextUnitFilterService;

  /**
   * Duration after which a pending MT is considered expired and will not be processed in AI
   * translation (as it will be eligible for third party syncs once the entity is older than the
   * expiry period).
   *
   * <p>If the pending MT is expired, it will be deleted which will remove it from AI translation
   * flow.
   */
  @Value("${l10n.ai.translation.pendingMT.expiryDuration:PT3H}")
  Duration expiryDuration;

  @Value("${l10n.ai.translation.reuseSourceOnLanguageMatch:false}")
  boolean isReuseSourceOnLanguageMatch;

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
                    if (aiTranslationTextUnitFilterService.isTranslatable(tmTextUnit, repository)) {
                      translateLocales(tmTextUnit, repository, input.getLocales());
                    } else {
                      logger.debug(
                          "Text unit with name: {} should not be translated, skipping AI translation.",
                          tmTextUnit.getName());
                      meterRegistry.counter(
                          "AITranslateJob.translate.notTranslatable",
                          Tags.of("repository", repository.getName()));
                    }
                  });
          meterRegistry
              .timer("AITranslateJob.timeToMT", Tags.of("repository", repository.getName()))
              .record(Duration.between(JSR310Migration.dateTimeNow(), pendingMT.getCreatedDate()));
        } else {
          // If the pending MT is expired, log an error and delete it
          logger.error("Pending MT for tmTextUnitId: {} is expired", input.getTmTextUnitId());
          meterRegistry.counter(
              "AITranslateJob.expired", Tags.of("repository", repository.getName()));
        }
      } else {
        logger.warn("No pending MT found for tmTextUnitId: {}", input.getTmTextUnitId());
        meterRegistry.counter(
            "AITranslateJob.noPendingMTFound", Tags.of("repository", repository.getName()));
      }
    } catch (Exception e) {
      logger.error("Error running job for text unit id {}", input.getTmTextUnitId(), e);
      meterRegistry.counter(
          "AITranslateJob.error", Tags.of("repository", input.getRepositoryId().toString()));
    } finally {
      TmTextUnitPendingMT pendingMT =
          tmTextUnitPendingMTRepository.findByTmTextUnitId(input.getTmTextUnitId());
      if (pendingMT != null) {
        logger.debug("Deleting pending MT for tmTextUnitId: {}", input.getTmTextUnitId());
        tmTextUnitPendingMTRepository.delete(pendingMT);
      }
    }
    return null;
  }

  private void translateLocales(
      TMTextUnit tmTextUnit, Repository repository, List<String> bcp47Tags) {

    Map<String, RepositoryLocaleAIPrompt> repositoryLocaleAIPrompts =
        repositoryLocaleAIPromptRepository
            .getActivePromptsByRepositoryAndPromptType(
                repository.getId(), PromptType.TRANSLATION.toString())
            .stream()
            .collect(
                Collectors.toMap(
                    rlap ->
                        rlap.getLocale() != null
                            ? rlap.getLocale().getBcp47Tag()
                            : REPOSITORY_DEFAULT_PROMPT,
                    Function.identity()));
    repository.getRepositoryLocales().stream()
        .map(RepositoryLocale::getLocale)
        .filter(locale -> bcp47Tags.contains(locale.getBcp47Tag()))
        .forEach(
            targetLocale -> {
              try {
                String sourceLang = repository.getSourceLocale().getBcp47Tag().split("-")[0];
                if (isReuseSourceOnLanguageMatch
                    && targetLocale.getBcp47Tag().startsWith(sourceLang)) {
                  reuseSourceStringAsTranslation(tmTextUnit, repository, targetLocale, sourceLang);
                  return;
                }
                // Get the prompt override for this locale if it exists, otherwise use the
                // repository default
                RepositoryLocaleAIPrompt repositoryLocaleAIPrompt =
                    repositoryLocaleAIPrompts.get(targetLocale.getBcp47Tag()) != null
                        ? repositoryLocaleAIPrompts.get(targetLocale.getBcp47Tag())
                        : repositoryLocaleAIPrompts.get(REPOSITORY_DEFAULT_PROMPT);
                if (repositoryLocaleAIPrompt != null && !repositoryLocaleAIPrompt.isDisabled()) {
                  logger.info(
                      "Translating text unit id {} for locale: {} using prompt: {}",
                      tmTextUnit.getId(),
                      targetLocale.getBcp47Tag(),
                      repositoryLocaleAIPrompt.getAiPrompt().getId());
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

  private void reuseSourceStringAsTranslation(
      TMTextUnit tmTextUnit, Repository repository, Locale targetLocale, String sourceLang) {
    logger.debug(
        "Target language {} matches source language {}, re-using source string as translation.",
        targetLocale.getBcp47Tag(),
        sourceLang);
    meterRegistry.counter(
        "AITranslateJob.translate.reuseSourceAsTranslation",
        Tags.of("repository", repository.getName(), "locale", targetLocale.getBcp47Tag()));

    tmService.addTMTextUnitVariant(
        tmTextUnit.getId(),
        targetLocale.getId(),
        tmTextUnit.getContent(),
        tmTextUnit.getComment(),
        TMTextUnitVariant.Status.MT_TRANSLATED,
        false,
        JSR310Migration.dateTimeNow());
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
