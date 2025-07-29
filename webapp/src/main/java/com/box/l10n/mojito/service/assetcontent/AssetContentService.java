package com.box.l10n.mojito.service.assetcontent;

import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionByBranchRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetTextUnitToTMTextUnitRepository;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.branch.BranchService;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Service to manage {@link AssetContent}.
 *
 * <p>An {@link AssetContent} is linked to a branch. It is possible that the system is used without
 * specifying branch name (eg. in the CLI). If that's the case, the asset content is linked to
 * branch with name: null.
 *
 * <p>We don't use {@link AssetContentRepository} (not public) is not used directly to avoid badly
 * handling the branch with null name.
 *
 * @author jeanaurambault
 */
@Service
public class AssetContentService {

  /** logger */
  static Logger logger = getLogger(AssetContentService.class);

  private final BranchService branchService;

  private final AssetContentRepository assetContentRepository;

  private final Optional<ContentService> contentService;

  private final AssetExtractionRepository assetExtractionRepository;

  private final AssetExtractionByBranchRepository assetExtractionByBranchRepository;

  private final AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository;

  private final AssetTextUnitRepository assetTextUnitRepository;

  @Autowired
  public AssetContentService(
      BranchService branchService,
      AssetContentRepository assetContentRepository,
      @Autowired(required = false) ContentService contentService,
      AssetExtractionRepository assetExtractionRepository,
      AssetExtractionByBranchRepository assetExtractionByBranchRepository,
      AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository,
      AssetTextUnitRepository assetTextUnitRepository) {
    this.branchService = branchService;
    this.assetContentRepository = assetContentRepository;
    this.contentService = ofNullable(contentService);
    this.assetExtractionRepository = assetExtractionRepository;
    this.assetExtractionByBranchRepository = assetExtractionByBranchRepository;
    this.assetTextUnitToTMTextUnitRepository = assetTextUnitToTMTextUnitRepository;
    this.assetTextUnitRepository = assetTextUnitRepository;
  }

  /**
   * Creates an {@link AssetContent} with no branch name specified. This will create a {@link
   * Branch} with branch name: null.
   *
   * @param asset
   * @param content
   * @return
   */
  public AssetContent createAssetContent(Asset asset, String content) {
    Branch branch =
        branchService.getUndeletedOrCreateBranch(asset.getRepository(), null, null, null, null);
    return createAssetContent(asset, content, false, branch);
  }

  /**
   * Creates an {@link AssetContent} for a given {@link Branch}.
   *
   * @param asset
   * @param content
   * @param extractedContent
   * @param branch
   * @return
   */
  public AssetContent createAssetContent(
      Asset asset, String content, boolean extractedContent, Branch branch) {
    logger.debug(
        "Create asset content for asset id: {} and branch id: {}", asset.getId(), branch.getId());
    AssetContent assetContent = new AssetContent();

    assetContent.setAsset(asset);
    assetContent.setContent(this.contentService.isPresent() ? "" : content);
    assetContent.setContentMd5(DigestUtils.md5Hex(content));
    assetContent.setBranch(branch);
    assetContent.setExtractedContent(extractedContent);

    assetContent = this.assetContentRepository.save(assetContent);
    if (this.contentService.isPresent()) {
      this.contentService.get().setContent(assetContent, content);
    }

    return assetContent;
  }

  /**
   * Proxy {@link AssetContentRepository#findById(Long)} ()}.
   *
   * @param id {@link com.box.l10n.mojito.entity.AssetContent#getId()}
   * @return the asset content
   */
  public AssetContent findOne(Long id) {
    Optional<AssetContent> optionalAssetContent = this.assetContentRepository.findById(id);
    if (optionalAssetContent.isPresent() && this.contentService.isPresent()) {
      AssetContent assetContent = optionalAssetContent.get();
      Optional<String> content = this.contentService.get().getContent(assetContent);
      if (content.isPresent()) {
        assetContent.setContent(content.get());
      } else {
        throw new ContentNotFoundException(
            "No content found for asset content: " + assetContent.getId());
      }
    }

    return optionalAssetContent.orElse(null);
  }

  private void deleteAssetTextUnits(long assetExtractionId, int batchSize) {
    List<Long> assetTextUnitIdsToDelete;
    do {
      assetTextUnitIdsToDelete =
          this.assetTextUnitRepository.findByAssetExtractionId(assetExtractionId, batchSize);
      for (Long assetTextUnitIdToDelete : assetTextUnitIdsToDelete) {
        this.assetTextUnitToTMTextUnitRepository.deleteByAssetTextUnitId(assetTextUnitIdToDelete);
        this.assetTextUnitRepository.deleteById(assetTextUnitIdToDelete);
      }
    } while (!assetTextUnitIdsToDelete.isEmpty());
  }

  private void deleteAssetContent(Long assetContentIdToDelete, int batchSize) {
    List<AssetExtraction> assetExtractions =
        this.assetExtractionRepository.findByAssetContentId(assetContentIdToDelete);
    assetExtractions.forEach(
        assetExtraction -> {
          long assetExtractionId = assetExtraction.getId();
          this.deleteAssetTextUnits(assetExtractionId, batchSize);
          int deleteCount =
              this.assetExtractionByBranchRepository.deleteByAssetExtractionId(assetExtractionId);
          logger.debug("Deleted {} Asset Extraction by Branch rows", deleteCount);
          this.assetExtractionRepository.deleteById(assetExtractionId);
        });
    logger.debug("Delete Asset Content with ID: {}", assetContentIdToDelete);
    this.assetContentRepository.deleteById(assetContentIdToDelete);
  }

  public void cleanAssetContentData(Period retentionPeriod, int batchSize) {
    List<Long> assetContentIdsToDelete;
    PageRequest pageable = PageRequest.of(0, batchSize);
    ZonedDateTime beforeDate = ZonedDateTime.now().minus(retentionPeriod);
    do {
      assetContentIdsToDelete =
          this.assetContentRepository.findStaleAssetContent(beforeDate, pageable);
      for (long assetContentIdToDelete : assetContentIdsToDelete) {
        this.deleteAssetContent(assetContentIdToDelete, batchSize);
      }
    } while (!assetContentIdsToDelete.isEmpty());
  }
}
