package com.box.l10n.mojito.service.evolve;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TranslationModeMapperTest {

  private TranslationModeMapper createMapper(String trueText, String falseText) {
    EvolveConfigurationProperties evolveConfigurationProperties =
        new EvolveConfigurationProperties();
    evolveConfigurationProperties.setRefreshWithParentAssetsAndStructureText(trueText);
    evolveConfigurationProperties.setPreserveAssetsAndStructureText(falseText);

    return new TranslationModeMapper(evolveConfigurationProperties);
  }

  @Test
  public void testMapToBooleanReturnsEmptyWhenTranslationModeIsNull() {
    TranslationModeMapper translationModeMapper =
        this.createMapper(
            "Refresh with parent assets and structure", "Preserve assets and structure");

    assertEquals(
        empty(), translationModeMapper.mapTranslationModeToReplaceLegacyEvolveLocaleContent(null));
  }

  @Test
  public void testMapToBooleanReturnsEmptyWhenTranslationModeIsEmpty() {
    TranslationModeMapper translationModeMapper =
        this.createMapper(
            "Refresh with parent assets and structure", "Preserve assets and structure");

    assertEquals(
        empty(), translationModeMapper.mapTranslationModeToReplaceLegacyEvolveLocaleContent(""));
  }

  @Test
  public void testMapToBooleanMapsConfiguredTrueValueIgnoringCase() {
    TranslationModeMapper translationModeMapper =
        this.createMapper(
            "Refresh with parent assets and structure", "Preserve assets and structure");

    assertEquals(
        of(true),
        translationModeMapper.mapTranslationModeToReplaceLegacyEvolveLocaleContent(
            "refresh WITH parent ASSETS and structure"));
  }

  @Test
  public void testMapToBooleanMapsConfiguredFalseValueIgnoringCase() {
    TranslationModeMapper translationModeMapper =
        this.createMapper(
            "Refresh with parent assets and structure", "Preserve assets and structure");

    assertEquals(
        of(false),
        translationModeMapper.mapTranslationModeToReplaceLegacyEvolveLocaleContent(
            "preserve ASSETS and structure"));
  }

  @Test
  public void testMapToBooleanReturnsEmptyForUnknownValue() {
    TranslationModeMapper translationModeMapper =
        this.createMapper(
            "Refresh with parent assets and structure", "Preserve assets and structure");

    assertEquals(
        empty(),
        translationModeMapper.mapTranslationModeToReplaceLegacyEvolveLocaleContent(
            "unmapped value"));
  }

  @Test
  public void testMapToBooleanThrowsWhenValueMatchesBothMappings() {
    TranslationModeMapper translationModeMapper = this.createMapper("same value", "same value");

    assertThrows(
        EvolveSyncException.class,
        () ->
            translationModeMapper.mapTranslationModeToReplaceLegacyEvolveLocaleContent(
                "same value"));
  }
}
