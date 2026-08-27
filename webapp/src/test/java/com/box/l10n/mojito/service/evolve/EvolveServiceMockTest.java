package com.box.l10n.mojito.service.evolve;

import static com.box.l10n.mojito.service.evolve.dto.TranslationStatusType.IN_TRANSLATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.LocaleMappingHelper;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetExtractionByBranch;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.entity.EvolveCoursePicture;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionByBranchRepository;
import com.box.l10n.mojito.service.assetcontent.AssetContentRepository;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchService;
import com.box.l10n.mojito.service.branch.BranchStatisticRepository;
import com.box.l10n.mojito.service.evolve.dto.CourseDTO;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.xliff.XliffUtils;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Unit tests for {@link EvolveService}'s course picture URL handling, i.e. the private {@code
 * applyCoursePictureUrls}.
 *
 * <p>That method is reached through the only public entry point of the service:
 *
 * <pre>
 * sync() -&gt; syncInTranslation() -&gt; updateCourseTranslations() -&gt; applyCoursePictureUrls()
 * </pre>
 *
 * <p>Its result is observable as the third argument of {@link
 * EvolveClient#updateCourseTranslation(int, Boolean, String)}, so every test drives {@code sync}
 * and asserts on that captured XLIFF content.
 *
 * <p>{@link XliffUtils} and {@link TranslationModeMapper} are intentionally real: the former is the
 * XML transformation under test and the latter produces the flag that selects the branch. Only the
 * outbound collaborators and the persistence layer are mocked.
 *
 * <p>NOTE: {@code sync} catches every per-course exception and only reports an aggregate {@code "N
 * course(s) failed to sync"} at the end. A missing stub therefore surfaces as that opaque message
 * rather than the underlying failure, so tests assert positively with {@code
 * verify(evolveClientMock).updateCourseTranslation(...)} instead of relying on "no exception was
 * thrown".
 */
public class EvolveServiceMockTest {

  private static final long REPOSITORY_ID = 1L;

  private static final int COURSE_ID = 1;

  private static final String BRANCH_NAME = "evolve/course_1";

  private static final String ASSET_PATH = "1.xliff";

  private static final String CONTENT_MD5 = "contentMd5";

  private static final String REFRESH_WITH_PARENT_ASSETS_AND_STRUCTURE =
      "Refresh with parent assets and structure";

  private static final String PRESERVE_ASSETS_AND_STRUCTURE = "Preserve assets and structure";

  private static final String PICTURE_ID = "course[1].picture.target_url";

  private static final String HERO_PICTURE_ID = "course[1].hero_picture.target_url";

  private static final String HERO_PICTURE_MOBILE_ID = "course[1].hero_picture.mobile_target_url";

  @Mock EvolveClient evolveClientMock;

  @Mock EvolveCoursePictureService evolveCoursePictureServiceMock;

  @Mock EvolveSlackNotificationSender evolveSlackNotificationSenderMock;

  @Mock RepositoryRepository repositoryRepositoryMock;

  @Mock AssetService assetServiceMock;

  @Mock PollableTaskService pollableTaskServiceMock;

  @Mock BranchRepository branchRepositoryMock;

  @Mock BranchStatisticRepository branchStatisticRepositoryMock;

  @Mock AssetContentRepository assetContentRepositoryMock;

  @Mock AssetExtractionByBranchRepository assetExtractionByBranchRepositoryMock;

  @Mock TMService tmServiceMock;

  @Mock BranchService branchServiceMock;

  @Captor ArgumentCaptor<Integer> courseIdCaptor;

  @Captor ArgumentCaptor<Boolean> refreshWithParentAssetsAndStructureCaptor;

  @Captor ArgumentCaptor<String> translatedCourseCaptor;

  AutoCloseable mocks;

  EvolveConfigurationProperties evolveConfigurationProperties;

  EvolveService evolveService;

  @BeforeEach
  public void setUp() {
    this.mocks = MockitoAnnotations.openMocks(this);

    this.evolveConfigurationProperties = new EvolveConfigurationProperties();
    this.evolveConfigurationProperties.setMaxRetries(0);
    this.evolveConfigurationProperties.setRetryMinBackoffSecs(0);
    this.evolveConfigurationProperties.setRetryMaxBackoffSecs(0);
    this.evolveConfigurationProperties.setEvolveSyncMaxRetries(0);
    this.evolveConfigurationProperties.setEvolveSyncRetryMinBackoffSecs(0);
    this.evolveConfigurationProperties.setEvolveSyncRetryMaxBackoffSecs(0);
    this.evolveConfigurationProperties.setRefreshWithParentAssetsAndStructureText(
        REFRESH_WITH_PARENT_ASSETS_AND_STRUCTURE);
    this.evolveConfigurationProperties.setPreserveAssetsAndStructureText(
        PRESERVE_ASSETS_AND_STRUCTURE);

    this.evolveService =
        new EvolveService(
            this.evolveConfigurationProperties,
            this.repositoryRepositoryMock,
            this.evolveClientMock,
            this.assetServiceMock,
            this.pollableTaskServiceMock,
            new XliffUtils(),
            this.branchRepositoryMock,
            this.branchStatisticRepositoryMock,
            this.assetContentRepositoryMock,
            this.tmServiceMock,
            this.branchServiceMock,
            this.assetExtractionByBranchRepositoryMock,
            new LocaleMappingHelper(),
            new TranslationModeMapper(this.evolveConfigurationProperties),
            this.evolveCoursePictureServiceMock,
            null,
            this.evolveSlackNotificationSenderMock);
  }

  @AfterEach
  public void tearDown() throws Exception {
    this.mocks.close();
  }

  private String getXliffContent(String fileName) throws IOException {
    return Files.readString(
        Path.of(Resources.getResource("com/box/l10n/mojito/service/evolve/" + fileName).getPath()));
  }

  private String getCourseWithPicturesXliffContent() throws IOException {
    return this.getXliffContent("course_with_pictures.xliff");
  }

  private String getCourseWithoutPicturesXliffContent() throws IOException {
    return this.getXliffContent("course_without_pictures.xliff");
  }

  private Locale createLocale(long id, String bcp47Tag) {
    Locale locale = new Locale();
    locale.setId(id);
    locale.setBcp47Tag(bcp47Tag);
    return locale;
  }

  private EvolveCoursePicture createCoursePicture(
      String localeBcp47Tag,
      String pictureUrl,
      String heroPictureUrl,
      String heroPictureMobileUrl) {
    EvolveCoursePicture evolveCoursePicture = new EvolveCoursePicture();
    evolveCoursePicture.setCourseId(COURSE_ID);
    evolveCoursePicture.setLocaleBcp47Tag(localeBcp47Tag);
    evolveCoursePicture.setPictureUrl(pictureUrl);
    evolveCoursePicture.setHeroPictureUrl(heroPictureUrl);
    evolveCoursePicture.setHeroPictureMobileUrl(heroPictureMobileUrl);
    return evolveCoursePicture;
  }

  /**
   * Stubs everything needed for {@code sync} to walk the IN_TRANSLATION path down to {@code
   * applyCoursePictureUrls} and to call {@link EvolveClient#updateCourseTranslation(int, Boolean,
   * String)} once per target locale.
   *
   * <p>The branch statistics deliberately report untranslated text units so {@code
   * syncInTranslation} stops short of {@code syncTranslated}, which keeps branch deletion and
   * pollable tasks out of the picture.
   */
  private void stubInTranslationSync(String translationMode, String... targetBcp47Tags) {
    Locale sourceLocale = this.createLocale(1L, "en");
    RepositoryLocale sourceRepositoryLocale = new RepositoryLocale();
    sourceRepositoryLocale.setId(1L);
    sourceRepositoryLocale.setLocale(sourceLocale);

    Set<RepositoryLocale> repositoryLocales = new LinkedHashSet<>();
    repositoryLocales.add(sourceRepositoryLocale);
    for (int i = 0; i < targetBcp47Tags.length; i++) {
      RepositoryLocale targetRepositoryLocale = new RepositoryLocale();
      targetRepositoryLocale.setId(2L + i);
      targetRepositoryLocale.setLocale(this.createLocale(2L + i, targetBcp47Tags[i]));
      targetRepositoryLocale.setParentLocale(sourceRepositoryLocale);
      repositoryLocales.add(targetRepositoryLocale);
    }

    Repository repository = new Repository();
    repository.setId(REPOSITORY_ID);
    repository.setRepositoryLocales(repositoryLocales);
    when(this.repositoryRepositoryMock.findById(REPOSITORY_ID)).thenReturn(Optional.of(repository));

    CourseDTO courseDTO = new CourseDTO();
    courseDTO.setId(COURSE_ID);
    courseDTO.setTranslationStatus(IN_TRANSLATION);
    courseDTO.setTranslationMode(translationMode);
    // Streams are single use, so a new one has to be created for every invocation.
    when(this.evolveClientMock.getCourses(any(CoursesGetRequest.class)))
        .thenAnswer(invocation -> Stream.of(courseDTO));

    Branch branch = new Branch();
    branch.setId(10L);
    branch.setName(BRANCH_NAME);
    branch.setRepository(repository);
    when(this.branchRepositoryMock.findByNameAndRepository(BRANCH_NAME, repository))
        .thenReturn(branch);

    BranchStatistic branchStatistic = new BranchStatistic();
    branchStatistic.setBranch(branch);
    branchStatistic.setTotalCount(4L);
    branchStatistic.setForTranslationCount(1L);
    when(this.branchStatisticRepositoryMock.findByBranch(branch)).thenReturn(branchStatistic);

    Asset asset = new Asset();
    asset.setId(20L);
    asset.setPath(ASSET_PATH);
    asset.setRepository(repository);
    when(this.assetServiceMock.findAll(REPOSITORY_ID, ASSET_PATH, false, false, branch.getId()))
        .thenReturn(List.of(asset));

    AssetExtraction assetExtraction = new AssetExtraction();
    assetExtraction.setId(30L);
    assetExtraction.setContentMd5(CONTENT_MD5);
    AssetExtractionByBranch assetExtractionByBranch = new AssetExtractionByBranch();
    assetExtractionByBranch.setAsset(asset);
    assetExtractionByBranch.setBranch(branch);
    assetExtractionByBranch.setAssetExtraction(assetExtraction);
    when(this.assetExtractionByBranchRepositoryMock.findByAssetAndBranch(asset, branch))
        .thenReturn(Optional.of(assetExtractionByBranch));

    AssetContent assetContent = new AssetContent();
    assetContent.setId(40L);
    assetContent.setAsset(asset);
    assetContent.setBranch(branch);
    assetContent.setContentMd5(CONTENT_MD5);
    assetContent.setContent("source content");
    when(this.assetContentRepositoryMock.findByAssetRepositoryIdAndBranchName(
            REPOSITORY_ID, BRANCH_NAME))
        .thenReturn(List.of(assetContent));
  }

  /** Makes {@code tmService.generateLocalized} return {@code localizedContent} for every locale. */
  private void stubGenerateLocalized(String localizedContent)
      throws com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException {
    // any() rather than anyString() for the null filterConfigIdOverride and pullRunName arguments,
    // anyString() does not match null.
    when(this.tmServiceMock.generateLocalized(
            any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(localizedContent);
  }

  /** Makes {@code tmService.generateLocalized} return content keyed by the output BCP47 tag. */
  private void stubGenerateLocalizedPerOutputBcp47Tag(Map<String, String> contentByOutputBcp47Tag)
      throws com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException {
    when(this.tmServiceMock.generateLocalized(
            any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenAnswer(
            invocation -> contentByOutputBcp47Tag.get(invocation.getArgument(3, String.class)));
  }

  private Document parse(String xmlContent) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xmlContent)));
  }

  /**
   * Maps every {@code bin-unit} id in the given XLIFF to its {@code bin-target/external-file href},
   * so assertions can be made on the document structure instead of on substrings.
   */
  private Map<String, String> getBinTargetHrefsById(String xmlContent) throws Exception {
    Map<String, String> hrefsById = new HashMap<>();
    NodeList binUnits = this.parse(xmlContent).getElementsByTagName("bin-unit");
    for (int i = 0; i < binUnits.getLength(); i++) {
      Element binUnit = (Element) binUnits.item(i);
      String href = null;
      NodeList binTargets = binUnit.getElementsByTagName("bin-target");
      if (binTargets.getLength() > 0) {
        NodeList externalFiles =
            ((Element) binTargets.item(0)).getElementsByTagName("external-file");
        if (externalFiles.getLength() > 0) {
          href = ((Element) externalFiles.item(0)).getAttribute("href");
        }
      }
      hrefsById.put(binUnit.getAttribute("id"), href);
    }
    return hrefsById;
  }

  private String syncAndCaptureTranslatedCourse() throws Exception {
    this.evolveService.sync(REPOSITORY_ID, null);

    verify(this.evolveClientMock)
        .updateCourseTranslation(
            this.courseIdCaptor.capture(),
            this.refreshWithParentAssetsAndStructureCaptor.capture(),
            this.translatedCourseCaptor.capture());
    assertEquals(COURSE_ID, (int) this.courseIdCaptor.getValue());

    return this.translatedCourseCaptor.getValue();
  }

  @Test
  public void testContentWithoutBinUnitsIsUploadedUnchanged() throws Exception {
    this.stubInTranslationSync(PRESERVE_ASSETS_AND_STRUCTURE, "es-ES");
    String localizedContent = this.getCourseWithoutPicturesXliffContent();
    this.stubGenerateLocalized(localizedContent);

    String translatedCourse = this.syncAndCaptureTranslatedCourse();

    assertEquals(localizedContent, translatedCourse);
    assertFalse(this.refreshWithParentAssetsAndStructureCaptor.getValue());
    // The picture URLs are never looked up: the method short circuits before that.
    verify(this.evolveCoursePictureServiceMock, never())
        .findByCourseIdAndLocaleBcp47Tag(anyInt(), anyString());
  }

  @Test
  public void testNullTranslationModeRemovesAllBinUnits() throws Exception {
    this.stubInTranslationSync(null, "es-ES");
    this.stubGenerateLocalized(this.getCourseWithPicturesXliffContent());

    String translatedCourse = this.syncAndCaptureTranslatedCourse();

    assertTrue(this.getBinTargetHrefsById(translatedCourse).isEmpty());
    assertNull(this.refreshWithParentAssetsAndStructureCaptor.getValue());
    verify(this.evolveCoursePictureServiceMock, never())
        .findByCourseIdAndLocaleBcp47Tag(anyInt(), anyString());
  }

  @Test
  public void testRefreshWithParentAssetsAndStructureModeRemovesAllBinUnits() throws Exception {
    this.stubInTranslationSync(REFRESH_WITH_PARENT_ASSETS_AND_STRUCTURE, "es-ES");
    this.stubGenerateLocalized(this.getCourseWithPicturesXliffContent());

    String translatedCourse = this.syncAndCaptureTranslatedCourse();

    assertTrue(this.getBinTargetHrefsById(translatedCourse).isEmpty());
    assertTrue(this.refreshWithParentAssetsAndStructureCaptor.getValue());
    verify(this.evolveCoursePictureServiceMock, never())
        .findByCourseIdAndLocaleBcp47Tag(anyInt(), anyString());
  }

  @Test
  public void testUnmappedTranslationModeRemovesAllBinUnits() throws Exception {
    this.stubInTranslationSync("some unmapped translation mode", "es-ES");
    this.stubGenerateLocalized(this.getCourseWithPicturesXliffContent());

    String translatedCourse = this.syncAndCaptureTranslatedCourse();

    assertTrue(this.getBinTargetHrefsById(translatedCourse).isEmpty());
    assertNull(this.refreshWithParentAssetsAndStructureCaptor.getValue());
    verify(this.evolveCoursePictureServiceMock, never())
        .findByCourseIdAndLocaleBcp47Tag(anyInt(), anyString());
  }

  @Test
  public void testPreserveAssetsAndStructureModeAppliesAllPictureUrls() throws Exception {
    this.stubInTranslationSync(PRESERVE_ASSETS_AND_STRUCTURE, "es-ES");
    this.stubGenerateLocalized(this.getCourseWithPicturesXliffContent());
    when(this.evolveCoursePictureServiceMock.findByCourseIdAndLocaleBcp47Tag(COURSE_ID, "es-ES"))
        .thenReturn(
            Optional.of(
                this.createCoursePicture(
                    "es-ES",
                    "https://cdn.test.com/es/picture.png",
                    "https://cdn.test.com/es/hero.png",
                    "https://cdn.test.com/es/hero_mobile.png")));

    String translatedCourse = this.syncAndCaptureTranslatedCourse();

    Map<String, String> hrefsById = this.getBinTargetHrefsById(translatedCourse);
    assertEquals(Set.of(PICTURE_ID, HERO_PICTURE_ID, HERO_PICTURE_MOBILE_ID), hrefsById.keySet());
    assertEquals("https://cdn.test.com/es/picture.png", hrefsById.get(PICTURE_ID));
    assertEquals("https://cdn.test.com/es/hero.png", hrefsById.get(HERO_PICTURE_ID));
    // The fixture has no mobile bin-unit, it gets created by cloning the hero picture one.
    assertEquals("https://cdn.test.com/es/hero_mobile.png", hrefsById.get(HERO_PICTURE_MOBILE_ID));
    assertFalse(this.refreshWithParentAssetsAndStructureCaptor.getValue());
  }

  @Test
  public void testPreserveAssetsAndStructureModeRemovesBinUnitsWithoutAUrl() throws Exception {
    this.stubInTranslationSync(PRESERVE_ASSETS_AND_STRUCTURE, "es-ES");
    this.stubGenerateLocalized(this.getCourseWithPicturesXliffContent());
    when(this.evolveCoursePictureServiceMock.findByCourseIdAndLocaleBcp47Tag(COURSE_ID, "es-ES"))
        .thenReturn(
            Optional.of(
                this.createCoursePicture(
                    "es-ES", "https://cdn.test.com/es/picture.png", null, null)));

    String translatedCourse = this.syncAndCaptureTranslatedCourse();

    Map<String, String> hrefsById = this.getBinTargetHrefsById(translatedCourse);
    assertEquals(Set.of(PICTURE_ID), hrefsById.keySet());
    assertEquals("https://cdn.test.com/es/picture.png", hrefsById.get(PICTURE_ID));
  }

  @Test
  public void testPreserveAssetsAndStructureModeTreatsAnEmptyUrlAsAbsent() throws Exception {
    this.stubInTranslationSync(PRESERVE_ASSETS_AND_STRUCTURE, "es-ES");
    this.stubGenerateLocalized(this.getCourseWithPicturesXliffContent());
    when(this.evolveCoursePictureServiceMock.findByCourseIdAndLocaleBcp47Tag(COURSE_ID, "es-ES"))
        .thenReturn(
            Optional.of(
                this.createCoursePicture("es-ES", "", "https://cdn.test.com/es/hero.png", null)));

    String translatedCourse = this.syncAndCaptureTranslatedCourse();

    Map<String, String> hrefsById = this.getBinTargetHrefsById(translatedCourse);
    assertEquals(Set.of(HERO_PICTURE_ID), hrefsById.keySet());
    assertEquals("https://cdn.test.com/es/hero.png", hrefsById.get(HERO_PICTURE_ID));
  }

  @Test
  public void testPreserveAssetsAndStructureModeWithoutAStoredPictureRemovesAllBinUnits()
      throws Exception {
    this.stubInTranslationSync(PRESERVE_ASSETS_AND_STRUCTURE, "es-ES");
    this.stubGenerateLocalized(this.getCourseWithPicturesXliffContent());
    when(this.evolveCoursePictureServiceMock.findByCourseIdAndLocaleBcp47Tag(COURSE_ID, "es-ES"))
        .thenReturn(Optional.empty());

    String translatedCourse = this.syncAndCaptureTranslatedCourse();

    assertTrue(this.getBinTargetHrefsById(translatedCourse).isEmpty());
  }

  @Test
  public void testPreserveAssetsAndStructureModeLooksUpTheMappedOutputLocale() throws Exception {
    this.stubInTranslationSync(PRESERVE_ASSETS_AND_STRUCTURE, "es-MX");
    this.stubGenerateLocalized(this.getCourseWithPicturesXliffContent());
    when(this.evolveCoursePictureServiceMock.findByCourseIdAndLocaleBcp47Tag(COURSE_ID, "es-419"))
        .thenReturn(
            Optional.of(
                this.createCoursePicture(
                    "es-419", "https://cdn.test.com/es-419/picture.png", null, null)));

    this.evolveService.sync(REPOSITORY_ID, "es-MX:es-419");

    verify(this.evolveClientMock)
        .updateCourseTranslation(eq(COURSE_ID), eq(false), this.translatedCourseCaptor.capture());
    // The lookup uses the mapped output tag, not the repository locale tag.
    verify(this.evolveCoursePictureServiceMock)
        .findByCourseIdAndLocaleBcp47Tag(COURSE_ID, "es-419");
    verify(this.evolveCoursePictureServiceMock, never())
        .findByCourseIdAndLocaleBcp47Tag(COURSE_ID, "es-MX");
    assertEquals(
        "https://cdn.test.com/es-419/picture.png",
        this.getBinTargetHrefsById(this.translatedCourseCaptor.getValue()).get(PICTURE_ID));
  }

  @Test
  public void testPreserveAssetsAndStructureModeAppliesPictureUrlsPerLocale() throws Exception {
    this.stubInTranslationSync(PRESERVE_ASSETS_AND_STRUCTURE, "es-ES", "fr-FR");
    String localizedContent = this.getCourseWithPicturesXliffContent();
    this.stubGenerateLocalizedPerOutputBcp47Tag(
        Map.of("es-ES", localizedContent, "fr-FR", localizedContent));
    when(this.evolveCoursePictureServiceMock.findByCourseIdAndLocaleBcp47Tag(COURSE_ID, "es-ES"))
        .thenReturn(
            Optional.of(
                this.createCoursePicture(
                    "es-ES", "https://cdn.test.com/es/picture.png", null, null)));
    when(this.evolveCoursePictureServiceMock.findByCourseIdAndLocaleBcp47Tag(COURSE_ID, "fr-FR"))
        .thenReturn(
            Optional.of(
                this.createCoursePicture(
                    "fr-FR", "https://cdn.test.com/fr/picture.png", null, null)));

    this.evolveService.sync(REPOSITORY_ID, null);

    verify(this.evolveClientMock, times(2))
        .updateCourseTranslation(eq(COURSE_ID), eq(false), this.translatedCourseCaptor.capture());
    List<String> translatedCourses = this.translatedCourseCaptor.getAllValues();
    assertEquals(
        List.of("https://cdn.test.com/es/picture.png", "https://cdn.test.com/fr/picture.png"),
        List.of(
            this.getBinTargetHrefsById(translatedCourses.get(0)).get(PICTURE_ID),
            this.getBinTargetHrefsById(translatedCourses.get(1)).get(PICTURE_ID)));
  }

  @Test
  public void testMalformedLocalizedContentFailsTheSyncWithoutUploading() throws Exception {
    this.stubInTranslationSync(PRESERVE_ASSETS_AND_STRUCTURE, "es-ES");
    this.stubGenerateLocalized("not xliff at all");

    assertThrows(EvolveSyncException.class, () -> this.evolveService.sync(REPOSITORY_ID, null));

    verify(this.evolveClientMock, never()).updateCourseTranslation(anyInt(), any(), anyString());
  }

  @Test
  public void testSyncWithUpdateCourseTranslationThrows422ThenFallbackSucceeds()
      throws IOException,
          UnsupportedAssetFilterTypeException,
          ParserConfigurationException,
          SAXException {
    this.stubInTranslationSync(PRESERVE_ASSETS_AND_STRUCTURE, "es-ES");
    String localizedContent = this.getCourseWithPicturesXliffContent();
    this.stubGenerateLocalized(localizedContent);
    when(this.evolveCoursePictureServiceMock.findByCourseIdAndLocaleBcp47Tag(COURSE_ID, "es-ES"))
        .thenReturn(
            Optional.of(
                this.createCoursePicture(
                    "es-ES", "https://cdn.test.com/es/picture.png", null, null)));

    HttpClientErrorException httpClientErrorException =
        new HttpClientErrorException(HttpStatusCode.valueOf(422), "Image reference has no content");
    doThrow(httpClientErrorException)
        .doAnswer(invocation -> null)
        .when(this.evolveClientMock)
        .updateCourseTranslation(eq(COURSE_ID), any(), anyString());
    this.evolveService.sync(REPOSITORY_ID, null);

    ArgumentCaptor<String> localizedContentCaptor = ArgumentCaptor.forClass(String.class);
    verify(this.evolveClientMock, times(2))
        .updateCourseTranslation(eq(COURSE_ID), any(), localizedContentCaptor.capture());

    XliffUtils xliffUtils = new XliffUtils();
    List<String> localizedContents = localizedContentCaptor.getAllValues();
    assertEquals(2, localizedContents.size());
    assertTrue(xliffUtils.containsBinUnitElement(localizedContents.get(0)));
    assertFalse(xliffUtils.containsBinUnitElement(localizedContents.get(1)));
  }

  @Test
  public void testSyncWithUpdateCourseTranslationThrows422AndFallbackAlsoFails()
      throws IOException,
          UnsupportedAssetFilterTypeException,
          ParserConfigurationException,
          SAXException {
    this.stubInTranslationSync(PRESERVE_ASSETS_AND_STRUCTURE, "es-ES");
    String localizedContent = this.getCourseWithPicturesXliffContent();
    this.stubGenerateLocalized(localizedContent);
    when(this.evolveCoursePictureServiceMock.findByCourseIdAndLocaleBcp47Tag(COURSE_ID, "es-ES"))
        .thenReturn(
            Optional.of(
                this.createCoursePicture(
                    "es-ES", "https://cdn.test.com/es/picture.png", null, null)));

    HttpClientErrorException httpClientErrorException =
        new HttpClientErrorException(HttpStatusCode.valueOf(422), "Image reference has no content");
    doThrow(httpClientErrorException)
        .doThrow(httpClientErrorException)
        .when(this.evolveClientMock)
        .updateCourseTranslation(eq(COURSE_ID), any(), anyString());

    assertThrows(EvolveSyncException.class, () -> this.evolveService.sync(REPOSITORY_ID, null));

    ArgumentCaptor<String> localizedContentCaptor = ArgumentCaptor.forClass(String.class);
    verify(this.evolveClientMock, times(2))
        .updateCourseTranslation(eq(COURSE_ID), any(), localizedContentCaptor.capture());

    XliffUtils xliffUtils = new XliffUtils();
    List<String> localizedContents = localizedContentCaptor.getAllValues();
    assertEquals(2, localizedContents.size());
    assertTrue(xliffUtils.containsBinUnitElement(localizedContents.get(0)));
    assertFalse(xliffUtils.containsBinUnitElement(localizedContents.get(1)));
  }
}
