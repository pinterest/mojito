package com.box.l10n.mojito;

import static org.junit.Assert.assertEquals;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import org.junit.Test;

public class DateTimeUtilsTest {

  @Test
  public void testLongOrStrAsZonedDateNull() {
    String source = null;
    ZonedDateTime expResult = null;
    assertEquals(expResult, DateTimeUtils.longOrStrAsZonedDate(source));
  }

  @Test
  public void testLongOrStrAsZonedDateMillisecond() {
    String source = "1525887295000";
    ZonedDateTime expResult = ZonedDateTime.parse("2018-05-09T17:34:55.000Z");
    ZonedDateTime result = DateTimeUtils.longOrStrAsZonedDate(source);
    assertEquals(expResult.toInstant(), result.toInstant());
  }

  @Test
  public void testLongOrStrAsZonedDateISO() {
    String source = "2018-05-09T17:34:55.000Z";
    ZonedDateTime expResult = ZonedDateTime.parse("2018-05-09T17:34:55.000Z");
    ZonedDateTime result = DateTimeUtils.longOrStrAsZonedDate(source);
    assertEquals(expResult.toInstant(), result.toInstant());
  }

  @Test
  public void testLongOrStrAsZonedDateISONoTZ() {
    String source = "2018-05-09T17:34:55.000";
    ZonedDateTime expResult = ZonedDateTime.parse("2018-05-09T17:34:55.000Z");
    ZonedDateTime result = DateTimeUtils.longOrStrAsZonedDate(source);
    assertEquals(expResult.toInstant(), result.toInstant());
  }

  @Test
  public void testLongOrStrAsZonedDateISOTZ() {
    String source = "2018-05-09T17:34:55.000-07:00";
    ZonedDateTime expResult = ZonedDateTime.parse("2018-05-10T00:34:55.000Z");
    ZonedDateTime result = DateTimeUtils.longOrStrAsZonedDate(source);
    assertEquals(expResult.toInstant(), result.toInstant());
  }

  @Test
  public void testLongOrStrAsZonedDateISOTZNoMillisecond() {
    String source = "2018-05-09T17:34:55-07:00";
    ZonedDateTime expResult = ZonedDateTime.parse("2018-05-10T00:34:55.000Z");
    ZonedDateTime result = DateTimeUtils.longOrStrAsZonedDate(source);
    assertEquals(expResult.toInstant(), result.toInstant());
  }

  @Test(expected = DateTimeParseException.class)
  public void testLongOrStrAsZonedDateInvalidDateFormat() {
    String source = "09-05-2018T17:34:55-07:00";
    DateTimeUtils.longOrStrAsZonedDate(source);
  }

  @Test()
  public void testFromDateToZonedDate() {
    Date date = new Date();
    ZonedDateTime expResult = date.toInstant().atZone(ZoneOffset.UTC);
    ZonedDateTime result = DateTimeUtils.fromDateToZonedDate(date);
    assertEquals(expResult, result);
  }

  @Test()
  public void testFromDateToZonedDateNull() {
    ZonedDateTime expResult = null;
    assertEquals(expResult, DateTimeUtils.fromDateToZonedDate(null));
  }

  @Test()
  public void testLongAsZonedDate() {
    Long source = 1525887295000L;
    ZonedDateTime expResult = ZonedDateTime.parse("2018-05-09T17:34:55.000Z");
    ZonedDateTime result = DateTimeUtils.longAsZonedDate(source);
    assertEquals(expResult.toInstant(), result.toInstant());
  }

  @Test(expected = NullPointerException.class)
  public void testLongAsZonedDateNull() {
    DateTimeUtils.longAsZonedDate(null);
  }

  @Test()
  public void testStrAsZonedDate() {
    String source = "2018-05-09T17:34:55.000Z";
    ZonedDateTime expResult = ZonedDateTime.parse("2018-05-09T17:34:55.000Z");
    ZonedDateTime result = DateTimeUtils.strAsZonedDate(source);
    assertEquals(expResult.toInstant(), result.toInstant());
  }

  @Test(expected = DateTimeParseException.class)
  public void testStrAsZonedDateParseException() {
    String source = "2018-05-09T17:34:55.000-INVALID";
    DateTimeUtils.strAsZonedDate(source);
  }

  @Test(expected = NullPointerException.class)
  public void testStrAsZonedDateNull() {
    DateTimeUtils.strAsZonedDate(null);
  }
}
