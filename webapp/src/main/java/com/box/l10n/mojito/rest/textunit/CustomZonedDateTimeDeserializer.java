package com.box.l10n.mojito.rest.textunit;

import com.box.l10n.mojito.DateTimeUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.ZonedDateTime;

/**
 * Deserialize an object into {@link ZonedDateTime}. The object can either be a number of
 * milliseconds from 1970-01-01T00:00:00Z or a string representing a date following the ISO format,
 * such as '2023-11-28T22:15:30.000Z'.
 *
 * @author hylston.barbosa
 */
public class CustomZonedDateTimeDeserializer extends JsonDeserializer<ZonedDateTime> {

  @Override
  public ZonedDateTime deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {

    return DateTimeUtils.longOrStrAsZonedDate(parser.getText());
  }
}
