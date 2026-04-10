package com.box.l10n.mojito.okapi.filters;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnit;
import org.junit.jupiter.api.Test;

class XliffFilterTest {
  @Test
  public void testMarkNotTranslatableIfBlankOrNoText() {
    TextUnit translatableTextUnit = new TextUnit();
    translatableTextUnit.setSource(new TextContainer("<p>&nbsp;&nbsp;</p>"));

    TextUnit nonTranslatableTextUnit1 = new TextUnit();
    nonTranslatableTextUnit1.setSource(new TextContainer("&nbsp;&nbsp;"));

    TextUnit nonTranslatableTextUnit2 = new TextUnit();
    nonTranslatableTextUnit2.setSource(new TextContainer("  "));

    TextUnit nonTranslatableTextUnit3 = new TextUnit();
    nonTranslatableTextUnit3.setSource(new TextContainer("\u00A0\u00A0"));
    try (XliffFilter xliffFilter = new XliffFilter()) {
      xliffFilter.markNotTranslatableIfBlankOrNoText(translatableTextUnit);

      assertTrue(translatableTextUnit.isTranslatable());

      xliffFilter.markNotTranslatableIfBlankOrNoText(nonTranslatableTextUnit1);

      assertFalse(nonTranslatableTextUnit1.isTranslatable());

      xliffFilter.markNotTranslatableIfBlankOrNoText(nonTranslatableTextUnit2);

      assertFalse(nonTranslatableTextUnit2.isTranslatable());

      xliffFilter.markNotTranslatableIfBlankOrNoText(nonTranslatableTextUnit3);

      assertFalse(nonTranslatableTextUnit3.isTranslatable());
    }
  }
}
