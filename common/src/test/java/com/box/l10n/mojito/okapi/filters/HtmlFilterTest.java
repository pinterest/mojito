package com.box.l10n.mojito.okapi.filters;

import static org.junit.Assert.assertEquals;
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
      htmlFilter.setEmptyAndNbspAsNotTranslatable(textUnit1);

      assertFalse(textUnit1.isTranslatable());

      htmlFilter.setEmptyAndNbspAsNotTranslatable(textUnit2);

      assertFalse(textUnit2.isTranslatable());
    }
  }

  @Test
  public void testSetEmptyAndNbspAsNotTranslatable_DoesNotMarkTextUnitsAsNotTranslatable() {
    final String textUnit1Source =
        "<sup style=\"\">Hi {{first_name}},<br><br>Please update your account.</sup>";
    TextUnit textUnit1 = new TextUnit();
    textUnit1.setSource(new TextContainer(textUnit1Source));

    final String textUnit2Source = "Example";
    TextUnit textUnit2 = new TextUnit();
    textUnit2.setSource(new TextContainer(textUnit2Source));

    final String textUnit3Source = "{% if show_cta %} Click here {% endif %}";
    TextUnit textUnit3 = new TextUnit();
    textUnit3.setSource(new TextContainer(textUnit3Source));
    try (HtmlFilter htmlFilter = new HtmlFilter()) {
      htmlFilter.setEmptyAndNbspAsNotTranslatable(textUnit1);

      assertTrue(textUnit1.isTranslatable());
      assertEquals(textUnit1Source, textUnit1.getSource().getFirstContent().getCodedText());

      htmlFilter.setEmptyAndNbspAsNotTranslatable(textUnit2);

      assertTrue(textUnit2.isTranslatable());
      assertEquals(textUnit2Source, textUnit2.getSource().getFirstContent().getCodedText());

      htmlFilter.setEmptyAndNbspAsNotTranslatable(textUnit3);

      assertTrue(textUnit3.isTranslatable());
      assertEquals(textUnit3Source, textUnit3.getSource().getFirstContent().getCodedText());
    }
  }
}
