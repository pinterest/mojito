package com.box.l10n.mojito.service.appender;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.BaseEntity;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.rest.asset.LocalizedAssetBody;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticService;
import com.box.l10n.mojito.service.pushrun.PushRunRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AssetAppenderService {

  static Logger logger = LoggerFactory.getLogger(AssetAppenderService.class);

  private final int HARD_TEXT_UNIT_LIMIT = 5;

  private final AssetAppenderFactory assetAppenderFactory;
  private final BranchRepository branchRepository;
  private final BranchStatisticService branchStatisticService;
  private final PushRunRepository pushRunRepository;
  private final AppendedAssetBlobStorage appendedAssetBlobStorage;

  @Autowired
  public AssetAppenderService(
      AssetAppenderFactory assetAppenderFactory,
      BranchRepository branchRepository,
      BranchStatisticService branchStatisticService,
      PushRunRepository pushRunRepository,
      AppendedAssetBlobStorage appendedAssetBlobStorage) {
    this.assetAppenderFactory = assetAppenderFactory;
    this.branchRepository = branchRepository;
    this.branchStatisticService = branchStatisticService;
    this.pushRunRepository = pushRunRepository;
    this.appendedAssetBlobStorage = appendedAssetBlobStorage;
  }

  /**
   * Fetches text units under qualifying branches for appending, passes them through the equivalent
   * appender and returns the source asset.
   *
   * @param asset
   * @param localizedAssetBody
   * @return Source content with text units appended from translated branches under the Assets
   *     repository.
   */
  public String appendBranchTextUnitsToSource(
      Asset asset, LocalizedAssetBody localizedAssetBody, String sourceContent) {

    String extension = FilenameUtils.getExtension(asset.getPath()).toLowerCase();
    Optional<AbstractAssetAppender> assetAppender =
        assetAppenderFactory.fromExtension(extension, sourceContent);

    if (assetAppender.isEmpty()) {
      logger.error(
          "Attempted to append branch text units to a source asset with an extension that did not map to a valid asset appender. Returning the original source content instead.");
      return sourceContent;
    }

    AbstractAssetAppender appender = assetAppender.get();

    HashSet<Long> lastPushedTextUnits =
        new HashSet<>(
            pushRunRepository.getAllTextUnitIdsFromLastPushRunByRepositoryId(
                asset.getRepository().getId()));

    List<Branch> branchesToAppend =
        branchRepository.findBranchesForAppending(asset.getRepository(), "master");

    List<Branch> appendedBranches = new ArrayList<>();

    int currentAppendCount = 0;
    for (Branch branch : branchesToAppend) {
      List<TextUnitDTO> textUnitsToAppend = branchStatisticService.getTextUnitDTOsForBranch(branch);
      if (currentAppendCount + textUnitsToAppend.size() > HARD_TEXT_UNIT_LIMIT) {
        logger.error("TODO: Log errors, emit metrics");
        break;
      }

      textUnitsToAppend =
          textUnitsToAppend.stream()
              .filter(tu -> !lastPushedTextUnits.contains(tu.getTmTextUnitId()))
              .toList();

      appender.appendTextUnits(textUnitsToAppend);
      currentAppendCount += textUnitsToAppend.size();
      appendedBranches.add(branch);
    }

    appendedBranches.forEach(
        b -> {
          logger.info("Appended branch {}", b.getName());
        });

    String appendedAssetContent = appender.getAssetContent();

    appendedAssetBlobStorage.saveAppendedSource(
        localizedAssetBody.getAppendBranchTextUnitsId(), appendedAssetContent);

    appendedAssetBlobStorage.saveAppendedBranches(
        localizedAssetBody.getAppendBranchTextUnitsId(),
        appendedBranches.stream().map(BaseEntity::getId).toList());

    return appendedAssetContent;
  }
}
