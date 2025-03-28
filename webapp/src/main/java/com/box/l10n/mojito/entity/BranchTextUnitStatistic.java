package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonBackReference;
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
import jakarta.persistence.Table;

/**
 * Entity to keep track of branch statistics.
 *
 * @author jeanaurambault
 */
@Entity
@Table(
    name = "branch_text_unit_statistic",
    indexes = {
      @Index(
          name = "UK__BTU_STAT__BTU_STAT_ID__TM_TEXT_UNIT_ID",
          columnList = "branch_statistic_id, tm_text_unit_id",
          unique = true)
    })
@NamedEntityGraph(
    name = "BranchTextUnitStatisticGraph",
    attributeNodes = {@NamedAttributeNode(value = "tmTextUnit")})
public class BranchTextUnitStatistic extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JsonBackReference
  @JoinColumn(
      name = "branch_statistic_id",
      foreignKey = @ForeignKey(name = "FK__BTU_STAT__BRANCH_STATISTIC__ID"))
  private BranchStatistic branchStatistic;

  @JsonView({View.BranchStatistic.class, View.BranchTextUnitStatistic.class})
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "tm_text_unit_id",
      foreignKey = @ForeignKey(name = "FK__BTU_STAT_BRANCH__TM_TEXT_UNIT__ID"))
  private TMTextUnit tmTextUnit;

  @JsonView({View.BranchStatistic.class, View.BranchTextUnitStatistic.class})
  @Column(name = "for_translation_count")
  private Long forTranslationCount = 0L;

  @JsonView({View.BranchStatistic.class, View.BranchTextUnitStatistic.class})
  @Column(name = "total_count")
  private Long totalCount = 0L;

  public BranchStatistic getBranchStatistic() {
    return branchStatistic;
  }

  public void setBranchStatistic(BranchStatistic branchStatistic) {
    this.branchStatistic = branchStatistic;
  }

  public TMTextUnit getTmTextUnit() {
    return tmTextUnit;
  }

  public void setTmTextUnit(TMTextUnit tmTextUnit) {
    this.tmTextUnit = tmTextUnit;
  }

  public Long getForTranslationCount() {
    return forTranslationCount;
  }

  public void setForTranslationCount(Long forTranslationCount) {
    this.forTranslationCount = forTranslationCount;
  }

  public Long getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(Long totalCount) {
    this.totalCount = totalCount;
  }
}
