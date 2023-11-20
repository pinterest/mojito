package com.box.l10n.mojito.converter;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import org.junit.Test;

/** @author jeanaurambault */
public class PeriodConverterTest {

  @Test
  public void testConvert() {
    PeriodConverter periodConverter = new PeriodConverter();
    Duration expResult = Duration.ofMillis(60000);
    Duration result = periodConverter.convert("60000");
    assertEquals(expResult, result);
  }

  @Test(expected = NumberFormatException.class)
  public void testConvertNull() {
    PeriodConverter periodConverter = new PeriodConverter();
    periodConverter.convert(null);
  }

  @Test(expected = NumberFormatException.class)
  public void testConvertInvalid() {
    PeriodConverter periodConverter = new PeriodConverter();
    periodConverter.convert("invalid");
  }
}
