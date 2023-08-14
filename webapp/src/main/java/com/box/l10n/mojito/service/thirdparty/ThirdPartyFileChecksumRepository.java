package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.entity.ThirdPartyFileChecksum;
import com.box.l10n.mojito.entity.ThirdPartyFileChecksumCompositeId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ThirdPartyFileChecksumRepository
    extends JpaRepository<ThirdPartyFileChecksum, ThirdPartyFileChecksumCompositeId> {

  Optional<ThirdPartyFileChecksum> findById(Long thirdPartyFileChecksumId);

  Optional<ThirdPartyFileChecksum> findByThirdPartyFileChecksumCompositeId(
      ThirdPartyFileChecksumCompositeId thirdPartyFileChecksumCompositeId);
}
