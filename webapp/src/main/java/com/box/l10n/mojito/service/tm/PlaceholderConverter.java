package com.box.l10n.mojito.service.tm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderConverter {
  private static String convertToPositional(String placeholder, int position) {
    // Handle Java width+precision format like %10.2f -> %1$10.2f
    if (placeholder.matches("%\\d+\\.\\d+[fFeEgGaA]")) {
      String formatSpec = placeholder.substring(1); // Remove %
      return "%" + position + "$" + formatSpec;
    }
    // Handle Java width format like %10s -> %1$10s
    else if (placeholder.matches("%\\d+[sdioxXeEfFgGaAcb]")) {
      String formatSpec = placeholder.substring(1); // Remove %
      return "%" + position + "$" + formatSpec;
    }
    // Handle precision format like %.2f -> %1$.2f (both iOS and Java)
    else if (placeholder.matches("%\\.\\d+[fFeEgGaA]") || placeholder.matches("%\\.\\d+[fs]")) {
      String formatSpec = placeholder.substring(1); // Remove %
      return "%" + position + "$" + formatSpec;
    }
    // Handle simple format like %s -> %1$s, %d -> %1$d, %@ -> %1$@
    else {
      String typeSpec = placeholder.substring(1); // Remove %
      return "%" + position + "$" + typeSpec;
    }
  }

  public static String convertToPositionalPlaceholders(String regex, String input) {
    Pattern sequentialPattern = Pattern.compile(regex);

    Matcher matcher = sequentialPattern.matcher(input);
    StringBuilder result = new StringBuilder();
    int position = 1;

    while (matcher.find()) {
      String placeholder = matcher.group();
      String converted = convertToPositional(placeholder, position++);
      matcher.appendReplacement(result, Matcher.quoteReplacement(converted));
    }
    matcher.appendTail(result);

    return result.toString();
  }
}
