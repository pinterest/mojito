package com.box.l10n.mojito.rest.textunit;

import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.List;

/**
 * DTO representing a text unit with its non-approved variant statuses for a specific branch.
 *
 * @author @maallen
 */
public class BranchTextUnitVariantStatusDTO {

  @JsonView(View.TranslationHistorySummary.class)
  private Long tmTextUnitId;

  @JsonView(View.TranslationHistorySummary.class)
  private String name;

  @JsonView(View.TranslationHistorySummary.class)
  private String content;

  @JsonView(View.TranslationHistorySummary.class)
  private String comment;

  @JsonView(View.TranslationHistorySummary.class)
  private String branchName;

  @JsonView(View.TranslationHistorySummary.class)
  private List<UnapprovedVariantDTO> unapprovedVariants;

  public static class UnapprovedVariantDTO {

    @JsonView(View.TranslationHistorySummary.class)
    private Long variantId;

    @JsonView(View.TranslationHistorySummary.class)
    private String localeCode;

    @JsonView(View.TranslationHistorySummary.class)
    private String variantContent;

    @JsonView(View.TranslationHistorySummary.class)
    private TMTextUnitVariant.Status status;

    public UnapprovedVariantDTO() {}

    public UnapprovedVariantDTO(
        Long variantId, String localeCode, String variantContent, TMTextUnitVariant.Status status) {
      this.variantId = variantId;
      this.localeCode = localeCode;
      this.variantContent = variantContent;
      this.status = status;
    }

    public Long getVariantId() {
      return variantId;
    }

    public void setVariantId(Long variantId) {
      this.variantId = variantId;
    }

    public String getLocaleCode() {
      return localeCode;
    }

    public void setLocaleCode(String localeCode) {
      this.localeCode = localeCode;
    }

    public String getVariantContent() {
      return variantContent;
    }

    public void setVariantContent(String variantContent) {
      this.variantContent = variantContent;
    }

    public TMTextUnitVariant.Status getStatus() {
      return status;
    }

    public void setStatus(TMTextUnitVariant.Status status) {
      this.status = status;
    }
  }

  public BranchTextUnitVariantStatusDTO() {}

  public BranchTextUnitVariantStatusDTO(
      Long tmTextUnitId,
      String name,
      String content,
      String comment,
      String branchName,
      List<UnapprovedVariantDTO> unapprovedVariants) {
    this.tmTextUnitId = tmTextUnitId;
    this.name = name;
    this.content = content;
    this.comment = comment;
    this.branchName = branchName;
    this.unapprovedVariants = unapprovedVariants;
  }

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

  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public List<UnapprovedVariantDTO> getUnapprovedVariants() {
    return unapprovedVariants;
  }

  public void setUnapprovedVariants(List<UnapprovedVariantDTO> unapprovedVariants) {
    this.unapprovedVariants = unapprovedVariants;
  }
}
