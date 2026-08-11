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

  /**
   * Maps an Evolve course translation mode string to the {@code
   * refreshWithParentAssetsAndStructure} flag used when pushing translations back.
   *
   * <ul>
   *   <li>{@code true} – when {@code translationMode} matches {@link
   *       EvolveConfigurationProperties#getRefreshWithParentAssetsAndStructureText()}
   *   <li>{@code false} – when it matches {@link
   *       EvolveConfigurationProperties#getPreserveAssetsAndStructureText()}
   *   <li>empty – when {@code translationMode} is {@code null} or matches neither value
   * </ul>
   *
   * @param translationMode the raw translation mode string from the course DTO, may be {@code null}
   * @return the mapped flag, or empty if the mode is unrecognised
   * @throws EvolveSyncException if {@code translationMode} matches both configured values
   */
  public Optional<Boolean> toRefreshWithParentAssetsAndStructure(String translationMode) {
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
