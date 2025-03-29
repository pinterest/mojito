package com.box.l10n.mojito.service;

import static com.box.l10n.mojito.evolve.TranslationStatusType.IN_TRANSLATION;
import static com.box.l10n.mojito.evolve.TranslationStatusType.READY_FOR_TRANSLATION;
import static com.box.l10n.mojito.evolve.TranslationStatusType.TRANSLATED;
import static java.util.Optional.ofNullable;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.AssetExtractionByBranch;
import com.box.l10n.mojito.entity.AuditableEntity;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.entity.Locale;
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
import com.box.l10n.mojito.service.asset.AssetService;
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
import org.hibernate.Hibernate;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class EvolveService {
  private ZonedDateTime earliestUpdatedOn;

  private ZonedDateTime currentEarliestUpdatedOn;

  private final EvolveConfigurationProperties evolveConfigurationProperties;

  private final RepositoryService repositoryService;

  private final EvolveClient evolveClient;

  private final AssetService assetService;

  private final PollableTaskService pollableTaskService;

  private final XliffUtils xliffUtils;

  private Repository repository;

  private String sourceLocaleBcp47Tag;

  private String targetLocaleBcp47Tag;

  private Set<String> additionalTargetLocaleBcp47Tags;

  private final BranchRepository branchRepository;

  private final BranchStatisticRepository branchStatisticRepository;

  private final AssetContentRepository assetContentRepository;

  private final TMService tmService;

  private final BranchService branchService;

  private final Long timeout = 3600L;

  private final EvolveService2 evolveService2;

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
      EvolveService2 evolveService2) {
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
    this.evolveService2 = evolveService2;
  }

  private void setSourceLocaleBcp47Tag() {
    Locale sourceLocale = this.repository.getSourceLocale();
    if (sourceLocale != null) {
      this.sourceLocaleBcp47Tag = sourceLocale.getBcp47Tag();
    } else {
      throw new RuntimeException("");
    }
  }

  private void setTargetLocaleBcp47Tags() {
    List<String> targetLocales =
        this.repository.getRepositoryLocales().stream()
            .filter(repositoryLocale -> repositoryLocale.getParentLocale() != null)
            .map(RepositoryLocale::getLocale)
            .map(Locale::getBcp47Tag)
            .toList();
    if (!targetLocales.isEmpty()) {
      this.targetLocaleBcp47Tag = targetLocales.getFirst();
      this.additionalTargetLocaleBcp47Tags =
          new HashSet<>(targetLocales.subList(1, targetLocales.size()));
    } else {
      throw new RuntimeException("");
    }
  }

  private SourceAsset importSourceAsset(SourceAsset sourceAsset) throws Throwable {
    Preconditions.checkNotNull(sourceAsset);

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
            sourceAsset.getFilterOptions());

    try {
      sourceAsset.setAddedAssetId(assetFuture.get().getId());
    } catch (ExecutionException ee) {
      throw ee.getCause();
    }

    sourceAsset.setPollableTask(assetFuture.getPollableTask());
    return sourceAsset;
  }

  private void startCourseTranslations(int courseId) throws Throwable {
    String localizedAssetContent =
        this.evolveClient.startCourseTranslation(
            courseId, this.targetLocaleBcp47Tag, this.additionalTargetLocaleBcp47Tags);

    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setBranch("course_" + courseId);
    sourceAsset.setRepositoryId(this.repository.getId());
    sourceAsset.setPath(courseId + ".xliff");
    sourceAsset.setContent(
        this.xliffUtils.removeAttribute(localizedAssetContent, "target-language"));
    sourceAsset = this.importSourceAsset(sourceAsset);

    this.pollableTaskService.waitForPollableTask(
        sourceAsset.getPollableTask().getId(), timeout * 1000, 10000);
  }

  private void updateCourse(CourseDTO courseDTO, ZonedDateTime currentDateTime) {
    Preconditions.checkNotNull(courseDTO);

    this.evolveClient.updateCourse(
        courseDTO.getId(), courseDTO.getTranslationStatus(), currentDateTime);
  }

  private void importSourceAssetToMaster(CourseDTO courseDTO, long branchId) {
    List<Asset> assets =
        this.assetService.findAll(
            this.repository.getId(), courseDTO.getId() + ".xliff", false, false, branchId);
    if (assets.isEmpty()) {
      throw new RuntimeException("Course with id " + courseDTO.getId() + " not found");
    }
    List<AssetContent> assetContents =
        this.assetContentRepository.findByAssetRepositoryIdAndBranchName(
            this.repository.getId(), "course_" + courseDTO.getId());
    AssetContent assetContent = assetContents.stream().max(Comparator.comparing(AuditableEntity::getLastModifiedDate))
        .orElse(null);
    /*AssetContent assetContent =
        assetContents.stream()
            .filter(
                content ->
                  this.assetContentRepository.getAssetExtractionsByAssetContentId(content.getId()).stream()
                        .anyMatch(
                            assetExtraction ->
                                Objects.equals(
                                    assetExtraction.getId(),
                                    assets.getFirst().getLastSuccessfulAssetExtraction().getId()))
            )
            .findFirst()
            .orElse(null);*/
    /*AssetContent assetContent =
        assetContents.stream()
            .filter(
                content ->
                    this.evolveService2.getAssetExtractions(content).stream()
                        .anyMatch(
                            assetExtraction ->
                                Objects.equals(
                                    assetExtraction.getId(),
                                    assets.getFirst().getLastSuccessfulAssetExtraction().getId())))
            .findFirst()
            .orElse(null);*/
    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setRepositoryId(this.repository.getId());
    sourceAsset.setPath(courseDTO.getId() + ".xliff");
    sourceAsset.setContent(assetContent.getContent());
    try {
      sourceAsset = this.importSourceAsset(sourceAsset);
      this.pollableTaskService.waitForPollableTask(sourceAsset.getPollableTask().getId());
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  private void updateCourseTranslations(int courseId, long branchId) {
    Asset asset =
        this.assetService
            .findAll(this.repository.getId(), courseId + ".xliff", false, false, branchId)
            .getFirst();
    List<AssetContent> assetContents =
        this.assetContentRepository.findByAssetRepositoryIdAndBranchName(
            this.repository.getId(), "course_" + courseId);
    //String contentMd5 = asset.getLastSuccessfulAssetExtraction().getContentMd5();
    //System.out.println(contentMd5);
    AssetContent assetContent =
        assetContents.stream().max(Comparator.comparing(AuditableEntity::getLastModifiedDate))
            .orElse(null);
    /*AssetContent assetContent =
        assetContents.stream()
            .filter(
                content ->
                  this.assetContentRepository.getAssetExtractionsByAssetContentId(content.getId()).stream()
                        .anyMatch(
                            assetExtraction ->
                                Objects.equals(
                                    assetExtraction.getId(),
                                    asset.getLastSuccessfulAssetExtraction().getId()))
                )
            .findFirst()
            .orElse(null);*/
    /*AssetContent assetContent =
        assetContents.stream()
            .filter(
                content ->
                    this.evolveService2.getAssetExtractions(content).stream()
                        .anyMatch(
                            assetExtraction ->
                                Objects.equals(
                                    assetExtraction.getId(),
                                    asset.getLastSuccessfulAssetExtraction().getId())))
            .findFirst()
            .orElse(null);*/
    String normalizedContent = NormalizationUtils.normalize(assetContent.getContent());
    this.repository.getRepositoryLocales().stream()
        .filter(repositoryLocale -> repositoryLocale.getParentLocale() != null)
        .forEach(
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

  public void sync() {
    List<Repository> repositories =
        this.repositoryService.findRepositoriesIsNotDeletedOrderByName(
            this.evolveConfigurationProperties.getRepositoryName());
    if (repositories.isEmpty()) {
      throw new RuntimeException(
          "No repositories found for name: "
              + this.evolveConfigurationProperties.getRepositoryName());
    }
    this.repository = repositories.getFirst();
    this.setSourceLocaleBcp47Tag();
    this.setTargetLocaleBcp47Tags();

    ZonedDateTime startDateTime = ZonedDateTime.now();
    CoursesGetRequest request =
        new CoursesGetRequest(this.sourceLocaleBcp47Tag, this.earliestUpdatedOn, startDateTime);
    this.evolveClient
        .getCourses(request)
        .forEach(
            courseDTO -> {
              if (courseDTO.getTranslationStatus() == READY_FOR_TRANSLATION) {
                try {
                  this.startCourseTranslations(courseDTO.getId());
                  courseDTO.setTranslationStatus(IN_TRANSLATION);
                  this.updateCourse(courseDTO, startDateTime);
                  if (this.currentEarliestUpdatedOn == null
                      || this.currentEarliestUpdatedOn.isAfter(courseDTO.getUpdatedOn())) {
                    this.currentEarliestUpdatedOn = courseDTO.getUpdatedOn();
                  }
                } catch (Throwable e) {
                  throw new RuntimeException(e);
                }
              } else if (courseDTO.getTranslationStatus() == IN_TRANSLATION) {
                Branch branch =
                    this.branchRepository.findByNameAndRepository(
                        "course_" + courseDTO.getId(), this.repository);
                BranchStatistic branchStatistic =
                    this.branchStatisticRepository.findByBranch(branch);
                if (branchStatistic.getTotalCount() > 0) {
                  if (branchStatistic.getForTranslationCount() == 0) {
                    this.updateCourseTranslations(courseDTO.getId(), branch.getId());
                    this.importSourceAssetToMaster(courseDTO, branch.getId());
                    this.deleteBranch(branch.getId());
                    courseDTO.setTranslationStatus(TRANSLATED);
                    this.updateCourse(courseDTO, startDateTime);
                  } else {
                    if (this.currentEarliestUpdatedOn == null
                        || this.currentEarliestUpdatedOn.isAfter(courseDTO.getUpdatedOn())) {
                      this.currentEarliestUpdatedOn = courseDTO.getUpdatedOn();
                    }
                  }
                } else {
                  this.deleteBranch(branch.getId());
                }
              }
            });
    this.earliestUpdatedOn =
        ofNullable(this.currentEarliestUpdatedOn).orElse(this.earliestUpdatedOn);
  }

  @VisibleForTesting
  public ZonedDateTime getEarliestUpdatedOn() {
    return earliestUpdatedOn;
  }
}
