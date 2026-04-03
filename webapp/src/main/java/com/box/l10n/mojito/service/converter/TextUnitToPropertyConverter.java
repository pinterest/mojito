package com.box.l10n.mojito.service.converter;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import joptsimple.internal.Strings;

import java.util.Arrays;
import java.util.stream.Collectors;

public class TextUnitToPropertyConverter implements TextUnitConverter {
  private final String COMMENT_SKELETON = "#${##}";

  private final String lineBreak;

  public TextUnitToPropertyConverter(String lineBreak) {
    this.lineBreak = lineBreak;
  }

  private String escape(String toEscape) {
    return toEscape
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r", "\\r")
        .replace("\n", "\\n");
  }

  private String replace(String skeleton, String replacement, boolean escape) {
    String repString = escape ? escape(replacement) : replacement;
    return skeleton.replace("${##}", repString);
  }

  private String getComment(TextUnitDTO textUnitDTO) {
    // Break down comment by new line, make sure each line starts with #.
    String[] comments = textUnitDTO.getComment().split(lineBreak);
    return Arrays.stream(comments)
        .map(comment -> replace(COMMENT_SKELETON, comment, false))
        .collect(Collectors.joining(lineBreak))
        + lineBreak;
  }

  @Override
  public String convert(TextUnitDTO textUnitDTO) {
    StringBuilder sb = new StringBuilder();

    if (!Strings.isNullOrEmpty(textUnitDTO.getComment())) sb.append(getComment(textUnitDTO));

    sb.append(String.format("%s=%s", textUnitDTO.getName(), textUnitDTO.getSource())).append(lineBreak + lineBreak);

    return sb.toString();
  }
}
