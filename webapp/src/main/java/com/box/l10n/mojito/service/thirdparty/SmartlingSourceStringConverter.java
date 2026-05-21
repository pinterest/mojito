package com.box.l10n.mojito.service.thirdparty;

import static java.util.Optional.ofNullable;

import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingOptions;
import com.box.l10n.mojito.service.tm.SourceStringConverter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.translate.NumericEntityUnescaper;

/**
 * Converts sequential format placeholders in strings to positional placeholders for compatibility
 * with Smartling. It also trims whitespace and replaces multiple consecutive spaces with a single
 * space.
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
 *   SmartlingSourceStringConverter converter = new SmartlingSourceStringConverter();
 *   String result = converter.convert("Hello %s, you have %d messages", List.of("placeholderFormat=java"));
 *   // result: "Hello %1$s, you have %2$d messages"
 * </pre>
 */
public class SmartlingSourceStringConverter implements SourceStringConverter {
  private static final NumericEntityUnescaper NUMERIC_ENTITY_UNESCAPER =
      new NumericEntityUnescaper();
  private static final Pattern NUMERIC_CHARACTER_REFERENCE_PATTERN =
      Pattern.compile("&#(?:\\d+|[xX][0-9a-fA-F]+);");

  private static final Map<String, String> REGEX_BY_FORMAT =
      Map.of(
          "java",
          "%[sdioxXeEfFgGaAcb]|%\\.\\d+[fFeEgGaA]|%\\d+[sdioxXeEfFgGaAcb]|%\\d+\\.\\d+[fFeEgGaA]",
          "ios",
          "%[@dsfcxXopu]|%\\.\\d+[fs]",
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

  private String convertPlaceholders(String input, List<String> options) {
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

  private boolean hasDoubleQuotedAttributes(String tag) {
    // We only consider *attribute delimiters* that use literal double-quotes, i.e. name="value".
    // This must not be triggered by:
    // - a literal '"' inside a single-quoted attribute value
    // - HTML entities like &quot; that represent quotes in escaped content
    boolean inSingleQuotedValue = false;
    boolean inDoubleQuotedValue = false;

    for (int i = 0; i < tag.length(); i++) {
      char c = tag.charAt(i);

      if (c == '"' && !inSingleQuotedValue) {
        inDoubleQuotedValue = !inDoubleQuotedValue;
        continue;
      }
      if (c == '\'' && !inDoubleQuotedValue) {
        inSingleQuotedValue = !inSingleQuotedValue;
        continue;
      }

      if (c == '=' && !inSingleQuotedValue && !inDoubleQuotedValue) {
        int j = i + 1;
        while (j < tag.length() && Character.isWhitespace(tag.charAt(j))) {
          j++;
        }
        if (j < tag.length() && tag.charAt(j) == '"') {
          return true;
        }
      }
    }

    return false;
  }

  private String convertNbspEntityToUnicodeNbsp(String input) {
    if (input == null || input.indexOf('&') < 0) {
      return input;
    }
    return input.replace("&nbsp;", "\u00A0");
  }

  /**
   * Replaces real newline characters (LF/CRLF/CR) that appear inside single-quoted HTML attribute
   * values with a literal {@code \n} sequence.
   *
   * <p>This is intentionally scoped to single-quoted attribute values to avoid changing tag
   * structure or whitespace semantics elsewhere.
   */
  private String escapeNewlines(String tag) {
    if (tag.indexOf('\'') < 0 || tag.indexOf('\n') < 0) {
      return tag;
    }
    StringBuilder result = new StringBuilder(tag.length() + 8);
    boolean inSingleQuotedValue = false;
    boolean inDoubleQuotedValue = false;
    for (int i = 0; i < tag.length(); i++) {
      char c = tag.charAt(i);
      if (c == '"' && !inSingleQuotedValue) {
        inDoubleQuotedValue = !inDoubleQuotedValue;
        result.append(c);
        continue;
      }
      if (c == '\'' && !inDoubleQuotedValue) {
        inSingleQuotedValue = !inSingleQuotedValue;
        result.append(c);
        continue;
      }
      if (inSingleQuotedValue) {
        if (c == '\n') {
          result.append("\\n");
          continue;
        }
      }
      result.append(c);
    }
    return result.toString();
  }

  /**
   * Processes an HTML tag to apply space replacement to its content (tag name and attributes) while
   * preserving the tag structure itself.
   *
   * @param tag the HTML tag to process (e.g., "<div class='foo'>")
   * @return the tag with space replacement applied
   */
  private String processHtmlTagContent(String tag) {
    int closePos = tag.indexOf('>');
    if (closePos == -1) {
      return tag;
    }

    String tagStart = tag.substring(0, 1); // "<"
    String tagEnd = tag.substring(closePos); // ">"
    String content = tag.substring(1, closePos); // Everything between < and >

    // Apply space replacement to the content (tag name + attributes)
    String processedContent = content.replaceAll(" {2,}", " ");

    return tagStart + processedContent + tagEnd;
  }

  private String normalizeSpaces(String input) {
    String inputWithUnicodeNbsp = this.convertNbspEntityToUnicodeNbsp(input);
    return inputWithUnicodeNbsp.replaceAll(" {2,}", " ");
  }

  private String unescapeNumericCharacterReferences(String input) {
    if (input == null || input.indexOf('&') < 0 || input.indexOf('#') < 0) {
      return input;
    }
    return NUMERIC_ENTITY_UNESCAPER.translate(input);
  }

  private String unescapeHtml4OnlyEntities(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }
    // Fast-path: if there is nothing that escapeHtml4 would change, keep as-is.
    // (escapeHtml4 primarily targets & < > " ')
    if (input.indexOf('&') < 0
        && input.indexOf('<') < 0
        && input.indexOf('>') < 0
        && input.indexOf('"') < 0
        && input.indexOf('\'') < 0) {
      return input;
    }
    // Decode all HTML4 entities, then re-escape only HTML3 entities.
    // Result: entities supported by HTML4 but not HTML3 remain as Unicode characters.
    String html4Unescaped = StringEscapeUtils.unescapeHtml4(input);
    String html3Escaped = StringEscapeUtils.escapeHtml3(html4Unescaped);

    // Keep named entities escaped but decode numeric references (e.g. &#39;, &#x27;).
    String unescapedNumericCharacterReferences =
        this.unescapeNumericCharacterReferences(html3Escaped);
    return this.convertNbspEntityToUnicodeNbsp(unescapedNumericCharacterReferences);
  }

  private String normalizeOutsideHtmlTagsText(String input) {
    String inputWithSpacesNormalized = this.normalizeSpaces(input);
    return this.unescapeHtml4OnlyEntities(inputWithSpacesNormalized);
  }

  private boolean hasHtml4OnlyEntityOrNumericCharacterReference(String input) {
    if (input == null || input.isEmpty() || input.indexOf('&') < 0) {
      return false;
    }

    // Numeric references are unescaped in a later stage and should trigger entity-aware
    // normalization in plain text.
    if (NUMERIC_CHARACTER_REFERENCE_PATTERN.matcher(input).find()) {
      return true;
    }

    // Detect entities that HTML4 decodes but HTML3 does not.
    String html4Unescaped = StringEscapeUtils.unescapeHtml4(input);
    if (html4Unescaped.equals(input)) {
      return false;
    }
    String html3Unescaped = StringEscapeUtils.unescapeHtml3(input);
    return !html4Unescaped.equals(html3Unescaped);
  }

  /**
   * Normalizes text for Smartling while treating HTML tags and non-tag text differently.
   *
   * <p>The input is split into HTML tag segments ({@code <...>}) and non-tag segments.
   *
   * <ul>
   *   <li>If no tags are found, only space normalization is applied.
   *   <li>For non-tag segments, runs of literal spaces are collapsed and HTML4-only entities are
   *       normalized via {@link #unescapeHtml4OnlyEntities(String)}.
   *   <li>For tag segments, tag structure is preserved.
   *   <li>Tags with no double-quoted attributes have newlines in single-quoted attribute values
   *       escaped as literal {@code \n} via {@link #escapeNewlines(String)}.
   *   <li>Tags with double-quoted attributes have repeated spaces in tag content collapsed via
   *       {@link #processHtmlTagContent(String)}.
   * </ul>
   */
  private String normalizeSmartlingTextHtmlAware(String input) {
    // Pattern to match HTML tags
    Pattern htmlTagPattern = Pattern.compile("<[^>]*>");
    Matcher htmlTagMatcher = htmlTagPattern.matcher(input);

    // For plain text, run entity-aware normalization only for HTML4-only entities or numeric NCRs.
    if (!htmlTagMatcher.find()) {
      if (this.hasHtml4OnlyEntityOrNumericCharacterReference(input)) {
        return this.normalizeOutsideHtmlTagsText(input);
      }
      return this.normalizeSpaces(input);
    }
    htmlTagMatcher.reset();

    StringBuilder result = new StringBuilder();
    int lastEnd = 0;

    while (htmlTagMatcher.find()) {
      int tagStart = htmlTagMatcher.start();
      int tagEnd = htmlTagMatcher.end();

      // Apply space replacement to text before the tag (outside HTML)
      String textBefore = input.substring(lastEnd, tagStart);
      result.append(this.normalizeOutsideHtmlTagsText(textBefore));

      // Process the HTML tag
      String tag = htmlTagMatcher.group();
      if (!this.hasDoubleQuotedAttributes(tag)) {
        // Skip space replacement for tags with double-quoted attributes
        result.append(this.escapeNewlines(tag));
      } else {
        // Apply space replacement to tag content
        String processedTag = this.processHtmlTagContent(tag);
        result.append(processedTag);
      }

      lastEnd = tagEnd;
    }

    // Apply space replacement to remaining text after the last tag (outside HTML)
    String remainingText = input.substring(lastEnd);
    result.append(this.normalizeOutsideHtmlTagsText(remainingText));

    return result.toString();
  }

  @Override
  public String convert(String input, List<String> options) {
    String result = this.convertPlaceholders(input, options);
    return this.normalizeSmartlingTextHtmlAware(result.trim());
  }
}
