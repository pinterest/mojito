package com.box.l10n.mojito.entity;

import static java.util.Optional.ofNullable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "tm_text_unit_variant_integrity_check_failure")
public class TMTUVIntegrityCheckFailure extends SettableAuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "tm_text_unit_id",
      foreignKey = @ForeignKey(name = "FK__TM_TUV_ICF__TM_TEXT_UNIT__ID"))
  private TMTextUnit tmTextUnit;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "locale_id", foreignKey = @ForeignKey(name = "FK__TM_TUV_ICF__LOCALE__ID"))
  private Locale locale;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "tm_text_unit_variant_id",
      foreignKey = @ForeignKey(name = "FK__TM_TUV_ICF__TM_TEXT_UNIT_VARIANT__ID"))
  private TMTextUnitVariant tmTextUnitVariant;

  @Column(name = "integrity_failure_name")
  private String integrityFailureName;

  public TMTextUnit getTmTextUnit() {
    return tmTextUnit;
  }

  public void setTmTextUnit(TMTextUnit tmTextUnit) {
    this.tmTextUnit = tmTextUnit;
  }

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public TMTextUnitVariant getTmTextUnitVariant() {
    return tmTextUnitVariant;
  }

  public void setTmTextUnitVariant(TMTextUnitVariant tmTextUnitVariant) {
    this.tmTextUnitVariant = tmTextUnitVariant;
  }

  public String getIntegrityFailureName() {
    return integrityFailureName;
  }

  public void setIntegrityFailureName(String integrityFailureName) {
    this.integrityFailureName = integrityFailureName;
  }

  public Long getLocale_Id() {
    return ofNullable(this.locale).map(Locale::getId).orElse(null);
  }

  public Long getTmTextUnit_Id() {
    return ofNullable(this.tmTextUnit).map(TMTextUnit::getId).orElse(null);
  }

  public Long getTmTextUnitVariant_Id() {
    return ofNullable(this.tmTextUnitVariant).map(TMTextUnitVariant::getId).orElse(null);
  }

  public TMTextUnitVariant.Status getTextUnitVariantStatus() {
    return ofNullable(this.tmTextUnitVariant).map(TMTextUnitVariant::getStatus).orElse(null);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    TMTUVIntegrityCheckFailure other = (TMTUVIntegrityCheckFailure) obj;
    return Objects.equals(this.getTmTextUnit_Id(), other.getTmTextUnit_Id())
        && Objects.equals(this.getLocale(), other.getLocale())
        && Objects.equals(this.getTmTextUnitVariant_Id(), other.getTmTextUnitVariant_Id())
        && Objects.equals(this.getIntegrityFailureName(), other.getIntegrityFailureName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.getTmTextUnit_Id(),
        this.getLocale(),
        this.getTmTextUnitVariant_Id(),
        this.getIntegrityFailureName());
  }
}
