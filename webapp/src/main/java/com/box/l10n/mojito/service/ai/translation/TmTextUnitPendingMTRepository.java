package com.box.l10n.mojito.service.ai.translation;

import com.box.l10n.mojito.entity.TmTextUnitPendingMT;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TmTextUnitPendingMTRepository extends JpaRepository<TmTextUnitPendingMT, Long> {

  TmTextUnitPendingMT findByTmTextUnitId(Long tmTextUnitId);
}
