package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.ThirdPartyFileChecksum;
import com.box.l10n.mojito.entity.ThirdPartyFileChecksumId;
import java.util.Optional;
import org.apache.commons.codec.digest.DigestUtils;

public class ThirdPartyTMSUtils {

  public static boolean isFileEqualToPreviousRun(
      ThirdPartyFileChecksumRepository thirdPartyFileChecksumRepository,
      Repository repository,
      Locale locale,
      String fileName,
      String fileContent) {

    String currentChecksum = DigestUtils.md5Hex(fileContent);
    ThirdPartyFileChecksumId thirdPartyFileChecksumId =
        new ThirdPartyFileChecksumId(repository, locale, fileName);

    Optional<ThirdPartyFileChecksum> thirdPartyFileChecksum =
        thirdPartyFileChecksumRepository.findByThirdPartyFileChecksumId(thirdPartyFileChecksumId);
    if (thirdPartyFileChecksum.isPresent()
        && thirdPartyFileChecksum.get().getChecksum().equals(currentChecksum)) {
      return true;
    } else {
      thirdPartyFileChecksumRepository.save(
          new ThirdPartyFileChecksum(thirdPartyFileChecksumId, currentChecksum));
    }

    return false;
  }
}
