package com.box.l10n.mojito.converter;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.JSR310Migration;
import org.junit.Test;
import org.threeten.extra.PeriodDuration;

/** @author jeanaurambault */
// TODO(jean) 2-JSR310 - rename test if renaming the class
public class PeriodConverterTest {

  @Test
  public void testConvert() {
    PeriodConverter periodConverter = new PeriodConverter();
    // TODO(jean) 2-JSR310 - update
    PeriodDuration expResult = JSR310Migration.newPeriodCtorWithHMSM(0, 1, 0, 0);
    PeriodDuration result = periodConverter.convert("60000");
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
