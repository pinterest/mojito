package com.box.l10n.mojito.service.delta;

import com.box.l10n.mojito.service.pullrun.PullRunService;
import com.box.l10n.mojito.service.pushrun.PushRunService;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author garion
 */
@Service
public class PushPullRunCleanupService {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(PushPullRunCleanupService.class);

  private final PushRunService pushRunService;

  private final PullRunService pullRunService;

  public PushPullRunCleanupService(PushRunService pushRunService, PullRunService pullRunService) {
    this.pushRunService = pushRunService;
    this.pullRunService = pullRunService;
  }

  /**
   * Method added for testing purposes
   *
   * @return Current date time
   */
  protected ZonedDateTime getCurrentDateTime() {
    return ZonedDateTime.now();
  }

  private ZonedDateTime cleanupPushPullRunsAndGetRetentionEndDate(
      PushPullRunCleanupConfigurationProperties configurationProperties) {
    ZonedDateTime currentDateTime = this.getCurrentDateTime();
    ZonedDateTime retentionEndDate =
        currentDateTime.minusSeconds(
            (int) configurationProperties.getRetentionDuration().getSeconds());
    ZonedDateTime retentionStartDate =
        retentionEndDate.minusWeeks(configurationProperties.getExtraNumberOfWeeksToRetain());
    logger.debug("Deleting push and pull runs from {} to {}", retentionStartDate, retentionEndDate);
    this.pushRunService.deletePushRunsByAsset(
        retentionStartDate,
        retentionEndDate,
        configurationProperties.getDeleteBatchSize(),
        configurationProperties.getMaxNumberOfBatches());
    this.pullRunService.deletePullRunsByAsset(
        retentionStartDate,
        retentionEndDate,
        configurationProperties.getDeleteBatchSize(),
        configurationProperties.getMaxNumberOfBatches());
    return retentionStartDate;
  }

  public void cleanOldPushPullData(
      PushPullRunCleanupConfigurationProperties configurationProperties) {
    ZonedDateTime retentionEndDate =
        this.cleanupPushPullRunsAndGetRetentionEndDate(configurationProperties);
    pushRunService.deleteAllPushEntitiesOlderThan(
        retentionEndDate, configurationProperties.getDeleteBatchSize());
    pullRunService.deleteAllPullEntitiesOlderThan(
        retentionEndDate, configurationProperties.getDeleteBatchSize());
  }
}
