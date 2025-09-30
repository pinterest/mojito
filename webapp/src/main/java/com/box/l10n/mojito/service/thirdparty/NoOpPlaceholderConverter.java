package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.service.tm.PlaceholderConverter;
import java.util.List;

public class NoOpPlaceholderConverter implements PlaceholderConverter {
  @Override
  public String convert(String input, List<String> options) {
    return input;
  }
}
