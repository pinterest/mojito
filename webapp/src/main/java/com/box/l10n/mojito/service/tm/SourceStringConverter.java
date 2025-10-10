package com.box.l10n.mojito.service.tm;

import java.util.List;

public interface SourceStringConverter {
  String convert(String input, List<String> options);
}
