package com.box.l10n.mojito.service.evolve;

import static com.box.l10n.mojito.service.evolve.dto.TranslationStatusType.IN_TRANSLATION;
import static com.box.l10n.mojito.service.evolve.dto.TranslationStatusType.READY_FOR_TRANSLATION;
import static com.box.l10n.mojito.service.evolve.dto.TranslationStatusType.TRANSLATED;
import static com.box.l10n.mojito.service.security.user.UserService.SYSTEM_USERNAME;
import static java.util.Optional.ofNullable;

import com.box.l10n.mojito.LocaleMappingHelper;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.AssetExtractionByBranch;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.entity.EvolveCoursePicture;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.okapi.InheritanceMode;
import com.box.l10n.mojito.okapi.Status;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.rest.asset.SourceAsset;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionByBranchRepository;
import com.box.l10n.mojito.service.assetcontent.AssetContentRepository;
import com.box.l10n.mojito.service.assetcontent.ContentService;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchService;
import com.box.l10n.mojito.service.branch.BranchStatisticRepository;
import com.box.l10n.mojito.service.evolve.dto.CourseDTO;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.xliff.XliffUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
@ConditionalOnProperty("l10n.evolve.url")
public class EvolveService {
  private static final Logger log = LoggerFactory.getLogger(EvolveService.class);

  private static final long TEN_SECONDS = 10000;

  private final RepositoryRepository repositoryRepository;

  private final EvolveConfigurationProperties evolveConfigurationProperties;

  private final EvolveClient evolveClient;

  private final AssetService assetService;

  private final PollableTaskService pollableTaskService;

  private final XliffUtils xliffUtils;

  private final BranchRepository branchRepository;

  private final BranchStatisticRepository branchStatisticRepository;

  private final AssetContentRepository assetContentRepository;

  private final TMService tmService;

  private final BranchService branchService;

  private final AssetExtractionByBranchRepository assetExtractionByBranchRepository;

  private final LocaleMappingHelper localeMappingHelper;

  private final Optional<ContentService> contentService;

  private final TranslationModeMapper translationModeMapper;

  private final EvolveCoursePictureService evolveCoursePictureService;

  private final Duration retryMinBackoff;

  private final Duration retryMaxBackoff;

  private final Duration evolveSyncRetryMinBackoff;

  private final Duration evolveSyncRetryMaxBackoff;

  private final EvolveSlackNotificationSender evolveSlackNotificationSender;

  @Autowired
  public EvolveService(
      EvolveConfigurationProperties evolveConfigurationProperties,
      RepositoryRepository repositoryRepository,
      EvolveClient evolveClient,
      AssetService assetService,
      PollableTaskService pollableTaskService,
      XliffUtils xliffUtils,
      BranchRepository branchRepository,
      BranchStatisticRepository branchStatisticRepository,
      AssetContentRepository assetContentRepository,
      TMService tmService,
      BranchService branchService,
      AssetExtractionByBranchRepository assetExtractionByBranchRepository,
      LocaleMappingHelper localeMappingHelper,
      TranslationModeMapper translationModeMapper,
      EvolveCoursePictureService evolveCoursePictureService,
      @Autowired(required = false) ContentService contentService,
      EvolveSlackNotificationSender evolveSlackNotificationSender) {
    this.evolveConfigurationProperties = evolveConfigurationProperties;
    this.repositoryRepository = repositoryRepository;
    this.evolveClient = evolveClient;
    this.assetService = assetService;
    this.pollableTaskService = pollableTaskService;
    this.xliffUtils = xliffUtils;
    this.branchRepository = branchRepository;
    this.branchStatisticRepository = branchStatisticRepository;
    this.assetContentRepository = assetContentRepository;
    this.tmService = tmService;
    this.branchService = branchService;
    this.assetExtractionByBranchRepository = assetExtractionByBranchRepository;
    this.localeMappingHelper = localeMappingHelper;
    this.translationModeMapper = translationModeMapper;
    this.evolveCoursePictureService = evolveCoursePictureService;
    this.contentService = ofNullable(contentService);
    this.retryMinBackoff =
        Duration.ofSeconds(evolveConfigurationProperties.getRetryMinBackoffSecs());
    this.retryMaxBackoff =
        Duration.ofSeconds(evolveConfigurationProperties.getRetryMaxBackoffSecs());
    this.evolveSyncRetryMinBackoff =
        Duration.ofSeconds(evolveConfigurationProperties.getEvolveSyncRetryMinBackoffSecs());
    this.evolveSyncRetryMaxBackoff =
        Duration.ofSeconds(evolveConfigurationProperties.getEvolveSyncRetryMaxBackoffSecs());
    this.evolveSlackNotificationSender = evolveSlackNotificationSender;
  }

  private SourceAsset importSourceAsset(SourceAsset sourceAsset)
      throws UnsupportedAssetFilterTypeException, ExecutionException, InterruptedException {
    Preconditions.checkNotNull(sourceAsset);

    String normalizedContent = NormalizationUtils.normalize(sourceAsset.getContent());
    PollableFuture<Asset> assetFuture;
    assetFuture =
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
    return sourceAsset;
  }

  String getBranchName(int courseId) {
    return String.format("evolve/course_%d", courseId);
  }

  String getAssetPath(int courseId) {
    return String.format("%d.xliff", courseId);
  }

  private void importSourceAsset(int courseId, String localizedAssetContent, long repositoryId)
      throws XPathExpressionException,
          ParserConfigurationException,
          IOException,
          TransformerException,
          SAXException,
          UnsupportedAssetFilterTypeException,
          ExecutionException,
          InterruptedException {
    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setBranch(this.getBranchName(courseId));
    sourceAsset.setRepositoryId(repositoryId);
    sourceAsset.setPath(this.getAssetPath(courseId));
    sourceAsset.setBranchCreatedByUsername(SYSTEM_USERNAME);
    sourceAsset.setContent(this.xliffUtils.removeTargetLanguageAttribute(localizedAssetContent));
    ofNullable(this.evolveConfigurationProperties.getUsagesKeyRegexp())
        .ifPresent(
            usagesKeyRegexp ->
                sourceAsset.setFilterOptions(
                    List.of(String.format("usagesKeyRegexp=%s", usagesKeyRegexp))));
    this.pollableTaskService.waitForPollableTask(
        this.importSourceAsset(sourceAsset).getPollableTask().getId(),
        this.evolveConfigurationProperties.getTaskTimeoutInSeconds() * 1000,
        TEN_SECONDS);
  }

  private void syncEvolve(int courseId) {
    Mono.fromRunnable(
            () -> {
              Map<?, ?> response = this.evolveClient.syncEvolve(courseId);
              if (!response.containsKey("status")
                  || !response.get("status").toString().equalsIgnoreCase("ready")) {
                throw new EvolveSyncException(
                    "Update Evolve cloud translations for course: "
                        + courseId
                        + " is not complete");
              }
            })
        .retryWhen(
            Retry.backoff(
                    this.evolveConfigurationProperties.getEvolveSyncMaxRetries(),
                    this.evolveSyncRetryMinBackoff)
                .maxBackoff(this.evolveSyncRetryMaxBackoff))
        .doOnError(e -> log.error("Update Evolve cloud translations is not complete", e))
        .block();
  }

  private void startCourseTranslations(
      int courseId,
      String courseType,
      long repositoryId,
      String targetLocaleBcp47Tag,
      Set<String> additionalTargetLocaleBcp47Tags)
      throws XPathExpressionException,
          UnsupportedAssetFilterTypeException,
          ParserConfigurationException,
          IOException,
          ExecutionException,
          InterruptedException,
          TransformerException,
          SAXException {
    Preconditions.checkNotNull(courseType);

    if (courseType.equalsIgnoreCase(this.evolveConfigurationProperties.getCourseEvolveType())) {
      this.syncEvolve(courseId);
    }
    String localizedAssetContent =
        Mono.fromCallable(
                () ->
                    this.evolveClient.startCourseTranslation(
                        courseId, targetLocaleBcp47Tag, additionalTargetLocaleBcp47Tags))
            .retryWhen(
                Retry.backoff(
                        this.evolveConfigurationProperties.getMaxRetries(), this.retryMinBackoff)
                    .maxBackoff(this.retryMaxBackoff))
            .doOnError(e -> log.error("Error while starting a course translation", e))
            .block();
    this.importSourceAsset(courseId, localizedAssetContent, repositoryId);
  }

  private void updateCourse(CourseDTO courseDTO, ZonedDateTime currentDateTime) {
    Preconditions.checkNotNull(courseDTO);

    Mono.fromRunnable(
            () ->
                this.evolveClient.updateCourse(
                    courseDTO.getId(), courseDTO.getTranslationStatus(), currentDateTime))
        .retryWhen(
            Retry.backoff(this.evolveConfigurationProperties.getMaxRetries(), this.retryMinBackoff)
                .maxBackoff(this.retryMaxBackoff))
        .doOnError(e -> log.error("Error while updating a course", e))
        .block();
  }

  private void importSourceAssetToPrimaryBranch(int courseId, String content, long repositoryId)
      throws UnsupportedAssetFilterTypeException, ExecutionException, InterruptedException {
    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setRepositoryId(repositoryId);
    sourceAsset.setPath(this.getAssetPath(courseId));
    sourceAsset.setBranchCreatedByUsername(SYSTEM_USERNAME);
    sourceAsset.setContent(content);
    ofNullable(this.evolveConfigurationProperties.getUsagesKeyRegexp())
        .ifPresent(
            usagesKeyRegexp ->
                sourceAsset.setFilterOptions(
                    List.of(String.format("usagesKeyRegexp=%s", usagesKeyRegexp))));
    this.pollableTaskService.waitForPollableTask(
        this.importSourceAsset(sourceAsset).getPollableTask().getId(),
        this.evolveConfigurationProperties.getTaskTimeoutInSeconds() * 1000,
        TEN_SECONDS);
  }

  private String applyCoursePictureUrls(
      int courseId,
      String localizedContent,
      Boolean refreshWithParentAssetsAndStructure,
      String outputBcp47Tag)
      throws XPathExpressionException,
          ParserConfigurationException,
          IOException,
          SAXException,
          TransformerException {
    if (!this.xliffUtils.containsBinUnitElement(localizedContent)) {
      return localizedContent;
    }
    if (refreshWithParentAssetsAndStructure == null || refreshWithParentAssetsAndStructure) {
      return this.xliffUtils.removeBinUnitElements(localizedContent);
    }
    EvolveCoursePicture evolveCoursePicture =
        this.evolveCoursePictureService
            .findByCourseIdAndLocaleBcp47Tag(courseId, outputBcp47Tag)
            .orElseGet(EvolveCoursePicture::new);
    return this.xliffUtils.replaceTargetMediaTargetUrls(
        courseId,
        localizedContent,
        evolveCoursePicture.getPictureUrl(),
        evolveCoursePicture.getHeroPictureUrl(),
        evolveCoursePicture.getHeroPictureMobileUrl());
  }

  private void updateCourseTranslations(
      CourseDTO courseDTO,
      Asset asset,
      String content,
      Map<String, String> localeMappings,
      List<RepositoryLocale> targetRepositoryLocales) {
    String normalizedContent = NormalizationUtils.normalize(content);
    Boolean refreshWithParentAssetsAndStructure =
        this.translationModeMapper
            .toRefreshWithParentAssetsAndStructure(courseDTO.getTranslationMode())
            .orElse(null);
    targetRepositoryLocales.forEach(
        repositoryLocale -> {
          String localeBcp47Tag = repositoryLocale.getLocale().getBcp47Tag();
          String outputBcp47Tag = localeMappings.getOrDefault(localeBcp47Tag, localeBcp47Tag);
          String localizedContent;
          try {
            localizedContent =
                tmService.generateLocalized(
                    asset,
                    normalizedContent,
                    repositoryLocale,
                    outputBcp47Tag,
                    null,
                    ImmutableList.of(),
                    Status.ACCEPTED,
                    InheritanceMode.USE_PARENT,
                    null);
          } catch (UnsupportedAssetFilterTypeException e) {
            throw new EvolveSyncException(e.getMessage(), e);
          }

          String localizedContentWithUpdatedTargetUrls;
          try {
            localizedContentWithUpdatedTargetUrls =
                this.applyCoursePictureUrls(
                    courseDTO.getId(),
                    localizedContent,
                    refreshWithParentAssetsAndStructure,
                    outputBcp47Tag);
          } catch (XPathExpressionException
              | ParserConfigurationException
              | IOException
              | SAXException
              | TransformerException e) {
            throw new EvolveSyncException(
                "Error while updating picture and hero picture target URLs", e);
          }

          Mono.fromRunnable(
                  () ->
                      this.evolveClient.updateCourseTranslation(
                          courseDTO.getId(),
                          refreshWithParentAssetsAndStructure,
                          localizedContentWithUpdatedTargetUrls))
              .retryWhen(
                  Retry.backoff(
                          this.evolveConfigurationProperties.getMaxRetries(), this.retryMinBackoff)
                      .maxBackoff(this.retryMaxBackoff))
              .doOnError(e -> log.error("Error while updating course translation", e))
              .block();
        });
  }

  private void deleteBranch(long repositoryId, long branchId) throws InterruptedException {
    PollableFuture<Void> pollableFuture =
        this.branchService.asyncDeleteBranch(repositoryId, branchId);
    this.pollableTaskService.waitForPollableTask(
        pollableFuture.getPollableTask().getId(),
        this.evolveConfigurationProperties.getTaskTimeoutInSeconds() * 1000,
        TEN_SECONDS);
  }

  private String getContentMd5(Asset asset, Branch branch) {
    Optional<AssetExtractionByBranch> assetExtractionByBranch =
        assetExtractionByBranchRepository.findByAssetAndBranch(asset, branch);
    if (assetExtractionByBranch.isPresent()) {
      return assetExtractionByBranch.get().getAssetExtraction().getContentMd5();
    } else {
      throw new EvolveSyncException(
          String.format(
              "Asset Extraction not found for asset ID: %d and branch: %s",
              asset.getId(), branch.getName()));
    }
  }

  private void sendSlackNotification(int courseId) {
    String slackChannel = this.evolveConfigurationProperties.getSlackChannel();
    String courseUrlTemplate = this.evolveConfigurationProperties.getCourseUrlTemplate();
    if (slackChannel != null && courseUrlTemplate != null) {
      this.evolveSlackNotificationSender.notifyFullyTranslatedCourse(
          courseId,
          slackChannel,
          courseUrlTemplate,
          Retry.backoff(this.evolveConfigurationProperties.getMaxRetries(), this.retryMinBackoff)
              .maxBackoff(this.retryMaxBackoff));
    }
  }

  private void syncTranslated(
      CourseDTO courseDTO,
      String content,
      Branch branch,
      ZonedDateTime startDateTime,
      long repositoryId)
      throws UnsupportedAssetFilterTypeException, ExecutionException, InterruptedException {
    this.importSourceAssetToPrimaryBranch(courseDTO.getId(), content, repositoryId);
    this.deleteBranch(repositoryId, branch.getId());
    courseDTO.setTranslationStatus(TRANSLATED);
    this.updateCourse(courseDTO, startDateTime);
    this.sendSlackNotification(courseDTO.getId());
  }

  private void syncReadyForTranslation(
      CourseDTO courseDTO,
      ZonedDateTime startDateTime,
      long repositoryId,
      String targetLocaleBcp47Tag,
      Set<String> additionalTargetLocaleBcp47Tags,
      Map<Integer, List<CourseDTO>> localizedCoursesByParentCourseId)
      throws XPathExpressionException,
          UnsupportedAssetFilterTypeException,
          ParserConfigurationException,
          IOException,
          ExecutionException,
          InterruptedException,
          TransformerException,
          SAXException {
    List<CourseDTO> currentLocalizedCourseDTOs =
        localizedCoursesByParentCourseId.getOrDefault(courseDTO.getId(), List.of());
    this.startCourseTranslations(
        courseDTO.getId(),
        courseDTO.getType(),
        repositoryId,
        targetLocaleBcp47Tag,
        additionalTargetLocaleBcp47Tags);
    courseDTO.setTranslationStatus(IN_TRANSLATION);
    this.updateCourse(courseDTO, startDateTime);
    this.evolveCoursePictureService.upsertLocalizedCoursePictureUrls(
        courseDTO.getId(), currentLocalizedCourseDTOs);
  }

  private Optional<String> getContent(
      CourseDTO courseDTO, Asset asset, Branch branch, long repositoryId) {
    List<AssetContent> assetContents =
        this.assetContentRepository.findByAssetRepositoryIdAndBranchName(
            repositoryId, this.getBranchName(courseDTO.getId()));
    Optional<AssetContent> assetContent =
        assetContents.stream()
            .filter(content -> content.getContentMd5().equals(this.getContentMd5(asset, branch)))
            .findFirst();
    return assetContent.map(
        content ->
            this.contentService
                .map(service -> service.getContent(content).orElseThrow())
                .orElse(content.getContent()));
  }

  private void syncInTranslation(
      CourseDTO courseDTO,
      ZonedDateTime startDateTime,
      Repository repository,
      Map<String, String> localeMappings,
      List<RepositoryLocale> targetRepositoryLocales)
      throws UnsupportedAssetFilterTypeException, ExecutionException, InterruptedException {
    Branch branch =
        this.branchRepository.findByNameAndRepository(
            this.getBranchName(courseDTO.getId()), repository);
    BranchStatistic branchStatistic = this.branchStatisticRepository.findByBranch(branch);
    Asset asset =
        this.assetService
            .findAll(
                repository.getId(),
                this.getAssetPath(courseDTO.getId()),
                false,
                false,
                branch.getId())
            .getFirst();
    Optional<String> content = this.getContent(courseDTO, asset, branch, repository.getId());
    if (content.isEmpty()) {
      throw new EvolveSyncException(
          "Couldn't find asset content for course with id: " + courseDTO.getId());
    }
    this.updateCourseTranslations(
        courseDTO, asset, content.get(), localeMappings, targetRepositoryLocales);
    this.evolveCoursePictureService.deleteByCourseId(courseDTO.getId());
    if (branchStatistic != null
        && branchStatistic.getTotalCount() > 0
        && branchStatistic.getForTranslationCount() == 0) {
      this.syncTranslated(courseDTO, content.get(), branch, startDateTime, repository.getId());
    }
  }

  private String getSourceLocaleBcp47Tag(Set<RepositoryLocale> repositoryLocales) {
    Optional<RepositoryLocale> sourceLocaleBcp47Tag =
        repositoryLocales.stream()
            .filter(repositoryLocale -> repositoryLocale.getParentLocale() == null)
            .findFirst();
    if (sourceLocaleBcp47Tag.isPresent()) {
      return sourceLocaleBcp47Tag.get().getLocale().getBcp47Tag();
    }
    throw new EvolveSyncException("The repository does not have a source locale");
  }

  private String getTargetLocaleBcp47Tag(
      Map<String, String> localeMappings, List<RepositoryLocale> targetRepositoryLocales) {
    if (!targetRepositoryLocales.isEmpty()) {
      String firstLocaleBcp47Tag = targetRepositoryLocales.getFirst().getLocale().getBcp47Tag();
      return localeMappings.getOrDefault(firstLocaleBcp47Tag, firstLocaleBcp47Tag);
    }
    throw new EvolveSyncException("The repository does not have a target locale");
  }

  private Set<String> getAdditionalTargetLocaleBcp47Tags(
      Map<String, String> localeMappings, List<RepositoryLocale> targetRepositoryLocales) {
    if (targetRepositoryLocales.size() > 1) {
      return targetRepositoryLocales.subList(1, targetRepositoryLocales.size()).stream()
          .map(RepositoryLocale::getLocale)
          .map(Locale::getBcp47Tag)
          .map(bcp47Tag -> localeMappings.getOrDefault(bcp47Tag, bcp47Tag))
          .collect(Collectors.toSet());
    } else {
      return ImmutableSet.of();
    }
  }

  /**
   * Fetches localized courses for the provided locales and keeps only courses whose equivalent
   * parent belongs to the provided parent course IDs.
   *
   * <p>Each locale request is retried using the configured backoff policy before failing.
   *
   * @param parentCourseIds set of source/parent course IDs used to filter localized courses
   * @param localeBcp47Tags set of locales to query from Evolve
   * @param startDateTime timestamp used by Evolve to scope the course query
   * @return flattened list of localized courses matching the requested parent course IDs across all
   *     requested locales
   */
  private List<CourseDTO> getCourses(
      Set<Integer> parentCourseIds, Set<String> localeBcp47Tags, ZonedDateTime startDateTime) {
    return localeBcp47Tags.stream()
        .map(
            localeBcp47Tag ->
                Mono.fromCallable(
                        () ->
                            this.evolveClient
                                .getCourses(new CoursesGetRequest(localeBcp47Tag, startDateTime))
                                .filter(
                                    courseDTO ->
                                        courseDTO.getEquivalentParent() != null
                                            && parentCourseIds.contains(
                                                courseDTO.getEquivalentParent().getId()))
                                .collect(Collectors.toList()))
                    .retryWhen(
                        Retry.backoff(
                                this.evolveConfigurationProperties.getMaxRetries(),
                                this.retryMinBackoff)
                            .maxBackoff(this.retryMaxBackoff))
                    .doOnError(
                        e -> log.error("Unable to fetch courses for locale: {}", localeBcp47Tag, e))
                    .block())
        .flatMap(List::stream)
        .toList();
  }

  /**
   * Fetches localized courses for the provided parent course IDs and groups them by parent course
   * ID.
   *
   * <p>For each parent course group, duplicate localized courses for the same locale are
   * de-duplicated by keeping the course with the highest course ID.
   *
   * @param parentCourseIds set of source/parent course IDs to match against
   * @param localeBcp47Tags set of locales to fetch from Evolve
   * @param startDateTime timestamp used by Evolve to scope the course query
   * @return map keyed by parent course ID with one course per locale (latest by ID)
   */
  private Map<Integer, List<CourseDTO>> getCoursesByParentCourseId(
      Set<Integer> parentCourseIds, Set<String> localeBcp47Tags, ZonedDateTime startDateTime) {
    return this.getCourses(parentCourseIds, localeBcp47Tags, startDateTime).stream()
        .collect(
            Collectors.groupingBy(
                courseDTO -> courseDTO.getEquivalentParent().getId(),
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    courses ->
                        courses.stream()
                            .collect(
                                Collectors.toMap(
                                    CourseDTO::getLocale,
                                    c -> c,
                                    (c1, c2) -> c1.getId() >= c2.getId() ? c1 : c2))
                            .values()
                            .stream()
                            .toList())));
  }

  public void sync(Long repositoryId, String localeMapping) {
    Optional<Repository> repositoryOptional = this.repositoryRepository.findById(repositoryId);
    if (repositoryOptional.isEmpty()) {
      throw new EvolveSyncException("No repository found with ID: " + repositoryId);
    }
    Repository repository = repositoryOptional.get();
    Map<String, String> localeMappings =
        ofNullable(this.localeMappingHelper.getLocaleMapping(localeMapping))
            .orElse(ImmutableMap.of());
    String sourceLocaleBcp47Tag = this.getSourceLocaleBcp47Tag(repository.getRepositoryLocales());
    List<RepositoryLocale> targetRepositoryLocales =
        repository.getRepositoryLocales().stream()
            .filter(repositoryLocale -> repositoryLocale.getParentLocale() != null)
            .toList();
    String targetLocaleBcp47Tag =
        this.getTargetLocaleBcp47Tag(localeMappings, targetRepositoryLocales);
    Set<String> additionalTargetLocaleBcp47Tags =
        this.getAdditionalTargetLocaleBcp47Tags(localeMappings, targetRepositoryLocales);
    Set<String> localeBcp47Tags = new HashSet<>(additionalTargetLocaleBcp47Tags);
    localeBcp47Tags.add(targetLocaleBcp47Tag);
    ZonedDateTime startDateTime = ZonedDateTime.now();
    CoursesGetRequest request = new CoursesGetRequest(sourceLocaleBcp47Tag, startDateTime);
    List<CourseDTO> sourceCourses =
        this.evolveClient
            .getCourses(request)
            .filter(
                courseDTO ->
                    courseDTO.getTranslationStatus() == READY_FOR_TRANSLATION
                        || courseDTO.getTranslationStatus() == IN_TRANSLATION)
            .toList();
    Set<Integer> sourceCourseIds =
        sourceCourses.stream().map(CourseDTO::getId).collect(Collectors.toSet());
    Map<Integer, List<CourseDTO>> localizedCoursesByParentCourseId =
        this.getCoursesByParentCourseId(sourceCourseIds, localeBcp47Tags, startDateTime);
    int failureCount = 0;
    for (CourseDTO courseDTO : sourceCourses) {
      try {
        if (courseDTO.getTranslationStatus() == READY_FOR_TRANSLATION) {
          this.syncReadyForTranslation(
              courseDTO,
              startDateTime,
              repository.getId(),
              targetLocaleBcp47Tag,
              additionalTargetLocaleBcp47Tags,
              localizedCoursesByParentCourseId);
        } else if (courseDTO.getTranslationStatus() == IN_TRANSLATION) {
          this.syncInTranslation(
              courseDTO, startDateTime, repository, localeMappings, targetRepositoryLocales);
        }
      } catch (Exception e) {
        log.error("Course sync failed: {}", courseDTO.getId(), e);
        failureCount++;
      }
    }
    if (failureCount > 0) {
      throw new EvolveSyncException(String.format("%d course(s) failed to sync", failureCount));
    }
  }
}
