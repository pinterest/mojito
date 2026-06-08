package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.PRINTF_LIKE_REGEX;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks that there are the same c-printf like placeholders in the source and target content, order
 * is not important.
 *
 * <p>This checker normalizes non-positional placeholders (e.g., %s, %d, %f) to positional format
 * (e.g., %1$s, %1$d, %1$f) before comparison. This ensures compatibility with translation
 * management systems like Smartling that automatically add position specifiers to translations to
 * avoid ambiguity.
 *
 * <p>Examples of valid translations:
 *
 * <ul>
 *   <li>Source: "Select %s answers" → Target: "Kies %1$s antwoorde" (PASS)
 *   <li>Source: "Hello %1$s!" → Target: "¡Hola %1$s!" (PASS)
 *   <li>Source: "%s of %s" → Target: "%1$s de %1$s" (PASS)
 * </ul>
 *
 * <p><strong>Important:</strong> When the source contains multiple identical non-positional
 * placeholders (e.g., "%s %s"), they will all normalize to the same positional placeholder (e.g.,
 * "%1$s %1$s"). If the target uses distinct position specifiers (e.g., "%1$s %2$s"), the check will
 * fail. This is expected behavior and indicates the source should use positional format from the
 * start.
 *
 * @author wyau
 */
public class PrintfLikeIntegrityChecker extends RegexIntegrityChecker {

  private Pattern printfLikePattern = Pattern.compile(PRINTF_LIKE_REGEX.getRegex());

  /**
   * Modified regex from Formatter#formatSpecifier = "%(\\d+\\$)?([-#+
   * 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";
   * (%[argument_index$][flags][width][.precision][t]conversion)
   *
   * @return
   */
  @Override
  public String getRegex() {
    return PRINTF_LIKE_REGEX.getRegex();
  }

  @Override
  public void check(String sourceContent, String targetContent)
      throws PrintfLikeIntegrityCheckerException {

    try {
      super.check(
          addParameterSpecifierToPlaceholders(sourceContent),
          addParameterSpecifierToPlaceholders(targetContent));
    } catch (RegexCheckerException rce) {
      throw new PrintfLikeIntegrityCheckerException((rce.getMessage()));
    }
  }

  /**
   * Replace all instances of non-positional placeholders with positional format. For example: '%s'
   * → '%1$s', '%.1f' → '%1$.1f', '%10s' → '%1$10s' Placeholders that already have positional
   * specifiers (e.g., '%1$s', '%2$d') are left unchanged.
   *
   * @param str the string to normalize
   * @return the string with non-positional placeholders converted to positional format
   */
  private String addParameterSpecifierToPlaceholders(String str) {
    if (str == null) {
      return null;
    }

    Matcher matcher = printfLikePattern.matcher(str);
    StringBuffer result = new StringBuffer();

    while (matcher.find()) {
      String placeholder = matcher.group();
      // Check if placeholder already has positional specifier (contains digit followed by $)
      // Examples: %1$s, %2$d, %3$.2f already have positional specifiers
      if (!placeholder.matches("%\\d+\\$.*")) {
        // No positional specifier, add %1$ after the %
        // Examples: %s → %1$s, %.1f → %1$.1f, %10s → %1$10s
        String normalized = placeholder.replaceFirst("%", "%1\\$");
        matcher.appendReplacement(result, Matcher.quoteReplacement(normalized));
      } else {
        // Already has positional specifier, keep as is
        matcher.appendReplacement(result, Matcher.quoteReplacement(placeholder));
      }
    }
    matcher.appendTail(result);

    return result.toString();
  }
}
