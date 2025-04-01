package com.box.l10n.mojito.evolve;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TranslationStatusType {
  READY_FOR_TRANSLATION("Ready for translation"),
  IN_TRANSLATION("In Translation"),
  TRANSLATED("Translated");

  private String name;

  TranslationStatusType(String name) {
    this.name = name;
  }

  @JsonValue
  public String getName() {
    return this.name;
  }

  @JsonCreator
  public static TranslationStatusType fromName(String name) {
    if (name.isBlank()) {
      return null;
    }
    for (TranslationStatusType translationStatusType : TranslationStatusType.values()) {
      if (translationStatusType.getName().equals(name)) {
        return translationStatusType;
      }
    }
    throw new IllegalArgumentException("Invalid name: " + name);
  }
}
