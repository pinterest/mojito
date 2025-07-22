package com.box.l10n.mojito.rest.textunit;

import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.List;

/**
 * DTO containing translation status for a branch including statistics and variant details.
 *
 * @author @maallen
 */
public class BranchTranslationStatusDTO {

  @JsonView(View.TranslationHistorySummary.class)
  private String branchName;

  @JsonView(View.TranslationHistorySummary.class)
  private TranslationStatistics statistics;

  @JsonView(View.TranslationHistorySummary.class)
  private List<TextUnitTranslationStatusDTO> textUnits;

  public static class TranslationStatistics {

    @JsonView(View.TranslationHistorySummary.class)
    private int totalApprovedTranslations;

    @JsonView(View.TranslationHistorySummary.class)
    private int totalInProgressTranslations; // unapproved + missing translations

    @JsonView(View.TranslationHistorySummary.class)
    private int totalTextUnits;

    @JsonView(View.TranslationHistorySummary.class)
    private int totalConfiguredLocales;

    @JsonView(View.TranslationHistorySummary.class)
    private int totalExpectedTranslations; // totalTextUnits * totalConfiguredLocales

    @JsonView(View.TranslationHistorySummary.class)
    private double
        completionPercentage; // (totalApprovedTranslations / totalExpectedTranslations) * 100

    public TranslationStatistics() {}

    public TranslationStatistics(
        int totalApprovedTranslations,
        int totalInProgressTranslations,
        int totalTextUnits,
        int totalConfiguredLocales) {
      this.totalApprovedTranslations = totalApprovedTranslations;
      this.totalInProgressTranslations = totalInProgressTranslations;
      this.totalTextUnits = totalTextUnits;
      this.totalConfiguredLocales = totalConfiguredLocales;
      this.totalExpectedTranslations = totalTextUnits * totalConfiguredLocales;
      this.completionPercentage =
          totalExpectedTranslations > 0
              ? (double) totalApprovedTranslations / totalExpectedTranslations * 100.0
              : 0.0;
    }

    // Getters and setters
    public int getTotalApprovedTranslations() {
      return totalApprovedTranslations;
    }

    public void setTotalApprovedTranslations(int totalApprovedTranslations) {
      this.totalApprovedTranslations = totalApprovedTranslations;
    }

    public int getTotalInProgressTranslations() {
      return totalInProgressTranslations;
    }

    public void setTotalInProgressTranslations(int totalInProgressTranslations) {
      this.totalInProgressTranslations = totalInProgressTranslations;
    }

    public int getTotalTextUnits() {
      return totalTextUnits;
    }

    public void setTotalTextUnits(int totalTextUnits) {
      this.totalTextUnits = totalTextUnits;
    }

    public int getTotalConfiguredLocales() {
      return totalConfiguredLocales;
    }

    public void setTotalConfiguredLocales(int totalConfiguredLocales) {
      this.totalConfiguredLocales = totalConfiguredLocales;
    }

    public int getTotalExpectedTranslations() {
      return totalExpectedTranslations;
    }

    public void setTotalExpectedTranslations(int totalExpectedTranslations) {
      this.totalExpectedTranslations = totalExpectedTranslations;
    }

    public double getCompletionPercentage() {
      return completionPercentage;
    }

    public void setCompletionPercentage(double completionPercentage) {
      this.completionPercentage = completionPercentage;
    }
  }

  public static class TextUnitTranslationStatusDTO {

    @JsonView(View.TranslationHistorySummary.class)
    private Long tmTextUnitId;

    @JsonView(View.TranslationHistorySummary.class)
    private String name;

    @JsonView(View.TranslationHistorySummary.class)
    private String content;

    @JsonView(View.TranslationHistorySummary.class)
    private String comment;

    @JsonView(View.TranslationHistorySummary.class)
    private List<VariantDTO> approvedVariants;

    @JsonView(View.TranslationHistorySummary.class)
    private List<VariantDTO> remainingVariants;

    @JsonView(View.TranslationHistorySummary.class)
    private List<MissingVariantDTO> missingVariants;

    public TextUnitTranslationStatusDTO() {}

    public TextUnitTranslationStatusDTO(
        Long tmTextUnitId, String name, String content, String comment) {
      this.tmTextUnitId = tmTextUnitId;
      this.name = name;
      this.content = content;
      this.comment = comment;
    }

    // Getters and setters
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

    public List<VariantDTO> getApprovedVariants() {
      return approvedVariants;
    }

    public void setApprovedVariants(List<VariantDTO> approvedVariants) {
      this.approvedVariants = approvedVariants;
    }

    public List<VariantDTO> getRemainingVariants() {
      return remainingVariants;
    }

    public void setRemainingVariants(List<VariantDTO> remainingVariants) {
      this.remainingVariants = remainingVariants;
    }

    public List<MissingVariantDTO> getMissingVariants() {
      return missingVariants;
    }

    public void setMissingVariants(List<MissingVariantDTO> missingVariants) {
      this.missingVariants = missingVariants;
    }
  }

  public static class VariantDTO {

    @JsonView(View.TranslationHistorySummary.class)
    private Long variantId;

    @JsonView(View.TranslationHistorySummary.class)
    private String localeCode;

    @JsonView(View.TranslationHistorySummary.class)
    private String variantContent;

    @JsonView(View.TranslationHistorySummary.class)
    private TMTextUnitVariant.Status status;

    public VariantDTO() {}

    public VariantDTO(
        Long variantId, String localeCode, String variantContent, TMTextUnitVariant.Status status) {
      this.variantId = variantId;
      this.localeCode = localeCode;
      this.variantContent = variantContent;
      this.status = status;
    }

    // Getters and setters
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

  public static class MissingVariantDTO {

    @JsonView(View.TranslationHistorySummary.class)
    private String localeCode;

    public MissingVariantDTO() {}

    public MissingVariantDTO(String localeCode) {
      this.localeCode = localeCode;
    }

    // Getters and setters
    public String getLocaleCode() {
      return localeCode;
    }

    public void setLocaleCode(String localeCode) {
      this.localeCode = localeCode;
    }
  }

  public BranchTranslationStatusDTO() {}

  public BranchTranslationStatusDTO(
      String branchName,
      TranslationStatistics statistics,
      List<TextUnitTranslationStatusDTO> textUnits) {
    this.branchName = branchName;
    this.statistics = statistics;
    this.textUnits = textUnits;
  }

  // Getters and setters
  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public TranslationStatistics getStatistics() {
    return statistics;
  }

  public void setStatistics(TranslationStatistics statistics) {
    this.statistics = statistics;
  }

  public List<TextUnitTranslationStatusDTO> getTextUnits() {
    return textUnits;
  }

  public void setTextUnits(List<TextUnitTranslationStatusDTO> textUnits) {
    this.textUnits = textUnits;
  }
}
