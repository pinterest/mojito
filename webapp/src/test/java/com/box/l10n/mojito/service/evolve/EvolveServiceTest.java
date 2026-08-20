package com.box.l10n.mojito.service.evolve;

import static com.box.l10n.mojito.service.evolve.dto.TranslationStatusType.IN_TRANSLATION;
import static com.box.l10n.mojito.service.evolve.dto.TranslationStatusType.READY_FOR_TRANSLATION;
import static com.box.l10n.mojito.service.evolve.dto.TranslationStatusType.TRANSLATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import com.box.l10n.mojito.service.evolve.dto.EquivalentParentDTO;
import com.box.l10n.mojito.service.evolve.dto.TranslationStatusType;
import com.box.l10n.mojito.service.gitblame.GitBlameService;
import com.box.l10n.mojito.service.gitblame.GitBlameWithUsage;
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Collection;
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
import reactor.util.retry.Retry;

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

  @Autowired GitBlameService gitBlameService;

  @Autowired EvolveCoursePictureService evolveCoursePictureService;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Mock EvolveClient evolveClientMock;

  @Mock EvolveSlackNotificationSender evolveSlackNotificationSenderMock;

  @Captor ArgumentCaptor<Integer> integerCaptor;

  @Captor ArgumentCaptor<Boolean> booleanCaptor;

  @Captor ArgumentCaptor<String> stringCaptor;

  @Captor ArgumentCaptor<Set<String>> additionalLocalesCaptor;

  @Captor ArgumentCaptor<TranslationStatusType> translationStatusTypeCaptor;

  @Captor ArgumentCaptor<CoursesGetRequest> coursesGetRequestCaptor;

  EvolveService evolveService;

  EvolveConfigurationProperties evolveConfigurationProperties;

  @Before
  public void before() {
    this.evolveConfigurationProperties = new EvolveConfigurationProperties();
    this.evolveConfigurationProperties.setMaxRetries(2);
    this.evolveConfigurationProperties.setRetryMinBackoffSecs(1);
    this.evolveConfigurationProperties.setRetryMaxBackoffSecs(1);
    this.evolveConfigurationProperties.setEvolveSyncMaxRetries(2);
    this.evolveConfigurationProperties.setEvolveSyncRetryMinBackoffSecs(1);
    this.evolveConfigurationProperties.setEvolveSyncRetryMaxBackoffSecs(1);
    this.evolveConfigurationProperties.setCourseEvolveType("CourseEvolve");
  }

  private void initEvolveService() {
    this.evolveService =
        new EvolveService(
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
            this.localeMappingHelper,
            new TranslationModeMapper(this.evolveConfigurationProperties),
            this.evolveCoursePictureService,
            null,
            this.evolveSlackNotificationSenderMock);
  }

  private String getXliffContent() throws IOException {
    return Files.readString(
        Path.of(
            Resources.getResource("com/box/l10n/mojito/service/evolve/" + "course.xliff")
                .getPath()));
  }

  private void initReadyForTranslationData() throws IOException {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO1.setType("CourseCurriculum");

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenAnswer(invocation -> Stream.of(courseDTO1));
    when(this.evolveClientMock.startCourseTranslation(anyInt(), anyString(), anySet()))
        .thenReturn(this.getXliffContent());

    this.initEvolveService();
  }

  @Test
  public void testSyncReadyForTranslationFetchesLocalizedCoursesAndUpsertsPictures()
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

    CourseDTO courseDTO = new CourseDTO();
    courseDTO.setId(1);
    courseDTO.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO.setType("CourseCurriculum");

    CourseDTO localizedCourseDTO = new CourseDTO();
    localizedCourseDTO.setId(2);
    localizedCourseDTO.setLocale("es-ES");
    EquivalentParentDTO equivalentParentCourseDTO = new EquivalentParentDTO();
    equivalentParentCourseDTO.setId(courseDTO.getId());
    localizedCourseDTO.setEquivalentParent(equivalentParentCourseDTO);
    com.box.l10n.mojito.service.evolve.dto.PictureDTO picture =
        new com.box.l10n.mojito.service.evolve.dto.PictureDTO();
    picture.setTargetUrl("https://cdn.example.com/es.jpg");
    localizedCourseDTO.setPicture(picture);

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenAnswer(invocation -> Stream.of(courseDTO))
        .thenAnswer(invocation -> Stream.of(localizedCourseDTO));
    when(this.evolveClientMock.startCourseTranslation(anyInt(), anyString(), anySet()))
        .thenReturn(this.getXliffContent());
    this.initEvolveService();

    this.evolveService.sync(repository.getId(), null);

    verify(this.evolveClientMock, times(2)).getCourses(this.coursesGetRequestCaptor.capture());
    CoursesGetRequest localizedCoursesRequest = this.coursesGetRequestCaptor.getAllValues().get(1);
    assertEquals("es-ES", localizedCoursesRequest.locale());

    assertTrue(
        this.evolveCoursePictureService
            .findByCourseIdAndLocaleBcp47Tag(courseDTO.getId(), "es-ES")
            .map(p -> "https://cdn.example.com/es.jpg".equals(p.getPictureUrl()))
            .orElse(false));
  }

  @Test
  public void
      testSyncReadyForTranslationFetchesSingleParentWithMultipleLocalizedCoursesSameLocaleAndUpsertsLatestPicture()
          throws RepositoryNameAlreadyUsedException,
              RepositoryLocaleCreationException,
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

    CourseDTO courseDTO = new CourseDTO();
    courseDTO.setId(1);
    courseDTO.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO.setType("CourseCurriculum");

    CourseDTO localizedCourseDTO1 = new CourseDTO();
    localizedCourseDTO1.setId(2);
    localizedCourseDTO1.setLocale("es-ES");
    EquivalentParentDTO equivalentParent1 = new EquivalentParentDTO();
    equivalentParent1.setId(courseDTO.getId());
    localizedCourseDTO1.setEquivalentParent(equivalentParent1);
    com.box.l10n.mojito.service.evolve.dto.PictureDTO picture1 =
        new com.box.l10n.mojito.service.evolve.dto.PictureDTO();
    picture1.setTargetUrl("https://cdn.example.com/es-v1.jpg");
    localizedCourseDTO1.setPicture(picture1);

    CourseDTO localizedCourseDTO2 = new CourseDTO();
    localizedCourseDTO2.setId(3);
    localizedCourseDTO2.setLocale("es-ES");
    EquivalentParentDTO equivalentParent2 = new EquivalentParentDTO();
    equivalentParent2.setId(courseDTO.getId());
    localizedCourseDTO2.setEquivalentParent(equivalentParent2);
    com.box.l10n.mojito.service.evolve.dto.PictureDTO picture2 =
        new com.box.l10n.mojito.service.evolve.dto.PictureDTO();
    picture2.setTargetUrl("https://cdn.example.com/es-v2.jpg");
    localizedCourseDTO2.setPicture(picture2);

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenAnswer(invocation -> Stream.of(courseDTO))
        .thenAnswer(invocation -> Stream.of(localizedCourseDTO1, localizedCourseDTO2));
    when(this.evolveClientMock.startCourseTranslation(anyInt(), anyString(), anySet()))
        .thenReturn(this.getXliffContent());
    this.initEvolveService();

    this.evolveService.sync(repository.getId(), null);

    verify(this.evolveClientMock, times(2)).getCourses(this.coursesGetRequestCaptor.capture());
    CoursesGetRequest localizedCoursesRequest = this.coursesGetRequestCaptor.getAllValues().get(1);
    assertEquals("es-ES", localizedCoursesRequest.locale());

    assertTrue(
        this.evolveCoursePictureService
            .findByCourseIdAndLocaleBcp47Tag(courseDTO.getId(), "es-ES")
            .map(p -> "https://cdn.example.com/es-v2.jpg".equals(p.getPictureUrl()))
            .orElse(false));
  }

  @Test
  public void
      testSyncReadyForTranslationFetchesLocalizedCoursesForMultipleLocalesAndUpsertsPictures()
          throws RepositoryNameAlreadyUsedException,
              RepositoryLocaleCreationException,
              IOException {
    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Locale frLocale = this.localeService.findByBcp47Tag("fr-FR");
    RepositoryLocale frRepositoryLocale = new RepositoryLocale();
    frRepositoryLocale.setLocale(frLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale, frRepositoryLocale));

    CourseDTO courseDTO = new CourseDTO();
    courseDTO.setId(1);
    courseDTO.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO.setType("CourseCurriculum");

    CourseDTO localizedEsCourseDTO = new CourseDTO();
    localizedEsCourseDTO.setId(2);
    localizedEsCourseDTO.setLocale("es-ES");
    EquivalentParentDTO esEquivalentParent = new EquivalentParentDTO();
    esEquivalentParent.setId(courseDTO.getId());
    localizedEsCourseDTO.setEquivalentParent(esEquivalentParent);
    com.box.l10n.mojito.service.evolve.dto.PictureDTO esPicture =
        new com.box.l10n.mojito.service.evolve.dto.PictureDTO();
    esPicture.setTargetUrl("https://cdn.example.com/es.jpg");
    localizedEsCourseDTO.setPicture(esPicture);

    CourseDTO localizedFrCourseDTO = new CourseDTO();
    localizedFrCourseDTO.setId(3);
    localizedFrCourseDTO.setLocale("fr-FR");
    EquivalentParentDTO frEquivalentParent = new EquivalentParentDTO();
    frEquivalentParent.setId(courseDTO.getId());
    localizedFrCourseDTO.setEquivalentParent(frEquivalentParent);
    com.box.l10n.mojito.service.evolve.dto.PictureDTO frPicture =
        new com.box.l10n.mojito.service.evolve.dto.PictureDTO();
    frPicture.setTargetUrl("https://cdn.example.com/fr.jpg");
    localizedFrCourseDTO.setPicture(frPicture);

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenAnswer(
            invocation -> {
              CoursesGetRequest req = invocation.getArgument(0);
              if ("es-ES".equals(req.locale())) return Stream.of(localizedEsCourseDTO);
              if ("fr-FR".equals(req.locale())) return Stream.of(localizedFrCourseDTO);
              return Stream.of(courseDTO);
            });
    when(this.evolveClientMock.startCourseTranslation(anyInt(), anyString(), anySet()))
        .thenReturn(this.getXliffContent());
    this.initEvolveService();

    this.evolveService.sync(repository.getId(), null);

    // 1 source call + 2 locale calls (es-ES and fr-FR)
    verify(this.evolveClientMock, times(3)).getCourses(this.coursesGetRequestCaptor.capture());
    List<String> localizedLocales =
        this.coursesGetRequestCaptor.getAllValues().stream()
            .map(CoursesGetRequest::locale)
            .filter(locale -> "es-ES".equals(locale) || "fr-FR".equals(locale))
            .toList();
    assertEquals(2, localizedLocales.size());
    assertTrue(localizedLocales.contains("es-ES"));
    assertTrue(localizedLocales.contains("fr-FR"));

    assertTrue(
        this.evolveCoursePictureService
            .findByCourseIdAndLocaleBcp47Tag(courseDTO.getId(), "es-ES")
            .map(p -> "https://cdn.example.com/es.jpg".equals(p.getPictureUrl()))
            .orElse(false));
    assertTrue(
        this.evolveCoursePictureService
            .findByCourseIdAndLocaleBcp47Tag(courseDTO.getId(), "fr-FR")
            .map(p -> "https://cdn.example.com/fr.jpg".equals(p.getPictureUrl()))
            .orElse(false));
  }

  @Test
  public void
      testSyncReadyForTranslationFetchesMultipleLocalizedCoursesWithSameLocaleAndUpsertsPictures()
          throws RepositoryNameAlreadyUsedException,
              RepositoryLocaleCreationException,
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

    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO1.setType("CourseCurriculum");

    CourseDTO courseDTO2 = new CourseDTO();
    courseDTO2.setId(2);
    courseDTO2.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO2.setType("CourseCurriculum");

    CourseDTO localizedCourse1DTO = new CourseDTO();
    localizedCourse1DTO.setId(3);
    localizedCourse1DTO.setLocale("es-ES");
    EquivalentParentDTO equivalentParent1 = new EquivalentParentDTO();
    equivalentParent1.setId(courseDTO1.getId());
    localizedCourse1DTO.setEquivalentParent(equivalentParent1);
    com.box.l10n.mojito.service.evolve.dto.PictureDTO picture1 =
        new com.box.l10n.mojito.service.evolve.dto.PictureDTO();
    picture1.setTargetUrl("https://cdn.example.com/course1-es.jpg");
    localizedCourse1DTO.setPicture(picture1);

    CourseDTO localizedCourse2DTO = new CourseDTO();
    localizedCourse2DTO.setId(4);
    localizedCourse2DTO.setLocale("es-ES");
    EquivalentParentDTO equivalentParent2 = new EquivalentParentDTO();
    equivalentParent2.setId(courseDTO2.getId());
    localizedCourse2DTO.setEquivalentParent(equivalentParent2);
    com.box.l10n.mojito.service.evolve.dto.PictureDTO picture2 =
        new com.box.l10n.mojito.service.evolve.dto.PictureDTO();
    picture2.setTargetUrl("https://cdn.example.com/course2-es.jpg");
    localizedCourse2DTO.setPicture(picture2);

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenAnswer(invocation -> Stream.of(courseDTO1, courseDTO2))
        .thenAnswer(invocation -> Stream.of(localizedCourse1DTO, localizedCourse2DTO));
    when(this.evolveClientMock.startCourseTranslation(anyInt(), anyString(), anySet()))
        .thenReturn(this.getXliffContent());
    this.initEvolveService();

    this.evolveService.sync(repository.getId(), null);

    // 1 source call + 1 locale call (es-ES covers both courses)
    verify(this.evolveClientMock, times(2)).getCourses(this.coursesGetRequestCaptor.capture());
    assertEquals("es-ES", this.coursesGetRequestCaptor.getAllValues().get(1).locale());

    assertTrue(
        this.evolveCoursePictureService
            .findByCourseIdAndLocaleBcp47Tag(courseDTO1.getId(), "es-ES")
            .map(p -> "https://cdn.example.com/course1-es.jpg".equals(p.getPictureUrl()))
            .orElse(false));
    assertTrue(
        this.evolveCoursePictureService
            .findByCourseIdAndLocaleBcp47Tag(courseDTO2.getId(), "es-ES")
            .map(p -> "https://cdn.example.com/course2-es.jpg".equals(p.getPictureUrl()))
            .orElse(false));
  }

  @Test
  public void
      testSyncReadyForTranslationFetchesTwoDifferentParentCoursesWithDifferentLocalesAndUpsertsPictures()
          throws RepositoryNameAlreadyUsedException,
              RepositoryLocaleCreationException,
              IOException {
    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Locale frLocale = this.localeService.findByBcp47Tag("fr-FR");
    RepositoryLocale frRepositoryLocale = new RepositoryLocale();
    frRepositoryLocale.setLocale(frLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale, frRepositoryLocale));

    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO1.setType("CourseCurriculum");

    CourseDTO courseDTO2 = new CourseDTO();
    courseDTO2.setId(2);
    courseDTO2.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO2.setType("CourseCurriculum");

    // course 1 has an es-ES localization, course 2 has a fr-FR localization
    CourseDTO localizedCourse1EsDTO = new CourseDTO();
    localizedCourse1EsDTO.setId(3);
    localizedCourse1EsDTO.setLocale("es-ES");
    EquivalentParentDTO equivalentParent1 = new EquivalentParentDTO();
    equivalentParent1.setId(courseDTO1.getId());
    localizedCourse1EsDTO.setEquivalentParent(equivalentParent1);
    com.box.l10n.mojito.service.evolve.dto.PictureDTO picture1 =
        new com.box.l10n.mojito.service.evolve.dto.PictureDTO();
    picture1.setTargetUrl("https://cdn.example.com/course1-es.jpg");
    localizedCourse1EsDTO.setPicture(picture1);

    CourseDTO localizedCourse2FrDTO = new CourseDTO();
    localizedCourse2FrDTO.setId(4);
    localizedCourse2FrDTO.setLocale("fr-FR");
    EquivalentParentDTO equivalentParent2 = new EquivalentParentDTO();
    equivalentParent2.setId(courseDTO2.getId());
    localizedCourse2FrDTO.setEquivalentParent(equivalentParent2);
    com.box.l10n.mojito.service.evolve.dto.PictureDTO picture2 =
        new com.box.l10n.mojito.service.evolve.dto.PictureDTO();
    picture2.setTargetUrl("https://cdn.example.com/course2-fr.jpg");
    localizedCourse2FrDTO.setPicture(picture2);

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenAnswer(
            invocation -> {
              CoursesGetRequest req = invocation.getArgument(0);
              if ("es-ES".equals(req.locale())) return Stream.of(localizedCourse1EsDTO);
              if ("fr-FR".equals(req.locale())) return Stream.of(localizedCourse2FrDTO);
              return Stream.of(courseDTO1, courseDTO2);
            });
    when(this.evolveClientMock.startCourseTranslation(anyInt(), anyString(), anySet()))
        .thenReturn(this.getXliffContent());
    this.initEvolveService();

    this.evolveService.sync(repository.getId(), null);

    // 1 source call + 2 locale calls (es-ES and fr-FR)
    verify(this.evolveClientMock, times(3)).getCourses(this.coursesGetRequestCaptor.capture());
    List<String> localizedLocales =
        this.coursesGetRequestCaptor.getAllValues().stream()
            .map(CoursesGetRequest::locale)
            .filter(locale -> "es-ES".equals(locale) || "fr-FR".equals(locale))
            .toList();
    assertEquals(2, localizedLocales.size());
    assertTrue(localizedLocales.contains("es-ES"));
    assertTrue(localizedLocales.contains("fr-FR"));

    assertTrue(
        this.evolveCoursePictureService
            .findByCourseIdAndLocaleBcp47Tag(courseDTO1.getId(), "es-ES")
            .map(p -> "https://cdn.example.com/course1-es.jpg".equals(p.getPictureUrl()))
            .orElse(false));
    assertTrue(
        this.evolveCoursePictureService
            .findByCourseIdAndLocaleBcp47Tag(courseDTO2.getId(), "fr-FR")
            .map(p -> "https://cdn.example.com/course2-fr.jpg".equals(p.getPictureUrl()))
            .orElse(false));
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
    final int courseId = 1;
    this.initReadyForTranslationData();

    this.evolveService.sync(repository.getId(), null);

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

    Branch branch =
        this.branchRepository.findByNameAndRepository(
            this.evolveService.getBranchName(courseId), repository);
    assertNotNull(branch);

    this.branchStatisticService.computeAndSaveBranchStatistics(branch);
    BranchStatistic branchStatistic = this.branchStatisticRepository.findByBranch(branch);

    assertEquals(4, branchStatistic.getTotalCount());
  }

  private void initInTranslationData() {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(IN_TRANSLATION);

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenAnswer(invocation -> Stream.of(courseDTO1));

    this.initEvolveService();
  }

  @Test
  public void testSyncForPartiallyTranslatedCourse()
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
    this.initInTranslationData();
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

    textUnits
        .subList(0, textUnits.size() - 1)
        .forEach(
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

    this.evolveService.sync(repository.getId(), null);

    branch =
        this.branchRepository.findByNameAndRepository(
            this.evolveService.getBranchName(courseId), repository);
    assertFalse(branch.getDeleted());

    verify(this.evolveClientMock)
        .updateCourseTranslation(
            integerCaptor.capture(), booleanCaptor.capture(), stringCaptor.capture());
    assertEquals(courseId, (int) integerCaptor.getValue());
    assertNull(booleanCaptor.getValue());
    assertTrue(stringCaptor.getValue().contains("target-language=\"es-ES\""));
    verify(this.evolveClientMock, times(0))
        .updateCourse(anyInt(), any(TranslationStatusType.class), any(ZonedDateTime.class));

    verify(this.evolveSlackNotificationSenderMock, times(0))
        .notifyFullyTranslatedCourse(anyInt(), anyString(), anyString(), any(Retry.class));

    branch = this.branchRepository.findByNameAndRepository(null, repository);
    assertNull(branch);
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
    final int courseId = 1;
    this.initInTranslationData();
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

    this.evolveService.sync(repository.getId(), null);

    branch =
        this.branchRepository.findByNameAndRepository(
            this.evolveService.getBranchName(courseId), repository);
    assertTrue(branch.getDeleted());

    verify(this.evolveClientMock)
        .updateCourseTranslation(
            integerCaptor.capture(), booleanCaptor.capture(), stringCaptor.capture());
    assertEquals(courseId, (int) integerCaptor.getValue());
    assertNull(this.booleanCaptor.getValue());
    assertTrue(stringCaptor.getValue().contains("target-language=\"es-ES\""));
    verify(this.evolveClientMock)
        .updateCourse(
            this.integerCaptor.capture(),
            this.translationStatusTypeCaptor.capture(),
            any(ZonedDateTime.class));
    assertEquals(courseId, (int) this.integerCaptor.getValue());
    assertEquals(TRANSLATED, this.translationStatusTypeCaptor.getValue());

    verify(this.evolveSlackNotificationSenderMock, times(0))
        .notifyFullyTranslatedCourse(anyInt(), anyString(), anyString(), any(Retry.class));

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
    final int courseId = 1;
    this.initInTranslationData();

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

    this.evolveService.sync(repository.getId(), null);

    branch =
        this.branchRepository.findByNameAndRepository(
            this.evolveService.getBranchName(courseId), repository);
    assertFalse(branch.getDeleted());

    verify(this.evolveClientMock).updateCourseTranslation(eq(courseId), any(), anyString());
    verify(this.evolveClientMock, times(0))
        .updateCourse(anyInt(), any(TranslationStatusType.class), any(ZonedDateTime.class));
    verify(this.evolveSlackNotificationSenderMock, times(0))
        .notifyFullyTranslatedCourse(anyInt(), anyString(), anyString(), any(Retry.class));
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
    final int courseId = 1;
    this.initInTranslationData();

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

    this.evolveService.sync(repository.getId(), null);

    branch =
        this.branchRepository.findByNameAndRepository(
            this.evolveService.getBranchName(courseId), repository);
    assertFalse(branch.getDeleted());

    verify(this.evolveClientMock).updateCourseTranslation(eq(courseId), any(), anyString());

    verify(this.evolveClientMock, times(0))
        .updateCourse(anyInt(), any(TranslationStatusType.class), any(ZonedDateTime.class));
  }

  private void initTwoCoursesData() throws IOException {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO1.setType("CourseCurriculum");

    CourseDTO courseDTO2 = new CourseDTO();
    courseDTO2.setId(2);
    courseDTO2.setTranslationStatus(IN_TRANSLATION);

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenAnswer(invocation -> Stream.of(courseDTO1, courseDTO2));
    when(this.evolveClientMock.startCourseTranslation(anyInt(), anyString(), anySet()))
        .thenReturn(this.getXliffContent());

    this.initEvolveService();
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
    final int inTranslationCourseId = 2;
    this.initTwoCoursesData();

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

    this.evolveService.sync(repository.getId(), null);

    verify(this.evolveClientMock, times(1)).startCourseTranslation(anyInt(), anyString(), anySet());
    verify(this.evolveClientMock, times(1))
        .updateCourse(anyInt(), any(TranslationStatusType.class), any(ZonedDateTime.class));
    verify(this.evolveClientMock, times(1)).updateCourseTranslation(eq(2), any(), anyString());
    verify(this.evolveClientMock, times(0)).syncEvolve(anyInt());

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
    final int inTranslationCourseId = 2;
    this.initTwoCoursesData();

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

    this.evolveService.sync(repository.getId(), null);

    verify(this.evolveClientMock, times(1)).startCourseTranslation(anyInt(), anyString(), anySet());
    verify(this.evolveClientMock, times(2))
        .updateCourse(anyInt(), any(TranslationStatusType.class), any(ZonedDateTime.class));
    verify(this.evolveClientMock, times(1)).updateCourseTranslation(anyInt(), any(), anyString());
    verify(this.evolveClientMock, times(0)).syncEvolve(anyInt());

    verify(this.evolveSlackNotificationSenderMock, times(0))
        .notifyFullyTranslatedCourse(anyInt(), anyString(), anyString(), any(Retry.class));

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
        () -> this.evolveService.sync(1L, null));
  }

  @Test
  public void testSyncForMultipleLocales()
      throws RepositoryNameAlreadyUsedException, RepositoryLocaleCreationException, IOException {
    this.initReadyForTranslationData();

    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("test"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet());
    this.initEvolveService();

    assertThrows(
        EvolveSyncException.class, () -> this.evolveService.sync(repository.getId(), null));

    this.repositoryService.addRepositoryLocale(repository, "es-ES");
    this.repositoryService.addRepositoryLocale(repository, "fr-FR");
    this.repositoryService.addRepositoryLocale(repository, "pt-BR");

    this.evolveService.sync(repository.getId(), null);

    verify(this.evolveClientMock, times(1))
        .startCourseTranslation(
            anyInt(), this.stringCaptor.capture(), this.additionalLocalesCaptor.capture());
    Set<String> bcp47Tags = ImmutableSet.of("es-ES", "fr-FR", "pt-BR");
    assertTrue(bcp47Tags.contains(this.stringCaptor.getValue()));
    assertEquals(2, this.additionalLocalesCaptor.getValue().size());
    this.additionalLocalesCaptor
        .getValue()
        .forEach(bcp47Tag -> assertTrue(bcp47Tags.contains(bcp47Tag)));
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
    final int courseId = 1;
    this.initInTranslationData();

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

    this.evolveService.sync(repository.getId(), null);

    verify(this.evolveClientMock, times(3)).updateCourseTranslation(anyInt(), any(), anyString());
    verify(this.evolveClientMock, times(1))
        .updateCourse(anyInt(), any(TranslationStatusType.class), any(ZonedDateTime.class));

    verify(this.evolveSlackNotificationSenderMock, times(0))
        .notifyFullyTranslatedCourse(anyInt(), anyString(), anyString(), any(Retry.class));
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
    this.initEvolveService();

    assertThrows(
        HttpClientErrorException.class, () -> this.evolveService.sync(repository.getId(), null));
  }

  private void initStartCourseTranslationExceptionData() throws IOException {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO1.setType("CourseCurriculum");

    CourseDTO courseDTO2 = new CourseDTO();
    courseDTO2.setId(2);
    courseDTO2.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO2.setType("CourseCurriculum");

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenAnswer(invocation -> Stream.of(courseDTO1, courseDTO2));
    when(this.evolveClientMock.startCourseTranslation(anyInt(), anyString(), anySet()))
        .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)))
        .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)))
        .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)))
        .thenReturn(this.getXliffContent());

    this.initEvolveService();
  }

  @Test
  public void testSyncForStartCourseTranslationWithException()
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
    this.initStartCourseTranslationExceptionData();

    assertThrows(
        EvolveSyncException.class, () -> this.evolveService.sync(repository.getId(), null));

    verify(this.evolveClientMock, times(4)).startCourseTranslation(anyInt(), anyString(), anySet());
    verify(this.evolveClientMock)
        .updateCourse(anyInt(), eq(IN_TRANSLATION), any(ZonedDateTime.class));
  }

  private void initUpdateCourseExceptionData() throws IOException {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO1.setType("CourseCurriculum");
    CourseDTO courseDTO2 = new CourseDTO();
    courseDTO2.setId(2);
    courseDTO2.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO2.setType("CourseCurriculum");

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenAnswer(invocation -> Stream.of(courseDTO1, courseDTO2));
    when(this.evolveClientMock.startCourseTranslation(anyInt(), anyString(), anySet()))
        .thenReturn(this.getXliffContent());
    doThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)))
        .doThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)))
        .doThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)))
        .doNothing()
        .when(this.evolveClientMock)
        .updateCourse(anyInt(), any(TranslationStatusType.class), any(ZonedDateTime.class));

    this.initEvolveService();
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
    this.initUpdateCourseExceptionData();

    assertThrows(RuntimeException.class, () -> this.evolveService.sync(repository.getId(), null));
    verify(this.evolveClientMock, times(2)).startCourseTranslation(anyInt(), anyString(), anySet());
    verify(this.evolveClientMock, times(4))
        .updateCourse(anyInt(), eq(IN_TRANSLATION), any(ZonedDateTime.class));
    verify(this.evolveClientMock, times(0)).syncEvolve(anyInt());
  }

  private void initUpdateTranslationException() {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(IN_TRANSLATION);

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenAnswer(invocation -> Stream.of(courseDTO1));
    doThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)))
        .when(this.evolveClientMock)
        .updateCourseTranslation(anyInt(), any(), anyString());

    this.initEvolveService();
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
    this.initUpdateTranslationException();

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

    assertThrows(RuntimeException.class, () -> this.evolveService.sync(repository.getId(), null));

    verify(this.evolveClientMock, times(0))
        .updateCourse(anyInt(), any(TranslationStatusType.class), any(ZonedDateTime.class));
  }

  private void initUpdateCourseInTranslationWithExceptionData() {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(IN_TRANSLATION);

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenAnswer(invocation -> Stream.of(courseDTO1));
    doThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)))
        .when(this.evolveClientMock)
        .updateCourse(anyInt(), any(TranslationStatusType.class), any(ZonedDateTime.class));

    this.initEvolveService();
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
    final int courseId = 1;
    this.initUpdateCourseInTranslationWithExceptionData();

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

    assertThrows(RuntimeException.class, () -> this.evolveService.sync(repository.getId(), null));

    verify(this.evolveClientMock, times(1)).updateCourseTranslation(anyInt(), any(), anyString());
    verify(this.evolveClientMock, times(3)).updateCourse(anyInt(), any(), any());
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
    this.initReadyForTranslationData();

    this.evolveService.sync(repository.getId(), "es-MX:es-419");

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
    this.initInTranslationData();

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

    this.evolveService.sync(repository.getId(), "es-MX:es-419");

    verify(this.evolveClientMock)
        .updateCourseTranslation(
            integerCaptor.capture(), booleanCaptor.capture(), stringCaptor.capture());
    assertEquals(courseId, (int) integerCaptor.getValue());
    assertNull(booleanCaptor.getValue());
    assertTrue(stringCaptor.getValue().contains("target-language=\"es-419\""));
  }

  private void initEvolveSyncRequestData() throws IOException {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO1.setType("CourseEvolve");

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenAnswer(invocation -> Stream.of(courseDTO1));
    when(this.evolveClientMock.startCourseTranslation(anyInt(), anyString(), anySet()))
        .thenThrow(
            new HttpClientErrorException(
                HttpStatusCode.valueOf(422),
                " Missing Evolve translation fields: you must poll `POST /api/v3/course_translations/1/evolve_sync"))
        .thenReturn(this.getXliffContent());
    Map response = new HashMap();
    response.put("status", "ready");
    when(this.evolveClientMock.syncEvolve(anyInt())).thenReturn(response);

    this.initEvolveService();
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
    this.initEvolveSyncRequestData();

    this.evolveService.sync(repository.getId(), null);

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
  }

  private void initEvolveSyncRequestWithExceptionData() {
    CourseDTO courseDTO1 = new CourseDTO();
    courseDTO1.setId(1);
    courseDTO1.setTranslationStatus(READY_FOR_TRANSLATION);
    courseDTO1.setType("CourseEvolve");

    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenAnswer(invocation -> Stream.of(courseDTO1));
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

    this.initEvolveService();
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
    this.initEvolveSyncRequestWithExceptionData();

    assertThrows(RuntimeException.class, () -> this.evolveService.sync(repository.getId(), null));

    verify(this.evolveClientMock, times(0)).startCourseTranslation(anyInt(), anyString(), anySet());
    verify(this.evolveClientMock, times(3)).syncEvolve(this.integerCaptor.capture());
    assertEquals(courseId, (int) this.integerCaptor.getValue());
    verify(this.evolveClientMock, times(0))
        .updateCourse(anyInt(), any(TranslationStatusType.class), any(ZonedDateTime.class));
  }

  @Test
  public void testSyncWithUsages()
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
    this.initReadyForTranslationData();

    this.evolveService.sync(repository.getId(), null);

    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParameters.Builder().repositoryId(repository.getId()).build();
    List<TextUnitDTO> textUnits = this.textUnitSearcher.search(textUnitSearcherParameters);

    textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setTmTextUnitIds(
        textUnits.stream().map(TextUnitDTO::getTmTextUnitId).toList());

    textUnitSearcherParameters.setForRootLocale(true);
    textUnitSearcherParameters.setPluralFormsFiltered(false);
    textUnitSearcherParameters.setLimit(10);
    textUnitSearcherParameters.setOffset(0);

    List<GitBlameWithUsage> gitBlameWithUsages =
        gitBlameService.getGitBlameWithUsages(textUnitSearcherParameters);
    Set<String> currentUsages =
        gitBlameWithUsages.stream()
            .map(GitBlameWithUsage::getUsages)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    assertEquals(0, currentUsages.size());

    Mockito.reset(this.evolveClientMock);
    this.initReadyForTranslationData();
    this.evolveConfigurationProperties.setUsagesKeyRegexp(
        "(exceed-preview-link|evolve-preview-link)");
    this.evolveService.sync(repository.getId(), null);

    textUnitSearcherParameters =
        new TextUnitSearcherParameters.Builder().repositoryId(repository.getId()).build();
    textUnits = this.textUnitSearcher.search(textUnitSearcherParameters);

    textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setTmTextUnitIds(
        textUnits.stream().map(TextUnitDTO::getTmTextUnitId).toList());

    textUnitSearcherParameters.setForRootLocale(true);
    textUnitSearcherParameters.setPluralFormsFiltered(false);
    textUnitSearcherParameters.setLimit(10);
    textUnitSearcherParameters.setOffset(0);

    gitBlameWithUsages = gitBlameService.getGitBlameWithUsages(textUnitSearcherParameters);
    Set<String> usages =
        ImmutableSet.of(
            "https://www.test.com/student/enrollments/create_enrollment_from_token/7654da9d8440ece99c33f63d",
            "https://evolve.test.com/courses/56rt56c0fc16ac5fd210c4cd/preview/index.html");
    currentUsages =
        gitBlameWithUsages.stream()
            .map(GitBlameWithUsage::getUsages)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    assertEquals(2, currentUsages.size());
    currentUsages.forEach(currentUsage -> assertTrue(usages.contains(currentUsage)));
  }

  @Test
  public void testSyncWhenSendingSlackNotification()
      throws RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          IOException,
          UnsupportedAssetFilterTypeException,
          ExecutionException,
          InterruptedException {
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
    this.evolveConfigurationProperties.setSlackChannel("@user");
    this.evolveConfigurationProperties.setCourseUrlTemplate(
        "https://www.test.com/courses/{courseId,number,#}/content");
    this.initInTranslationData();
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

    this.evolveService.sync(repository.getId(), null);

    verify(this.evolveSlackNotificationSenderMock, times(1))
        .notifyFullyTranslatedCourse(
            eq(courseId),
            eq(this.evolveConfigurationProperties.getSlackChannel()),
            eq(this.evolveConfigurationProperties.getCourseUrlTemplate()),
            any(Retry.class));
  }
}
