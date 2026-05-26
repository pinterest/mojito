package com.box.l10n.mojito.rest.rewriterule;

import com.box.l10n.mojito.entity.RewriteRule;
import java.time.ZonedDateTime;

public class RewriteRuleDTO {

  Long id;
  ZonedDateTime lastModifiedDate;
  Long repositoryId;
  Long localeId;
  String rewriteFrom;
  String rewriteTo;
  Boolean enabled;
  Long createdByUserId;
  String createdByUserName;

  public static RewriteRuleDTO fromEntity(RewriteRule rewriteRule) {
    RewriteRuleDTO dto = new RewriteRuleDTO();
    dto.setId(rewriteRule.getId());
    dto.setLastModifiedDate(rewriteRule.getLastModifiedDate());
    dto.setRepositoryId(
        rewriteRule.getRepository() == null ? null : rewriteRule.getRepository().getId());
    dto.setLocaleId(rewriteRule.getLocale() == null ? null : rewriteRule.getLocale().getId());
    dto.setRewriteFrom(rewriteRule.getRewriteFrom());
    dto.setRewriteTo(rewriteRule.getRewriteTo());
    dto.setEnabled(rewriteRule.isEnabled());
    dto.setCreatedByUserId(
        rewriteRule.getCreatedByUser() == null ? null : rewriteRule.getCreatedByUser().getId());
    dto.setCreatedByUserName(
        rewriteRule.getCreatedByUser() == null
            ? null
            : rewriteRule.getCreatedByUser().getUsername());
    return dto;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ZonedDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(ZonedDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public Long getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(Long repositoryId) {
    this.repositoryId = repositoryId;
  }

  public Long getLocaleId() {
    return localeId;
  }

  public void setLocaleId(Long localeId) {
    this.localeId = localeId;
  }

  public String getRewriteFrom() {
    return rewriteFrom;
  }

  public void setRewriteFrom(String rewriteFrom) {
    this.rewriteFrom = rewriteFrom;
  }

  public String getRewriteTo() {
    return rewriteTo;
  }

  public void setRewriteTo(String rewriteTo) {
    this.rewriteTo = rewriteTo;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Long getCreatedByUserId() {
    return createdByUserId;
  }

  public void setCreatedByUserId(Long createdByUserId) {
    this.createdByUserId = createdByUserId;
  }

  public String getCreatedByUserName() {
    return createdByUserName;
  }

  public void setCreatedByUserName(String createdByUserName) {
    this.createdByUserName = createdByUserName;
  }
}
