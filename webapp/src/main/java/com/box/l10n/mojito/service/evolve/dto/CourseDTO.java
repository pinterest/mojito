package com.box.l10n.mojito.service.evolve.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CourseDTO {

  private int id;

  private String code;

  private String locale;

  @JsonProperty("custom_j")
  private TranslationStatusType translationStatus;

  @JsonProperty("custom_i")
  private String translationMode;

  @JsonProperty("type")
  private String type;

  private PictureDTO picture;

  @JsonProperty("hero_picture")
  private PictureDTO heroPicture;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public void setCode(String code) {
    this.code = code;
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

  public PictureDTO getPicture() {
    return picture;
  }

  public void setPicture(PictureDTO picture) {
    this.picture = picture;
  }

  public PictureDTO getHeroPicture() {
    return heroPicture;
  }

  public void setHeroPicture(PictureDTO heroPicture) {
    this.heroPicture = heroPicture;
  }

  public String getPictureUrl() {
    if (picture != null) {
      return picture.getTargetUrl();
    }
    return null;
  }

  public String getHeroPictureUrl() {
    if (heroPicture != null) {
      return heroPicture.getTargetUrl();
    }
    return null;
  }

  public String getHeroPictureMobileUrl() {
    if (heroPicture != null) {
      return heroPicture.getMobileTargetUrl();
    }
    return null;
  }
}
