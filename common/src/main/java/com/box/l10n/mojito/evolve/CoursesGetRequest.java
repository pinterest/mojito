package com.box.l10n.mojito.evolve;

import java.time.ZonedDateTime;

public record CoursesGetRequest(
    String locale, boolean active, ZonedDateTime updatedOnFrom, ZonedDateTime updatedOnTo) {
  public CoursesGetRequest(String locale, ZonedDateTime updatedOnFrom, ZonedDateTime updatedOnTo) {
    this(locale, true, updatedOnFrom, updatedOnTo);
  }
}
