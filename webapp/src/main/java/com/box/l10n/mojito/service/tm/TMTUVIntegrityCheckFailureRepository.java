package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.TMTUVIntegrityCheckFailure;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface TMTUVIntegrityCheckFailureRepository
    extends JpaRepository<TMTUVIntegrityCheckFailure, Long> {
  List<TMTUVIntegrityCheckFailure> findByTmTextUnitIdAndLocaleId(Long tmTextUnitId, Long localeId);
}
