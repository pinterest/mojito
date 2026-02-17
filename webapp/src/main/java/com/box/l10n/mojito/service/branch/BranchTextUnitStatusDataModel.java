package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.entity.TMTextUnitVariant;
import java.time.ZonedDateTime;

public class BranchTextUnitStatusDataModel {

  public Long branchId;
  private String bcp47Tag;
  private String tuName;
  private String tuContent;
  private String tuComment;
  private String content;
  private String comment;
  private TMTextUnitVariant.Status status;
  private String repositoryName;
  private String branchName;
  private Long textUnitId;
  private Long variantId;
  private Long currentVariantId;
  private ZonedDateTime createdDate;
  private String srcLocaleBcpTag;

  public BranchTextUnitStatusDataModel() {}

  public BranchTextUnitStatusDataModel(
      Long branchId,
      String bcp47Tag,
      String tuName,
      String tuContent,
      String tuComment,
      String content,
      String comment,
      String srcLocaleBcpTag,
      TMTextUnitVariant.Status status,
      String repositoryName,
      String branchName,
      Long textUnitId,
      Long variantId,
      Long currentVariantId,
      ZonedDateTime createdDate) {
    this.branchId = branchId;
    this.bcp47Tag = bcp47Tag;
    this.tuName = tuName;
    this.tuContent = tuContent;
    this.tuComment = tuComment;
    this.content = content;
    this.comment = comment;
    this.srcLocaleBcpTag = srcLocaleBcpTag;
    this.status = status;
    this.repositoryName = repositoryName;
    this.branchName = branchName;
    this.textUnitId = textUnitId;
    this.variantId = variantId;
    this.currentVariantId = currentVariantId;
    this.createdDate = createdDate;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getBcp47Tag() {
    return bcp47Tag;
  }

  public String getTuName() {
    return tuName;
  }

  public String getTuContent() {
    return tuContent;
  }

  public String getTuComment() {
    return tuComment;
  }

  public String getContent() {
    return content;
  }

  public String getComment() {
    return comment;
  }

  public TMTextUnitVariant.Status getStatus() {
    return status;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public String getBranchName() {
    return branchName;
  }

  public Long getTextUnitId() {
    return textUnitId;
  }

  public Long getVariantId() {
    return variantId;
  }

  public Long getCurrentVariantId() {
    return currentVariantId;
  }

  public ZonedDateTime getCreatedDate() {
    return createdDate;
  }

  public String getSrcLocaleBcpTag() {
    return srcLocaleBcpTag;
  }
}
