package com.box.l10n.mojito.service.sla;

import com.box.l10n.mojito.utils.DateTimeUtils;
import com.google.common.collect.Sets;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** @author jeanaurambault */
@Component
public class DropScheduleService {

  static Logger logger = LoggerFactory.getLogger(DropScheduleService.class);

  @Autowired DropScheduleConfig dropScheduleConfig;

  @Autowired DateTimeUtils dateTimeUtils;

  public ZonedDateTime getLastDropCreatedDate() {
    ZonedDateTime now = dateTimeUtils.now(dropScheduleConfig.getTimezone());
    ZonedDateTime lastDropDueDate = getLastDropDueDate(now);
    return getDropCreatedDate(lastDropDueDate);
  }

  ZonedDateTime getDropCreatedDate(ZonedDateTime dropDueDate) {

    ZonedDateTime dropCreatedDate =
        dropDueDate
            .withHour(dropScheduleConfig.getCreatedLocalTime().getHour())
            .withMinute(dropScheduleConfig.getCreatedLocalTime().getMinute())
            .withSecond(dropScheduleConfig.getCreatedLocalTime().getSecond())
            .withNano(dropScheduleConfig.getCreatedLocalTime().getNano());

    Integer dropDueDateDay = dropDueDate.getDayOfWeek().getValue();
    Integer dropStartDateDay = getDueDayToStartDay().get(dropDueDateDay);

    dropCreatedDate = dropCreatedDate.with(DayOfWeek.of(dropStartDateDay));

    if (dropStartDateDay > dropDueDateDay) {
      dropCreatedDate = dropCreatedDate.minusWeeks(1);
    }

    return dropCreatedDate;
  }

  ZonedDateTime getLastDropDueDate(ZonedDateTime before) {

    ZonedDateTime lastDropDueDate = null;

    HashSet<Integer> dropDueDaysSet = Sets.newHashSet(dropScheduleConfig.getDueDays());

    for (int daysToSubtract = 0; daysToSubtract <= 7; daysToSubtract++) {
      ZonedDateTime candidate =
          before.minusDays(daysToSubtract).with(dropScheduleConfig.getDueLocalTime());

      if (dropDueDaysSet.contains(candidate.getDayOfWeek().getValue())
          && !candidate.isAfter(before)) {
        lastDropDueDate = candidate;
        break;
      }
    }

    return lastDropDueDate;
  }

  Map<Integer, Integer> getDueDayToStartDay() {
    Map<Integer, Integer> dueDayToStartDay = new HashMap<>();

    for (int i = 0; i < dropScheduleConfig.getDueDays().size(); i++) {
      dueDayToStartDay.put(
          dropScheduleConfig.getDueDays().get(i), dropScheduleConfig.getCreatedDays().get(i));
    }

    return dueDayToStartDay;
  }
}
