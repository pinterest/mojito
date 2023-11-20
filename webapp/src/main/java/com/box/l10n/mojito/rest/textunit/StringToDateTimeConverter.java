package com.box.l10n.mojito.rest.textunit;

import com.box.l10n.mojito.DateTimeUtils;
import java.time.ZonedDateTime;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converts a {@link String} into {@link ZonedDateTime}. The string can either be a number of
 * milliseconds from 1970-01-01T00:00:00Z or a string representing a date following the ISO format,
 * such as '2011-12-03T10:15:30'.
 *
 * @author jeanaurambault
 */
@Component
public class StringToDateTimeConverter implements Converter<String, ZonedDateTime> {

  @Override
  public ZonedDateTime convert(String source) {

    return DateTimeUtils.longOrStrAsZonedDate(source);
  }
}
