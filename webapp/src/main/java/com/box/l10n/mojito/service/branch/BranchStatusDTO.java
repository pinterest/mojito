package com.box.l10n.mojito.service.branch;

import java.time.ZonedDateTime;
import java.util.List;

public class BranchStatusDTO {

  private Long branchId;
  private String branchName;
  private String repositoryName;
  private ZonedDateTime createdDate;
  private long forTranslationCount;
  private ZonedDateTime translatedDate;
  private boolean safeI18nEnabled;
  private String mergeTargetCommitName;
  private List<BranchStatusTextUnitDTO> textUnits;

  public Long getBranchId() {
    return branchId;
  }

  public void setBranchId(Long branchId) {
    this.branchId = branchId;
  }

  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public ZonedDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(ZonedDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public long getForTranslationCount() {
    return forTranslationCount;
  }

  public void setForTranslationCount(long forTranslationCount) {
    this.forTranslationCount = forTranslationCount;
  }

  public ZonedDateTime getTranslatedDate() {
    return translatedDate;
  }

  public void setTranslatedDate(ZonedDateTime translatedDate) {
    this.translatedDate = translatedDate;
  }

  public boolean isSafeI18nEnabled() {
    return safeI18nEnabled;
  }

  public void setSafeI18nEnabled(boolean safeI18nEnabled) {
    this.safeI18nEnabled = safeI18nEnabled;
  }

  public String getMergeTargetCommitName() {
    return mergeTargetCommitName;
  }

  public void setMergeTargetCommitName(String mergeTargetCommitName) {
    this.mergeTargetCommitName = mergeTargetCommitName;
  }

  public List<BranchStatusTextUnitDTO> getTextUnits() {
    return textUnits;
  }

  public void setTextUnits(List<BranchStatusTextUnitDTO> textUnits) {
    this.textUnits = textUnits;
  }
}
