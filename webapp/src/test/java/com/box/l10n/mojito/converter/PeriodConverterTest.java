package com.box.l10n.mojito.converter;

import static org.junit.Assert.assertEquals;

import java.time.Period;
import org.junit.Test;

/** @author jeanaurambault */
public class PeriodConverterTest {

  @Test
  public void testConvert() {
    PeriodConverter periodConverter = new PeriodConverter();
    // TODO(jean) JSR310 - update
    Period expResult = new Period(0, 1, 0, 0);
    Period result = periodConverter.convert("60000");
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
