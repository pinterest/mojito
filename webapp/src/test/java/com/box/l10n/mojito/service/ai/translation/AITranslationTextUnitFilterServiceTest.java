package com.box.l10n.mojito.service.ai.translation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.entity.PluralForm;
import com.box.l10n.mojito.entity.TMTextUnit;
import java.util.regex.Pattern;
import org.junit.Before;
import org.junit.Test;

public class AITranslationTextUnitFilterServiceTest {

  AITranslationTextUnitFilterService textUnitFilterService;

  @Before
  public void setUp() {
    textUnitFilterService = new AITranslationTextUnitFilterService();

    textUnitFilterService.excludePlurals = true;
    textUnitFilterService.excludePlaceholders = true;
    textUnitFilterService.excludeHtmlTags = true;
    textUnitFilterService.excludePlaceholdersRegex = "\\{[^\\}]*\\}";
    textUnitFilterService.excludePlaceholdersPattern =
        Pattern.compile(textUnitFilterService.excludePlaceholdersRegex);
  }

  @Test
  public void testIsTranslatable() {
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setName("test");
    tmTextUnit.setContent("test content");
    assertTrue(textUnitFilterService.isTranslatable(tmTextUnit));
  }

  @Test
  public void testIsTranslatableWithPlural() {
    textUnitFilterService.excludePlaceholders = false;
    textUnitFilterService.excludeHtmlTags = false;
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setName("test");
    tmTextUnit.setContent("test content");

    PluralForm otherPluralForm = new PluralForm();
    otherPluralForm.setName("other");
    tmTextUnit.setPluralForm(otherPluralForm);
    assertFalse(textUnitFilterService.isTranslatable(tmTextUnit));
  }

  @Test
  public void testIsTranslatableWithPlaceholder() {
    textUnitFilterService.excludePlurals = false;
    textUnitFilterService.excludeHtmlTags = false;
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setName("test");
    tmTextUnit.setContent("test {content}");
    assertFalse(textUnitFilterService.isTranslatable(tmTextUnit));
  }

  @Test
  public void testIsTranslatableWithHtmlTags() {
    textUnitFilterService.excludePlurals = false;
    textUnitFilterService.excludePlaceholders = false;
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setName("test");
    tmTextUnit.setContent("test <b>content</b>");
    assertFalse(textUnitFilterService.isTranslatable(tmTextUnit));
  }

  @Test
  public void testIsTranslatableWithHtmlTagsAndPlaceholders() {
    textUnitFilterService.excludePlurals = false;
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setName("test");
    tmTextUnit.setContent("test <b>{content}</b>");
    assertFalse(textUnitFilterService.isTranslatable(tmTextUnit));
  }

  @Test
  public void isTranslatableShouldReturnTrueWhenAllExclusionsAreFalse() {
    textUnitFilterService.excludePlurals = false;
    textUnitFilterService.excludePlaceholders = false;
    textUnitFilterService.excludeHtmlTags = false;

    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setContent("Text with <b>html</b> and {placeholder}, including plurals.");
    assertTrue(textUnitFilterService.isTranslatable(tmTextUnit));
  }

  @Test
  public void isTranslatableShouldReturnFalseForTextWithMultipleExclusions() {
    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setContent("This text has <b>html</b>, {placeholder}, and could be a plural form.");
    tmTextUnit.setPluralForm(new PluralForm());
    assertFalse(textUnitFilterService.isTranslatable(tmTextUnit));
  }

  @Test
  public void isTranslatableShouldReturnTrueWhenNoExclusionEnabled() {
    textUnitFilterService.excludePlurals = false;
    textUnitFilterService.excludePlaceholders = false;
    textUnitFilterService.excludeHtmlTags = false;

    TMTextUnit tmTextUnit = new TMTextUnit();
    tmTextUnit.setPluralForm(new PluralForm());
    tmTextUnit.setContent("Text with <b>html</b> and {placeholder}");
    assertTrue(textUnitFilterService.isTranslatable(tmTextUnit));
  }
}
