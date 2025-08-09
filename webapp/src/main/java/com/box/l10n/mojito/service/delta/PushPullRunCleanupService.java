package com.box.l10n.mojito.service.delta;

import com.box.l10n.mojito.service.pullrun.PullRunService;
import com.box.l10n.mojito.service.pushrun.PushRunService;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
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

  private List<DateRange> getDateRanges(
      ZonedDateTime retentionStartDate, int extraNumberOfWeeksToRetain) {
    List<DateRange> dateRanges = new ArrayList<>();
    ZonedDateTime endWeekDateTime = retentionStartDate;
    for (int i = 0; i < extraNumberOfWeeksToRetain; i++) {
      ZonedDateTime startWeekDateTime = endWeekDateTime.minusWeeks(1);
      DateRange dateRange = new DateRange(startWeekDateTime, endWeekDateTime);
      dateRanges.add(dateRange);
      endWeekDateTime = startWeekDateTime;
    }
    return dateRanges;
  }

  private ZonedDateTime cleanupPushPullRunsAndGetRetentionEndDate(
      PushPullRunCleanupConfigurationProperties configurationProperties) {
    ZonedDateTime currentDateTime = this.getCurrentDateTime();
    ZonedDateTime retentionStartDate =
        currentDateTime.minusSeconds(
            (int) configurationProperties.getRetentionDuration().getSeconds());
    List<DateRange> dateRanges =
        this.getDateRanges(
            retentionStartDate, configurationProperties.getExtraNumberOfWeeksToRetain());
    ZonedDateTime retentionEndDate = retentionStartDate;
    for (DateRange dateRange : dateRanges) {
      logger.debug(
          "Deleting push and pull runs from {} to {}", dateRange.startDate, dateRange.endDate);
      this.pushRunService.deletePushRunsByAsset(
          dateRange.startDate,
          dateRange.endDate,
          configurationProperties.getDeleteBatchSize(),
          configurationProperties.getMaxNumberOfBatches());
      this.pullRunService.deletePullRunsByAsset(
          dateRange.startDate,
          dateRange.endDate,
          configurationProperties.getDeleteBatchSize(),
          configurationProperties.getMaxNumberOfBatches());
      retentionEndDate = dateRange.startDate;
    }
    return retentionEndDate;
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

  private record DateRange(ZonedDateTime startDate, ZonedDateTime endDate) {}
}
