package com.box.l10n.mojito;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.assertj.core.api.Assertions;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;
import org.junit.Assert;
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

  @Test
  public void newDateTimeCtorWithISO8601Str() {
    // TODO(jean) JSR310 - review - usage of this method, seems mostly for test + lots of dupplication
    DateTime dateTime = newDateTimeCtorWithISO8601StrOld("2018-05-09T17:34:55.000Z");
    ZonedDateTime zonedDateTime = JSR310Migration.newDateTimeCtorWithISO8601Str("2018-05-09T17:34:55.000Z");
    Assertions.assertThat(dateTime.toInstant().getMillis()).isEqualTo(zonedDateTime.toInstant().toEpochMilli());

    DateTime dateTimeWithOffset = newDateTimeCtorWithISO8601StrOld("2018-06-08T14:00:00.000-07:00");
    ZonedDateTime zonedDateTimeWithOffset = JSR310Migration.newDateTimeCtorWithISO8601Str("2018-06-08T14:00:00.000-07:00");
    Assertions.assertThat(dateTimeWithOffset.toInstant().getMillis()).isEqualTo(zonedDateTimeWithOffset.toInstant().toEpochMilli());

    // TODO(jean) JSR310 - that's another toString difference to review
    // instant are same but the round trip str -> date -> str is lost with zonedDateTime while there is not change
    // with dateTime
    DateTimeFormatter JODA_FORMATTER_ATTEMPT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    Assertions.assertThat(dateTime.toString()).isEqualTo("2018-05-09T17:34:55.000Z");
    Assertions.assertThat(zonedDateTime.toString()).isEqualTo("2018-05-09T17:34:55Z");
    Assertions.assertThat(zonedDateTime.format(JODA_FORMATTER_ATTEMPT)).isEqualTo("2018-05-09T17:34:55.000Z");

    Assertions.assertThat(dateTimeWithOffset.toString()).isEqualTo("2018-06-08T21:00:00.000Z");
    Assertions.assertThat(zonedDateTimeWithOffset.format(JODA_FORMATTER_ATTEMPT))
            .isEqualTo("2018-06-08T14:00:00.000Z"); // TODO(jean) 2-JSR310 This is totally wrong but just attempt
    Assertions.assertThat(zonedDateTimeWithOffset.toString()).isEqualTo("2018-06-08T14:00-07:00");
  }

  @Test
  public void newDateTimeCtorWithLongAndString() {
    Assertions.assertThat(
            newDateTimeCtorWithLongAndStringOld("2018-05-09T17:34:55.000Z").toInstant().getMillis())
        .isEqualTo(
            JSR310Migration.newDateTimeCtorWithLongAndString("2018-05-09T17:34:55.000Z")
                .toInstant()
                .toEpochMilli());

    Assertions.assertThat(
            newDateTimeCtorWithLongAndStringOld(1594339100000L).toInstant().getMillis())
        .isEqualTo(
            JSR310Migration.newDateTimeCtorWithLongAndString(1594339100000L)
                .toInstant()
                .toEpochMilli());

    Assert.assertThrows(
        IllegalStateException.class,
        () -> JSR310Migration.newDateTimeCtorWithLongAndString(new Date()));
  }

  @Test
  public void dateTimeGetMillisOfSecond() {
    DateTime start = new DateTime(2020, 7, 10, 0, 0, 12, 345);
    ZonedDateTime zstart =
            ZonedDateTime.of(2020, 7, 10, 0, 0, 12, 345000000, ZoneId.systemDefault());
    Assertions.assertThat(dateTimeGetMillisOfSecondOld(start)).isEqualTo(JSR310Migration.dateTimeGetMillisOfSecond(zstart));
  }

  @Test
  public void dateTimeWithMillisOfSeconds() {
    Assertions.assertThat(dateTimeWithMillisOfSecondsOld(dateTime, 963).toInstant().getMillis())
            .isEqualTo(JSR310Migration.dateTimeWithMillisOfSeconds(zonedDateTime, 963).toInstant().toEpochMilli());
  }

  @Test
  public void newDateTimeCtorWithDate() {
    Date date = new Date();
    Assertions.assertThat(newDateTimeCtorWithDateOld(date).toInstant().getMillis())
        .isEqualTo(JSR310Migration.newDateTimeCtorWithDate(date).toInstant().toEpochMilli());

    Assertions.assertThat(newDateTimeCtorWithDateOld(null).toInstant().getMillis())
        .isEqualTo(JSR310Migration.newDateTimeCtorWithDate(null).toInstant().toEpochMilli());
  }

  @Test
  public void dateTimeWithLocalTime() {
    Assertions.assertThat(dateTimeWithLocalTimeOld(dateTime, new LocalTime(10, 15)).toInstant().getMillis())
            .isEqualTo(JSR310Migration.dateTimeWithLocalTime(zonedDateTime, java.time.LocalTime.of(10, 15)).toInstant().toEpochMilli());
  }

  @Test
  public void newLocalTimeWithString() {
    String source = "12:34";
    LocalTime joda = newLocalTimeWithStringOld(source);
    java.time.LocalTime jsr = JSR310Migration.newLocalTimeWithString(source);
    Assertions.assertThat(joda.getMillisOfSecond() * 1000).isEqualTo(jsr.getNano());
  }

  @Test
  public void newLocalTimeWithHMS() {
    LocalTime joda = newLocalTimeWithHMSOld(10, 20, 30);
    java.time.LocalTime jsr = JSR310Migration.newLocalTimeWithHMS(10, 20, 30);
    Assertions.assertThat(joda.getMillisOfSecond() * 1000).isEqualTo(jsr.getNano());
  }

  @Test
  public void dateTimeGetDayOfWeek() {
    Assertions.assertThat(dateTimeGetDayOfWeekOld(dateTime)).isEqualTo(JSR310Migration.dateTimeGetDayOfWeek(zonedDateTime));
  }

  @Test
  public void dateTimeWithDayOfWeek() {
    Assertions.assertThat(dateTimeWithDayOfWeekOld(dateTime, 4).toInstant().getMillis())
            .isEqualTo(JSR310Migration.dateTimeWithDayOfWeek(zonedDateTime, 4).toInstant().toEpochMilli());
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

  /**
   * TODO(jean) JSR310 this is actually new DateTime(Object instant) behind the scene. Start with string test
   * but this can be replaced
   */
  public static DateTime newDateTimeCtorWithISO8601StrOld(String str) {
    // from doc: the String formats are described by ISODateTimeFormat.dateTimeParser().
    return new DateTime(str);
  }

  /**
   * Same as above
   *
   * <p>TODO(jean) JSR310 this is actually new DateTime(Object instant) behind the scene. There is a
   * ussage with Long and String
   */
  public static DateTime newDateTimeCtorWithLongAndStringOld(Object instant) {
    return new DateTime(instant);
  }

  public static int dateTimeGetMillisOfSecondOld(DateTime dateTime) {
    return dateTime.getMillisOfSecond();
  }

  public static DateTime dateTimeWithMillisOfSecondsOld(DateTime dateTime, int millis) {
    return dateTime.withMillisOfSecond(millis);
  }

  public static DateTime newDateTimeCtorWithDateOld(Date date) {
    return new DateTime(date);
  }

  public static DateTime dateTimeWithLocalTimeOld(DateTime dateTime, LocalTime localTime) {
    return dateTime.withTime(localTime);
  }

  public static LocalTime newLocalTimeWithStringOld(String source) {
    return new LocalTime(source);
  }

  public static LocalTime newLocalTimeWithHMSOld(int hourOfDay, int minuteOfHour, int secondOfMinute) {
    return new LocalTime(hourOfDay, minuteOfHour, secondOfMinute);
  }

  public static Integer dateTimeGetDayOfWeekOld(DateTime dateTime) {
    return dateTime.getDayOfWeek();
  }

  public static DateTime dateTimeWithDayOfWeekOld(DateTime dateTime, int dayOfWeek) { return dateTime.withDayOfWeek(dayOfWeek);}
}
