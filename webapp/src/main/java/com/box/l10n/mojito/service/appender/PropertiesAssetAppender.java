package com.box.l10n.mojito.service.appender;

import com.box.l10n.mojito.service.converter.TextUnitConverter;
import com.box.l10n.mojito.service.converter.TextUnitToPropertyConverter;
import com.box.l10n.mojito.utils.AssetContentUtils;

public class PropertiesAssetAppender extends AbstractAssetAppender {
  private final TextUnitConverter converter;

  public PropertiesAssetAppender(String content) {
    String lineBreak = AssetContentUtils.determineLineSeparator(content);
    converter = new TextUnitToPropertyConverter(lineBreak);
    append(content);
    append(lineBreak + lineBreak);
  }

  @Override
  protected TextUnitConverter getConverter() {
    return converter;
  }
}
