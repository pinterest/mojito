package com.box.l10n.mojito.service.evolve;

import java.time.ZonedDateTime;

public interface SyncDateService {
  ZonedDateTime getDate();

  void setDate(ZonedDateTime date);
}
