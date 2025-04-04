package com.box.l10n.mojito.service.evolve;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.service.assetcontent.S3ContentService;
import com.box.l10n.mojito.service.blobstorage.s3.S3BlobStorage;
import com.google.common.base.Preconditions;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.slf4j.Logger;

public class S3SyncDateService implements SyncDateService {
  private static final Logger log = getLogger(S3ContentService.class);

  private final S3BlobStorage s3BlobStorage;

  private final String s3FilePath;

  public S3SyncDateService(S3BlobStorage s3BlobStorage, String s3FilePath) {
    Preconditions.checkNotNull(s3BlobStorage);
    Preconditions.checkNotNull(s3FilePath);
    Preconditions.checkArgument(!s3FilePath.isEmpty());

    this.s3BlobStorage = s3BlobStorage;
    this.s3FilePath = s3FilePath;
  }

  @Override
  public ZonedDateTime getDate() {
    Optional<String> date = this.s3BlobStorage.getString(this.s3FilePath);
    if (date.isPresent()) {
      log.debug("Retrieved date: {}", date.get());
      try {
        return ZonedDateTime.parse(date.get());
      } catch (DateTimeParseException e) {
        log.error("Date format is invalid", e);
      }
    }
    log.info("Date does not exists or is invalid");
    return null;
  }

  @Override
  public void setDate(ZonedDateTime date) {
    log.debug("Upload date: {}", date.toString());
    s3BlobStorage.put(this.s3FilePath, date.toString());
  }
}
