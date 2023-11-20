package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.IStringConverter;
import com.box.l10n.mojito.DateTimeUtils;
import java.time.ZonedDateTime;

public class DateTimeConverter implements IStringConverter<ZonedDateTime> {

  @Override
  public ZonedDateTime convert(String dateAsText) {

    return DateTimeUtils.strAsZonedDate(dateAsText);
  }
}
