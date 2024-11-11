package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedBy;

/**
 * Represents a text unit.
 *
 * <p>A text unit is an entity that needs to be translated from a source language into multiple
 * target languages.
 *
 * <p>A text unit is uniquely identified within a {@link TM} by its name + content + comment (which
 * is also the MD5 value).
 *
 * @author jaurambault
 */
@Entity
@Table(
    name = "tm_text_unit",
    indexes = {
      @Index(
          name = "UK__TM_TEXT_UNIT__MD5__TM_ID__ASSET_ID",
          columnList = "md5, tm_id, asset_id",
          unique = true),
      @Index(name = "I__TM_TEXT_UNIT__NAME", columnList = "name"),
      @Index(name = "I__TM_TEXT_UNIT__CONTENT_MD5", columnList = "content_md5"),
      @Index(name = "I__TM_TEXT_UNIT__PLURAL_FORM_OTHER", columnList = "plural_form_other")
    })
@BatchSize(size = 1000)
@NamedEntityGraph(
    name = "TMTextUnit.legacy",
    attributeNodes = {@NamedAttributeNode("asset")})
public class TMTextUnit extends SettableAuditableEntity {

  @JsonView(View.IdAndName.class)
  @Column(name = "name", length = Integer.MAX_VALUE)
  private String name;

  @JsonView(View.TmTextUnitSummary.class)
  @Column(name = "content", length = Integer.MAX_VALUE)
  private String content;

  /** should be built from the name, content and the comment field */
  @JsonView(View.Default.class)
  @Column(name = "md5", length = 32)
  String md5;

  /** should be built from the content only */
  @JsonView(View.Default.class)
  @Column(name = "content_md5", length = 32)
  private String contentMd5;

  @JsonView(View.Default.class)
  @Column(name = "comment", length = Integer.MAX_VALUE)
  private String comment;

  @JsonView(View.Default.class)
  @Column(name = "word_count")
  private Integer wordCount;

  @JsonView(View.Default.class)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tm_id", foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT__TM__ID"))
  private TM tm;

  @JsonView(View.Default.class)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "asset_id",
      foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT__ASSET__ID"),
      nullable = false)
  private Asset asset;

  @JsonView(View.Default.class)
  @CreatedBy
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = BaseEntity.CreatedByUserColumnName,
      foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT__USER__ID"))
  protected User createdByUser;

  @JsonView(View.Default.class)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "plural_form_id",
      foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT__PLURAL_FORM__ID"))
  protected PluralForm pluralForm;

  @JsonView(View.Default.class)
  @Column(name = "plural_form_other", length = Integer.MAX_VALUE)
  protected String pluralFormOther;

  @JsonView(View.Default.class)
  @OneToOne(mappedBy = "tmTextUnit", fetch = FetchType.LAZY)
  protected TMTextUnitStatistic tmTextUnitStatistic;

  public User getCreatedByUser() {
    return createdByUser;
  }

  public void setCreatedByUser(User createdByUser) {
    this.createdByUser = createdByUser;
  }

  public Asset getAsset() {
    return asset;
  }

  public void setAsset(Asset asset) {
    this.asset = asset;
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

  public String getMd5() {
    return md5;
  }

  public void setMd5(String md5) {
    this.md5 = md5;
  }

  public String getContentMd5() {
    return contentMd5;
  }

  public void setContentMd5(String contentMd5) {
    this.contentMd5 = contentMd5;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Integer getWordCount() {
    return wordCount;
  }

  public void setWordCount(Integer wordCount) {
    this.wordCount = wordCount;
  }

  public TM getTm() {
    return tm;
  }

  public void setTm(TM tm) {
    this.tm = tm;
  }

  public PluralForm getPluralForm() {
    return pluralForm;
  }

  public void setPluralForm(PluralForm pluralForm) {
    this.pluralForm = pluralForm;
  }

  public String getPluralFormOther() {
    return pluralFormOther;
  }

  public void setPluralFormOther(String pluralFormOther) {
    this.pluralFormOther = pluralFormOther;
  }

  public TMTextUnitStatistic getTmTextUnitStatistic() {
    return tmTextUnitStatistic;
  }

  public void setTmTextUnitStatistic(TMTextUnitStatistic tmTextUnitStatistic) {
    this.tmTextUnitStatistic = tmTextUnitStatistic;
  }
}
