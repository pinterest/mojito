package com.box.l10n.mojito.entity;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

/** Entity that stores the checksum of a translated file downloaded via a third party sync. */
@Entity
@Table(
    name = "third_party_sync_file_checksum",
    indexes = {
      @Index(
          name = "I__TP_FILE_CHECKSUM__REPO_ID__LOCALE_ID__FILE_NAME",
          columnList = "repository_id, locale_id, file_name",
          unique = true),
    })
public class ThirdPartyFileChecksum extends AuditableEntity {

  @Embedded private ThirdPartyFileChecksumCompositeId thirdPartyFileChecksumCompositeId;

  @Column(name = "md5")
  private String md5;

  public ThirdPartyFileChecksum() {}

  public ThirdPartyFileChecksum(
      ThirdPartyFileChecksumCompositeId thirdPartyFileChecksumCompositeId, String md5) {
    this.thirdPartyFileChecksumCompositeId = thirdPartyFileChecksumCompositeId;
    this.md5 = md5;
  }

  public ThirdPartyFileChecksumCompositeId getThirdPartyFileChecksumCompositeId() {
    return thirdPartyFileChecksumCompositeId;
  }

  public void setThirdPartyFileChecksumCompositeId(
      ThirdPartyFileChecksumCompositeId thirdPartyFileChecksumCompositeId) {
    this.thirdPartyFileChecksumCompositeId = thirdPartyFileChecksumCompositeId;
  }

  public String getMd5() {
    return md5;
  }

  public void setMd5(String checksum) {
    this.md5 = checksum;
  }

  public Locale getLocale() {
    return thirdPartyFileChecksumCompositeId.getLocale();
  }

  public Repository getRepository() {
    return thirdPartyFileChecksumCompositeId.getRepository();
  }

  public String getFileName() {
    return thirdPartyFileChecksumCompositeId.getFileName();
  }
}
