package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.ThirdPartyFileChecksum;
import com.box.l10n.mojito.entity.ThirdPartyFileChecksumCompositeId;
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
    ThirdPartyFileChecksumCompositeId thirdPartyFileChecksumCompositeId =
        new ThirdPartyFileChecksumCompositeId(repository, locale, fileName);

    Optional<ThirdPartyFileChecksum> thirdPartyFileChecksumOpt =
        thirdPartyFileChecksumRepository.findByThirdPartyFileChecksumCompositeId(
            thirdPartyFileChecksumCompositeId);
    if (thirdPartyFileChecksumOpt.isPresent()
        && thirdPartyFileChecksumOpt.get().getMd5().equals(currentChecksum)) {
      return true;
    } else if (thirdPartyFileChecksumOpt.isPresent()) {
      ThirdPartyFileChecksum thirdPartyFileChecksum = thirdPartyFileChecksumOpt.get();
      thirdPartyFileChecksum.setMd5(currentChecksum);
      thirdPartyFileChecksumRepository.save(thirdPartyFileChecksum);
    } else {
      thirdPartyFileChecksumRepository.save(
          new ThirdPartyFileChecksum(thirdPartyFileChecksumCompositeId, currentChecksum));
    }

    return false;
  }
}
