package com.box.l10n.mojito.service.evolve;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TranslationModeMapper {

  private final EvolveConfigurationProperties evolveConfigurationProperties;

  public TranslationModeMapper(EvolveConfigurationProperties evolveConfigurationProperties) {
    this.evolveConfigurationProperties = evolveConfigurationProperties;
  }

  public Optional<Boolean> mapTranslationModeToReplaceLegacyEvolveLocaleContent(
      String translationMode) {
    if (translationMode == null) {
      return empty();
    }

    boolean mappedToTrue =
        translationMode.equalsIgnoreCase(
            this.evolveConfigurationProperties.getRefreshWithParentAssetsAndStructureText());
    boolean mappedToFalse =
        translationMode.equalsIgnoreCase(
            this.evolveConfigurationProperties.getPreserveAssetsAndStructureText());

    if (mappedToTrue && mappedToFalse) {
      throw new EvolveSyncException(
          "Translation mode value is configured for both true and false mapping: "
              + translationMode);
    }

    if (mappedToTrue) {
      return of(true);
    }

    if (mappedToFalse) {
      return of(false);
    }

    return empty();
  }
}
