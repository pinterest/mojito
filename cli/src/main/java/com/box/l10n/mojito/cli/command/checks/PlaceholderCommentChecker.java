package com.box.l10n.mojito.cli.command.checks;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.regex.PlaceholderRegularExpressions;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AbstractCliChecker} that verifies that a description of a placeholder is present in the
 * associated comment in the form <placeholder name>:<description> or <placeholder
 * position>:<description>
 *
 * @author mallen
 */
public class PlaceholderCommentChecker extends AbstractCliChecker {

  static Logger logger = LoggerFactory.getLogger(PlaceholderCommentChecker.class);

  @Override
  public CliCheckResult run(List<AssetExtractionDiff> assetExtractionDiffs) {
    CliCheckResult cliCheckResult =
        new CliCheckResult(isHardFail(), CliCheckerType.PLACEHOLDER_COMMENT_CHECKER.name());
    Set<PlaceholderCommentCheckResult> placeholderCommentCheckResultSet =
        checkForPlaceholderDescriptionsInComment(assetExtractionDiffs);
    Map<String, List<CliCheckResult.CheckFailure>> sourceToFailureMap =
        placeholderCommentCheckResultSet.stream()
            .collect(
                Collectors.toMap(
                    PlaceholderCommentCheckResult::getSource,
                    PlaceholderCommentCheckResult::getFailures));
    Map<String, List<CliCheckResult.CheckFailure>> nameToFailureMap =
        placeholderCommentCheckResultSet.stream()
            .collect(
                Collectors.toMap(
                    PlaceholderCommentCheckResult::getName,
                    PlaceholderCommentCheckResult::getFailures));
    if (!sourceToFailureMap.isEmpty()) {
      cliCheckResult.setSuccessful(false);
      cliCheckResult.setNotificationText(buildNotificationText(sourceToFailureMap).toString());
      cliCheckResult.appendToFailuresMap(
          nameToFailureMap.entrySet().stream()
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey,
                      (entry) ->
                          new CliCheckResult.CheckFailure(
                              CheckerRuleId.AGGREGATE_PLACEHOLDER_COMMENT_CHECKER_VIOLATION,
                              entry.getValue().stream()
                                  .map(CliCheckResult.CheckFailure::failureMessage)
                                  .collect(Collectors.joining("\n"))))));
    }
    return cliCheckResult;
  }

  protected Set<PlaceholderCommentCheckResult> checkForPlaceholderDescriptionsInComment(
      List<AssetExtractionDiff> assetExtractionDiffs) {
    List<AbstractPlaceholderDescriptionCheck> placeholderDescriptionChecks =
        getPlaceholderCommentChecks();

    return getAddedTextUnitsExcludingInconsistentComments(assetExtractionDiffs).stream()
        .map(
            assetExtractorTextUnit ->
                getPlaceholderCommentCheckResult(
                    placeholderDescriptionChecks, assetExtractorTextUnit))
        .filter(result -> !result.getFailures().isEmpty())
        .collect(Collectors.toSet());
  }

  private PlaceholderCommentCheckResult getPlaceholderCommentCheckResult(
      List<AbstractPlaceholderDescriptionCheck> placeholderDescriptionChecks,
      AssetExtractorTextUnit assetExtractorTextUnit) {
    PlaceholderCommentCheckResult result;
    String name = assetExtractorTextUnit.getName();
    String source = assetExtractorTextUnit.getSource();
    String comment = assetExtractorTextUnit.getComments();
    if (StringUtils.isBlank(comment)) {
      result =
          new PlaceholderCommentCheckResult(
              source,
              name,
              Lists.newArrayList(
                  new CliCheckResult.CheckFailure(
                      CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Comment is empty.")));
    } else {
      List<CliCheckResult.CheckFailure> failures =
          placeholderDescriptionChecks.stream()
              .flatMap(check -> check.checkCommentForDescriptions(source, comment).stream())
              .collect(Collectors.toList());
      result = new PlaceholderCommentCheckResult(source, name, failures);
    }

    return result;
  }

  private List<AbstractPlaceholderDescriptionCheck> getPlaceholderCommentChecks() {
    return cliCheckerOptions.getParameterRegexSet().stream()
        .map(this::getPlaceholderDescriptionCheck)
        .collect(Collectors.toList());
  }

  private AbstractPlaceholderDescriptionCheck getPlaceholderDescriptionCheck(
      PlaceholderRegularExpressions placeholderRegularExpression) {
    AbstractPlaceholderDescriptionCheck placeholderDescriptionCheck;
    switch (placeholderRegularExpression) {
      case SINGLE_BRACE_REGEX:
        placeholderDescriptionCheck = new SingleBracesPlaceholderDescriptionChecker();
        break;
      case DOUBLE_BRACE_REGEX:
        placeholderDescriptionCheck = new DoubleBracesPlaceholderDescriptionChecker();
        break;
      case PRINTF_LIKE_VARIABLE_TYPE_REGEX:
        placeholderDescriptionCheck = new PrintfLikeVariableTypePlaceholderDescriptionChecker();
        break;
      default:
        placeholderDescriptionCheck =
            new SimpleRegexPlaceholderDescriptionChecker(placeholderRegularExpression);
    }
    return placeholderDescriptionCheck;
  }

  private StringBuilder buildNotificationText(
      Map<String, List<CliCheckResult.CheckFailure>> failureMap) {
    StringBuilder notificationText = new StringBuilder();
    notificationText.append("Placeholder description in comment check failed.");
    notificationText.append(System.lineSeparator());
    notificationText.append(System.lineSeparator());
    notificationText.append(
        failureMap.keySet().stream()
            .map(
                source -> {
                  StringBuilder sb = new StringBuilder();
                  sb.append("String " + QUOTE_MARKER + source + QUOTE_MARKER + " failed check:");
                  sb.append(System.lineSeparator());
                  return sb.append(
                          failureMap.get(source).stream()
                              .map(checkFailure -> BULLET_POINT + checkFailure.failureMessage())
                              .collect(Collectors.joining(System.lineSeparator())))
                      .toString();
                })
            .collect(Collectors.joining(System.lineSeparator())));

    return notificationText;
  }

  protected class PlaceholderCommentCheckResult {
    final List<CliCheckResult.CheckFailure> failures;
    final String source;
    final String name;

    PlaceholderCommentCheckResult(
        String source, String name, List<CliCheckResult.CheckFailure> failures) {
      this.source = source;
      this.failures = failures;
      this.name = name;
    }

    public List<CliCheckResult.CheckFailure> getFailures() {
      return failures;
    }

    public String getSource() {
      return source;
    }

    public String getName() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      PlaceholderCommentCheckResult result = (PlaceholderCommentCheckResult) o;
      return Objects.equals(failures, result.failures) && Objects.equals(source, result.source);
    }

    @Override
    public int hashCode() {
      return Objects.hash(failures, source);
    }
  }
}
