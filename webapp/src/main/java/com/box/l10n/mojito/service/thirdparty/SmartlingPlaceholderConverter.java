package com.box.l10n.mojito.service.thirdparty;

import static java.util.Optional.ofNullable;

import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingOptions;
import com.box.l10n.mojito.service.tm.PlaceholderConverter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts sequential format placeholders in strings to positional placeholders for compatibility
 * with Smartling.
 *
 * <p>This converter supports Java, iOS, and C-style format strings, transforming placeholders such
 * as {@code %s}, {@code %d}, {@code %@}, {@code %10.2f}, etc., into their positional equivalents
 * (e.g., {@code %1$s}, {@code %2$d}, etc.).
 *
 * <p>The conversion is controlled by the {@code SmartlingOptions} provided in the options list,
 * specifically the {@code placeholderFormat} option, which determines the format style to use.
 *
 * <p>Supported formats:
 *
 * <ul>
 *   <li>{@code java} - Java-style format strings
 *   <li>{@code ios} - iOS-style format strings
 *   <li>{@code c} - C-style format strings
 * </ul>
 *
 * <p>If the format is not recognized or not specified, the input string is returned unchanged.
 *
 * <p>Example usage:
 *
 * <pre>
 *   SmartlingPlaceholderConverter converter = new SmartlingPlaceholderConverter();
 *   String result = converter.convert("Hello %s, you have %d messages", List.of("placeholderFormat=java"));
 *   // result: "Hello %1$s, you have %2$d messages"
 * </pre>
 */
public class SmartlingPlaceholderConverter implements PlaceholderConverter {
  private static final Map<String, String> REGEX_BY_FORMAT =
      Map.of(
          "java",
              "%[sdioxXeEfFgGaAcb]|%\\.\\d+[fFeEgGaA]|%\\d+[sdioxXeEfFgGaAcb]|%\\d+\\.\\d+[fFeEgGaA]",
          "ios", "%[@dsfcxXopu]|%\\.\\d+[fs]",
          "c",
              "%[sdioxXeEfFgGaAcb]|%\\.\\d+[fFeEgGaA]|%\\d+[sdioxXeEfFgGaAcb]|%\\d+\\.\\d+[fFeEgGaA]|%[hlL]*[dioxXeEfFgGaAcbsp]");

  private String convertJavaToPositional(String placeholder, int position) {
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
    // Handle precision format like %.2f -> %1$.2f
    else if (placeholder.matches("%\\.\\d+[fFeEgGaA]")) {
      String formatSpec = placeholder.substring(1); // Remove %
      return "%" + position + "$" + formatSpec;
    }
    // Handle simple format like %s -> %1$s, %d -> %1$d
    else {
      String typeSpec = placeholder.substring(1); // Remove %
      return "%" + position + "$" + typeSpec;
    }
  }

  private String convertIOSToPositional(String placeholder, int position) {
    // Handle precision format like %.2f -> %1$.2f, %.1s -> %1$.1s
    if (placeholder.matches("%\\.\\d+[fs]")) {
      String formatSpec = placeholder.substring(1); // Remove %
      return "%" + position + "$" + formatSpec;
    }
    // Handle simple format like %@ -> %1$@, %d -> %1$d, %s -> %1$s
    else {
      String typeSpec = placeholder.substring(1); // Remove %
      return "%" + position + "$" + typeSpec;
    }
  }

  private String convertCToPositional(String placeholder, int position) {
    // Handle C length modifiers + precision format like %ld -> %1$ld, %10.2f -> %1$10.2f
    if (placeholder.matches("%\\d+\\.\\d+[fFeEgGaA]")) {
      String formatSpec = placeholder.substring(1); // Remove %
      return "%" + position + "$" + formatSpec;
    }
    // Handle C width format like %10s -> %1$10s, %10ld -> %1$10ld
    else if (placeholder.matches("%\\d+[sdioxXeEfFgGaAcb]")
        || placeholder.matches("%\\d+[hlL]*[dioxXeEfFgGaAcbsp]")) {
      String formatSpec = placeholder.substring(1); // Remove %
      return "%" + position + "$" + formatSpec;
    }
    // Handle precision format like %.2f -> %1$.2f
    else if (placeholder.matches("%\\.\\d+[fFeEgGaA]")) {
      String formatSpec = placeholder.substring(1); // Remove %
      return "%" + position + "$" + formatSpec;
    }
    // Handle length modifiers like %ld -> %1$ld, %hd -> %1$hd, %Ld -> %1$Ld
    else if (placeholder.matches("%[hlL]+[dioxXeEfFgGaAcbsp]")) {
      String formatSpec = placeholder.substring(1); // Remove %
      return "%" + position + "$" + formatSpec;
    }
    // Handle simple format like %s -> %1$s, %d -> %1$d, %p -> %1$p
    else {
      String typeSpec = placeholder.substring(1); // Remove %
      return "%" + position + "$" + typeSpec;
    }
  }

  private String convertToPositional(String placeholder, int position, String format) {
    return switch (format) {
      case "java" -> convertJavaToPositional(placeholder, position);
      case "ios" -> convertIOSToPositional(placeholder, position);
      case "c" -> convertCToPositional(placeholder, position);
      default -> placeholder;
    };
  }

  @Override
  public String convert(String input, List<String> options) {
    SmartlingOptions smartlingOptions = SmartlingOptions.parseList(options);
    Optional<String> smartlingPlaceholderFormat =
        ofNullable(smartlingOptions.getPlaceholderFormat()).map(String::toLowerCase);

    if (smartlingPlaceholderFormat.isEmpty()
        || !REGEX_BY_FORMAT.containsKey(smartlingPlaceholderFormat.get())) {
      return input;
    }

    String format = smartlingPlaceholderFormat.get();
    String regex = REGEX_BY_FORMAT.get(format);
    Pattern sequentialPattern = Pattern.compile(regex);

    Matcher matcher = sequentialPattern.matcher(input);
    StringBuilder result = new StringBuilder();
    int position = 1;

    while (matcher.find()) {
      String placeholder = matcher.group();
      String converted = convertToPositional(placeholder, position++, format);
      matcher.appendReplacement(result, Matcher.quoteReplacement(converted));
    }
    matcher.appendTail(result);

    return result.toString();
  }
}
