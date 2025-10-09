package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.service.tm.SourceStringConverter;
import java.util.List;

public class NoOpSourceStringConverter implements SourceStringConverter {
  @Override
  public String convert(String input, List<String> options) {
    return input;
  }
}
