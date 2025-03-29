package com.box.l10n.mojito.evolve;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CourseRequestDTO {
  private int id;

  @JsonProperty("custom_j")
  private TranslationStatusType translationStatus;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public TranslationStatusType getTranslationStatus() {
    return translationStatus;
  }

  public void setTranslationStatus(TranslationStatusType translationStatus) {
    this.translationStatus = translationStatus;
  }
}
