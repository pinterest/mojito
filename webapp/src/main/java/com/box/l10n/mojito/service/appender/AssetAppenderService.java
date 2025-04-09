package com.box.l10n.mojito.service.appender;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.BaseEntity;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.rest.asset.LocalizedAssetBody;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticService;
import com.box.l10n.mojito.service.pushrun.PushRunRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
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
  private final ObjectMapper objectMapper;
  private final MeterRegistry meterRegistry;

  @Autowired
  public AssetAppenderService(
      AssetAppenderFactory assetAppenderFactory,
      BranchRepository branchRepository,
      BranchStatisticService branchStatisticService,
      PushRunRepository pushRunRepository,
      AppendedAssetBlobStorage appendedAssetBlobStorage,
      ObjectMapper objectMapper,
      MeterRegistry meterRegistry) {
    this.assetAppenderFactory = assetAppenderFactory;
    this.branchRepository = branchRepository;
    this.branchStatisticService = branchStatisticService;
    this.pushRunRepository = pushRunRepository;
    this.appendedAssetBlobStorage = appendedAssetBlobStorage;
    this.objectMapper = objectMapper;
    this.meterRegistry = meterRegistry;
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
          "Attempted to append branch text units to a source asset with an extension that did not map to a valid asset appender. No appending has taken place, returning original source asset.");
      return sourceContent;
    }

    AbstractAssetAppender appender = assetAppender.get();

    HashSet<Long> lastPushRunTextUnits =
        new HashSet<>(
            pushRunRepository.getAllTextUnitIdsFromLastPushRunByRepositoryId(
                asset.getRepository().getId()));

    // Fetch all branches to be appended, sorted by translated date in ascending order to ensure
    // older translated branches make it in
    List<Branch> branchesToAppend =
        branchRepository.findBranchesForAppending(asset.getRepository());

    List<Branch> appendedBranches = new ArrayList<>();
    int appendedCount = 0;
    for (Branch branch : branchesToAppend) {
      List<TextUnitDTO> textUnitsToAppend = branchStatisticService.getTextUnitDTOsForBranch(branch);
      // Are we going to go over the hard limit ? If yes emit metrics and break out.
      if (appendedCount + textUnitsToAppend.size() > HARD_TEXT_UNIT_LIMIT) {
        int countFailedToAppend =
            branchesToAppend.stream()
                .skip(appendedBranches.size())
                .map(b -> branchStatisticService.getTextUnitDTOsForBranch(b).size())
                .mapToInt(Integer::intValue)
                .sum();

        logger.warn(
            "Asset text unit appending limit reached for asset '{}' in repository '{}'. A total of {}/{} branches were appended to the asset. '{}' text units were appended to the asset successfully, whilst '{}' text units were not appended. All branch ids to be appended: {}",
            asset.getPath(),
            asset.getRepository().getName(),
            appendedBranches.size(),
            branchesToAppend.size(),
            appendedCount,
            countFailedToAppend,
            objectMapper.writeValueAsStringUnchecked(
                branchesToAppend.stream().map(BaseEntity::getId).toList()));

        // Log the amount of text units we missed
        meterRegistry
            .counter(
                "AssetAppenderService.appendBranchTextUnitsToSource.exceededAppendLimitCount",
                Tags.of(
                    "repository",
                    asset.getRepository().getName(),
                    "asset",
                    asset.getPath(),
                    "jobId",
                    localizedAssetBody.getAppendBranchTextUnitsId()))
            .increment(countFailedToAppend);
        break;
      }

      // Filter the text units to remove the ones in the last push run
      textUnitsToAppend =
          textUnitsToAppend.stream()
              .filter(tu -> !lastPushRunTextUnits.contains(tu.getTmTextUnitId()))
              .toList();

      // Append the text units
      appender.appendTextUnits(textUnitsToAppend);

      appendedCount += textUnitsToAppend.size();
      appendedBranches.add(branch);
    }

    String appendedAssetContent = appender.getAssetContent();

    saveResults(
        localizedAssetBody.getAppendBranchTextUnitsId(), appendedAssetContent, appendedBranches);

    meterRegistry
        .counter(
            "AssetAppenderService.appendBranchTextUnitsToSource.appendCount",
            Tags.of(
                "repository",
                asset.getRepository().getName(),
                "asset",
                asset.getPath(),
                "jobId",
                localizedAssetBody.getAppendBranchTextUnitsId()))
        .increment(appendedCount);

    return appendedAssetContent;
  }

  private void saveResults(
      String appendJobId, String appendedSourceContent, List<Branch> appendedBranches) {
    // Save the source content to blob storage
    appendedAssetBlobStorage.saveAppendedSource(appendJobId, appendedSourceContent);

    // Save branches to blob storage, will be serialized to a JSON string
    appendedAssetBlobStorage.saveAppendedBranches(
        appendJobId, appendedBranches.stream().map(BaseEntity::getId).toList());
  }
}
