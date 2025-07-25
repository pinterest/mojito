package com.box.l10n.mojito.service.pushrun;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.PushRunAsset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.service.commit.CommitToPushRunRepository;
import com.google.common.collect.Lists;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service that manages PushRuns. Allows creation of PushRuns, association and retrieval of
 * connected TextUnit data.
 *
 * @author garion
 */
@Service
public class PushRunService {

  /** Logger */
  static Logger logger = LoggerFactory.getLogger(PushRunService.class);

  public static final int BATCH_SIZE = 1000;

  static final int DELETE_BATCH_SIZE = 100000;

  final EntityManager entityManager;

  final JdbcTemplate jdbcTemplate;

  final CommitToPushRunRepository commitToPushRunRepository;

  final PushRunRepository pushRunRepository;

  final PushRunAssetRepository pushRunAssetRepository;

  final PushRunAssetTmTextUnitRepository pushRunAssetTmTextUnitRepository;

  public PushRunService(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      CommitToPushRunRepository commitToPushRunRepository,
      PushRunRepository pushRunRepository,
      PushRunAssetRepository pushRunAssetRepository,
      PushRunAssetTmTextUnitRepository pushRunAssetTmTextUnitRepository) {
    this.entityManager = entityManager;
    this.jdbcTemplate = jdbcTemplate;
    this.commitToPushRunRepository = commitToPushRunRepository;
    this.pushRunRepository = pushRunRepository;
    this.pushRunAssetRepository = pushRunAssetRepository;
    this.pushRunAssetTmTextUnitRepository = pushRunAssetTmTextUnitRepository;
  }

  /** Creates a new PushRun entry with a new UUID as the logical name. */
  public PushRun createPushRun(Repository repository) {
    return createPushRun(repository, null);
  }

  /**
   * Creates a new PushRun entry with the specified name. Note: if the name is null/blank, a UUID
   * will be generated and used instead.
   */
  public PushRun createPushRun(Repository repository, String pushRunName) {
    if (StringUtils.isBlank(pushRunName)) {
      pushRunName = UUID.randomUUID().toString();
    }

    PushRun newPushRun = new PushRun();

    newPushRun.setRepository(repository);
    newPushRun.setName(pushRunName);

    return pushRunRepository.save(newPushRun);
  }

  /** Removes the linked PushRunAssets and pushRunAssetTmTextUnits from the PushRun. */
  @Transactional
  public void clearPushRunLinkedData(PushRun pushRun) {
    List<PushRunAsset> existingPushRunAssets = pushRunAssetRepository.findByPushRun(pushRun);
    existingPushRunAssets.forEach(pushRunAssetTmTextUnitRepository::deleteByPushRunAsset);
    pushRunAssetRepository.deleteByPushRun(pushRun);
  }

  /** Associates a set of TextUnits to a PushRunAsset and a PushRun. */
  @Transactional
  public void associatePushRunToTextUnitIds(PushRun pushRun, Asset asset, List<Long> textUnitIds) {
    PushRunAsset pushRunAsset =
        pushRunAssetRepository.findByPushRunAndAsset(pushRun, asset).orElse(null);

    if (pushRunAsset == null) {
      pushRunAsset = new PushRunAsset();

      pushRunAsset.setPushRun(pushRun);
      pushRunAsset.setAsset(asset);

      pushRunAssetRepository.save(pushRunAsset);
    } else {
      pushRunAssetTmTextUnitRepository.deleteByPushRunAsset(pushRunAsset);
    }

    ZonedDateTime createdTime = ZonedDateTime.now();
    PushRunAsset finalPushRunAsset = pushRunAsset;
    Lists.partition(textUnitIds, BATCH_SIZE)
        .forEach(
            textUnitIdsBatch -> saveTextUnits(finalPushRunAsset, textUnitIdsBatch, createdTime));
  }

  /** Retrieves the list of TextUnits associated with a PushRun. */
  public List<TMTextUnit> getPushRunTextUnits(PushRun pushRun, Pageable pageable) {
    return pushRunAssetTmTextUnitRepository.findByPushRun(pushRun, pageable);
  }

  void saveTextUnits(PushRunAsset pushRunAsset, List<Long> textUnitIds, ZonedDateTime createdTime) {
    String sql =
        "insert into push_run_asset_tm_text_unit(push_run_asset_id, tm_text_unit_id, created_date) values"
            + textUnitIds.stream()
                .map(
                    tuId ->
                        String.format(
                            "(%s, %s, '%s') ",
                            pushRunAsset.getId(), tuId, JSR310Migration.toRawSQL(createdTime)))
                .collect(Collectors.joining(","));

    jdbcTemplate.update(sql);
  }

  public PushRun getPushRunById(long id) {
    return pushRunRepository
        .findById(id)
        .orElseThrow(
            () -> new RuntimeException(String.format("Could not find a PushRun for id: %s", id)));
  }

  public void deleteAllPushEntitiesOlderThan(Duration retentionDuration) {
    ZonedDateTime beforeDate =
        ZonedDateTime.now().minusSeconds((int) retentionDuration.getSeconds());

    int batchNumber = 1;
    int deleteCount;
    do {
      deleteCount =
          pushRunAssetTmTextUnitRepository.deleteAllByPushRunWithCreatedDateBefore(
              beforeDate, DELETE_BATCH_SIZE);
      logger.debug(
          "Deleted {} pushRunAssetTmTextUnit rows in batch: {}", deleteCount, batchNumber++);
    } while (deleteCount == DELETE_BATCH_SIZE);

    pushRunAssetRepository.deleteAllByPushRunWithCreatedDateBefore(beforeDate);
    commitToPushRunRepository.deleteAllByPushRunWithCreatedDateBefore(beforeDate);
    pushRunRepository.deleteAllByCreatedDateBefore(beforeDate);
  }

  public void deletePushRunsByAsset(ZonedDateTime startDate, ZonedDateTime endDate) {
    List<Long> latestPushRunIdsPerAsset =
        this.pushRunRepository.getLatestPushRunIdsPerAsset(startDate, endDate);
    int batchNumber = 1;
    int deleteCount;
    do {
      deleteCount =
          pushRunAssetTmTextUnitRepository.deleteByPushRunsNotLatestPerAsset(
              startDate, endDate, latestPushRunIdsPerAsset, DELETE_BATCH_SIZE);
      logger.debug(
          "Deleted {} pushRunAssetTmTextUnit rows in batch: {} without IDs: {}",
          deleteCount,
          batchNumber++,
          latestPushRunIdsPerAsset);
    } while (deleteCount == DELETE_BATCH_SIZE);
    this.pushRunAssetRepository.deleteByPushRunsNotLatestPerAsset(
        startDate, endDate, latestPushRunIdsPerAsset);
    this.commitToPushRunRepository.deleteByPushRunsNotLatestPerAsset(
        startDate, endDate, latestPushRunIdsPerAsset);
    this.pushRunRepository.deletePushRunsNotLatestPerAsset(
        startDate, endDate, latestPushRunIdsPerAsset);
  }
}
