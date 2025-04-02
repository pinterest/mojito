package com.box.l10n.mojito.service.converter;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.Arrays;
import java.util.stream.Collectors;
import joptsimple.internal.Strings;
import org.springframework.stereotype.Component;

@Component
public class GettextConverter implements TextUnitDTOStringConverter {

  private final String COMMENT_SKELETON = "#. ${##}\n";
  private final String CONTEXT_SKELETON = "msgctxt \"${##}\"";
  private final String MSGID_SKELETON = "msgid \"${##}\"";
  private final String MSGID_PLURAL = "msgid_plural \"${##}\"";
  private final String MSGSTR = "msgstr \"\"";
  private final String CONTEXT_SEPARATOR = " --- ";

  @Override
  public String convert(TextUnitDTO textUnitDTO) {
    boolean hasContext = textUnitDTO.getName().contains(CONTEXT_SEPARATOR);

    StringBuilder sb = new StringBuilder();

    if (!Strings.isNullOrEmpty(textUnitDTO.getComment())) sb.append(getComment(textUnitDTO));
    if (hasContext) sb.append(getContext(textUnitDTO)).append("\n");

    if (textUnitDTO.getPluralForm() != null && textUnitDTO.getPluralForm().equals("other")) {
      String singular = textUnitDTO.getName().split(" _other")[0];
      String plural = textUnitDTO.getSource();

      if (hasContext) {
        singular = singular.split(CONTEXT_SEPARATOR)[0];
      }

      sb.append(replace(MSGID_SKELETON, singular, false)).append("\n");
      sb.append(replace(MSGID_PLURAL, plural, true)).append("\n");
      // TODO: Extend this for other parent locales, only supports en right now
      sb.append("msgstr[0] \"\"").append("\n");
      sb.append("msgstr[1] \"\"").append("\n\n");
    } else {
      sb.append(getMsgId(textUnitDTO)).append("\n");
      sb.append(MSGSTR).append("\n\n");
    }

    return sb.toString();
  }

  private String getComment(TextUnitDTO textUnitDTO) {
    // Break down comment by new line, make sure each line starts with #.
    String[] comments = textUnitDTO.getComment().split("\n");
    return Arrays.stream(comments)
        .map(comment -> replace(COMMENT_SKELETON, comment, false))
        .collect(Collectors.joining());
  }

  private String getContext(TextUnitDTO textUnitDTO) {
    String context = textUnitDTO.getName().split(CONTEXT_SEPARATOR)[1];
    if (textUnitDTO.getPluralForm() != null) {
      context = context.split(" _" + textUnitDTO.getPluralForm())[0];
    }
    return replace(CONTEXT_SKELETON, context, false);
  }

  private String getMsgId(TextUnitDTO textUnitDTO) {
    return replace(MSGID_SKELETON, textUnitDTO.getSource(), true);
  }

  private String replace(String skeleton, String replacement, boolean escape) {
    String repString = escape ? escape(replacement) : replacement;
    return skeleton.replace("${##}", repString);
  }

  private String escape(String toEscape) {
    return toEscape.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
  }
}
