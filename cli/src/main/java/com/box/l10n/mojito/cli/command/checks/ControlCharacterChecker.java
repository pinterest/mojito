package com.box.l10n.mojito.cli.command.checks;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.google.common.base.CharMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ControlCharacterChecker extends AbstractCliChecker {

  class ControlCharacterCheckerResult {
    boolean isSuccessful = true;
    String failureText = "";
  }

  @Override
  public CliCheckResult run(List<AssetExtractionDiff> assetExtractionDiffs) {
    final Map<String, CliCheckResult.CheckFailure> failedFeatureMap = new HashMap<>();
    getAddedTextUnitsExcludingInconsistentComments(assetExtractionDiffs)
        .forEach(
            textUnit -> {
              ControlCharacterCheckerResult checkerResult =
                  getControlCharacterCheckerResult(textUnit.getSource());
              if (!checkerResult.isSuccessful) {
                failedFeatureMap.put(
                    textUnit.getName(),
                    new CliCheckResult.CheckFailure(
                        CheckerRuleId.CONTROL_CHARACTER_DETECTED, checkerResult.failureText));
              }
            });
    List<String> failures =
        failedFeatureMap.values().stream()
            .map(CliCheckResult.CheckFailure::failureMessage)
            .collect(Collectors.toList());
    CliCheckResult result = createCliCheckerResult();
    result.appendToFailuresMap(failedFeatureMap);
    if (!failures.isEmpty()) {
      result.setSuccessful(false);
      result.setNotificationText(getNotificationText(failures));
    }
    return result;
  }

  private ControlCharacterCheckerResult getControlCharacterCheckerResult(String source) {
    ControlCharacterCheckerResult result = new ControlCharacterCheckerResult();
    char[] characters = source.toCharArray();
    List<Integer> indexMatches = new ArrayList<>();
    for (int i = 0; i < characters.length; i++) {
      if (CharMatcher.anyOf("\t\r\n")
          .negate()
          .and(CharMatcher.javaIsoControl())
          .matches(characters[i])) {
        indexMatches.add(i);
        result.isSuccessful = false;
      }
    }

    if (!result.isSuccessful) {
      StringBuilder sb = new StringBuilder();
      sb.append(BULLET_POINT)
          .append("Control character found in source string ")
          .append(QUOTE_MARKER)
          .append(source)
          .append(QUOTE_MARKER)
          .append(" at index ")
          .append(
              indexMatches.stream()
                  .map(index -> Integer.toString(index))
                  .collect(Collectors.joining(", ")));
      sb.append(".");
      result.failureText = sb.toString();
    }
    return result;
  }

  private String getNotificationText(List<String> failures) {
    StringBuilder sb = new StringBuilder();
    sb.append(failures.stream().collect(Collectors.joining(System.lineSeparator()))).toString();
    sb.append(System.lineSeparator() + System.lineSeparator());
    sb.append("Please remove control characters from source strings.");
    return sb.toString();
  }
}
