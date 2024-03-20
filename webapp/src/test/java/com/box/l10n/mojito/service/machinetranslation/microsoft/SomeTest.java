package com.box.l10n.mojito.service.machinetranslation.microsoft;

import com.box.l10n.mojito.service.machinetranslation.MachineTranslationConfiguration;
import com.box.l10n.mojito.service.machinetranslation.MachineTranslationEngine;
import com.box.l10n.mojito.service.machinetranslation.PlaceholderEncoder;
import com.box.l10n.mojito.service.machinetranslation.TextType;
import com.box.l10n.mojito.service.machinetranslation.TranslationDTO;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.Currency;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;


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
