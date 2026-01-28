package com.box.l10n.mojito.entity;

import java.util.Arrays;
import java.util.Optional;

public enum PromptType {
  SOURCE_STRING_CHECKER,
  PLURALIZATION_CHECKER,
  TRANSLATION;

  public static Optional<PromptType> fromName(String name) {
    return Arrays.stream(values()).filter(value -> value.name().equalsIgnoreCase(name)).findFirst();
  }

  public String toString() {
    return name();
  }
}
