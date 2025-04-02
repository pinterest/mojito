package com.box.l10n.mojito.service.evolve;

import static com.box.l10n.mojito.evolve.TranslationStatusType.IN_TRANSLATION;
import static com.box.l10n.mojito.evolve.TranslationStatusType.READY_FOR_TRANSLATION;
import static com.box.l10n.mojito.evolve.TranslationStatusType.TRANSLATED;
import static com.box.l10n.mojito.service.security.user.UserService.SYSTEM_USERNAME;
import static java.util.Optional.ofNullable;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.AssetExtractionByBranch;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.evolve.CourseDTO;
import com.box.l10n.mojito.evolve.CoursesGetRequest;
import com.box.l10n.mojito.evolve.EvolveClient;
import com.box.l10n.mojito.evolve.EvolveConfigurationProperties;
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
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.xliff.XliffUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class EvolveService {
  private String sourceLocaleBcp47Tag;

  private String targetLocaleBcp47Tag;

  private Set<String> additionalTargetLocaleBcp47Tags;

  private List<RepositoryLocale> targetRepositoryLocales;

  private ZonedDateTime earliestUpdatedOn;

  private ZonedDateTime currentEarliestUpdatedOn;

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
      AssetExtractionByBranchRepository assetExtractionByBranchRepository) {
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
  }

  private void setLocaleBcp47Tags() {
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

  private SourceAsset importSourceAsset(SourceAsset sourceAsset) {
    Preconditions.checkNotNull(sourceAsset);

    String normalizedContent = NormalizationUtils.normalize(sourceAsset.getContent());
    PollableFuture<Asset> assetFuture;
    try {
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
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }

    sourceAsset.setPollableTask(assetFuture.getPollableTask());
    return sourceAsset;
  }

  private String getBranchName(int courseId) {
    return String.format("course_%d", courseId);
  }

  private String getAssetPath(int courseId) {
    return String.format("%d.xliff", courseId);
  }

  private void startCourseTranslations(int courseId) {
    String localizedAssetContent =
        this.evolveClient.startCourseTranslation(
            courseId, this.targetLocaleBcp47Tag, this.additionalTargetLocaleBcp47Tags);
    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setBranch(this.getBranchName(courseId));
    sourceAsset.setRepositoryId(this.repository.getId());
    sourceAsset.setPath(this.getAssetPath(courseId));
    sourceAsset.setBranchCreatedByUsername(SYSTEM_USERNAME);
    try {
      sourceAsset.setContent(
          this.xliffUtils.removeAttribute(localizedAssetContent, "target-language"));
      this.pollableTaskService.waitForPollableTask(
          this.importSourceAsset(sourceAsset).getPollableTask().getId(), timeout * 1000, 10000);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private void updateCourse(CourseDTO courseDTO, ZonedDateTime currentDateTime) {
    Preconditions.checkNotNull(courseDTO);

    this.evolveClient.updateCourse(
        courseDTO.getId(), courseDTO.getTranslationStatus(), currentDateTime);
  }

  private void importSourceAssetToMaster(CourseDTO courseDTO, AssetContent assetContent) {
    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setRepositoryId(this.repository.getId());
    sourceAsset.setPath(this.getAssetPath(courseDTO.getId()));
    sourceAsset.setBranchCreatedByUsername(SYSTEM_USERNAME);
    sourceAsset.setContent(assetContent.getContent());
    try {
      this.pollableTaskService.waitForPollableTask(
          this.importSourceAsset(sourceAsset).getPollableTask().getId());
    } catch (Throwable e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private void updateCourseTranslations(int courseId, Asset asset, AssetContent assetContent) {
    String normalizedContent = NormalizationUtils.normalize(assetContent.getContent());
    this.targetRepositoryLocales.forEach(
        repositoryLocale -> {
          try {
            String generateLocalized =
                tmService.generateLocalized(
                    asset,
                    normalizedContent,
                    repositoryLocale,
                    repositoryLocale.getLocale().getBcp47Tag(),
                    null,
                    List.of(),
                    Status.ACCEPTED,
                    InheritanceMode.USE_PARENT,
                    null);
            this.evolveClient.updateCourseTranslation(courseId, generateLocalized);
          } catch (UnsupportedAssetFilterTypeException e) {
            throw new RuntimeException(e);
          }
        });
  }

  private void deleteBranch(long branchId) {
    PollableFuture<Void> pollableFuture =
        this.branchService.asyncDeleteBranch(this.repository.getId(), branchId);
    try {
      this.pollableTaskService.waitForPollableTask(pollableFuture.getPollableTask().getId());
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void setCurrentEarliestUpdatedOn(ZonedDateTime updatedOn) {
    if (this.currentEarliestUpdatedOn == null || this.currentEarliestUpdatedOn.isAfter(updatedOn)) {
      this.currentEarliestUpdatedOn = updatedOn;
    }
  }

  private String getContentMd5(Asset asset, Branch branch) {
    AssetExtractionByBranch assetExtractionByBranch =
        assetExtractionByBranchRepository.findByAssetAndBranch(asset, branch).get();
    return assetExtractionByBranch.getAssetExtraction().getContentMd5();
  }

  private void syncTranslated(CourseDTO courseDTO, Branch branch, ZonedDateTime startDateTime) {
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

  private void syncReadyForTranslation(CourseDTO courseDTO, ZonedDateTime startDateTime) {
    this.startCourseTranslations(courseDTO.getId());
    courseDTO.setTranslationStatus(IN_TRANSLATION);
    this.updateCourse(courseDTO, startDateTime);
    this.setCurrentEarliestUpdatedOn(courseDTO.getUpdatedOn());
  }

  private void syncInTranslation(CourseDTO courseDTO, ZonedDateTime startDateTime) {
    Branch branch =
        this.branchRepository.findByNameAndRepository(
            this.getBranchName(courseDTO.getId()), this.repository);
    BranchStatistic branchStatistic = this.branchStatisticRepository.findByBranch(branch);
    if (branchStatistic.getTotalCount() > 0) {
      if (branchStatistic.getForTranslationCount() == 0) {
        this.syncTranslated(courseDTO, branch, startDateTime);
      } else {
        this.setCurrentEarliestUpdatedOn(courseDTO.getUpdatedOn());
      }
    } else {
      this.deleteBranch(branch.getId());
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
    this.setLocaleBcp47Tags();
    ZonedDateTime startDateTime = ZonedDateTime.now();
    CoursesGetRequest request =
        new CoursesGetRequest(this.sourceLocaleBcp47Tag, this.earliestUpdatedOn, startDateTime);
    this.evolveClient
        .getCourses(request)
        .forEach(
            courseDTO -> {
              if (courseDTO.getTranslationStatus() == READY_FOR_TRANSLATION) {
                this.syncReadyForTranslation(courseDTO, startDateTime);
              } else if (courseDTO.getTranslationStatus() == IN_TRANSLATION) {
                this.syncInTranslation(courseDTO, startDateTime);
              }
            });
    this.earliestUpdatedOn = ofNullable(this.currentEarliestUpdatedOn).orElse(startDateTime);
  }

  @VisibleForTesting
  public ZonedDateTime getEarliestUpdatedOn() {
    return earliestUpdatedOn;
  }
}
