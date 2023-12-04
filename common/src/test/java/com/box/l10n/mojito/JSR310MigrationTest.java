package com.box.l10n.mojito;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.assertj.core.api.Assertions;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;
import org.junit.Test;

/**
 * Note that common pom.xml uses -Duser.timezone=UTC. it is most likely because the surefire plugin
 * configuration was copied from webapp, which requires it. Ran the common test with or witout it
 * and the resutls are the same. The can probably remove it. TODO(jean)
 */
public class JSR310MigrationTest {

  @Test
  public void makeDefaultTestRunUTC() {
    // The TZ is currently hardcoded to UTC in the pom.xml
    // Value can be changed in the POM file
    // Alternative values for testing: "America/Los_Angeles", "America/New_York"
    Assertions.assertThat(ZoneId.systemDefault().getId()).isEqualTo("UTC");
    //    Assertions.assertThat(ZoneId.systemDefault().getId()).isEqualTo("America/Los_Angeles");
  }

  static final DateTime dateTime = newDateTimeCtorOld(2020, 7, 10, 0, 0);
  static final ZonedDateTime zonedDateTime = JSR310Migration.newDateTimeCtor(2020, 7, 10, 0, 0);

  @Test
  public void toStringEquivalence() {

    if (ZoneId.systemDefault().getId().equals("UTC")) {
      // run in maven:
      // mvn clean install -Dtest=JodaMigrationTest -P'!frontend' -Duser.timezone=UTC

      // the old to string shows miliseconds and just the offset
      Assertions.assertThat(dateTime.toString()).isEqualTo("2020-07-10T00:00:00.000Z");
      // the new to string does not show miliseconds and adds the zone id
      Assertions.assertThat(zonedDateTime.toString()).isEqualTo("2020-07-10T00:00Z[UTC]");

      // As previous assert shows, the output of the 2 toString() method are not the same.
      //
      // In places where number formating matters, toString() must be change and an equivalent call
      // which to be
      // TODO(jean) Before digging more, let's make sure we actually use it or if we can afford the
      // difference
      //
      // A first try is to look at the DateTimeFormatter, this gives a pretty similar result, but it
      // is still missing the miliseconds
      Assertions.assertThat(zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
          .isEqualTo("2020-07-10T00:00:00Z");

    } else if (ZoneId.systemDefault().getId().equals("America/Los_Angeles")) {

      System.out.println(ZoneId.systemDefault().toString());
      Assertions.assertThat(dateTime.toString()).isEqualTo("2020-07-10T00:00:00.000-07:00");
      Assertions.assertThat(zonedDateTime.toString())
          .isEqualTo("2020-07-10T00:00-07:00[America/Los_Angeles]");
      Assertions.assertThat(zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
          .isEqualTo("2020-07-10T00:00:00-07:00");

    } else if (ZoneId.systemDefault().getId().equals("America/New_York")) {

      System.out.println(ZoneId.systemDefault().toString());
      Assertions.assertThat(dateTime.toString()).isEqualTo("2020-07-10T00:00:00.000-04:00");
      Assertions.assertThat(zonedDateTime.toString())
          .isEqualTo("2020-07-10T00:00-04:00[America/New_York]");
      Assertions.assertThat(zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
          .isEqualTo("2020-07-10T00:00:00-04:00");
    }
  }

  @Test
  public void dateTimeNow() {
    Assertions.assertThat(dateTimeNowOld())
        .isBetween(
            Instant.now().toDateTime().minusMillis(100),
            Instant.now().toDateTime().plusMillis(100));

    Assertions.assertThat(JSR310Migration.dateTimeNow())
        .isBetween(
            ZonedDateTime.now().minus(100, ChronoUnit.MILLIS),
            ZonedDateTime.now().plus(100, ChronoUnit.MILLIS));
  }

  @Test
  public void getMillis() {
    Assertions.assertThat(getMillisOld(dateTime))
        .isEqualTo(JSR310Migration.getMillis(zonedDateTime));
  }

  @Test
  public void toWordBasedDuration() {
    DateTime start = new DateTime(2020, 7, 10, 0, 0, 12, 345);
    System.out.println(start.toInstant().getMillis());
    DateTime end = new DateTime(2020, 8, 10, 0, 0, 13, 450);
    System.out.println(end.toInstant().getMillis());
    String joda = toWordBaseDurationOld(start, end);

    ZonedDateTime zstart =
        ZonedDateTime.of(2020, 7, 10, 0, 0, 12, 345000000, ZoneId.systemDefault());
    ZonedDateTime zend = ZonedDateTime.of(2020, 8, 10, 0, 0, 13, 450000000, ZoneId.systemDefault());
    String jsr = JSR310Migration.toWordBasedDuration(zstart, zend);

    System.out.println(joda);
    System.out.println(jsr);

    Assertions.assertThat(joda).isEqualTo(jsr);
  }

  /**
   * TODO(jean) this is used in test right now, so the difference is acceptable if no other code
   * uses that...
   */
  @Test
  public void toWordBasedDurationDifference() {
    DateTime start = new DateTime(2020, 7, 10, 0, 0, 12, 345);
    DateTime end = new DateTime(2020, 10, 25, 0, 0, 13, 450);
    String joda = toWordBaseDurationOld(start, end);

    ZonedDateTime zstart =
        ZonedDateTime.of(2020, 7, 10, 0, 0, 12, 345000000, ZoneId.systemDefault());
    ZonedDateTime zend =
        ZonedDateTime.of(2020, 10, 25, 0, 0, 13, 450000000, ZoneId.systemDefault());
    String jsr = JSR310Migration.toWordBasedDuration(zstart, zend);

    Assertions.assertThat(joda)
        .isEqualTo("3 months, 2 weeks, 1 day, 1 second and 105 milliseconds");
    Assertions.assertThat(jsr).isEqualTo("3 months, 15 days, 1 second and 105 milliseconds");
  }

  @Test
  public void toWordBasedDurationLong() {
    long start = 1594339212345L;
    long end = 1597017613450L;
    String joda = toWordBaseDurationOld(start, end);
    String jsr = JSR310Migration.toWordBasedDuration(start, end);
    Assertions.assertThat(joda).isEqualTo(jsr);
  }

  @Test
  public void dateTimeToDate() {
    Date joda = dateTimeToDateOld(dateTime);
    Date jsr = JSR310Migration.dateTimeToDate(zonedDateTime);
    Assertions.assertThat(joda).isEqualTo(jsr);
  }

  @Test
  public void dateTimeIsAfter() {
    Assertions.assertThat(dateTimeIsAfterEpochMillisOld(dateTime, 1594339100000L)).isTrue();
    Assertions.assertThat(JSR310Migration.dateTimeIsAfterEpochMillis(zonedDateTime, 1594339100000L)).isTrue();

    Assertions.assertThat(dateTimeIsAfterEpochMillisOld(dateTime, 1594339200000L)).isFalse();
    Assertions.assertThat(JSR310Migration.dateTimeIsAfterEpochMillis(zonedDateTime, 1594339200000L)).isFalse();
  }



  public static DateTime newDateTimeCtorOld(
      int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour) {
    return new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour);
  }

  public static DateTime dateTimeNowOld() {
    return DateTime.now();
  }

  public static long getMillisOld(DateTime dateTime) {
    return dateTime.getMillis();
  }

  public static String toWordBaseDurationOld(DateTime start, DateTime end) {
    Period period = new Period(start, end);
    return PeriodFormat.getDefault().print(period.toPeriod());
  }

  public static String toWordBaseDurationOld(long start, long end) {
    Period period = new Period(start, end);
    return PeriodFormat.getDefault().print(period.toPeriod());
  }

  public static Date dateTimeToDateOld(DateTime dateTime) {
     return dateTime.toDate();
  }

  public static boolean dateTimeIsAfterEpochMillisOld(DateTime dateTime, long afterMillis) {
    return dateTime.isAfter(afterMillis);
  }
}
