package com.box.l10n.mojito.service.evolve.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;

public class CourseDTO {

  private int id;

  @JsonProperty("updated_on")
  private ZonedDateTime updatedOn;

  @JsonProperty("custom_j")
  private TranslationStatusType translationStatus;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public ZonedDateTime getUpdatedOn() {
    return updatedOn;
  }

  public void setUpdatedOn(ZonedDateTime updatedOn) {
    this.updatedOn = updatedOn;
  }

  public TranslationStatusType getTranslationStatus() {
    return translationStatus;
  }

  public void setTranslationStatus(TranslationStatusType translationStatus) {
    this.translationStatus = translationStatus;
  }
}
