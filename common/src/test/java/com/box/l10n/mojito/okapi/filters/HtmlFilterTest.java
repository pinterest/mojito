package com.box.l10n.mojito.okapi.filters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnit;
import org.junit.Test;

public class HtmlFilterTest {
  @Test
  public void testSetEmptyAndNbspAsNotTranslatable_MarksTextUnitsAsNotTranslatable() {
    TextUnit textUnit1 = new TextUnit();
    textUnit1.setSource(
        new TextContainer(
            "{% if image_signature and image_signature != \"null\" %} {% else %} {% endif %}"));

    TextUnit textUnit2 = new TextUnit();
    textUnit2.setSource(new TextContainer("{{lba_username}}"));
    try (HtmlFilter htmlFilter = new HtmlFilter()) {
      htmlFilter.processWithCustomInlineCodeFinder(textUnit1);
      htmlFilter.setEmptyAndNbspAsNotTranslatable(textUnit1);

      assertFalse(textUnit1.isTranslatable());

      htmlFilter.processWithCustomInlineCodeFinder(textUnit2);
      htmlFilter.setEmptyAndNbspAsNotTranslatable(textUnit2);

      assertFalse(textUnit2.isTranslatable());
    }
  }

  @Test
  public void testSetEmptyAndNbspAsNotTranslatable_DoesNotMarkTextUnitsAsNotTranslatable() {
    TextUnit textUnit1 = new TextUnit();
    textUnit1.setSource(
        new TextContainer(
            "<sup style=\"\">Hi {{first_name}},<br><br>Please update your account.</sup>"));

    TextUnit textUnit2 = new TextUnit();
    textUnit2.setSource(new TextContainer("Example"));
    try (HtmlFilter htmlFilter = new HtmlFilter()) {
      htmlFilter.processWithCustomInlineCodeFinder(textUnit1);
      htmlFilter.setEmptyAndNbspAsNotTranslatable(textUnit1);

      assertTrue(textUnit1.isTranslatable());

      htmlFilter.processWithCustomInlineCodeFinder(textUnit2);
      htmlFilter.setEmptyAndNbspAsNotTranslatable(textUnit2);

      assertTrue(textUnit2.isTranslatable());
    }
  }
}
