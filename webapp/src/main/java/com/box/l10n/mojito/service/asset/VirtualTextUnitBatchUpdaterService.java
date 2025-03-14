package com.box.l10n.mojito.service.asset;

import static com.box.l10n.mojito.service.asset.VirtualAssetService.logger;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.AssetTextUnitToTMTextUnit;
import com.box.l10n.mojito.entity.PluralForm;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.service.ai.translation.AITranslationService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.AssetTextUnitToTMTextUnitRepository;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.leveraging.LeveragerByContentForSourceLeveraging;
import com.box.l10n.mojito.service.leveraging.LeveragerByTmTextUnit;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pluralform.PluralFormService;
import com.box.l10n.mojito.service.repository.statistics.RepositoryStatisticsJobScheduler;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.textunitdtocache.TextUnitDTOsCacheService;
import com.box.l10n.mojito.service.tm.textunitdtocache.UpdateType;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author aurambaj
 */
@Component
public class VirtualTextUnitBatchUpdaterService {

  @Autowired AssetExtractionService assetExtractionService;

  @Autowired AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository;

  @Autowired AssetExtractionRepository assetExtractionRepository;

  @Autowired LeveragerByContentForSourceLeveraging leveragerByContentForSourceLeveraging;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired TextUnitUtils textUnitUtils;

  @Autowired TMService tmService;

  @Autowired AssetTextUnitRepository assetTextUnitRepository;

  @Autowired TextUnitSearcher textUnitSearcher;

  @Autowired PluralFormService pluralFormService;

  @Autowired RepositoryStatisticsJobScheduler repositoryStatisticsJobScheduler;

  @Autowired EntityManager entityManager;

  @Autowired TextUnitDTOsCacheService textUnitDTOsCacheService;

  @Autowired LocaleService localeService;

  @Autowired(required = false)
  AITranslationService aiTranslationService;

  @Transactional
  public void updateTextUnits(
      Asset asset, List<VirtualAssetTextUnit> virtualAssetTextUnits, boolean replace)
      throws VirtualAssetRequiredException {

    logger.debug("Update text unit for asset: {}", asset.getPath());

    logger.debug("Build maps by name and md5 for existing text units");

    List<TextUnitDTO> allAssetTextUnitDTOs =
        textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocale(
            asset.getId(), localeService.getDefaultLocale().getId(), true, UpdateType.ALWAYS);

    HashMap<String, TextUnitDTO> nameToUsedtextUnitDTOs = new HashMap<>();
    HashMap<String, TextUnitDTO> md5ToTextUnitDTOs = new HashMap<>();
    HashMap<String, TextUnitDTO> contentToTextUnitDTOs = new HashMap<>();

    for (TextUnitDTO textUnitDTO : allAssetTextUnitDTOs) {
      String md5 =
          textUnitUtils.computeTextUnitMD5(
              textUnitDTO.getName(), textUnitDTO.getSource(), textUnitDTO.getComment());
      md5ToTextUnitDTOs.put(md5, textUnitDTO);
      contentToTextUnitDTOs.put(textUnitDTO.getSource(), textUnitDTO);

      if (textUnitDTO.isUsed()) {
        nameToUsedtextUnitDTOs.put(textUnitDTO.getName(), textUnitDTO);
      }
    }

    logger.debug("Build map by md5 for text units to import");
    HashMap<String, VirtualAssetTextUnit> md5ToVirtualTextUnits = new HashMap<>();
    for (VirtualAssetTextUnit virtualAssetTextUnit : virtualAssetTextUnits) {
      String md5 =
          textUnitUtils.computeTextUnitMD5(
              virtualAssetTextUnit.getName(),
              virtualAssetTextUnit.getContent(),
              virtualAssetTextUnit.getComment());
      md5ToVirtualTextUnits.put(md5, virtualAssetTextUnit);
    }

    HashMap<TMTextUnit, VirtualAssetTextUnit> savedTextUnits =
        saveVirtualTextUnits(
            md5ToVirtualTextUnits, md5ToTextUnitDTOs, nameToUsedtextUnitDTOs, asset, replace);

    performLeveraging(savedTextUnits, nameToUsedtextUnitDTOs, contentToTextUnitDTOs);

    if (aiTranslationService != null) {
      scheduleAITranslation(savedTextUnits.keySet(), asset.getRepository());
    }

    if (replace) {
      deleteOldAssetTextUnits(md5ToTextUnitDTOs, md5ToVirtualTextUnits);
    }
  }

  void performLeveraging(
      HashMap<TMTextUnit, VirtualAssetTextUnit> saveTextUnits,
      HashMap<String, TextUnitDTO> nameToUsedtextUnitDTOs,
      HashMap<String, TextUnitDTO> contentToUsedtextUnitDTOs) {

    for (Map.Entry<TMTextUnit, VirtualAssetTextUnit> entry : saveTextUnits.entrySet()) {
      TextUnitDTO matchByName = nameToUsedtextUnitDTOs.get(entry.getValue().getName());
      TextUnitDTO matchByContent = contentToUsedtextUnitDTOs.get(entry.getValue().getContent());
      List<TMTextUnit> toBeLeveraged = new ArrayList<>();
      toBeLeveraged.add(entry.getKey());

      if (matchByName != null) {
        logger.debug("Found previous version by name, apply leveraging");
        new LeveragerByTmTextUnit(matchByName.getTmTextUnitId())
            .performLeveragingFor(toBeLeveraged, null, null);
      } else if (matchByContent != null) {
        logger.debug("Leverage by content");
        leveragerByContentForSourceLeveraging.performLeveragingFor(toBeLeveraged, null, null);
      }
    }
  }

  HashMap<TMTextUnit, VirtualAssetTextUnit> saveVirtualTextUnits(
      HashMap<String, VirtualAssetTextUnit> md5ToVirtualTextUnits,
      HashMap<String, TextUnitDTO> md5ToTextUnitDTOs,
      HashMap<String, TextUnitDTO> nameToUsedtextUnitDTOs,
      Asset asset,
      boolean replace) {

    HashMap<TMTextUnit, VirtualAssetTextUnit> addedTmTextUnits = new HashMap<>();

    for (Map.Entry<String, VirtualAssetTextUnit> entry : md5ToVirtualTextUnits.entrySet()) {
      String md5 = entry.getKey();
      VirtualAssetTextUnit virtualAssetTextUnit = entry.getValue();
      PluralForm pluralForm =
          pluralFormService.findByPluralFormString(virtualAssetTextUnit.getPluralForm());
      boolean doNotTranslate =
          MoreObjects.firstNonNull(virtualAssetTextUnit.getDoNotTranslate(), false);

      TextUnitDTO exactMatch = md5ToTextUnitDTOs.get(md5);

      if (exactMatch != null) {
        logger.debug("Found exact match for: {}", virtualAssetTextUnit.getName());

        if (exactMatch.isUsed()) {

          if (exactMatch.isDoNotTranslate() == doNotTranslate) {
            logger.debug("Exact match is used and no change to doNotTranslate, nothing to do");
          } else {
            logger.debug("Exact match is used but doNotTranslate changed, update AssetTextUnit");
            AssetTextUnit assetTextUnit =
                assetTextUnitRepository.findById(exactMatch.getAssetTextUnitId()).orElse(null);
            assetTextUnit.setDoNotTranslate(doNotTranslate);
            assetTextUnitRepository.save(assetTextUnit);

            repositoryStatisticsJobScheduler.schedule(asset.getRepository().getId());
          }
        } else {
          logger.debug(
              "Exact match not used, need to create an asset text unit and map it to the tm text unit");
          createMappedAssetTextUnit(
              asset,
              virtualAssetTextUnit,
              pluralForm,
              doNotTranslate,
              exactMatch.getTmTextUnitId());

          if (!replace) {
            logger.debug(
                "Not in replace mode, need to remove the previous entry (when in replace mode that operation will be done by deleteOldAssetTextUnits())");
            removePreviousEntryWithSameName(nameToUsedtextUnitDTOs, virtualAssetTextUnit);
          }
        }
      } else {
        logger.debug("No exact match, create asset and tm text units");
        TMTextUnit tmTextUnit =
            tmService.addTMTextUnit(
                asset.getRepository().getTm(),
                asset,
                virtualAssetTextUnit.getName(),
                virtualAssetTextUnit.getContent(),
                virtualAssetTextUnit.getComment(),
                null,
                pluralForm,
                virtualAssetTextUnit.getPluralFormOther());

        removePreviousEntryWithSameName(nameToUsedtextUnitDTOs, virtualAssetTextUnit);
        createMappedAssetTextUnit(
            asset, virtualAssetTextUnit, pluralForm, doNotTranslate, tmTextUnit.getId());
        addedTmTextUnits.put(tmTextUnit, virtualAssetTextUnit);
      }
    }

    return addedTmTextUnits;
  }

  void removePreviousEntryWithSameName(
      HashMap<String, TextUnitDTO> nameToUsedtextUnitDTOs,
      VirtualAssetTextUnit virtualAssetTextUnit) {
    TextUnitDTO previousByName = nameToUsedtextUnitDTOs.get(virtualAssetTextUnit.getName());

    if (previousByName != null) {
      logger.debug("Asset text unit has changed, remove all previous entries");
      assetTextUnitToTMTextUnitRepository.deleteByAssetTextUnitId(
          previousByName.getAssetTextUnitId());
      assetTextUnitRepository.deleteById(previousByName.getAssetTextUnitId());
    }
  }

  void deleteOldAssetTextUnits(
      HashMap<String, TextUnitDTO> md5ToTextUnitDTOs,
      HashMap<String, VirtualAssetTextUnit> md5ToVirtualTextUnits) {
    List<AssetTextUnit> assetTextUnitsToDelete = new ArrayList<>();
    List<Long> assetTextUnitToTmTextUnitsToDelete = new ArrayList<>();

    for (Map.Entry<String, TextUnitDTO> entry : md5ToTextUnitDTOs.entrySet()) {
      String md5 = entry.getKey();
      TextUnitDTO textUnitDTO = entry.getValue();

      if (textUnitDTO.isUsed() && md5ToVirtualTextUnits.get(md5) == null) {
        assetTextUnitsToDelete.add(
            assetTextUnitRepository.getOne(textUnitDTO.getAssetTextUnitId()));
        assetTextUnitToTmTextUnitsToDelete.add(textUnitDTO.getAssetTextUnitId());
      }
    }

    deleteAssetTextUnitToTmTextUnits(assetTextUnitToTmTextUnitsToDelete);
    deleteAssetTextUnits(assetTextUnitsToDelete);
  }

  void deleteAssetTextUnits(List<AssetTextUnit> assetTextUnitsToDelete) {

    Iterable<List<AssetTextUnit>> batches = Iterables.partition(assetTextUnitsToDelete, 100);

    for (List<AssetTextUnit> batch : batches) {
      assetTextUnitRepository.deleteInBatch(batch);
    }
  }

  void deleteAssetTextUnitToTmTextUnits(List<Long> ids) {

    Iterable<List<Long>> batchIds = Iterables.partition(ids, 100);

    for (List<Long> batchId : batchIds) {
      logger.debug("Deleting asset text unit to tm text unit map: {}", batchId.size());
      StringBuilder builder =
          new StringBuilder("delete from asset_text_unit_to_tm_text_unit where ");

      for (int i = 0; i < batchId.size(); i++) {
        builder.append("asset_text_unit_id = :p").append(i);

        if (i != batchId.size() - 1) {
          builder.append(" or ");
        }
      }

      Query query = entityManager.createNativeQuery(builder.toString());

      for (int i = 0; i < batchId.size(); i++) {
        query.setParameter("p" + i, batchId.get(i));
      }

      int executeUpdate = query.executeUpdate();
      logger.debug("Deleted: {} asset_text_unit_to_tm_text_unit", executeUpdate);
    }
  }

  void createMappedAssetTextUnit(
      Asset asset,
      VirtualAssetTextUnit virtualAssetTextUnit,
      PluralForm pluralForm,
      boolean doNotTranslate,
      Long tmTextUnitId) {
    AssetTextUnit assetTextUnit =
        assetExtractionService.createAssetTextUnit(
            asset.getLastSuccessfulAssetExtraction().getId(),
            virtualAssetTextUnit.getName(),
            virtualAssetTextUnit.getContent(),
            virtualAssetTextUnit.getComment(),
            pluralForm,
            virtualAssetTextUnit.getPluralFormOther(),
            doNotTranslate,
            null,
            null);

    logger.debug("Map asset text unit to textunit");
    AssetTextUnitToTMTextUnit assetTextUnitToTMTextUnit = new AssetTextUnitToTMTextUnit();
    assetTextUnitToTMTextUnit.setAssetExtraction(
        assetExtractionRepository.getOne(asset.getLastSuccessfulAssetExtraction().getId()));
    assetTextUnitToTMTextUnit.setAssetTextUnit(assetTextUnit);
    assetTextUnitToTMTextUnit.setTmTextUnit(tmTextUnitRepository.getOne(tmTextUnitId));
    assetTextUnitToTMTextUnitRepository.save(assetTextUnitToTMTextUnit);
  }

  @Async
  void scheduleAITranslation(Set<TMTextUnit> textUnits, Repository repository) {
    Set<Long> tmTextUnitIds = textUnits.stream().map(TMTextUnit::getId).collect(Collectors.toSet());
    aiTranslationService.createPendingMTEntitiesInBatches(repository.getId(), tmTextUnitIds);
  }
}
