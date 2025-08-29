package com.box.l10n.mojito.service.ai.translation;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PromptType;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.RepositoryLocaleAIPrompt;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.entity.TmTextUnitPendingMT;
import com.box.l10n.mojito.rest.ai.AIException;
import com.box.l10n.mojito.service.ai.LLMService;
import com.box.l10n.mojito.service.ai.RepositoryLocaleAIPromptRepository;
import com.box.l10n.mojito.service.ratelimiter.SlidingWindowRateLimiter;
import com.box.l10n.mojito.service.thirdparty.smartling.glossary.GlossaryCacheService;
import com.box.l10n.mojito.service.thirdparty.smartling.glossary.GlossaryTerm;
import com.box.l10n.mojito.service.tm.AddTMTextUnitCurrentVariantResult;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantCommentService;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.stereotype.Component;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Quartz job that translates text units in batches via AI.
 *
 * @author maallen
 */
@Component
@Configuration
@ConditionalOnProperty(value = "l10n.ai.translation.enabled", havingValue = "true")
public class AITranslateCronJob implements Job {

  static Logger logger = LoggerFactory.getLogger(AITranslateCronJob.class);

  private static final String REPOSITORY_DEFAULT_PROMPT = "repository_default_prompt";

  @Autowired(required = false)
  @Qualifier("aiTranslationRateLimiter")
  SlidingWindowRateLimiter rateLimiter;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired TMTextUnitVariantRepository tmTextUnitVariantRepository;

  @Autowired LLMService llmService;

  @Autowired MeterRegistry meterRegistry;

  @Autowired RepositoryLocaleAIPromptRepository repositoryLocaleAIPromptRepository;

  @Autowired AITranslationTextUnitFilterService aiTranslationTextUnitFilterService;

  @Autowired AITranslationConfiguration aiTranslationConfiguration;

  @Autowired AITranslationService aiTranslationService;

  @Autowired TMTextUnitVariantCommentService tmTextUnitVariantCommentService;

  @Lazy @Autowired TMService tmService;

  @Autowired TmTextUnitPendingMTRepository tmTextUnitPendingMTRepository;

  @Autowired TextUnitSearcher textUnitSearcher;

  @Autowired JdbcTemplate jdbcTemplate;

  @Autowired(required = false)
  GlossaryCacheService glossaryCacheService;

  @Value("${l10n.ai.translation.job.threads:1}")
  int threads;

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
                    JSR310Migration.getMillis(JSR310Migration.dateTimeNow())
                        - JSR310Migration.getMillis(pendingMT.getCreatedDate()),
                    TimeUnit.MILLISECONDS);
          } else {
            logger.debug(
                "Text unit with name: {} should not be translated, skipping AI translation.",
                tmTextUnit.getName());
            meterRegistry
                .counter(
                    "AITranslateCronJob.translate.notTranslatable",
                    Tags.of("repository", repository.getName()))
                .increment();
          }
        } else {
          // If the pending MT is expired, log an error and delete it
          logger.error("Pending MT for tmTextUnitId: {} is expired", tmTextUnit.getId());
          meterRegistry
              .counter("AITranslateCronJob.expired", Tags.of("repository", repository.getName()))
              .increment();
        }
      }
    } catch (AITranslateTimeoutException e) {
      meterRegistry
          .counter(
              "AITranslateCronJob.translate.timeout", Tags.of("repository", repository.getName()))
          .increment();
      throw e;
    } catch (Exception e) {
      logger.error("Error running job for text unit id {}", tmTextUnit.getId(), e);
      meterRegistry
          .counter("AITranslateCronJob.error", Tags.of("repository", repository.getName()))
          .increment();
    }
  }

  private Set<Locale> getLocalesForMT(Repository repository, TMTextUnit tmTextUnit) {
    Set<Locale> localesWithVariants =
        tmTextUnitVariantRepository.findLocalesWithVariantByTmTextUnit_Id(tmTextUnit.getId());
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
    long startTime = System.currentTimeMillis();

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
            if (aiTranslationConfiguration.getRepositorySettings(repository.getName()) != null
                && aiTranslationConfiguration
                    .getRepositorySettings(repository.getName())
                    .isReuseSourceOnLanguageMatch()
                && targetLocale.getBcp47Tag().startsWith(sourceLang)) {
              addAITranslationCurrentVariant(
                  reuseSourceStringAsTranslation(tmTextUnit, repository, targetLocale, sourceLang));
              return;
            }
            // Get the prompt override for this locale if it exists, otherwise use the
            // repository default
            RepositoryLocaleAIPrompt repositoryLocaleAIPrompt =
                repositoryLocaleAIPrompts.get(targetLocale.getBcp47Tag()) != null
                    ? repositoryLocaleAIPrompts.get(targetLocale.getBcp47Tag())
                    : repositoryLocaleAIPrompts.get(REPOSITORY_DEFAULT_PROMPT);
            if (repositoryLocaleAIPrompt != null && !repositoryLocaleAIPrompt.isDisabled()) {
              // If rate limiting is configured, wait and check if we can proceed
              if (rateLimiter != null) {
                // Wait for rate limit before executing the translation
                waitForRateLimit(startTime);
              } else {
                // No rate limiter, still check if the thread should time out
                if (System.currentTimeMillis() - startTime
                    >= aiTranslationConfiguration.getTimeout().toMillis()) {
                  throw new AITranslateTimeoutException();
                }
              }
              logger.info(
                  "Translating text unit id {} for locale: {} using prompt: {}",
                  tmTextUnit.getId(),
                  targetLocale.getBcp47Tag(),
                  repositoryLocaleAIPrompt.getAiPrompt().getId());
              if (aiTranslationConfiguration.getRepositorySettings(repository.getName()) != null
                  && aiTranslationConfiguration
                      .getRepositorySettings(repository.getName())
                      .isInjectGlossaryMatches()) {
                addAITranslationCurrentVariant(
                    executeGlossaryMatchTranslationPrompt(
                        tmTextUnit,
                        repository,
                        targetLocale,
                        repositoryLocaleAIPrompt,
                        glossaryCacheService
                            .getGlossaryTermsInText(tmTextUnit.getContent())
                            .stream()
                            .filter(
                                term ->
                                    term.getLocaleTranslation(targetLocale.getBcp47Tag()) != null
                                        || term.isDoNotTranslate())
                            .collect(Collectors.toList())));
              } else {
                addAITranslationCurrentVariant(
                    executeTranslationPrompt(
                        tmTextUnit, repository, targetLocale, repositoryLocaleAIPrompt));
              }

            } else {
              if (repositoryLocaleAIPrompt != null && repositoryLocaleAIPrompt.isDisabled()) {
                logger.debug(
                    "AI translation is disabled for locale "
                        + repositoryLocaleAIPrompt.getLocale().getBcp47Tag()
                        + " in repository "
                        + repository.getName()
                        + ", skipping AI translation.");
              } else {
                logger.debug(
                    "No active translation prompt found for locale: {}, skipping AI translation.",
                    targetLocale.getBcp47Tag());
                meterRegistry
                    .counter(
                        "AITranslateCronJob.translate.noActivePrompt",
                        Tags.of(
                            "repository",
                            repository.getName(),
                            "locale",
                            targetLocale.getBcp47Tag()))
                    .increment();
              }
            }
          } catch (AITranslateTimeoutException e) {
            throw e;
          } catch (Exception e) {
            logger.error(
                "Error translating text unit id {} for locale: {}",
                tmTextUnit.getId(),
                targetLocale.getBcp47Tag(),
                e);
            meterRegistry
                .counter(
                    "AITranslateCronJob.translate.error",
                    Tags.of(
                        "repository", repository.getName(), "locale", targetLocale.getBcp47Tag()))
                .increment();
          }
        });
  }

  private void addAITranslationCurrentVariant(AITranslation aiTranslation) {
    AddTMTextUnitCurrentVariantResult result =
        tmService.addTMTextUnitCurrentVariantWithResult(
            aiTranslation.getTmTextUnit().getId(),
            aiTranslation.getLocaleId(),
            aiTranslation.getTranslation(),
            aiTranslation.getComment(),
            aiTranslation.getStatus(),
            aiTranslation.isIncludedInLocalizedFile(),
            aiTranslation.getCreatedDate());
    tmTextUnitVariantCommentService.addComment(
        result.getTmTextUnitCurrentVariant().getTmTextUnitVariant().getId(),
        TMTextUnitVariantComment.Type.AI_TRANSLATION,
        TMTextUnitVariantComment.Severity.INFO,
        "Translated via AI translation job.");
  }

  private AITranslation reuseSourceStringAsTranslation(
      TMTextUnit tmTextUnit, Repository repository, Locale targetLocale, String sourceLang) {
    logger.debug(
        "Target language {} matches source language {}, re-using source string as translation.",
        targetLocale.getBcp47Tag(),
        sourceLang);
    meterRegistry
        .counter(
            "AITranslateCronJob.translate.reuseSourceAsTranslation",
            Tags.of("repository", repository.getName(), "locale", targetLocale.getBcp47Tag()))
        .increment();

    return createAITranslationDTO(tmTextUnit, targetLocale, tmTextUnit.getContent());
  }

  private AITranslation executeTranslationPrompt(
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
    meterRegistry
        .counter(
            "AITranslateCronJob.translate.success",
            Tags.of("repository", repository.getName(), "locale", targetLocale.getBcp47Tag()))
        .increment();
    return createAITranslationDTO(tmTextUnit, targetLocale, translation);
  }

  private AITranslation executeGlossaryMatchTranslationPrompt(
      TMTextUnit tmTextUnit,
      Repository repository,
      Locale targetLocale,
      RepositoryLocaleAIPrompt repositoryLocaleAIPrompt,
      List<GlossaryTerm> glossaryTerms) {
    recordGlossaryMatchStats(repository, targetLocale, glossaryTerms);
    String translation =
        llmService.translate(
            tmTextUnit,
            repository.getSourceLocale().getBcp47Tag(),
            targetLocale.getBcp47Tag(),
            repositoryLocaleAIPrompt.getAiPrompt(),
            glossaryTerms);
    meterRegistry
        .counter(
            "AITranslateCronJob.translate.success",
            Tags.of("repository", repository.getName(), "locale", targetLocale.getBcp47Tag()))
        .increment();
    return createAITranslationDTO(tmTextUnit, targetLocale, translation);
  }

  private void recordGlossaryMatchStats(
      Repository repository, Locale targetLocale, List<GlossaryTerm> glossaryTerms) {
    meterRegistry
        .counter(
            "AITranslateCronJob.translate.glossaryMatch",
            Tags.of("repository", repository.getName(), "locale", targetLocale.getBcp47Tag()))
        .increment(glossaryTerms.size());
    meterRegistry
        .counter(
            "AITranslateCronJob.translate.glossaryMatch.dnt",
            Tags.of("repository", repository.getName(), "locale", targetLocale.getBcp47Tag()))
        .increment(glossaryTerms.stream().filter(GlossaryTerm::isDoNotTranslate).count());
    meterRegistry
        .counter(
            "AITranslateCronJob.translate.glossaryMatch.caseSensitiveMatch",
            Tags.of("repository", repository.getName(), "locale", targetLocale.getBcp47Tag()))
        .increment(glossaryTerms.stream().filter(GlossaryTerm::isCaseSensitive).count());
    meterRegistry
        .counter(
            "AITranslateCronJob.translate.glossaryMatch.exactMatch",
            Tags.of("repository", repository.getName(), "locale", targetLocale.getBcp47Tag()))
        .increment(
            glossaryTerms.stream()
                .filter(term -> term.isExactMatch() && !term.isCaseSensitive())
                .count());
  }

  private AITranslation createAITranslationDTO(
      TMTextUnit tmTextUnit, Locale locale, String translation) {
    AITranslation aiTranslation = new AITranslation();
    aiTranslation.setTmTextUnit(tmTextUnit);
    aiTranslation.setContentMd5(DigestUtils.md5Hex(translation));
    aiTranslation.setLocaleId(locale.getId());
    aiTranslation.setTranslation(translation);
    aiTranslation.setComment(tmTextUnit.getComment());
    aiTranslation.setIncludedInLocalizedFile(false);
    aiTranslation.setStatus(TMTextUnitVariant.Status.MT_TRANSLATED);
    aiTranslation.setCreatedDate(JSR310Migration.dateTimeNow());
    return aiTranslation;
  }

  private boolean isExpired(TmTextUnitPendingMT pendingMT) {
    return pendingMT
        .getCreatedDate()
        .isBefore(
            JSR310Migration.newDateTimeEmptyCtor()
                .minus(aiTranslationConfiguration.getExpiryDuration()));
  }

  private List<Long> getUnusedIds(List<TmTextUnitPendingMT> pendingMTList) {
    TextUnitSearcherParameters params = new TextUnitSearcherParameters();
    params.setTmTextUnitIds(
        pendingMTList.stream()
            .map(TmTextUnitPendingMT::getTmTextUnitId)
            .collect(Collectors.toList()));
    params.setUsedFilter(UsedFilter.UNUSED);
    return textUnitSearcher.search(params).stream()
        .map(TextUnitDTO::getTmTextUnitId)
        .collect(Collectors.toList());
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

    aiTranslationService.resetExpiredProcessingStartedAtEntries(
        aiTranslationConfiguration.getTimeout());

    ExecutorService executorService = Executors.newFixedThreadPool(threads);
    meterRegistry
        .counter("AITranslateCronJob.pendingMT.queueSize")
        .increment(tmTextUnitPendingMTRepository.count());

    try {
      List<TmTextUnitPendingMT> pendingMTs =
          tmTextUnitPendingMTRepository.findBatch(aiTranslationConfiguration.getBatchSize());

      if (pendingMTs.isEmpty()) {
        logger.info("No pending MTs to process, finishing early.");
        return;
      }

      // Update the processing started time for all pending MTs we are about to process
      // Locks the pending MTs so that no other concurrent job can process them
      aiTranslationService.bulkUpdateProcessingStartedAt(pendingMTs);

      logger.info("Processing {} pending MTs", pendingMTs.size());

      Queue<TmTextUnitPendingMT> textUnitsToClearPendingMT = new ConcurrentLinkedQueue<>();
      Queue<TmTextUnitPendingMT> timedOutTextUnits = new ConcurrentLinkedQueue<>();

      if (glossaryCacheService != null) {
        // Load glossary cache from blob storage to ensure it's up to date
        glossaryCacheService.loadGlossaryCache();
      }
      List<Long> unusedIds = getUnusedIds(pendingMTs);
      List<CompletableFuture<Void>> futures =
          pendingMTs.stream()
              .peek(
                  pendingMT -> {
                    if (isUnused(pendingMT, unusedIds)) {
                      logger.info(
                          "Skipping AI translation for tmTextUnitId: {} as it is unused & removing it from queue",
                          pendingMT.getTmTextUnitId());
                      textUnitsToClearPendingMT.add(pendingMT);
                    }
                  })
              .filter(pendingMT -> !isUnused(pendingMT, unusedIds))
              .map(
                  pendingMT ->
                      CompletableFuture.runAsync(
                          () -> {
                            try {
                              TMTextUnit tmTextUnit = getTmTextUnit(pendingMT);
                              Repository repository = tmTextUnit.getAsset().getRepository();
                              translate(repository, tmTextUnit, pendingMT);
                            } catch (AITranslateTimeoutException e) {
                              logger.warn(
                                  "Translation job timed out for text unit id: {}",
                                  pendingMT.getTmTextUnitId());
                              timedOutTextUnits.add(pendingMT);
                            } catch (Exception e) {
                              logger.error(
                                  "Error processing pending MT for text unit id: {}",
                                  pendingMT.getTmTextUnitId(),
                                  e);
                              meterRegistry
                                  .counter("AITranslateCronJob.pendingMT.error")
                                  .increment();
                            } finally {
                              logger.debug(
                                  "Sending pending MT for tmTextUnitId: {} for deletion",
                                  pendingMT.getTmTextUnitId());

                              // Only add to the clear queue if it didn't time out
                              if (!timedOutTextUnits.contains(pendingMT)) {
                                textUnitsToClearPendingMT.add(pendingMT);
                              }
                            }
                          },
                          executorService))
              .toList();

      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

      if (!timedOutTextUnits.isEmpty()) {
        aiTranslationService.resetProcessingStartedAtForTextUnits(timedOutTextUnits);
      }
      aiTranslationService.deleteBatch(textUnitsToClearPendingMT);
    } finally {
      shutdownExecutor(executorService);
    }

    logger.info("Finished executing AITranslateCronJob");
  }

  private static boolean isUnused(TmTextUnitPendingMT pendingMT, List<Long> unusedIds) {
    return unusedIds.contains(pendingMT.getTmTextUnitId());
  }

  private static void shutdownExecutor(ExecutorService executorService) {
    try {
      executorService.shutdown();
      if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
        logger.error("Thread pool tasks didn't finish in the expected time.");
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      executorService.shutdownNow();
    }
  }

  private TMTextUnit getTmTextUnit(TmTextUnitPendingMT pendingMT) {
    return tmTextUnitRepository
        .findByIdWithAssetAndRepositoryAndTMFetched(pendingMT.getTmTextUnitId())
        .orElseThrow(
            () -> new AIException("TMTextUnit not found for id: " + pendingMT.getTmTextUnitId()));
  }

  @Bean(name = "aiTranslateCron")
  public JobDetailFactoryBean jobDetailAiTranslateCronJob() {
    JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
    jobDetailFactory.setJobClass(AITranslateCronJob.class);
    jobDetailFactory.setDescription("Translate text units in batches via AI");
    jobDetailFactory.setDurability(true);
    jobDetailFactory.setName("aiTranslateCron");
    return jobDetailFactory;
  }

  @Bean
  public CronTriggerFactoryBean triggerAiTranslateCronJob(
      @Qualifier("aiTranslateCron") JobDetail job,
      AITranslationConfiguration aiTranslationConfiguration) {
    CronTriggerFactoryBean trigger = new CronTriggerFactoryBean();
    trigger.setJobDetail(job);
    trigger.setCronExpression(aiTranslationConfiguration.getCron());
    return trigger;
  }

  private void waitForRateLimit(long startTime) {
    long waitTime = aiTranslationConfiguration.getRateLimit().getMinPollInterval().toMillis();
    try {
      while (System.currentTimeMillis() - startTime
          < aiTranslationConfiguration.getTimeout().toMillis()) {
        if (rateLimiter.isAllowed()) return;
        // Block until the rate limiter allows another request
        logger.debug("Rate limit exceeded for AI translation, waiting before retrying...");
        meterRegistry.counter("AITranslateCronJob.translate.rateLimited").increment();
        Thread.sleep(waitTime);
        waitTime =
            Math.min(
                waitTime * 2,
                aiTranslationConfiguration.getRateLimit().getMaxPollInterval().toMillis());
      }
      throw new AITranslateTimeoutException();
    } catch (JedisException | InterruptedException e) {
      logger.error("Error checking rate limit for AI translation, proceeding with translation");
      meterRegistry
          .counter(
              "AITranslateCronJob.translate.rateLimitException",
              Tags.of("exception", e.getClass().getSimpleName()))
          .increment();
    }
  }
}
