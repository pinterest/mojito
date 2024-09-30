package com.box.l10n.mojito.service.ai.translation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.AIPrompt;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.RepositoryLocaleAIPrompt;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TmTextUnitPendingMT;
import com.box.l10n.mojito.service.ai.LLMService;
import com.box.l10n.mojito.service.ai.RepositoryLocaleAIPromptRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.google.common.collect.Sets;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AITranslateJobTest {

  @Mock TMService tmService;

  @Mock MeterRegistry meterRegistry;

  @Mock LLMService llmService;

  @Mock TMTextUnitRepository tmTextUnitRepository;

  @Mock TmTextUnitPendingMTRepository tmTextUnitPendingMTRepository;

  @Mock RepositoryRepository repositoryRepository;

  @Mock RepositoryLocaleAIPromptRepository repositoryLocaleAIPromptRepository;

  @Mock AITranslationTextUnitFilterService aiTranslationTextUnitFilterService;

  @Mock AIPrompt aiPrompt;

  @Mock TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

  AITranslateJob aiTranslateJob;

  AITranslateJobInput aiTranslateJobInput;

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
    aiTranslateJob = new AITranslateJob();
    aiTranslateJob.tmService = tmService;
    aiTranslateJob.meterRegistry = meterRegistry;
    aiTranslateJob.llmService = llmService;
    aiTranslateJob.tmTextUnitRepository = tmTextUnitRepository;
    aiTranslateJob.tmTextUnitPendingMTRepository = tmTextUnitPendingMTRepository;
    aiTranslateJob.repositoryRepository = repositoryRepository;
    aiTranslateJob.repositoryLocaleAIPromptRepository = repositoryLocaleAIPromptRepository;
    aiTranslateJob.aiTranslationTextUnitFilterService = aiTranslationTextUnitFilterService;
    aiTranslateJob.tmTextUnitCurrentVariantRepository = tmTextUnitCurrentVariantRepository;
    aiTranslateJob.expiryDuration = Duration.ofHours(3);
    aiTranslateJob.isReuseSourceOnLanguageMatch = false;

    aiTranslateJobInput = new AITranslateJobInput();
    aiTranslateJobInput.setRepositoryId(1L);
    aiTranslateJobInput.setTmTextUnitId(1L);
    Repository testRepo = new Repository();
    testRepo.setId(1L);
    testRepo.setName("testRepo");
    Locale english = new Locale();
    english.setBcp47Tag("en-GB");
    english.setId(1L);
    RepositoryLocale englishRepoLocale = new RepositoryLocale(testRepo, english, true, null);
    Locale french = new Locale();
    french.setBcp47Tag("fr-FR");
    french.setId(2L);
    Locale german = new Locale();
    german.setBcp47Tag("de-DE");
    german.setId(3L);
    Locale hibernoEnglish = new Locale();
    hibernoEnglish.setBcp47Tag("en-IE");
    hibernoEnglish.setId(4L);
    RepositoryLocale frenchRepoLocale =
        new RepositoryLocale(testRepo, french, true, englishRepoLocale);
    frenchRepoLocale.setId(2L);
    RepositoryLocale germanRepoLocale =
        new RepositoryLocale(testRepo, german, true, englishRepoLocale);
    germanRepoLocale.setId(3L);
    RepositoryLocale hibernoEnglishRepoLocale =
        new RepositoryLocale(testRepo, hibernoEnglish, true, englishRepoLocale);
    hibernoEnglishRepoLocale.setId(4L);
    testRepo.setRepositoryLocales(
        Sets.newHashSet(frenchRepoLocale, germanRepoLocale, hibernoEnglishRepoLocale));
    testRepo.setSourceLocale(english);

    when(repositoryRepository.findById(1L)).thenReturn(Optional.of(testRepo));
    TmTextUnitPendingMT testPendingMT = new TmTextUnitPendingMT();
    testPendingMT.setTmTextUnitId(1L);
    testPendingMT.setId(1L);
    testPendingMT.setCreatedDate(JSR310Migration.dateTimeNow());
    when(tmTextUnitPendingMTRepository.findByTmTextUnitId(1L)).thenReturn(testPendingMT);
    when(aiTranslationTextUnitFilterService.isTranslatable(
            isA(TMTextUnit.class), isA(Repository.class)))
        .thenReturn(true);

    RepositoryLocaleAIPrompt testPrompt1 = new RepositoryLocaleAIPrompt();
    testPrompt1.setId(1L);
    testPrompt1.setRepository(testRepo);
    testPrompt1.setLocale(french);
    testPrompt1.setAiPrompt(aiPrompt);

    RepositoryLocaleAIPrompt testPrompt2 = new RepositoryLocaleAIPrompt();
    testPrompt2.setId(2L);
    testPrompt2.setRepository(testRepo);
    testPrompt2.setLocale(german);
    testPrompt2.setAiPrompt(aiPrompt);

    RepositoryLocaleAIPrompt testPrompt3 = new RepositoryLocaleAIPrompt();
    testPrompt3.setId(3L);
    testPrompt3.setRepository(testRepo);
    testPrompt3.setLocale(null);
    testPrompt3.setAiPrompt(aiPrompt);

    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setId(1L);
    tmTextUnit.setContent("content");
    tmTextUnit.setComment("comment");
    tmTextUnit.setName("name");
    when(tmTextUnitRepository.findById(1L)).thenReturn(Optional.of(tmTextUnit));
    when(repositoryLocaleAIPromptRepository.getActiveTranslationPromptsByRepository(
            testRepo.getId()))
        .thenReturn(Lists.list(testPrompt1, testPrompt2, testPrompt3));
    when(llmService.translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class)))
        .thenReturn("translated");
    when(tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(any(), any()))
        .thenReturn(null);
  }

  @Test
  public void testTranslateSuccess() throws Exception {
    aiTranslateJob.call(aiTranslateJobInput);
    verify(llmService, times(3))
        .translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class));
    verify(tmService, times(1))
        .addTMTextUnitVariant(
            eq(1L),
            eq(2L),
            eq("translated"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmService, times(1))
        .addTMTextUnitVariant(
            eq(1L),
            eq(3L),
            eq("translated"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmService, times(1))
        .addTMTextUnitVariant(
            eq(1L),
            eq(4L),
            eq("translated"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmTextUnitPendingMTRepository, times(1)).delete(isA(TmTextUnitPendingMT.class));
  }

  @Test
  public void testTranslateFailure() throws Exception {
    aiTranslateJob.call(aiTranslateJobInput);
    when(llmService.translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class)))
        .thenThrow(new RuntimeException("test"));
    verify(llmService, times(3))
        .translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class));
    verify(tmTextUnitPendingMTRepository, times(1)).delete(isA(TmTextUnitPendingMT.class));
  }

  @Test
  public void testTranslateReuseSource() throws Exception {
    aiTranslateJob.isReuseSourceOnLanguageMatch = true;
    aiTranslateJob.call(aiTranslateJobInput);
    verify(llmService, times(2))
        .translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class));
    verify(tmService, times(1))
        .addTMTextUnitVariant(
            eq(1L),
            eq(2L),
            eq("translated"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmService, times(1))
        .addTMTextUnitVariant(
            eq(1L),
            eq(3L),
            eq("translated"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmService, times(1))
        .addTMTextUnitVariant(
            eq(1L),
            eq(4L),
            eq("content"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmTextUnitPendingMTRepository, times(1)).delete(isA(TmTextUnitPendingMT.class));
    verify(meterRegistry, times(1))
        .counter(eq("AITranslateJob.translate.reuseSourceAsTranslation"), any(Tags.class));
  }

  @Test
  public void testPendingMTEntityIsExpired() throws Exception {
    TmTextUnitPendingMT expiredPendingMT = new TmTextUnitPendingMT();
    expiredPendingMT.setCreatedDate(ZonedDateTime.now().minusHours(4));
    when(tmTextUnitPendingMTRepository.findByTmTextUnitId(1L)).thenReturn(expiredPendingMT);
    aiTranslateJob.call(aiTranslateJobInput);
    verify(llmService, never())
        .translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class));
    verify(tmService, never())
        .addTMTextUnitVariant(
            eq(1L),
            eq(2L),
            eq("translated"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmService, never())
        .addTMTextUnitVariant(
            eq(1L),
            eq(3L),
            eq("translated"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmService, never())
        .addTMTextUnitVariant(
            eq(1L),
            eq(4L),
            eq("content"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmTextUnitPendingMTRepository, times(1)).delete(isA(TmTextUnitPendingMT.class));
    verify(meterRegistry, times(1)).counter(eq("AITranslateJob.expired"), any(Tags.class));
  }

  @Test
  public void testFilterMatchNoTranslation() throws Exception {
    when(aiTranslationTextUnitFilterService.isTranslatable(
            isA(TMTextUnit.class), isA(Repository.class)))
        .thenReturn(false);
    aiTranslateJob.call(aiTranslateJobInput);
    verify(llmService, never())
        .translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class));
    verify(tmService, never())
        .addTMTextUnitVariant(
            eq(1L),
            eq(2L),
            eq("translated"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmService, never())
        .addTMTextUnitVariant(
            eq(1L),
            eq(3L),
            eq("translated"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmService, never())
        .addTMTextUnitVariant(
            eq(1L),
            eq(4L),
            eq("content"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmTextUnitPendingMTRepository, times(1)).delete(isA(TmTextUnitPendingMT.class));
  }

  @Test
  public void testTranslationAlreadyLeveraged() throws Exception {
    TMTextUnitCurrentVariant existingVariant = new TMTextUnitCurrentVariant();
    when(tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(2L, 1L))
        .thenReturn(existingVariant);
    aiTranslateJob.call(aiTranslateJobInput);
    verify(llmService, times(2))
        .translate(
            isA(TMTextUnit.class), isA(String.class), isA(String.class), isA(AIPrompt.class));
    verify(tmService, never())
        .addTMTextUnitVariant(
            eq(1L),
            eq(2L),
            eq("translated"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmService, times(1))
        .addTMTextUnitVariant(
            eq(1L),
            eq(3L),
            eq("translated"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmService, times(1))
        .addTMTextUnitVariant(
            eq(1L),
            eq(4L),
            eq("translated"),
            eq("comment"),
            eq(TMTextUnitVariant.Status.MT_TRANSLATED),
            eq(false),
            isA(ZonedDateTime.class));
    verify(tmTextUnitPendingMTRepository, times(1)).delete(isA(TmTextUnitPendingMT.class));
  }
}
