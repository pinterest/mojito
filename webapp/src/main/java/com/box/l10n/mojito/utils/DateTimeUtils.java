package com.box.l10n.mojito.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Component;

/** @author jeanaurambault */
@Component
public class DateTimeUtils {

  public ZonedDateTime now() {
    // TODO(jean) JSR310 - replace
    return new ZonedDateTime();
  }

  public ZonedDateTime now(ZoneId dateTimeZone) {
    // TODO(jean) JSR310 - replace
    return new ZonedDateTime(dateTimeZone);
  }
}
