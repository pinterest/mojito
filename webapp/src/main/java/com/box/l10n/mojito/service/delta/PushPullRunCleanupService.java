package com.box.l10n.mojito.service.delta;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import com.box.l10n.mojito.service.assetExtraction.AssetExtractionCleanupJob;
import com.box.l10n.mojito.service.pullrun.PullRunService;
import com.box.l10n.mojito.service.pushrun.PushRunService;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author garion
 */
@Service
public class PushPullRunCleanupService {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(AssetExtractionCleanupJob.class);

  @Autowired PushRunService pushRunService;

  @Autowired PullRunService pullRunService;

  @Autowired CleanPushPullPerAssetConfigurationProperties configurationProperties;

  private boolean isEqualOrAfter(ZonedDateTime dateTime1, ZonedDateTime dateTime2) {
    return dateTime1.isEqual(dateTime2) || dateTime1.isAfter(dateTime2);
  }

  private Optional<DateRange> getRange(
      ZonedDateTime validStartDateTimeRange,
      ZonedDateTime currentDateTime,
      int startDay,
      int endDay) {
    ZonedDateTime startDateRange =
        validStartDateTimeRange
            .toLocalDate()
            .withDayOfMonth(
                Math.min(startDay, validStartDateTimeRange.toLocalDate().lengthOfMonth()))
            .atStartOfDay(ZoneId.systemDefault());
    ZonedDateTime endDateRange =
        validStartDateTimeRange
            .toLocalDate()
            .withDayOfMonth(Math.min(endDay, validStartDateTimeRange.toLocalDate().lengthOfMonth()))
            .atStartOfDay(ZoneId.systemDefault());
    if (this.isEqualOrAfter(startDateRange, validStartDateTimeRange)
        && startDateRange.isBefore(currentDateTime)) {
      return of(
          new DateRange(
              startDateRange,
              endDateRange.isBefore(currentDateTime) ? endDateRange : currentDateTime));
    } else if (endDateRange.isAfter(validStartDateTimeRange)) {
      return of(
          new DateRange(
              validStartDateTimeRange,
              endDateRange.isBefore(currentDateTime) ? endDateRange : currentDateTime));
    }
    return empty();
  }

  private void cleanPushPullPerAsset(Duration retentionDuration) {
    List<DateRange> dateRanges = new ArrayList<>();
    ZonedDateTime currentDateTime = ZonedDateTime.now();
    ZonedDateTime validStartDateTimeRange =
        currentDateTime.minusSeconds((int) retentionDuration.getSeconds());
    while ((validStartDateTimeRange.getYear() < currentDateTime.getYear())
        || (validStartDateTimeRange.getYear() == currentDateTime.getYear()
            && validStartDateTimeRange.getMonthValue() <= currentDateTime.getMonthValue())) {
      Optional<DateRange> firstDateRange =
          this.getRange(
              validStartDateTimeRange,
              currentDateTime,
              this.configurationProperties.getStartDayFirstRange(),
              this.configurationProperties.getEndDayFirstRange());
      firstDateRange.ifPresent(dateRanges::add);
      Optional<DateRange> secondDateRange =
          this.getRange(
              validStartDateTimeRange,
              currentDateTime,
              this.configurationProperties.getStartDaySecondRange(),
              this.configurationProperties.getEndDaySecondRange());
      secondDateRange.ifPresent(dateRanges::add);

      validStartDateTimeRange =
          validStartDateTimeRange
              .toLocalDate()
              .withDayOfMonth(1)
              .plusMonths(1)
              .atStartOfDay(ZoneId.systemDefault());
    }
    dateRanges.forEach(
        dateRange -> {
          this.pushRunService.cleanPushRunPerAsset(dateRange.startDate, dateRange.endDate);
          this.pullRunService.cleanPullRunPerAsset(dateRange.startDate, dateRange.endDate);
        });
  }

  public void cleanOldPushPullData(Duration retentionDuration) {
    pushRunService.deleteAllPushEntitiesOlderThan(retentionDuration);
    pullRunService.deleteAllPullEntitiesOlderThan(retentionDuration);

    this.cleanPushPullPerAsset(retentionDuration);
  }

  private record DateRange(ZonedDateTime startDate, ZonedDateTime endDate) {}
}
