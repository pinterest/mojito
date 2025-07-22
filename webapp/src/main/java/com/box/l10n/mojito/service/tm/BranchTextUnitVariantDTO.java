package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.TMTextUnitVariant;

/**
 * DTO representing a single row from the branch text unit variant query.
 *
 * @author @mallen
 */
public class BranchTextUnitVariantDTO {

  private Long textUnitId;
  private String textUnitName;
  private String textUnitContent;
  private String textUnitComment;
  private String branchName;
  private Long variantId;
  private String variantContent;
  private TMTextUnitVariant.Status variantStatus;
  private String localeCode;

  public BranchTextUnitVariantDTO() {}

  public BranchTextUnitVariantDTO(
      Long textUnitId,
      String textUnitName,
      String textUnitContent,
      String textUnitComment,
      String branchName,
      Long variantId,
      String variantContent,
      TMTextUnitVariant.Status variantStatus,
      String localeCode) {
    this.textUnitId = textUnitId;
    this.textUnitName = textUnitName;
    this.textUnitContent = textUnitContent;
    this.textUnitComment = textUnitComment;
    this.branchName = branchName;
    this.variantId = variantId;
    this.variantContent = variantContent;
    this.variantStatus = variantStatus;
    this.localeCode = localeCode;
  }

  public BranchTextUnitVariantDTO(
      Long textUnitId,
      String textUnitName,
      String textUnitContent,
      String textUnitComment,
      String branchName,
      String localeCode) {
    this.textUnitId = textUnitId;
    this.textUnitName = textUnitName;
    this.textUnitContent = textUnitContent;
    this.textUnitComment = textUnitComment;
    this.branchName = branchName;
    this.localeCode = localeCode;
  }

  /** Checks if this represents a missing variant (no variant exists yet for this locale) */
  public boolean isMissingVariant() {
    return variantId == null;
  }

  // Getters and setters
  public Long getTextUnitId() {
    return textUnitId;
  }

  public void setTextUnitId(Long textUnitId) {
    this.textUnitId = textUnitId;
  }

  public String getTextUnitName() {
    return textUnitName;
  }

  public void setTextUnitName(String textUnitName) {
    this.textUnitName = textUnitName;
  }

  public String getTextUnitContent() {
    return textUnitContent;
  }

  public void setTextUnitContent(String textUnitContent) {
    this.textUnitContent = textUnitContent;
  }

  public String getTextUnitComment() {
    return textUnitComment;
  }

  public void setTextUnitComment(String textUnitComment) {
    this.textUnitComment = textUnitComment;
  }

  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public Long getVariantId() {
    return variantId;
  }

  public void setVariantId(Long variantId) {
    this.variantId = variantId;
  }

  public String getVariantContent() {
    return variantContent;
  }

  public void setVariantContent(String variantContent) {
    this.variantContent = variantContent;
  }

  public TMTextUnitVariant.Status getVariantStatus() {
    return variantStatus;
  }

  public void setVariantStatus(TMTextUnitVariant.Status variantStatus) {
    this.variantStatus = variantStatus;
  }

  public String getLocaleCode() {
    return localeCode;
  }

  public void setLocaleCode(String localeCode) {
    this.localeCode = localeCode;
  }
}
