package com.box.l10n.mojito;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * Util class to provide Date/DateTime utility methods such as converters.
 *
 * @author hylston.barbosa
 */
public class DateTimeUtils {

  /**
   * Converts an Instant into {@link ZonedDateTime}. The input should be an {@link Instant}. Defines
   * the timezone as UTC.
   *
   * @author hylston.barbosa
   */
  public static ZonedDateTime fromDateToZonedDate(Date date) {

    if (date != null) {
      return date.toInstant().atZone(ZoneOffset.UTC);
    }

    return null;
  }

  /**
   * Converts an instant into {@link ZonedDateTime}. The input should be an instant long value such
   * as 1701374258000. Defines the timezone as UTC.
   *
   * @author hylston.barbosa
   */
  public static ZonedDateTime longAsZonedDate(Long instant) {
    return Instant.ofEpochMilli(instant).atZone(ZoneOffset.UTC);
  }

  /**
   * Converts a string into {@link ZonedDateTime}. The input should be a string representing a date
   * following the ISO format, such as '2023-11-28T22:15:30.000Z'. If the provided input does not
   * have a timezone defined, sets the UTC as the timezone.
   *
   * @author hylston.barbosa
   */
  public static ZonedDateTime strAsZonedDate(String dateStr) {
    try {
      return ZonedDateTime.parse(dateStr);
    } catch (DateTimeParseException e) {
      return LocalDateTime.parse(dateStr).atZone(ZoneOffset.UTC);
    }
  }

  /**
   * Converts a string into {@link ZonedDateTime}. The object can either be a number of milliseconds
   * from 1970-01-01T00:00:00Z such as 1701374258000 or a string representing a date following the
   * ISO format, such as '2023-11-28T22:15:30.000Z'.
   *
   * @author hylston.barbosa
   */
  public static ZonedDateTime longOrStrAsZonedDate(String datetime) {
    if (datetime == null) {
      return null;
    }

    try {
      return longAsZonedDate(Long.parseLong(datetime));
    } catch (NumberFormatException nfe) {
      return strAsZonedDate(datetime);
    }
  }
}
