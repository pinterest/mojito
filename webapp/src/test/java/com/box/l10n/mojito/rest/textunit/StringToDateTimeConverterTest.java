package com.box.l10n.mojito.rest.textunit;

import static org.junit.Assert.assertEquals;

import java.time.ZonedDateTime;
import org.junit.Test;

/** @author jeanaurambault */
public class StringToDateTimeConverterTest {

  @Test
  public void testConvertNull() {
    String source = null;
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    ZonedDateTime expResult = null;
    ZonedDateTime result = instance.convert(source);
    assertEquals(expResult, result);
  }

  @Test
  public void testConvertMillisecond() {
    String source = "1525887295000";
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    // TODO(jean) JSR310 - replace
    ZonedDateTime expResult = new ZonedDateTime("2018-05-09T17:34:55.000Z");
    ZonedDateTime result = instance.convert(source);
    assertEquals(expResult, result);
  }

  @Test
  public void testConvertISO() {
    String source = "2018-05-09T17:34:55.000Z";
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    // TODO(jean) JSR310 - replace
    ZonedDateTime expResult = new ZonedDateTime("2018-05-09T17:34:55.000Z");
    ZonedDateTime result = instance.convert(source);
    assertEquals(expResult, result);
  }

  @Test
  public void testConvertISONoTZ() {
    String source = "2018-05-09T17:34:55.000";
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    // TODO(jean) JSR310 - replace
    ZonedDateTime expResult = new ZonedDateTime("2018-05-09T17:34:55.000Z");
    ZonedDateTime result = instance.convert(source);
    assertEquals(expResult, result);
  }

  @Test
  public void testConvertISOUTC() {
    String source = "2018-05-09T17:34:55.000";
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    // TODO(jean) JSR310 - replace
    ZonedDateTime expResult = new ZonedDateTime("2018-05-09T17:34:55.000Z");
    ZonedDateTime result = instance.convert(source);
    assertEquals(expResult, result);
  }

  @Test
  public void testConvertISOTZ() {
    String source = "2018-05-09T17:34:55.000-07:00";
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    // TODO(jean) JSR310 - replace
    ZonedDateTime expResult = new ZonedDateTime("2018-05-10T00:34:55.000Z");
    ZonedDateTime result = instance.convert(source);
    assertEquals(expResult, result);
  }

  @Test
  public void testConvertISOTZNoMillisecond() {
    String source = "2018-05-09T17:34:55-07:00";
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    // TODO(jean) JSR310 - replace
    ZonedDateTime expResult = new ZonedDateTime("2018-05-10T00:34:55.000Z");
    ZonedDateTime result = instance.convert(source);
    assertEquals(expResult, result);
  }
}
