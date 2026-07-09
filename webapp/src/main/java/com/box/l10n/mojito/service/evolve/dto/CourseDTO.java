package com.box.l10n.mojito.service.evolve.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CourseDTO {

  private int id;

  @JsonProperty("custom_j")
  private TranslationStatusType translationStatus;

  @JsonProperty("custom_i")
  private String translationMode;

  @JsonProperty("type")
  private String type;

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

  public String getTranslationMode() {
    return translationMode;
  }

  public void setTranslationMode(String translationMode) {
    this.translationMode = translationMode;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
