package com.box.l10n.mojito.cli.command.checks;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.base.Strings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AbstractCliChecker} that verifies the comment and context parameters are provided and are
 * not identical.
 *
 * @author mallen
 */
public class ContextAndCommentCliChecker extends AbstractCliChecker {

  static Logger logger = LoggerFactory.getLogger(ContextAndCommentCliChecker.class);

  static class ContextAndCommentCliCheckerResult {
    String sourceString;
    String failureMessage;
    String ruleId;
    boolean failed;

    public ContextAndCommentCliCheckerResult(
        boolean failed, String sourceString, String failureMessage, String ruleId) {
      this.sourceString = sourceString;
      this.failureMessage = failureMessage;
      this.failed = failed;
      this.ruleId = ruleId;
    }

    public ContextAndCommentCliCheckerResult(boolean failed) {
      this.failed = failed;
    }

    public String getSourceString() {
      return sourceString;
    }

    public String getFailureMessage() {
      return failureMessage;
    }

    public String getRuleId() {
      return ruleId;
    }

    public boolean isFailed() {
      return failed;
    }
  }

  @Override
  public CliCheckResult run(List<AssetExtractionDiff> assetExtractionDiffs) {
    CliCheckResult cliCheckResult = createCliCheckerResult();
    List<ContextAndCommentCliCheckerResult> results = runChecks(assetExtractionDiffs);
    if (results.stream().anyMatch(ContextAndCommentCliCheckerResult::isFailed)) {
      Map<String, CliCheckResult.CheckFailure> featureFailureMap =
          results.stream()
              .filter(ContextAndCommentCliCheckerResult::isFailed)
              .collect(
                  Collectors.toMap(
                      ContextAndCommentCliCheckerResult::getSourceString,
                      result ->
                          new CliCheckResult.CheckFailure(
                              result.getRuleId(), result.getFailureMessage())));
      cliCheckResult.appendToFieldFailuresMap(featureFailureMap);
      cliCheckResult.setSuccessful(false);
      cliCheckResult.setNotificationText(
          "Context and comment check found failures:"
              + System.lineSeparator()
              + results.stream()
                  .filter(ContextAndCommentCliCheckerResult::isFailed)
                  .map(
                      result ->
                          BULLET_POINT
                              + "Source string "
                              + QUOTE_MARKER
                              + result.getSourceString()
                              + QUOTE_MARKER
                              + " failed check with error: "
                              + result.getFailureMessage())
                  .collect(Collectors.joining(System.lineSeparator()))
              + System.lineSeparator());
    }
    return cliCheckResult;
  }

  private Optional<Pattern> getContextCommentExcludeFilesPattern() {
    if (StringUtils.isNotBlank(this.cliCheckerOptions.getContextCommentExcludeFilesPattern())) {
      return of(Pattern.compile(this.cliCheckerOptions.getContextCommentExcludeFilesPattern()));
    }
    return empty();
  }

  private List<ContextAndCommentCliCheckerResult> runChecks(
      List<AssetExtractionDiff> assetExtractionDiffs) {
    Optional<Pattern> excludeFilesPattern = this.getContextCommentExcludeFilesPattern();
    return getAddedTextUnitsExcludingInconsistentComments(assetExtractionDiffs).stream()
        .filter(
            assetExtractorTextUnit -> {
              if (excludeFilesPattern.isPresent() && assetExtractorTextUnit.getUsages() != null) {
                return assetExtractorTextUnit.getUsages().stream()
                    .map(usage -> usage.replaceAll(":\\d+$", ""))
                    .noneMatch(usage -> excludeFilesPattern.get().matcher(usage).find());
              }
              return true;
            })
        .map(
            assetExtractorTextUnit ->
                getContextAndCommentCliCheckerResult(
                    assetExtractorTextUnit, checkTextUnit(assetExtractorTextUnit)))
        .collect(Collectors.toList());
  }

  private ContextAndCommentCliCheckerResult getContextAndCommentCliCheckerResult(
      AssetExtractorTextUnit assetExtractorTextUnit, CliCheckResult.CheckFailure checkFailure) {
    ContextAndCommentCliCheckerResult result;
    if (checkFailure != null) {
      logger.debug(
          "'{}' source string failed check with error: {}",
          assetExtractorTextUnit.getSource(),
          checkFailure.failureMessage());
      result =
          new ContextAndCommentCliCheckerResult(
              true,
              assetExtractorTextUnit.getSource(),
              checkFailure.failureMessage(),
              checkFailure.ruleId());
    } else {
      result = new ContextAndCommentCliCheckerResult(false);
    }
    return result;
  }

  private CliCheckResult.CheckFailure checkTextUnit(AssetExtractorTextUnit assetExtractorTextUnit) {
    String failureText = null;
    String[] splitNameArray = assetExtractorTextUnit.getName().split("---");
    String context = null;
    if (isPlural(assetExtractorTextUnit) && cliCheckerOptions.getPluralsSkipped()) {
      return null;
    }
    if (splitNameArray.length > 1) {
      context = splitNameArray[1];
    }
    String comment = assetExtractorTextUnit.getComments();
    String ruleId = "UNKNOWN";

    if (!isBlank(context) && !isBlank(comment)) {
      if (context.trim().equalsIgnoreCase(comment.trim())) {
        ruleId = "EQUAL_CONTEXT_AND_COMMENT_STRINGS";
        failureText = "Context & comment strings should not be identical.";
      }
    } else if (isBlank(context) && isBlank(comment)) {
      ruleId = "EMPTY_CONTEXT_AND_COMMENT_STRINGS";
      failureText = "Context and comment strings are both empty.";
    } else if (isBlank(context)) {
      ruleId = "EMPTY_CONTEXT_STRING";
      failureText = "Context string is empty.";
    } else if (isBlank(comment)) {
      ruleId = "EMPTY_COMMENT_STRING";
      failureText = "Comment string is empty.";
    }

    if (ruleId.equals("UNKNOWN")) {
      return null;
    }

    return new CliCheckResult.CheckFailure(ruleId, failureText);
  }

  private boolean isPlural(AssetExtractorTextUnit assetExtractorTextUnit) {
    return !Strings.isNullOrEmpty(assetExtractorTextUnit.getPluralForm());
  }

  private boolean isBlank(String string) {
    return StringUtils.isBlank(string);
  }
}
