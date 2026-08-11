package com.box.l10n.mojito.service.evolve;

import java.time.ZonedDateTime;
import java.util.Set;

public record CoursesGetRequest(
    Set<String> codes, String locale, boolean active, ZonedDateTime updatedOnTo) {
  public CoursesGetRequest(String locale, ZonedDateTime updatedOnTo) {
    this(null, locale, true, updatedOnTo);
  }

  public CoursesGetRequest(Set<String> codes, ZonedDateTime updatedOnTo) {
    this(codes, null, true, updatedOnTo);
  }
}
