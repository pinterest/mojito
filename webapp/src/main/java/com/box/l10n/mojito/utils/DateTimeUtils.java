package com.box.l10n.mojito.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/** @author jeanaurambault */
@Component
public class DateTimeUtils {

  public ZonedDateTime now() {
    return ZonedDateTime.now();
  }

  public ZonedDateTime now(ZoneId zoneId) {
    return ZonedDateTime.now(zoneId);
  }
}
