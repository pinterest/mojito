package com.box.l10n.mojito.service.evolve;

import static com.box.l10n.mojito.service.evolve.dto.TranslationStatusType.IN_TRANSLATION;
import static com.box.l10n.mojito.service.evolve.dto.TranslationStatusType.READY_FOR_TRANSLATION;
import static com.box.l10n.mojito.service.evolve.dto.TranslationStatusType.TRANSLATED;
import static com.box.l10n.mojito.service.security.user.UserService.SYSTEM_USERNAME;
import static java.util.Optional.ofNullable;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.AssetExtractionByBranch;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchStatistic;
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
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchService;
import com.box.l10n.mojito.service.branch.BranchStatisticRepository;
import com.box.l10n.mojito.service.evolve.dto.CourseDTO;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.xliff.XliffUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.xml.sax.SAXException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public class EvolveService {
  private List<RepositoryLocale> targetRepositoryLocales;

  private String sourceLocaleBcp47Tag;

  private String targetLocaleBcp47Tag;

  private Set<String> additionalTargetLocaleBcp47Tags;

  private ZonedDateTime earliestUpdatedOn;

  private Repository repository;

  private final RepositoryService repositoryService;

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

  private final Long timeout = 3600L;

  private final AssetExtractionByBranchRepository assetExtractionByBranchRepository;

  private final SyncDateService syncDateService;

  public EvolveService(
      EvolveConfigurationProperties evolveConfigurationProperties,
      RepositoryService repositoryService,
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
      SyncDateService syncDateService) {
    this.evolveConfigurationProperties = evolveConfigurationProperties;
    this.repositoryService = repositoryService;
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
    this.syncDateService = syncDateService;
  }

  private void setLocales() {
    Preconditions.checkNotNull(this.repository);

    this.targetRepositoryLocales = new ArrayList<>();
    this.additionalTargetLocaleBcp47Tags = new HashSet<>();
    for (RepositoryLocale repositoryLocale : this.repository.getRepositoryLocales()) {
      String localeBcp47Tag = repositoryLocale.getLocale().getBcp47Tag();
      if (repositoryLocale.getParentLocale() == null) {
        this.sourceLocaleBcp47Tag = localeBcp47Tag;
      } else {
        this.targetRepositoryLocales.add(repositoryLocale);
        if (this.targetLocaleBcp47Tag == null) {
          this.targetLocaleBcp47Tag = localeBcp47Tag;
        } else {
          this.additionalTargetLocaleBcp47Tags.add(localeBcp47Tag);
        }
      }
    }
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
            sourceAsset.getFilterOptions());
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

  private void importSourceAsset(int courseId, String localizedAssetContent)
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
    sourceAsset.setRepositoryId(this.repository.getId());
    sourceAsset.setPath(this.getAssetPath(courseId));
    sourceAsset.setBranchCreatedByUsername(SYSTEM_USERNAME);
    sourceAsset.setContent(
        this.xliffUtils.removeAttribute(localizedAssetContent, "target-language"));
    this.pollableTaskService.waitForPollableTask(
        this.importSourceAsset(sourceAsset).getPollableTask().getId(), timeout * 1000, 10000);
  }

  private int getMaxRetries() {
    return ofNullable(this.evolveConfigurationProperties.getMaxRetries()).orElse(2);
  }

  private Duration getRetryMinBackoff() {
    return ofNullable(this.evolveConfigurationProperties.getRetryMinBackoffSecs())
        .map(Duration::ofSeconds)
        .orElse(Duration.ofSeconds(2));
  }

  private Duration getRetryMaxBackoff() {
    return ofNullable(this.evolveConfigurationProperties.getRetryMaxBackoffSecs())
        .map(Duration::ofSeconds)
        .orElse(Duration.ofSeconds(30));
  }

  private void startCourseTranslations(int courseId)
      throws XPathExpressionException,
          UnsupportedAssetFilterTypeException,
          ParserConfigurationException,
          IOException,
          ExecutionException,
          InterruptedException,
          TransformerException,
          SAXException {
    String localizedAssetContent =
        Mono.fromCallable(
                () ->
                    this.evolveClient.startCourseTranslation(
                        courseId, this.targetLocaleBcp47Tag, this.additionalTargetLocaleBcp47Tags))
            .retryWhen(
                Retry.backoff(this.getMaxRetries(), this.getRetryMinBackoff())
                    .maxBackoff(this.getRetryMaxBackoff()))
            .doOnError(
                e -> {
                  throw new RuntimeException("Error while starting a course translation", e);
                })
            .block();
    this.importSourceAsset(courseId, localizedAssetContent);
  }

  private void updateCourse(CourseDTO courseDTO, ZonedDateTime currentDateTime) {
    Preconditions.checkNotNull(courseDTO);

    Mono.fromRunnable(
            () ->
                this.evolveClient.updateCourse(
                    courseDTO.getId(), courseDTO.getTranslationStatus(), currentDateTime))
        .retryWhen(
            Retry.backoff(this.getMaxRetries(), this.getRetryMinBackoff())
                .maxBackoff(this.getRetryMaxBackoff()))
        .doOnError(
            e -> {
              throw new RuntimeException("Error while updating a course", e);
            })
        .block();
  }

  private void importSourceAssetToMaster(CourseDTO courseDTO, AssetContent assetContent)
      throws UnsupportedAssetFilterTypeException, ExecutionException, InterruptedException {
    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setRepositoryId(this.repository.getId());
    sourceAsset.setPath(this.getAssetPath(courseDTO.getId()));
    sourceAsset.setBranchCreatedByUsername(SYSTEM_USERNAME);
    sourceAsset.setContent(assetContent.getContent());
    this.pollableTaskService.waitForPollableTask(
        this.importSourceAsset(sourceAsset).getPollableTask().getId(), timeout * 1000, 10000);
  }

  private void updateCourseTranslations(int courseId, Asset asset, AssetContent assetContent) {
    String normalizedContent = NormalizationUtils.normalize(assetContent.getContent());
    this.targetRepositoryLocales.forEach(
        repositoryLocale -> {
          String generateLocalized;
          try {
            generateLocalized =
                tmService.generateLocalized(
                    asset,
                    normalizedContent,
                    repositoryLocale,
                    repositoryLocale.getLocale().getBcp47Tag(),
                    null,
                    ImmutableList.of(),
                    Status.ACCEPTED,
                    InheritanceMode.USE_PARENT,
                    null);
          } catch (UnsupportedAssetFilterTypeException e) {
            throw new RuntimeException(e.getMessage(), e);
          }
          Mono.fromRunnable(
                  () -> this.evolveClient.updateCourseTranslation(courseId, generateLocalized))
              .retryWhen(
                  Retry.backoff(this.getMaxRetries(), this.getRetryMinBackoff())
                      .maxBackoff(this.getRetryMaxBackoff()))
              .doOnError(
                  e -> {
                    throw new RuntimeException("Error while updating course translation", e);
                  })
              .block();
        });
  }

  private void deleteBranch(long branchId) throws InterruptedException {
    PollableFuture<Void> pollableFuture =
        this.branchService.asyncDeleteBranch(this.repository.getId(), branchId);
    this.pollableTaskService.waitForPollableTask(
        pollableFuture.getPollableTask().getId(), timeout * 1000, 10000);
  }

  private void setCurrentEarliestUpdatedOn(ZonedDateTime updatedOn) {
    if (this.earliestUpdatedOn == null || this.earliestUpdatedOn.isAfter(updatedOn)) {
      this.earliestUpdatedOn = updatedOn;
    }
  }

  private String getContentMd5(Asset asset, Branch branch) {
    Optional<AssetExtractionByBranch> assetExtractionByBranch =
        assetExtractionByBranchRepository.findByAssetAndBranch(asset, branch);
    if (assetExtractionByBranch.isPresent()) {
      return assetExtractionByBranch.get().getAssetExtraction().getContentMd5();
    } else {
      throw new RuntimeException(
          String.format(
              "Asset Extraction not found for asset ID: %d and branch: %s",
              asset.getId(), branch.getName()));
    }
  }

  private void syncTranslated(CourseDTO courseDTO, Branch branch, ZonedDateTime startDateTime)
      throws UnsupportedAssetFilterTypeException, ExecutionException, InterruptedException {
    Asset asset =
        this.assetService
            .findAll(
                this.repository.getId(),
                this.getAssetPath(courseDTO.getId()),
                false,
                false,
                branch.getId())
            .getFirst();
    List<AssetContent> assetContents =
        this.assetContentRepository.findByAssetRepositoryIdAndBranchName(
            this.repository.getId(), this.getBranchName(courseDTO.getId()));
    Optional<AssetContent> assetContent =
        assetContents.stream()
            .filter(content -> content.getContentMd5().equals(this.getContentMd5(asset, branch)))
            .findFirst();
    if (assetContent.isPresent()) {
      this.updateCourseTranslations(courseDTO.getId(), asset, assetContent.get());
      this.importSourceAssetToMaster(courseDTO, assetContent.get());
      this.deleteBranch(branch.getId());
      courseDTO.setTranslationStatus(TRANSLATED);
      this.updateCourse(courseDTO, startDateTime);
    } else {
      throw new RuntimeException(
          "Couldn't find asset content for course with id: " + courseDTO.getId());
    }
  }

  private void syncReadyForTranslation(CourseDTO courseDTO, ZonedDateTime startDateTime)
      throws XPathExpressionException,
          UnsupportedAssetFilterTypeException,
          ParserConfigurationException,
          IOException,
          ExecutionException,
          InterruptedException,
          TransformerException,
          SAXException {
    this.startCourseTranslations(courseDTO.getId());
    courseDTO.setTranslationStatus(IN_TRANSLATION);
    this.updateCourse(courseDTO, startDateTime);
    this.setCurrentEarliestUpdatedOn(startDateTime);
  }

  private void syncInTranslation(CourseDTO courseDTO, ZonedDateTime startDateTime)
      throws UnsupportedAssetFilterTypeException, ExecutionException, InterruptedException {
    Branch branch =
        this.branchRepository.findByNameAndRepository(
            this.getBranchName(courseDTO.getId()), this.repository);
    BranchStatistic branchStatistic = this.branchStatisticRepository.findByBranch(branch);
    if (branchStatistic == null || branchStatistic.getTotalCount() == 0) {
      this.setCurrentEarliestUpdatedOn(courseDTO.getUpdatedOn());
    } else if (branchStatistic.getTotalCount() > 0) {
      if (branchStatistic.getForTranslationCount() == 0) {
        this.syncTranslated(courseDTO, branch, startDateTime);
      } else {
        this.setCurrentEarliestUpdatedOn(courseDTO.getUpdatedOn());
      }
    }
  }

  public void sync() {
    List<Repository> repositories =
        this.repositoryService.findRepositoriesIsNotDeletedOrderByName(
            this.evolveConfigurationProperties.getRepositoryName());
    if (repositories.isEmpty()) {
      throw new RuntimeException(
          "No repository found with name: "
              + this.evolveConfigurationProperties.getRepositoryName());
    }
    this.repository = repositories.getFirst();
    this.setLocales();
    ZonedDateTime startDateTime = ZonedDateTime.now();
    CoursesGetRequest request =
        new CoursesGetRequest(
            this.sourceLocaleBcp47Tag, this.syncDateService.getDate(), startDateTime);
    this.evolveClient
        .getCourses(request)
        .forEach(
            courseDTO -> {
              try {
                if (courseDTO.getTranslationStatus() == READY_FOR_TRANSLATION) {
                  this.syncReadyForTranslation(courseDTO, startDateTime);
                } else if (courseDTO.getTranslationStatus() == IN_TRANSLATION) {
                  this.syncInTranslation(courseDTO, startDateTime);
                }
              } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
              }
            });
    this.syncDateService.setDate(ofNullable(this.earliestUpdatedOn).orElse(startDateTime));
  }

  public List<RepositoryLocale> getRepositoryLocales() {
    return this.targetRepositoryLocales;
  }

  public String getSourceLocaleBcp47Tag() {
    return this.sourceLocaleBcp47Tag;
  }

  public String getTargetLocaleBcp47Tag() {
    return this.targetLocaleBcp47Tag;
  }

  public Set<String> getAdditionalTargetLocaleBcp47Tags() {
    return this.additionalTargetLocaleBcp47Tags;
  }
}
