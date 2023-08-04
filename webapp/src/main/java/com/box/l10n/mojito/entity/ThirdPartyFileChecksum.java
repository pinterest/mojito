package com.box.l10n.mojito.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

/** Entity that stores the checksum of a translated file downloaded via a third party sync. */
@Entity
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@Table(
    name = "third_party_sync_file_checksum",
    indexes = {
      @Index(
          name = "UK__THIRD_PARTY_FILE_CHECKSUM__REPOSITORY_ID__FILE_NAME",
          columnList = "repository_id, file_name",
          unique = true),
    })
public class ThirdPartyFileChecksum implements Serializable {

  @EmbeddedId private ThirdPartyFileChecksumId thirdPartyFileChecksumId;

  @Column(name = "checksum")
  private String checksum;

  public ThirdPartyFileChecksum() {}

  public ThirdPartyFileChecksum(
      ThirdPartyFileChecksumId thirdPartyFileChecksumId, String checksum) {
    this.thirdPartyFileChecksumId = thirdPartyFileChecksumId;
    this.checksum = checksum;
  }

  public ThirdPartyFileChecksumId getThirdPartyFileChecksumId() {
    return thirdPartyFileChecksumId;
  }

  public void setThirdPartyFileChecksumId(ThirdPartyFileChecksumId thirdPartyFileChecksumId) {
    this.thirdPartyFileChecksumId = thirdPartyFileChecksumId;
  }

  public String getChecksum() {
    return checksum;
  }

  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  public Locale getLocale() {
    return thirdPartyFileChecksumId.getLocale();
  }

  public Repository getRepository() {
    return thirdPartyFileChecksumId.getRepository();
  }

  public String getFileName() {
    return thirdPartyFileChecksumId.getFileName();
  }
}
