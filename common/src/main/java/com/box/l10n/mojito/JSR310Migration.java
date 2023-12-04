package com.box.l10n.mojito;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Locale;
import org.threeten.extra.AmountFormats;
import org.threeten.extra.PeriodDuration;

public class JSR310Migration {

  public static ZonedDateTime newDateTimeCtor(
      int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour) {
    return ZonedDateTime.of(
        year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, 0, 0, ZoneId.systemDefault());
  }

  public static ZonedDateTime newDateTimeEmptyCtor() {
    return ZonedDateTime.now();
  }

  public static ZonedDateTime newDateTimeCtorWithISO8601Str(String str) {
    return ZonedDateTime.parse(str);
  }

  public static ZonedDateTime newDateTimeCtorWithEpochMilli(long value) {
    return Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault());
  }
  public static ZonedDateTime newDateTimeCtorAtEpoch() {
    return newDateTimeCtorWithEpochMilli(Instant.EPOCH.toEpochMilli());
  }

  public static ZonedDateTime dateTimeNow() {
    return ZonedDateTime.now();
  }

  public static long getMillis(ZonedDateTime zonedDateTime) {
    // TODO(jean) JSR has more precision so there is an issue with conversion?
    // what about    zonedDateTime.toInstant().truncatedTo(ChronoUnit.MILLIS)
    return zonedDateTime.toInstant().toEpochMilli();
  }

  public static String toWordBasedDuration(ZonedDateTime start, ZonedDateTime end) {
    PeriodDuration between = PeriodDuration.between(start, end);
    return AmountFormats.wordBased(between.getPeriod(), between.getDuration(), Locale.getDefault());
  }

  public static String toWordBasedDuration(long start, long end) {
    return toWordBasedDuration(
        Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()),
        Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault()));
  }

  public static Date dateTimeToDate(ZonedDateTime zonedDateTime) {
    return Date.from(zonedDateTime.toInstant());
  }
}
