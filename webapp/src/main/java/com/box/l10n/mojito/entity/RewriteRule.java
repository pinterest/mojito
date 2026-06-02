package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedBy;

@Entity
@Table(name = "rewrite_rules")
public class RewriteRule extends AuditableEntity {

  @ManyToOne
  @JoinColumn(
      name = "repository_id",
      foreignKey = @ForeignKey(name = "FK__REWRITE_RULES__REPOSITORY__ID"))
  private Repository repository;

  @ManyToOne(optional = false)
  @JoinColumn(
      name = "locale_id",
      foreignKey = @ForeignKey(name = "FK__REWRITE_RULES__LOCALE__ID"),
      nullable = false)
  private Locale locale;

  @Basic(optional = false)
  @Column(name = "rewrite_from", length = 512, nullable = false)
  private String rewriteFrom;

  @Basic(optional = false)
  @Column(name = "rewrite_to", length = 512, nullable = false)
  private String rewriteTo;

  @Basic(optional = false)
  @Column(name = "enabled", nullable = false)
  private Boolean enabled;

  @CreatedBy
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = BaseEntity.CreatedByUserColumnName,
      foreignKey = @ForeignKey(name = "FK__REWRITE_RULES__USER__ID"))
  @JsonView(View.AssetSummary.class)
  private User createdByUser;

  public Repository getRepository() {
    return repository;
  }

  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
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

  public Boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public User getCreatedByUser() {
    return createdByUser;
  }

  public void setCreatedByUser(User createdByUser) {
    this.createdByUser = createdByUser;
  }
}
