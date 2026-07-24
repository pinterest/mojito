package com.box.l10n.mojito.cli.command.utils;

import com.box.l10n.mojito.cli.command.checks.CheckerRuleId;
import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.sarif.model.ResultLevel;
import com.google.common.io.Files;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Service for generating GitHub PR review comments based on i18n check failures. This
 * implementation follows the same pattern as SarifFileGenerator to ensure consistency between SARIF
 * annotations and inline PR review comments.
 */
@Component
public class GithubReviewCommentService {

  static Logger logger = LoggerFactory.getLogger(GithubReviewCommentService.class);

  private final MeterRegistry meterRegistry;

  // File extensions for where the comments are extracted from comments above the translation
  // function call
  private final String[] extractedCommentFileExtensions;

  private final int lineNumberErrorAllowance;

  GithubReviewCommentService(
      @Value(
              "#{'${l10n.extraction-check.sarif.extracted-comments.fileExtensions:py,xml}'.split(',')}")
          String[] extractedCommentFileExtensions,
      @Value("${l10n.extraction-check.sarif.lineNumberErrorAllowance:2}")
          int lineNumberErrorAllowance,
      MeterRegistry meterRegistry) {
    this.extractedCommentFileExtensions = extractedCommentFileExtensions;
    this.meterRegistry = meterRegistry;
    this.lineNumberErrorAllowance = lineNumberErrorAllowance;
  }

  private String buildCheckDisplayName(String checkName) {
    return "I18N_" + checkName;
  }

  private String buildCheckDisplayName(CliCheckResult checkResult) {
    return buildCheckDisplayName(checkResult.getCheckName());
  }

  private static boolean hasUsages(AssetExtractorTextUnit assetExtractorTextUnit) {
    return assetExtractorTextUnit != null
        && assetExtractorTextUnit.getUsages() != null
        && !assetExtractorTextUnit.getUsages().isEmpty();
  }

  /**
   * Github only accepts line numbers which were modified in the PR: all other lines are ignored. If
   * a comment is flagged by the checker, the usage reported is the line where the string was added.
   * Hence, if only a comment is changed, then the line number will be wrong. We try to add or
   * subtract one to find a valid modified line (before or after the line) to get a line number
   * Github will accept
   */
  private UsageLocation estimateLocationLineNumber(
      Set<Integer> modifiedLines,
      String[] extractedCommentFileExtensions,
      String repoName,
      String fileUri,
      int startLineNumber) {

    int fullStopIndex = fileUri.lastIndexOf('.');
    if (fullStopIndex == -1) {
      return new UsageLocation(fileUri, startLineNumber);
    }

    String fileExtension = Files.getFileExtension(fileUri);
    if (Arrays.stream(extractedCommentFileExtensions)
        .noneMatch(x -> x.equalsIgnoreCase(fileExtension))) {
      return new UsageLocation(fileUri, startLineNumber);
    }

    if (modifiedLines.contains(startLineNumber)) {
      return new UsageLocation(fileUri, startLineNumber);
    }

    // Find the first line which exists in the modified lines any range of line numbers
    // up to a max (inclusive) of the lineNumberErrorAllowance
    for (int i = 1; i < this.lineNumberErrorAllowance + 1; i++) {
      int lineNumber = startLineNumber - i;
      if (modifiedLines.contains(lineNumber)) {
        return new UsageLocation(fileUri, lineNumber);
      } else {
        lineNumber = startLineNumber + i;
        if (modifiedLines.contains(lineNumber)) {
          return new UsageLocation(fileUri, lineNumber);
        }
      }
    }

    meterRegistry
        .counter("GithubReviewCommentService.LineNumberVariationNotFound", "repository", repoName)
        .increment();

    return new UsageLocation(fileUri, startLineNumber);
  }

  private List<UsageLocation> getUsageLocations(
      AssetExtractorTextUnit assetExtractorTextUnit,
      String[] extractedCommentFileExtensions,
      Map<String, Set<Integer>> githubModifiedLines,
      String repoName,
      String prefixToRemoveFromFileUri,
      boolean isCommentRelatedCheck) {
    return assetExtractorTextUnit.getUsages().stream()
        .map(
            usage -> {
              int colonIndex = usage.lastIndexOf(':');
              if (colonIndex == -1) {
                return null;
              }

              try {
                String fileUri = usage.substring(0, colonIndex);
                if (prefixToRemoveFromFileUri != null
                    && !prefixToRemoveFromFileUri.isEmpty()
                    && fileUri.startsWith(prefixToRemoveFromFileUri)) {
                  fileUri = fileUri.substring(prefixToRemoveFromFileUri.length());
                }
                int startLineNumber = Integer.parseInt(usage.substring(colonIndex + 1));

                Set<Integer> modifiedLines = githubModifiedLines.get(fileUri);
                if (modifiedLines == null || modifiedLines.isEmpty()) {
                  return new UsageLocation(fileUri, startLineNumber);
                }

                if (!modifiedLines.contains(startLineNumber)) {
                  meterRegistry
                      .counter(
                          "GithubReviewCommentService.LineNumberIncorrect", "repository", repoName)
                      .increment();
                }

                if (!isCommentRelatedCheck) {
                  return new UsageLocation(fileUri, startLineNumber);
                }

                return estimateLocationLineNumber(
                    modifiedLines,
                    extractedCommentFileExtensions,
                    repoName,
                    fileUri,
                    startLineNumber);

              } catch (NumberFormatException e) {
                logger.warn(
                    "Review Comment Generation - Unable to parse line number: {}",
                    usage.substring(colonIndex + 1));
                return null;
              }
            })
        .filter(Objects::nonNull)
        .toList();
  }

  private String formatReviewCommentBody(
      String checkName, ResultLevel level, CheckerRuleId ruleId, String message) {
    String icon = level == ResultLevel.ERROR ? "🛑" : "⚠️";
    String levelText = level == ResultLevel.ERROR ? "Error" : "Warning";

    return String.format(
        "%s **%s: %s**\n\n**Rule:** %s\n\n%s",
        icon, levelText, checkName, ruleId.toString(), message);
  }

  /**
   * Generates GitHub PR review comments based on CLI check failures. This method mirrors the logic
   * in SarifFileGenerator.generateSarifFile to ensure that review comments are consistent with
   * SARIF annotations.
   *
   * @param cliCheckerFailures List of check failures from CLI checkers
   * @param assetExtractionDiffs List of asset extraction diffs containing text units with usages
   * @param githubModifiedLines Map of file paths to sets of modified line numbers in the PR
   * @param repoName Repository name for metrics tracking
   * @param prefixToRemoveFromFileUris Optional prefix to remove from file URIs
   * @return List of ReviewComment objects ready to be posted to GitHub
   */
  public List<GithubClient.ReviewComment> generateReviewComments(
      List<CliCheckResult> cliCheckerFailures,
      List<AssetExtractionDiff> assetExtractionDiffs,
      Map<String, Set<Integer>> githubModifiedLines,
      String repoName,
      String prefixToRemoveFromFileUris) {

    List<GithubClient.ReviewComment> reviewComments = new ArrayList<>();
    Map<String, AssetExtractorTextUnit> nameToAssetTextUnitMap =
        assetExtractionDiffs.stream()
            .flatMap(diff -> diff.getAddedTextunits().stream())
            .collect(Collectors.toMap(AssetExtractorTextUnit::getName, x -> x));

    for (CliCheckResult checkFailure : cliCheckerFailures) {
      ResultLevel resultLevel = checkFailure.isHardFail() ? ResultLevel.ERROR : ResultLevel.WARNING;
      String checkDisplayName = buildCheckDisplayName(checkFailure);

      for (Map.Entry<String, CliCheckResult.CheckFailure> entry :
          checkFailure.getNameToFailuresMap().entrySet()) {

        String source = entry.getKey();
        CliCheckResult.CheckFailure resultCheckFailure = entry.getValue();
        AssetExtractorTextUnit assetExtractorTextUnit = nameToAssetTextUnitMap.get(source);
        CheckerRuleId ruleId = resultCheckFailure.ruleId();

        if (hasUsages(assetExtractorTextUnit)) {
          List<UsageLocation> usageLocations =
              getUsageLocations(
                  assetExtractorTextUnit,
                  extractedCommentFileExtensions,
                  githubModifiedLines,
                  repoName,
                  prefixToRemoveFromFileUris,
                  ruleId.isCommentRelated());

          for (UsageLocation location : usageLocations) {
            String commentBody =
                formatReviewCommentBody(
                    checkDisplayName, resultLevel, ruleId, resultCheckFailure.failureMessage());
            reviewComments.add(
                new GithubClient.ReviewComment(
                    commentBody, location.getFilePath(), location.getLineNumber()));
          }
        }
      }
    }

    long droppedCommentCount =
        reviewComments.stream()
            .filter(
                comment -> {
                  Set<Integer> modifiedLines = githubModifiedLines.get(comment.getPath());
                  return modifiedLines == null || !modifiedLines.contains(comment.getLine());
                })
            .count();
    if (droppedCommentCount > 0) {
      meterRegistry
          .counter("GithubReviewCommentService.CommentsNotOnModifiedLine", "repository", repoName)
          .increment(droppedCommentCount);
      logger.info(
          "{} of {} review comments for repository '{}' do not fall on a modified line and will be"
              + " dropped by GitHub",
          droppedCommentCount,
          reviewComments.size(),
          repoName);
    }

    logger.info(
        "Generated {} review comments for repository '{}'", reviewComments.size(), repoName);
    return reviewComments;
  }

  /** Data class for representing a usage location */
  static class UsageLocation {
    private final String filePath;
    private final int lineNumber;

    public UsageLocation(String filePath, int lineNumber) {
      this.filePath = filePath;
      this.lineNumber = lineNumber;
    }

    public String getFilePath() {
      return filePath;
    }

    public int getLineNumber() {
      return lineNumber;
    }
  }
}
