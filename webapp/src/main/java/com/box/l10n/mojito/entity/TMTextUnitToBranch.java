package com.box.l10n.mojito.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tm_text_unit_to_branch")
public class TMTextUnitToBranch extends BaseEntity {
  @ManyToOne
  @JoinColumn(name = "tm_text_unit_id")
  private TMTextUnit tmTextUnit;

  @ManyToOne
  @JoinColumn(name = "branch_id")
  private Branch branch;

  public TMTextUnit getTmTextUnit() {
    return tmTextUnit;
  }

  public void setTmTextUnit(TMTextUnit tmTextUnit) {
    this.tmTextUnit = tmTextUnit;
  }

  public Branch getBranch() {
    return branch;
  }

  public void setBranch(Branch branch) {
    this.branch = branch;
  }
}
