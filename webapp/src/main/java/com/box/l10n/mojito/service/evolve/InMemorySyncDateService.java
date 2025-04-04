package com.box.l10n.mojito.service.evolve;

import java.time.ZonedDateTime;

public class InMemorySyncDateService implements SyncDateService {
  private ZonedDateTime syncDate;

  @Override
  public ZonedDateTime getDate() {
    return this.syncDate;
  }

  @Override
  public void setDate(ZonedDateTime date) {
    this.syncDate = date;
  }
}
