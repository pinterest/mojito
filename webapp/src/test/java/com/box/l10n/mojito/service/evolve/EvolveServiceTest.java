package com.box.l10n.mojito.service.evolve;

import static com.box.l10n.mojito.service.evolve.dto.TranslationStatusType.IN_TRANSLATION;
import static com.box.l10n.mojito.service.evolve.dto.TranslationStatusType.READY_FOR_TRANSLATION;
import static com.box.l10n.mojito.service.evolve.dto.TranslationStatusType.TRANSLATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.LocaleMappingHelper;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.rest.asset.SourceAsset;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionByBranchRepository;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.assetcontent.AssetContentRepository;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchService;
import com.box.l10n.mojito.service.branch.BranchStatisticRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticService;
import com.box.l10n.mojito.service.evolve.dto.CourseDTO;
import com.box.l10n.mojito.service.evolve.dto.TranslationStatusType;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.box.l10n.mojito.xliff.XliffUtils;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

public class EvolveServiceTest extends ServiceTestBase {

  @Autowired RepositoryService repositoryService;

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired AssetService assetService;

  @Autowired PollableTaskService pollableTaskService;

  @Autowired XliffUtils xliffUtils;

  @Autowired LocaleService localeService;

  @Autowired BranchRepository branchRepository;

  @Autowired BranchStatisticRepository branchStatisticRepository;

  @Autowired BranchStatisticService branchStatisticService;

  @Autowired AssetContentRepository assetContentRepository;

  @Autowired TMService tmService;

  @Autowired TextUnitSearcher textUnitSearcher;

  @Autowired BranchService branchService;

  @Autowired AssetExtractionByBranchRepository assetExtractionByBranchRepository;

  @Autowired LocaleMappingHelper localeMappingHelper;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Mock EvolveClient evolveClientMock;

  SyncDateService syncDateService;

  @Captor ArgumentCaptor<Integer> integerCaptor;

  @Captor ArgumentCaptor<String> stringCaptor;

  @Captor ArgumentCaptor<Set<String>> additionalLocalesCaptor;

  @Captor ArgumentCaptor<TranslationStatusType> translationStatusTypeCaptor;

  EvolveService evolveService;

  EvolveConfigurationProperties evolveConfigurationProperties;

  @Before
  public void before() {
    this.evolveConfigurationProperties = new EvolveConfigurationProperties();
    this.evolveConfigurationProperties.setMaxRetries(2);
    this.evolveConfigurationProperties.setRetryMinBackoffSecs(1);
    this.evolveConfigurationProperties.setRetryMaxBackoffSecs(1);
    this.evolveConfigurationProperties.setCourseEvolveType("CourseEvolve");
    this.syncDateService = new InMemorySyncDateService();
  }

  private void initEvolveService(Long repositoryId, String localeMapping) {
    this.evolveService =
        new EvolveService(
            repositoryId,
            localeMapping,
            this.evolveConfigurationProperties,
            this.repositoryRepository,
            this.evolveClientMock,
            this.assetService,
            this.pollableTaskService,
            this.xliffUtils,
            this.branchRepository,
            this.branchStatisticRepository,
            this.assetContentRepository,
            this.tmService,
            this.branchService,
            this.assetExtractionByBranchRepository,
            this.syncDateService,
            this.localeMappingHelper);
  }

  private String getXliffContent() throws IOException {
    return Files.readString(
        Path.of(
            Resources.getResource("com/box/l10n/mojito/service/evolve/" + "course.xliff")
                .getPath()));
  }

  private void initReadyForTranslationData(
      Long repositoryId, String localeMapping, ZonedDateTime updatedOn) throws IOException {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO1.setUpdatedOn(updatedOn);
    courseDTO1.setType("CourseCurriculum");

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenReturn(Stream.of(courseDTO1));
    when(this.evolveClientMock.startCourseTranslation(anyInt(), anyString(), anySet()))
        .thenReturn(this.getXliffContent());

    this.initEvolveService(repositoryId, localeMapping);
  }

  @Test
  public void testSyncForReadyForTranslationCourse()
      throws RepositoryNameAlreadyUsedException, RepositoryLocaleCreationException, IOException {
    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));
    ZonedDateTime now = ZonedDateTime.now();
    ZonedDateTime updatedOn = now.minusDays(1);
    final int courseId = 1;
    this.initReadyForTranslationData(repository.getId(), null, updatedOn);

    this.evolveService.sync();

    verify(this.evolveClientMock)
        .startCourseTranslation(
            this.integerCaptor.capture(),
            this.stringCaptor.capture(),
            this.additionalLocalesCaptor.capture());
    assertEquals(courseId, (int) this.integerCaptor.getValue());
    assertEquals("es-ES", this.stringCaptor.getValue());
    assertTrue(this.additionalLocalesCaptor.getValue().isEmpty());
    verify(this.evolveClientMock)
        .updateCourse(
            this.integerCaptor.capture(),
            this.translationStatusTypeCaptor.capture(),
            any(ZonedDateTime.class));
    assertEquals(1, (int) this.integerCaptor.getValue());
    assertEquals(IN_TRANSLATION, this.translationStatusTypeCaptor.getValue());
    verify(this.evolveClientMock, times(0)).syncEvolve(anyInt());
    assertTrue(
        this.syncDateService.getDate().isEqual(now) || this.syncDateService.getDate().isAfter(now));

    Branch branch =
        this.branchRepository.findByNameAndRepository(
            this.evolveService.getBranchName(courseId), repository);
    assertNotNull(branch);

    this.branchStatisticService.computeAndSaveBranchStatistics(branch);
    BranchStatistic branchStatistic = this.branchStatisticRepository.findByBranch(branch);

    assertEquals(4, branchStatistic.getTotalCount());
  }

  private void initInTranslationData(
      Long repositoryId, String localeMapping, ZonedDateTime updatedOn) {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(IN_TRANSLATION);
    courseDTO1.setUpdatedOn(updatedOn);

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenReturn(Stream.of(courseDTO1));

    this.initEvolveService(repositoryId, localeMapping);
  }

  @Test
  public void testSyncForFullyTranslatedCourse()
      throws RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          UnsupportedAssetFilterTypeException,
          ExecutionException,
          InterruptedException,
          IOException {
    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));
    ZonedDateTime updatedOn = ZonedDateTime.now().minusDays(1);
    final int courseId = 1;
    this.initInTranslationData(repository.getId(), null, updatedOn);
    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setBranch(this.evolveService.getBranchName(courseId));
    sourceAsset.setRepositoryId(repository.getId());
    sourceAsset.setPath(this.evolveService.getAssetPath(courseId));
    sourceAsset.setContent(this.getXliffContent());

    String normalizedContent = NormalizationUtils.normalize(sourceAsset.getContent());
    PollableFuture<Asset> assetFuture =
        this.assetService.addOrUpdateAssetAndProcessIfNeeded(
            sourceAsset.getRepositoryId(),
            sourceAsset.getPath(),
            normalizedContent,
            sourceAsset.isExtractedContent(),
            sourceAsset.getBranch(),
            sourceAsset.getBranchCreatedByUsername(),
            sourceAsset.getBranchNotifiers(),
            null,
            sourceAsset.getFilterConfigIdOverride(),
            sourceAsset.getFilterOptions(),
            true);

    sourceAsset.setAddedAssetId(assetFuture.get().getId());
    sourceAsset.setPollableTask(assetFuture.getPollableTask());
    this.pollableTaskService.waitForPollableTask(sourceAsset.getPollableTask().getId());

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParameters.Builder().repositoryId(repository.getId()).build();
    List<TextUnitDTO> textUnits = this.textUnitSearcher.search(textUnitSearcherParameters);

    textUnits.forEach(
        textUnitDTO ->
            tmService.addTMTextUnitCurrentVariant(
                textUnitDTO.getTmTextUnitId(),
                esLocale.getId(),
                "Text",
                textUnitDTO.getTargetComment(),
                TMTextUnitVariant.Status.APPROVED,
                true));

    Branch branch =
        this.branchRepository.findByNameAndRepository(
            this.evolveService.getBranchName(courseId), repository);
    assertNotNull(branch);
    assertFalse(branch.getDeleted());

    this.branchStatisticService.computeAndSaveBranchStatistics(branch);

    this.evolveService.sync();

    branch =
        this.branchRepository.findByNameAndRepository(
            this.evolveService.getBranchName(courseId), repository);
    assertTrue(branch.getDeleted());

    verify(this.evolveClientMock)
        .updateCourseTranslation(integerCaptor.capture(), stringCaptor.capture());
    assertEquals(courseId, (int) integerCaptor.getValue());
    assertTrue(stringCaptor.getValue().contains("target-language=\"es-ES\""));
    verify(this.evolveClientMock)
        .updateCourse(
            this.integerCaptor.capture(),
            this.translationStatusTypeCaptor.capture(),
            any(ZonedDateTime.class));
    assertEquals(courseId, (int) this.integerCaptor.getValue());
    assertEquals(TRANSLATED, this.translationStatusTypeCaptor.getValue());
    assertNotEquals(updatedOn, this.syncDateService.getDate());

    branch = this.branchRepository.findByNameAndRepository(null, repository);
    assertNotNull(branch);
  }

  @Test
  public void testSyncForNotFullyTranslatedCourse()
      throws RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          UnsupportedAssetFilterTypeException,
          ExecutionException,
          InterruptedException,
          IOException {
    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));
    ZonedDateTime updatedOn = ZonedDateTime.now().minusDays(1);
    final int courseId = 1;
    this.initInTranslationData(repository.getId(), null, updatedOn);

    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setBranch(this.evolveService.getBranchName(courseId));
    sourceAsset.setRepositoryId(repository.getId());
    sourceAsset.setPath(this.evolveService.getAssetPath(courseId));
    sourceAsset.setContent(this.getXliffContent());

    String normalizedContent = NormalizationUtils.normalize(sourceAsset.getContent());
    PollableFuture<Asset> assetFuture =
        this.assetService.addOrUpdateAssetAndProcessIfNeeded(
            sourceAsset.getRepositoryId(),
            sourceAsset.getPath(),
            normalizedContent,
            sourceAsset.isExtractedContent(),
            sourceAsset.getBranch(),
            sourceAsset.getBranchCreatedByUsername(),
            sourceAsset.getBranchNotifiers(),
            null,
            sourceAsset.getFilterConfigIdOverride(),
            sourceAsset.getFilterOptions(),
            true);

    sourceAsset.setAddedAssetId(assetFuture.get().getId());

    sourceAsset.setPollableTask(assetFuture.getPollableTask());

    this.pollableTaskService.waitForPollableTask(sourceAsset.getPollableTask().getId());

    Branch branch =
        this.branchRepository.findByNameAndRepository(
            this.evolveService.getBranchName(courseId), repository);
    assertNotNull(branch);
    assertFalse(branch.getDeleted());

    this.branchStatisticService.computeAndSaveBranchStatistics(branch);

    this.evolveService.sync();

    branch =
        this.branchRepository.findByNameAndRepository(
            this.evolveService.getBranchName(courseId), repository);
    assertFalse(branch.getDeleted());

    verify(this.evolveClientMock, times(0)).updateCourseTranslation(anyInt(), anyString());

    verify(this.evolveClientMock, times(0))
        .updateCourse(anyInt(), any(TranslationStatusType.class), any(ZonedDateTime.class));

    assertEquals(updatedOn, this.syncDateService.getDate());
  }

  @Test
  public void testSyncForBranchStatisticsNotComputed()
      throws RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          UnsupportedAssetFilterTypeException,
          ExecutionException,
          InterruptedException,
          IOException {
    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));
    ZonedDateTime updatedOn = ZonedDateTime.now().minusDays(1);
    final int courseId = 1;
    this.initInTranslationData(repository.getId(), null, updatedOn);

    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setBranch(this.evolveService.getBranchName(courseId));
    sourceAsset.setRepositoryId(repository.getId());
    sourceAsset.setPath(this.evolveService.getAssetPath(courseId));
    sourceAsset.setContent(this.getXliffContent());

    String normalizedContent = NormalizationUtils.normalize(sourceAsset.getContent());
    PollableFuture<Asset> assetFuture =
        this.assetService.addOrUpdateAssetAndProcessIfNeeded(
            sourceAsset.getRepositoryId(),
            sourceAsset.getPath(),
            normalizedContent,
            sourceAsset.isExtractedContent(),
            sourceAsset.getBranch(),
            sourceAsset.getBranchCreatedByUsername(),
            sourceAsset.getBranchNotifiers(),
            null,
            sourceAsset.getFilterConfigIdOverride(),
            sourceAsset.getFilterOptions(),
            true);

    sourceAsset.setAddedAssetId(assetFuture.get().getId());

    sourceAsset.setPollableTask(assetFuture.getPollableTask());

    this.pollableTaskService.waitForPollableTask(sourceAsset.getPollableTask().getId());

    Branch branch =
        this.branchRepository.findByNameAndRepository(
            this.evolveService.getBranchName(courseId), repository);
    assertNotNull(branch);
    assertFalse(branch.getDeleted());

    this.evolveService.sync();

    branch =
        this.branchRepository.findByNameAndRepository(
            this.evolveService.getBranchName(courseId), repository);
    assertFalse(branch.getDeleted());

    verify(this.evolveClientMock, times(0)).updateCourseTranslation(anyInt(), anyString());

    verify(this.evolveClientMock, times(0))
        .updateCourse(anyInt(), any(TranslationStatusType.class), any(ZonedDateTime.class));

    assertEquals(updatedOn, this.syncDateService.getDate());
  }

  private void initTwoCoursesData(
      Long repositoryId, ZonedDateTime updatedOn1, ZonedDateTime updatedOn2) throws IOException {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO1.setUpdatedOn(updatedOn1);
    courseDTO1.setType("CourseCurriculum");

    CourseDTO courseDTO2 = new CourseDTO();
    courseDTO2.setId(2);
    courseDTO2.setTranslationStatus(IN_TRANSLATION);
    courseDTO2.setUpdatedOn(updatedOn2);

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenReturn(Stream.of(courseDTO1, courseDTO2));
    when(this.evolveClientMock.startCourseTranslation(anyInt(), anyString(), anySet()))
        .thenReturn(this.getXliffContent());

    this.initEvolveService(repositoryId, null);
  }

  @Test
  public void testSyncForTwoCourses()
      throws RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          UnsupportedAssetFilterTypeException,
          ExecutionException,
          InterruptedException,
          IOException {
    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));
    ZonedDateTime now = ZonedDateTime.now();
    ZonedDateTime updatedOn1 = now.minusDays(1);
    ZonedDateTime updatedOn2 = updatedOn1.minusDays(1);
    final int inTranslationCourseId = 2;
    this.initTwoCoursesData(repository.getId(), updatedOn1, updatedOn2);

    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setBranch(this.evolveService.getBranchName(inTranslationCourseId));
    sourceAsset.setRepositoryId(repository.getId());
    sourceAsset.setPath(this.evolveService.getAssetPath(inTranslationCourseId));
    sourceAsset.setContent(this.getXliffContent());

    String normalizedContent = NormalizationUtils.normalize(sourceAsset.getContent());
    PollableFuture<Asset> assetFuture =
        this.assetService.addOrUpdateAssetAndProcessIfNeeded(
            sourceAsset.getRepositoryId(),
            sourceAsset.getPath(),
            normalizedContent,
            sourceAsset.isExtractedContent(),
            sourceAsset.getBranch(),
            sourceAsset.getBranchCreatedByUsername(),
            sourceAsset.getBranchNotifiers(),
            null,
            sourceAsset.getFilterConfigIdOverride(),
            sourceAsset.getFilterOptions(),
            true);

    sourceAsset.setAddedAssetId(assetFuture.get().getId());

    sourceAsset.setPollableTask(assetFuture.getPollableTask());

    this.pollableTaskService.waitForPollableTask(sourceAsset.getPollableTask().getId());

    Branch branch =
        this.branchRepository.findByNameAndRepository(
            this.evolveService.getBranchName(inTranslationCourseId), repository);
    assertNotNull(branch);
    assertFalse(branch.getDeleted());

    this.branchStatisticService.computeAndSaveBranchStatistics(branch);

    this.evolveService.sync();

    verify(this.evolveClientMock, times(1)).startCourseTranslation(anyInt(), anyString(), anySet());
    verify(this.evolveClientMock, times(1))
        .updateCourse(anyInt(), any(TranslationStatusType.class), any(ZonedDateTime.class));
    verify(this.evolveClientMock, times(0)).updateCourseTranslation(anyInt(), anyString());
    verify(this.evolveClientMock, times(0)).syncEvolve(anyInt());
    assertEquals(updatedOn2, this.syncDateService.getDate());

    branch =
        this.branchRepository.findByNameAndRepository(
            this.evolveService.getBranchName(inTranslationCourseId), repository);
    assertFalse(branch.getDeleted());
  }

  @Test
  public void testSyncForTwoCoursesAndOneIsFullyTranslated()
      throws RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          UnsupportedAssetFilterTypeException,
          ExecutionException,
          InterruptedException,
          IOException {
    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));
    ZonedDateTime now = ZonedDateTime.now();
    ZonedDateTime updatedOn1 = now.minusDays(1);
    ZonedDateTime updatedOn2 = updatedOn1.minusDays(1);
    final int inTranslationCourseId = 2;
    this.initTwoCoursesData(repository.getId(), updatedOn1, updatedOn2);

    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setBranch(this.evolveService.getBranchName(inTranslationCourseId));
    sourceAsset.setRepositoryId(repository.getId());
    sourceAsset.setPath(this.evolveService.getAssetPath(inTranslationCourseId));
    sourceAsset.setContent(this.getXliffContent());

    String normalizedContent = NormalizationUtils.normalize(sourceAsset.getContent());
    PollableFuture<Asset> assetFuture =
        this.assetService.addOrUpdateAssetAndProcessIfNeeded(
            sourceAsset.getRepositoryId(),
            sourceAsset.getPath(),
            normalizedContent,
            sourceAsset.isExtractedContent(),
            sourceAsset.getBranch(),
            sourceAsset.getBranchCreatedByUsername(),
            sourceAsset.getBranchNotifiers(),
            null,
            sourceAsset.getFilterConfigIdOverride(),
            sourceAsset.getFilterOptions(),
            true);

    sourceAsset.setAddedAssetId(assetFuture.get().getId());
    sourceAsset.setPollableTask(assetFuture.getPollableTask());
    this.pollableTaskService.waitForPollableTask(sourceAsset.getPollableTask().getId());

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParameters.Builder().repositoryId(repository.getId()).build();
    List<TextUnitDTO> textUnits = this.textUnitSearcher.search(textUnitSearcherParameters);

    textUnits.forEach(
        textUnitDTO ->
            tmService.addTMTextUnitCurrentVariant(
                textUnitDTO.getTmTextUnitId(),
                esLocale.getId(),
                "Text",
                textUnitDTO.getTargetComment(),
                TMTextUnitVariant.Status.APPROVED,
                true));

    Branch branch =
        this.branchRepository.findByNameAndRepository(
            this.evolveService.getBranchName(inTranslationCourseId), repository);
    assertNotNull(branch);
    assertFalse(branch.getDeleted());

    this.branchStatisticService.computeAndSaveBranchStatistics(branch);

    this.evolveService.sync();

    verify(this.evolveClientMock, times(1)).startCourseTranslation(anyInt(), anyString(), anySet());
    verify(this.evolveClientMock, times(2))
        .updateCourse(anyInt(), any(TranslationStatusType.class), any(ZonedDateTime.class));
    verify(this.evolveClientMock, times(1)).updateCourseTranslation(anyInt(), anyString());
    verify(this.evolveClientMock, times(0)).syncEvolve(anyInt());
    assertTrue(
        this.syncDateService.getDate().isEqual(now) || this.syncDateService.getDate().isAfter(now));

    branch =
        this.branchRepository.findByNameAndRepository(
            this.evolveService.getBranchName(inTranslationCourseId), repository);
    assertTrue(branch.getDeleted());
  }

  @Test
  public void testSyncForNotExistingRepository() {
    assertThrows(
        "No repository found for name: " + this.testIdWatcher.getEntityName("test"),
        NullPointerException.class,
        () -> this.evolveService.sync());
  }

  @Test
  public void testSyncForMultipleLocales()
      throws RepositoryNameAlreadyUsedException, RepositoryLocaleCreationException {
    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class))).thenReturn(Stream.of());

    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Locale frLocale = this.localeService.findByBcp47Tag("fr-FR");
    RepositoryLocale frRepositoryLocale = new RepositoryLocale();
    frRepositoryLocale.setLocale(frLocale);
    Locale ptLocale = this.localeService.findByBcp47Tag("pt-BR");
    RepositoryLocale ptRepositoryLocale = new RepositoryLocale();
    ptRepositoryLocale.setLocale(ptLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale, frRepositoryLocale, ptRepositoryLocale));
    this.initEvolveService(repository.getId(), null);

    this.evolveService.sync();

    Set<String> targetLocaleBcp47Tags = Sets.newHashSet("es-ES", "fr-FR", "pt-BR");

    assertEquals(3, this.evolveService.getRepositoryLocales().size());
    assertEquals("en", this.evolveService.getSourceLocaleBcp47Tag());
    assertTrue(targetLocaleBcp47Tags.contains(this.evolveService.getTargetLocaleBcp47Tag()));
    Set<String> additionalTargetLocaleBcp47Tags =
        targetLocaleBcp47Tags.stream()
            .filter(
                targetLocaleBcp47Tag ->
                    !targetLocaleBcp47Tag.equals(this.evolveService.getTargetLocaleBcp47Tag()))
            .collect(Collectors.toSet());
    this.evolveService
        .getAdditionalTargetLocaleBcp47Tags()
        .forEach(
            additionalTargetLocale ->
                assertTrue(additionalTargetLocaleBcp47Tags.contains(additionalTargetLocale)));
  }

  @Test
  public void testSyncForFullyTranslatedCourseAndMultipleLocales()
      throws RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          IOException,
          UnsupportedAssetFilterTypeException,
          ExecutionException,
          InterruptedException {
    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Locale frLocale = this.localeService.findByBcp47Tag("fr-FR");
    RepositoryLocale frRepositoryLocale = new RepositoryLocale();
    frRepositoryLocale.setLocale(frLocale);
    Locale ptLocale = this.localeService.findByBcp47Tag("pt-BR");
    RepositoryLocale ptRepositoryLocale = new RepositoryLocale();
    ptRepositoryLocale.setLocale(ptLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale, frRepositoryLocale, ptRepositoryLocale));
    ZonedDateTime updatedOn = ZonedDateTime.now().minusDays(1);
    final int courseId = 1;
    this.initInTranslationData(repository.getId(), null, updatedOn);

    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setBranch(this.evolveService.getBranchName(courseId));
    sourceAsset.setRepositoryId(repository.getId());
    sourceAsset.setPath(this.evolveService.getAssetPath(courseId));
    sourceAsset.setContent(this.getXliffContent());

    String normalizedContent = NormalizationUtils.normalize(sourceAsset.getContent());
    PollableFuture<Asset> assetFuture =
        this.assetService.addOrUpdateAssetAndProcessIfNeeded(
            sourceAsset.getRepositoryId(),
            sourceAsset.getPath(),
            normalizedContent,
            sourceAsset.isExtractedContent(),
            sourceAsset.getBranch(),
            sourceAsset.getBranchCreatedByUsername(),
            sourceAsset.getBranchNotifiers(),
            null,
            sourceAsset.getFilterConfigIdOverride(),
            sourceAsset.getFilterOptions(),
            true);

    sourceAsset.setAddedAssetId(assetFuture.get().getId());

    sourceAsset.setPollableTask(assetFuture.getPollableTask());

    this.pollableTaskService.waitForPollableTask(sourceAsset.getPollableTask().getId());

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParameters.Builder().repositoryId(repository.getId()).build();
    List<TextUnitDTO> textUnits = this.textUnitSearcher.search(textUnitSearcherParameters);

    textUnits.forEach(
        textUnitDTO ->
            tmService.addTMTextUnitCurrentVariant(
                textUnitDTO.getTmTextUnitId(),
                esLocale.getId(),
                "Text",
                textUnitDTO.getTargetComment(),
                TMTextUnitVariant.Status.APPROVED,
                true));

    textUnits.forEach(
        textUnitDTO ->
            tmService.addTMTextUnitCurrentVariant(
                textUnitDTO.getTmTextUnitId(),
                frLocale.getId(),
                "Text",
                textUnitDTO.getTargetComment(),
                TMTextUnitVariant.Status.APPROVED,
                true));

    textUnits.forEach(
        textUnitDTO ->
            tmService.addTMTextUnitCurrentVariant(
                textUnitDTO.getTmTextUnitId(),
                ptLocale.getId(),
                "Text",
                textUnitDTO.getTargetComment(),
                TMTextUnitVariant.Status.APPROVED,
                true));

    Branch branch =
        this.branchRepository.findByNameAndRepository(
            this.evolveService.getBranchName(courseId), repository);
    assertNotNull(branch);
    assertFalse(branch.getDeleted());

    this.branchStatisticService.computeAndSaveBranchStatistics(branch);

    this.evolveService.sync();

    verify(this.evolveClientMock, times(3)).updateCourseTranslation(anyInt(), anyString());
    verify(this.evolveClientMock, times(1))
        .updateCourse(anyInt(), any(TranslationStatusType.class), any(ZonedDateTime.class));
  }

  @Test
  public void testSyncForGetCoursesWithException()
      throws RepositoryNameAlreadyUsedException, RepositoryLocaleCreationException {
    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)));

    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));
    this.initEvolveService(repository.getId(), null);

    assertThrows(HttpClientErrorException.class, () -> this.evolveService.sync());
    assertNull(this.syncDateService.getDate());

    ZonedDateTime syncDate = ZonedDateTime.now().minusDays(2);
    this.syncDateService.setDate(syncDate);
    assertThrows(HttpClientErrorException.class, () -> this.evolveService.sync());
    assertEquals(syncDate, this.syncDateService.getDate());
  }

  private void initStartCourseTranslationExceptionData(Long repositoryId) {
    Mockito.reset(this.evolveClientMock);
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO1.setUpdatedOn(ZonedDateTime.now().minusDays(1));
    courseDTO1.setType("CourseCurriculum");

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenReturn(Stream.of(courseDTO1));
    when(this.evolveClientMock.startCourseTranslation(anyInt(), anyString(), anySet()))
        .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)));

    this.initEvolveService(repositoryId, null);
  }

  @Test
  public void testSyncForStartCourseTranslationWithException()
      throws RepositoryNameAlreadyUsedException, RepositoryLocaleCreationException {
    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));
    this.initStartCourseTranslationExceptionData(repository.getId());

    EvolveSyncException exception =
        assertThrows(EvolveSyncException.class, () -> this.evolveService.sync());
    Throwable firstCauseException = exception.getCause();
    Throwable secondCauseException = firstCauseException.getCause();
    assertTrue(firstCauseException instanceof IllegalStateException);
    assertTrue(secondCauseException instanceof HttpClientErrorException);
    assertNull(this.syncDateService.getDate());

    this.initStartCourseTranslationExceptionData(repository.getId());
    ZonedDateTime syncDate = ZonedDateTime.now().minusDays(2);
    this.syncDateService.setDate(syncDate);
    exception = assertThrows(EvolveSyncException.class, () -> this.evolveService.sync());
    firstCauseException = exception.getCause();
    secondCauseException = firstCauseException.getCause();
    assertTrue(firstCauseException instanceof IllegalStateException);
    assertTrue(secondCauseException instanceof HttpClientErrorException);
    assertEquals(syncDate, this.syncDateService.getDate());
  }

  private void initUpdateCourseExceptionData(Long repositoryId, ZonedDateTime updatedOn)
      throws IOException {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO1.setUpdatedOn(updatedOn);
    courseDTO1.setType("CourseCurriculum");

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenReturn(Stream.of(courseDTO1));
    when(this.evolveClientMock.startCourseTranslation(anyInt(), anyString(), anySet()))
        .thenReturn(this.getXliffContent());
    doThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)))
        .when(this.evolveClientMock)
        .updateCourse(anyInt(), any(TranslationStatusType.class), any(ZonedDateTime.class));

    this.initEvolveService(repositoryId, null);
  }

  @Test
  public void testSyncForUpdateCourseWithException()
      throws RepositoryNameAlreadyUsedException, RepositoryLocaleCreationException, IOException {
    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));
    this.initUpdateCourseExceptionData(repository.getId(), ZonedDateTime.now());

    assertThrows(RuntimeException.class, () -> this.evolveService.sync());
    verify(this.evolveClientMock).startCourseTranslation(anyInt(), anyString(), anySet());
    verify(this.evolveClientMock, times(0)).syncEvolve(anyInt());
    assertNull(this.syncDateService.getDate());

    ZonedDateTime syncDate = ZonedDateTime.now().minusDays(2);
    this.syncDateService.setDate(syncDate);
    assertThrows(RuntimeException.class, () -> this.evolveService.sync());
    assertEquals(syncDate, this.syncDateService.getDate());
  }

  private void initUpdateTranslationException(Long repositoryId, ZonedDateTime updatedOn) {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(IN_TRANSLATION);
    courseDTO1.setUpdatedOn(updatedOn);

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenReturn(Stream.of(courseDTO1));
    doThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)))
        .when(this.evolveClientMock)
        .updateCourseTranslation(anyInt(), anyString());

    this.initEvolveService(repositoryId, null);
  }

  @Test
  public void testSyncForUpdateTranslationWithException()
      throws RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          UnsupportedAssetFilterTypeException,
          ExecutionException,
          InterruptedException,
          IOException {
    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));
    final int courseId = 1;
    ZonedDateTime updatedOn = ZonedDateTime.now().minusDays(1);
    this.initUpdateTranslationException(repository.getId(), updatedOn);

    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setBranch(this.evolveService.getBranchName(courseId));
    sourceAsset.setRepositoryId(repository.getId());
    sourceAsset.setPath(this.evolveService.getAssetPath(courseId));
    sourceAsset.setContent(this.getXliffContent());

    String normalizedContent = NormalizationUtils.normalize(sourceAsset.getContent());
    PollableFuture<Asset> assetFuture =
        this.assetService.addOrUpdateAssetAndProcessIfNeeded(
            sourceAsset.getRepositoryId(),
            sourceAsset.getPath(),
            normalizedContent,
            sourceAsset.isExtractedContent(),
            sourceAsset.getBranch(),
            sourceAsset.getBranchCreatedByUsername(),
            sourceAsset.getBranchNotifiers(),
            null,
            sourceAsset.getFilterConfigIdOverride(),
            sourceAsset.getFilterOptions(),
            true);

    sourceAsset.setAddedAssetId(assetFuture.get().getId());

    sourceAsset.setPollableTask(assetFuture.getPollableTask());

    this.pollableTaskService.waitForPollableTask(sourceAsset.getPollableTask().getId());

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParameters.Builder().repositoryId(repository.getId()).build();
    List<TextUnitDTO> textUnits = this.textUnitSearcher.search(textUnitSearcherParameters);

    textUnits.forEach(
        textUnitDTO ->
            tmService.addTMTextUnitCurrentVariant(
                textUnitDTO.getTmTextUnitId(),
                esLocale.getId(),
                "Text",
                textUnitDTO.getTargetComment(),
                TMTextUnitVariant.Status.APPROVED,
                true));

    Branch branch =
        this.branchRepository.findByNameAndRepository(
            this.evolveService.getBranchName(courseId), repository);
    assertNotNull(branch);
    assertFalse(branch.getDeleted());

    this.branchStatisticService.computeAndSaveBranchStatistics(branch);

    assertThrows(RuntimeException.class, () -> this.evolveService.sync());

    verify(this.evolveClientMock, times(0))
        .updateCourse(anyInt(), any(TranslationStatusType.class), any(ZonedDateTime.class));
    assertNull(this.syncDateService.getDate());

    ZonedDateTime syncDate = ZonedDateTime.now().minusDays(2);
    this.syncDateService.setDate(syncDate);
    assertThrows(RuntimeException.class, () -> this.evolveService.sync());
    assertEquals(syncDate, this.syncDateService.getDate());
  }

  private void initUpdateCourseInTranslationWithExceptionData(
      Long repositoryId, ZonedDateTime updatedOn) {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(IN_TRANSLATION);
    courseDTO1.setUpdatedOn(updatedOn);

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenReturn(Stream.of(courseDTO1));
    doThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)))
        .when(this.evolveClientMock)
        .updateCourse(anyInt(), any(TranslationStatusType.class), any(ZonedDateTime.class));

    this.initEvolveService(repositoryId, null);
  }

  @Test
  public void testSyncWhenUpdateCourseThrowsAnException()
      throws RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          UnsupportedAssetFilterTypeException,
          ExecutionException,
          InterruptedException,
          IOException {
    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));
    ZonedDateTime updatedOn = ZonedDateTime.now().minusDays(1);
    final int courseId = 1;
    this.initUpdateCourseInTranslationWithExceptionData(repository.getId(), updatedOn);

    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setBranch(this.evolveService.getBranchName(courseId));
    sourceAsset.setRepositoryId(repository.getId());
    sourceAsset.setPath(this.evolveService.getAssetPath(courseId));
    sourceAsset.setContent(this.getXliffContent());

    String normalizedContent = NormalizationUtils.normalize(sourceAsset.getContent());
    PollableFuture<Asset> assetFuture =
        this.assetService.addOrUpdateAssetAndProcessIfNeeded(
            sourceAsset.getRepositoryId(),
            sourceAsset.getPath(),
            normalizedContent,
            sourceAsset.isExtractedContent(),
            sourceAsset.getBranch(),
            sourceAsset.getBranchCreatedByUsername(),
            sourceAsset.getBranchNotifiers(),
            null,
            sourceAsset.getFilterConfigIdOverride(),
            sourceAsset.getFilterOptions(),
            true);

    sourceAsset.setAddedAssetId(assetFuture.get().getId());

    sourceAsset.setPollableTask(assetFuture.getPollableTask());

    this.pollableTaskService.waitForPollableTask(sourceAsset.getPollableTask().getId());

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParameters.Builder().repositoryId(repository.getId()).build();
    List<TextUnitDTO> textUnits = this.textUnitSearcher.search(textUnitSearcherParameters);

    textUnits.forEach(
        textUnitDTO ->
            tmService.addTMTextUnitCurrentVariant(
                textUnitDTO.getTmTextUnitId(),
                esLocale.getId(),
                "Text",
                textUnitDTO.getTargetComment(),
                TMTextUnitVariant.Status.APPROVED,
                true));

    Branch branch =
        this.branchRepository.findByNameAndRepository(
            this.evolveService.getBranchName(courseId), repository);
    assertNotNull(branch);
    assertFalse(branch.getDeleted());

    this.branchStatisticService.computeAndSaveBranchStatistics(branch);

    assertThrows(RuntimeException.class, () -> this.evolveService.sync());

    verify(this.evolveClientMock, times(1)).updateCourseTranslation(anyInt(), anyString());
    verify(this.evolveClientMock, times(3)).updateCourse(anyInt(), any(), any());
    assertNull(this.syncDateService.getDate());

    ZonedDateTime syncDate = ZonedDateTime.now().minusDays(2);
    this.syncDateService.setDate(syncDate);
    assertThrows(RuntimeException.class, () -> this.evolveService.sync());
    assertEquals(syncDate, this.syncDateService.getDate());
  }

  @Test
  public void testSyncForReadyForTranslationCourseWithLocaleMappings()
      throws RepositoryNameAlreadyUsedException, RepositoryLocaleCreationException, IOException {
    Locale esLocale = this.localeService.findByBcp47Tag("es-MX");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));
    final int courseId = 1;
    this.initReadyForTranslationData(
        repository.getId(), "es-MX:es-419", ZonedDateTime.now().minusDays(1));

    this.evolveService.sync();

    verify(this.evolveClientMock)
        .startCourseTranslation(
            this.integerCaptor.capture(),
            this.stringCaptor.capture(),
            this.additionalLocalesCaptor.capture());
    assertEquals(courseId, (int) this.integerCaptor.getValue());
    assertEquals("es-419", this.stringCaptor.getValue());
    assertTrue(this.additionalLocalesCaptor.getValue().isEmpty());
    verify(this.evolveClientMock, times(0)).syncEvolve(anyInt());
  }

  @Test
  public void testSyncForFullyTranslatedCourseWithLocaleMappings()
      throws RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          IOException,
          UnsupportedAssetFilterTypeException,
          ExecutionException,
          InterruptedException {
    Locale esLocale = this.localeService.findByBcp47Tag("es-MX");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));
    final int courseId = 1;
    this.initInTranslationData(
        repository.getId(), "es-MX:es-419", ZonedDateTime.now().minusDays(1));

    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setBranch(this.evolveService.getBranchName(courseId));
    sourceAsset.setRepositoryId(repository.getId());
    sourceAsset.setPath(this.evolveService.getAssetPath(courseId));
    sourceAsset.setContent(this.getXliffContent());

    String normalizedContent = NormalizationUtils.normalize(sourceAsset.getContent());
    PollableFuture<Asset> assetFuture =
        this.assetService.addOrUpdateAssetAndProcessIfNeeded(
            sourceAsset.getRepositoryId(),
            sourceAsset.getPath(),
            normalizedContent,
            sourceAsset.isExtractedContent(),
            sourceAsset.getBranch(),
            sourceAsset.getBranchCreatedByUsername(),
            sourceAsset.getBranchNotifiers(),
            null,
            sourceAsset.getFilterConfigIdOverride(),
            sourceAsset.getFilterOptions(),
            true);

    sourceAsset.setAddedAssetId(assetFuture.get().getId());
    sourceAsset.setPollableTask(assetFuture.getPollableTask());
    this.pollableTaskService.waitForPollableTask(sourceAsset.getPollableTask().getId());

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParameters.Builder().repositoryId(repository.getId()).build();
    List<TextUnitDTO> textUnits = this.textUnitSearcher.search(textUnitSearcherParameters);

    textUnits.forEach(
        textUnitDTO ->
            tmService.addTMTextUnitCurrentVariant(
                textUnitDTO.getTmTextUnitId(),
                esLocale.getId(),
                "Text",
                textUnitDTO.getTargetComment(),
                TMTextUnitVariant.Status.APPROVED,
                true));

    Branch branch =
        this.branchRepository.findByNameAndRepository(
            this.evolveService.getBranchName(courseId), repository);
    assertNotNull(branch);
    assertFalse(branch.getDeleted());

    this.branchStatisticService.computeAndSaveBranchStatistics(branch);

    this.evolveService.sync();

    verify(this.evolveClientMock)
        .updateCourseTranslation(integerCaptor.capture(), stringCaptor.capture());
    assertEquals(courseId, (int) integerCaptor.getValue());
    assertTrue(stringCaptor.getValue().contains("target-language=\"es-419\""));
  }

  private void initEvolveSyncRequestData(Long repositoryId) throws IOException {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO1.setUpdatedOn(ZonedDateTime.now().minusDays(1));
    courseDTO1.setType("CourseEvolve");

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenReturn(Stream.of(courseDTO1));
    when(this.evolveClientMock.startCourseTranslation(anyInt(), anyString(), anySet()))
        .thenThrow(
            new HttpClientErrorException(
                HttpStatusCode.valueOf(422),
                " Missing Evolve translation fields: you must poll `POST /api/v3/course_translations/1/evolve_sync"))
        .thenReturn(this.getXliffContent());
    Map response = new HashMap();
    response.put("status", "ready");
    when(this.evolveClientMock.syncEvolve(anyInt())).thenReturn(response);

    this.initEvolveService(repositoryId, null);
  }

  @Test
  public void testSyncForEvolveSyncRequest()
      throws IOException, RepositoryNameAlreadyUsedException, RepositoryLocaleCreationException {
    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));
    final int courseId = 1;
    final ZonedDateTime now = ZonedDateTime.now();
    this.initEvolveSyncRequestData(repository.getId());

    this.evolveService.sync();

    verify(this.evolveClientMock, times(2))
        .startCourseTranslation(
            this.integerCaptor.capture(),
            this.stringCaptor.capture(),
            this.additionalLocalesCaptor.capture());
    assertEquals(courseId, (int) this.integerCaptor.getValue());
    assertEquals("es-ES", this.stringCaptor.getValue());
    assertTrue(this.additionalLocalesCaptor.getValue().isEmpty());
    verify(this.evolveClientMock).syncEvolve(this.integerCaptor.capture());
    assertEquals(courseId, (int) this.integerCaptor.getValue());
    verify(this.evolveClientMock)
        .updateCourse(
            this.integerCaptor.capture(),
            this.translationStatusTypeCaptor.capture(),
            any(ZonedDateTime.class));
    assertEquals(1, (int) this.integerCaptor.getValue());
    assertEquals(IN_TRANSLATION, this.translationStatusTypeCaptor.getValue());
    assertTrue(
        this.syncDateService.getDate().isEqual(now) || this.syncDateService.getDate().isAfter(now));
  }

  private void initEvolveSyncRequestWithExceptionData(Long repositoryId) {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO1.setUpdatedOn(ZonedDateTime.now().minusDays(1));
    courseDTO1.setType("CourseEvolve");

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenReturn(Stream.of(courseDTO1));
    when(this.evolveClientMock.startCourseTranslation(anyInt(), anyString(), anySet()))
        .thenThrow(
            new HttpClientErrorException(
                HttpStatusCode.valueOf(422),
                " Missing Evolve translation fields: you must poll `POST /api/v3/course_translations/1/evolve_sync"));
    Map response = new HashMap();
    response.put("status", "not ready");
    when(this.evolveClientMock.syncEvolve(anyInt()))
        .thenReturn(response)
        .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(500)));

    this.initEvolveService(repositoryId, null);
  }

  @Test
  public void testSyncForEvolveSyncRequestWhenThrowingAnException()
      throws RepositoryNameAlreadyUsedException, RepositoryLocaleCreationException {
    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));
    final int courseId = 1;
    this.initEvolveSyncRequestWithExceptionData(repository.getId());

    assertThrows(RuntimeException.class, () -> this.evolveService.sync());

    verify(this.evolveClientMock, times(0)).startCourseTranslation(anyInt(), anyString(), anySet());
    verify(this.evolveClientMock, times(3)).syncEvolve(this.integerCaptor.capture());
    assertEquals(courseId, (int) this.integerCaptor.getValue());
    verify(this.evolveClientMock, times(0))
        .updateCourse(anyInt(), any(TranslationStatusType.class), any(ZonedDateTime.class));
    assertNull(this.syncDateService.getDate());

    ZonedDateTime syncDate = ZonedDateTime.now().minusDays(2);
    this.syncDateService.setDate(syncDate);
    assertThrows(RuntimeException.class, () -> this.evolveService.sync());
    assertEquals(syncDate, this.syncDateService.getDate());
  }
}
