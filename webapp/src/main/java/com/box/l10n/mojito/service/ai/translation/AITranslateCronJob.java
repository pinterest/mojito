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
import com.box.l10n.mojito.rest.ai.AIException;
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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.stereotype.Component;

/**
 * Quartz job that translates text units in batches via AI.
 *
 * @author maallen
 */
@Component
@Configuration
@ConditionalOnProperty(value = "l10n.ai.translation.enabled", havingValue = "true")
@DisallowConcurrentExecution
public class AITranslateCronJob implements Job {

  static Logger logger = LoggerFactory.getLogger(AITranslateCronJob.class);

  private static final String REPOSITORY_DEFAULT_PROMPT = "repository_default_prompt";

  @Autowired TmTextUnitPendingMTRepository tmTextUnitPendingMTRepository;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

  @Lazy @Autowired TMService tmService;

  @Autowired LLMService llmService;

  @Autowired MeterRegistry meterRegistry;

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired RepositoryLocaleAIPromptRepository repositoryLocaleAIPromptRepository;

  @Autowired AITranslationTextUnitFilterService aiTranslationTextUnitFilterService;

  @Autowired AITranslationConfiguration aiTranslationConfiguration;

  @Timed("AITranslateCronJob.translate")
  public void translate(Repository repository, TMTextUnit tmTextUnit, TmTextUnitPendingMT pendingMT)
      throws AIException {

    try {
      if (pendingMT != null) {
        if (!isExpired(pendingMT)) {
          if (aiTranslationTextUnitFilterService.isTranslatable(tmTextUnit, repository)) {
            translateLocales(tmTextUnit, repository, getLocalesForMT(repository, tmTextUnit));
            meterRegistry
                .timer("AITranslateCronJob.timeToMT", Tags.of("repository", repository.getName()))
                .record(
                    Duration.between(JSR310Migration.dateTimeNow(), pendingMT.getCreatedDate()));
          } else {
            logger.debug(
                "Text unit with name: {} should not be translated, skipping AI translation.",
                tmTextUnit.getName());
            meterRegistry.counter(
                "AITranslateCronJob.translate.notTranslatable",
                Tags.of("repository", repository.getName()));
          }
        } else {
          // If the pending MT is expired, log an error and delete it
          logger.error("Pending MT for tmTextUnitId: {} is expired", tmTextUnit.getId());
          meterRegistry.counter(
              "AITranslateCronJob.expired", Tags.of("repository", repository.getName()));
        }
      }
    } catch (Exception e) {
      logger.error("Error running job for text unit id {}", tmTextUnit.getId(), e);
      meterRegistry.counter(
          "AITranslateCronJob.error", Tags.of("repository", repository.getName()));
    }
  }

  private Set<Locale> getLocalesForMT(Repository repository, TMTextUnit tmTextUnit) {
    Set<Locale> localesWithVariants =
        tmTextUnitCurrentVariantRepository.findLocalesWithVariantByTmTextUnit_Id(
            tmTextUnit.getId());
    return repository.getRepositoryLocales().stream()
        .map(RepositoryLocale::getLocale)
        .filter(
            locale ->
                !localesWithVariants.contains(locale)
                    && !locale.equals(repository.getSourceLocale()))
        .collect(Collectors.toSet());
  }

  private void translateLocales(
      TMTextUnit tmTextUnit, Repository repository, Set<Locale> localesForMT) {

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
    localesForMT.forEach(
        targetLocale -> {
          try {
            String sourceLang = repository.getSourceLocale().getBcp47Tag().split("-")[0];
            if (aiTranslationConfiguration
                    .getRepositorySettings(repository.getName())
                    .isReuseSourceOnLanguageMatch()
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
                  "AITranslateCronJob.translate.noActivePrompt",
                  Tags.of(
                      "repository", repository.getName(), "locale", targetLocale.getBcp47Tag()));
            }
          } catch (Exception e) {
            logger.error(
                "Error translating text unit id {} for locale: {}",
                tmTextUnit.getId(),
                targetLocale.getBcp47Tag(),
                e);
            meterRegistry.counter(
                "AITranslateCronJob.translate.error",
                Tags.of("repository", repository.getName(), "locale", targetLocale.getBcp47Tag()));
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
        "AITranslateCronJob.translate.reuseSourceAsTranslation",
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
        "AITranslateCronJob.translate.success",
        Tags.of("repository", repository.getName(), "locale", targetLocale.getBcp47Tag()));
  }

  private boolean isExpired(TmTextUnitPendingMT pendingMT) {
    return pendingMT
        .getCreatedDate()
        .isBefore(
            JSR310Migration.newDateTimeEmptyCtor()
                .minus(aiTranslationConfiguration.getExpiryDuration()));
  }

  /**
   * Iterates over all pending MTs and translates them.
   *
   * <p>As each individual {@link TMTextUnit} is translated into all locales, the associated {@link
   * TmTextUnitPendingMT} is deleted.
   *
   * @param jobExecutionContext
   * @throws JobExecutionException
   */
  @Override
  @Timed("AITranslateCronJob.execute")
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    logger.info("Executing AITranslateCronJob");
    List<TmTextUnitPendingMT> pendingMTs;
    do {
      pendingMTs =
          tmTextUnitPendingMTRepository.findBatch(aiTranslationConfiguration.getBatchSize());
      logger.info("Processing {} pending MTs", pendingMTs.size());
      pendingMTs.forEach(
          pendingMT -> {
            try {
              TMTextUnit tmTextUnit = getTmTextUnit(pendingMT);
              Repository repository = tmTextUnit.getAsset().getRepository();
              translate(repository, tmTextUnit, pendingMT);
            } catch (Exception e) {
              logger.error(
                  "Error processing pending MT for text unit id: {}",
                  pendingMT.getTmTextUnitId(),
                  e);
              meterRegistry.counter("AITranslateCronJob.pendingMT.error");
            } finally {
              if (pendingMT != null) {
                logger.debug(
                    "Deleting pending MT for tmTextUnitId: {}", pendingMT.getTmTextUnitId());
                tmTextUnitPendingMTRepository.delete(pendingMT);
              }
            }
          });
    } while (!pendingMTs.isEmpty());
    logger.info("Finished executing AITranslateCronJob");
  }

  private TMTextUnit getTmTextUnit(TmTextUnitPendingMT pendingMT) {
    return tmTextUnitRepository
        .findByIdWithAssetAndRepositoryFetched(pendingMT.getTmTextUnitId())
        .orElseThrow(
            () -> new AIException("TMTextUnit not found for id: " + pendingMT.getTmTextUnitId()));
  }

  @Bean(name = "aiTranslateCron")
  public JobDetailFactoryBean jobDetailAiTranslateCronJob() {
    JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
    jobDetailFactory.setJobClass(AITranslateCronJob.class);
    jobDetailFactory.setDescription("Translate text units in batches via AI");
    jobDetailFactory.setDurability(true);
    return jobDetailFactory;
  }

  @Bean
  public CronTriggerFactoryBean triggerSlaCheckerCronJob(
      @Qualifier("aiTranslateCron") JobDetail job,
      AITranslationConfiguration aiTranslationConfiguration) {
    CronTriggerFactoryBean trigger = new CronTriggerFactoryBean();
    trigger.setJobDetail(job);
    trigger.setCronExpression(aiTranslationConfiguration.getCron());
    return trigger;
  }
}
