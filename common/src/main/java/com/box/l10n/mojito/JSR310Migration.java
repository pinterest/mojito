package com.box.l10n.mojito;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class JSR310Migration {

  public static ZonedDateTime newDateTimeCtor(
      int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour) {
    return ZonedDateTime.of(
        year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, 0, 0, ZoneId.systemDefault());
  }

  public static ZonedDateTime dateTimeNow() {
    return ZonedDateTime.now();
  }
}
