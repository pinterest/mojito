package com.box.l10n.mojito.service.branch;

import java.time.ZonedDateTime;
import java.util.List;

public class BranchStatusTextUnitDTO {

  private Long tmTextUnitId;
  private String name;
  private String content;
  private String comment;
  private ZonedDateTime createdDate;
  private Long forTranslationCount;
  private List<String> missingLocales;

  public Long getTmTextUnitId() {
    return tmTextUnitId;
  }

  public void setTmTextUnitId(Long tmTextUnitId) {
    this.tmTextUnitId = tmTextUnitId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public ZonedDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(ZonedDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public Long getForTranslationCount() {
    return forTranslationCount;
  }

  public void setForTranslationCount(Long forTranslationCount) {
    this.forTranslationCount = forTranslationCount;
  }

  public List<String> getMissingLocales() {
    return missingLocales;
  }

  public void setMissingLocales(List<String> missingLocales) {
    this.missingLocales = missingLocales;
  }
}
