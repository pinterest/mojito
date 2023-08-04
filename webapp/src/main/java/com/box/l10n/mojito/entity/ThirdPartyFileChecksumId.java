package com.box.l10n.mojito.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Embeddable
public class ThirdPartyFileChecksumId implements Serializable {

  @ManyToOne(optional = false)
  @JoinColumn(
      name = "repository_id",
      foreignKey = @ForeignKey(name = "FK__THIRD_PARTY_CHECKSUM__REPO__ID"))
  private Repository repository;

  @Column(name = "file_name")
  private String fileName;

  @ManyToOne(optional = false)
  @JoinColumn(
      name = "locale_id",
      foreignKey = @ForeignKey(name = "FK__THIRD_PARTY_CHECKSUM__LOCALE__ID"))
  private Locale locale;

  public ThirdPartyFileChecksumId() {}

  public ThirdPartyFileChecksumId(Repository repository, Locale locale, String fileName) {
    this.repository = repository;
    this.locale = locale;
    this.fileName = fileName;
  }

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public Repository getRepository() {
    return repository;
  }

  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }
}
