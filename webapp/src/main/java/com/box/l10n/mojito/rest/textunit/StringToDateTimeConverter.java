package com.box.l10n.mojito.rest.textunit;

import com.box.l10n.mojito.JSR310Migration;
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
// TODO(jean) 2-JSR310 - I guess that's the key for API to be backward compatible - removing all
// TODO from controllers
// need to test a few controller query though
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
      // TODO(jean) 2-JSR310 - replaced but may be refactored?
      converted = JSR310Migration.newDateTimeCtorWithLongAndString(instant);
    }

    return converted;
  }
}
