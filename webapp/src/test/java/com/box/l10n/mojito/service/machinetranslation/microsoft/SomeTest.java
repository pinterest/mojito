package com.box.l10n.mojito.service.machinetranslation.microsoft;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.Currency;
import java.util.Locale;
import org.junit.Test;

public class SomeTest {

  @Test
  public void testTranslate() {

    final Locale locale = Locale.forLanguageTag("en-US");
    final NumberFormat instance = NumberFormat.getCurrencyInstance(locale);
    System.out.println(instance.format(123));

    final Currency usd = Currency.getInstance("USD");

    final Locale localeFrFr = Locale.forLanguageTag("fr-FR");
    final NumberFormat instanceFrFr = NumberFormat.getCurrencyInstance(localeFrFr);

    instanceFrFr.setCurrency(usd);
    System.out.println(instanceFrFr.format(123));

    final Locale localeHeIL = Locale.forLanguageTag("he-IL");
    final NumberFormat instanceHeIL = NumberFormat.getCurrencyInstance(localeHeIL);
    instanceHeIL.setCurrency(usd);
    System.out.println(instanceHeIL.format(123));
  }
}
