package com.box.l10n.mojito.rest.textunit;

import java.time.ZonedDateTime;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converts a {@link String} into {@link ZonedDateTime}. The string can either be a number of
 * milliseconds from 1970-01-01T00:00:00Z or any format recognized by {@link
 * ZonedDateTime#ZonedDateTime(java.lang.Object) }
 *
 * @author jeanaurambault
 */
@Component
public class StringToDateTimeConverter implements Converter<String, ZonedDateTime> {

  @Override
  public ZonedDateTime convert(String source) {

    ZonedDateTime converted = null;

    if (source != null) {
      Object instant;

      try {
        instant = Long.parseLong(source);
      } catch (NumberFormatException nfe) {
        instant = source;
      }
      // TODO(jean) JSR310 - replace
      converted = new ZonedDateTime(instant);
    }

    return converted;
  }
}
