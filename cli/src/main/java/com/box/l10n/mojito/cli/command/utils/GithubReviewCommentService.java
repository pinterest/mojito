package com.box.l10n.mojito.cli.command.utils;

import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.sarif.model.ResultLevel;
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

  /**
   * Number of lines a usage is allowed to sit before a line modified in the PR to still be
   * considered as pointing at that modification.
   */
  private final int lineNumberErrorAllowance;

  GithubReviewCommentService(
      @Value("${l10n.extraction-check.review-comments.lineNumberErrorAllowance:5}")
          int lineNumberErrorAllowance,
      MeterRegistry meterRegistry) {
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
   * A usage is worth commenting on when it points at a change made in the PR. That is the case when
   * its line was modified, or when a modified line follows it within {@link
   * #lineNumberErrorAllowance} lines: when a check flags a comment, the usage reported is the line
   * the comment was extracted from, which sits a few lines before the modified string declaration.
   *
   * <p>The line number of the usage is always kept as reported, it is never moved onto the modified
   * line.
   */
  private boolean isUsageOnPrChange(
      Set<Integer> modifiedLines, String repoName, String fileUri, int startLineNumber) {

    if (modifiedLines.contains(startLineNumber)) {
      return true;
    }

    meterRegistry
        .counter("GithubReviewCommentService.LineNumberIncorrect", "repository", repoName)
        .increment();

    // Accept the usage when it falls in the range of lines preceding a modified line, up to a max
    // (inclusive) of the lineNumberErrorAllowance
    for (int i = 1; i <= this.lineNumberErrorAllowance; i++) {
      if (modifiedLines.contains(startLineNumber + i)) {
        return true;
      }
    }

    logger.debug(
        "Review Comment Generation - Discarding usage {}:{}, no line modified in the PR within {}"
            + " lines after it",
        fileUri,
        startLineNumber,
        this.lineNumberErrorAllowance);

    return false;
  }

  private List<UsageLocation> getUsageLocations(
      AssetExtractorTextUnit assetExtractorTextUnit,
      Map<String, Set<Integer>> githubModifiedLines,
      String repoName,
      String prefixToRemoveFromFileUri) {
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
                  meterRegistry
                      .counter(
                          "GithubReviewCommentService.FileNotModifiedInPr", "repository", repoName)
                      .increment();
                  logger.debug(
                      "Review Comment Generation - Discarding usage {}:{}, the file has no line"
                          + " modified in the PR",
                      fileUri,
                      startLineNumber);
                  return null;
                }

                if (!isUsageOnPrChange(modifiedLines, repoName, fileUri, startLineNumber)) {
                  return null;
                }

                return new UsageLocation(fileUri, startLineNumber);

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

  private String formatReviewCommentBody(String checkName, ResultLevel level, String message) {
    String icon = level == ResultLevel.ERROR ? "🛑" : "⚠️";
    String levelText = level == ResultLevel.ERROR ? "Error" : "Warning";

    return String.format("%s **%s: %s**\n\n%s", icon, levelText, checkName, message);
  }

  /**
   * Generates GitHub PR review comments based on CLI check failures.
   *
   * <p>Usages pointing at a file that has no line modified in the PR are discarded, as are usages
   * that do not point at a change made in the PR (see {@link #isUsageOnPrChange}). The comments
   * that are kept are reported on the line of the usage.
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

        if (hasUsages(assetExtractorTextUnit)) {
          List<UsageLocation> usageLocations =
              getUsageLocations(
                  assetExtractorTextUnit,
                  githubModifiedLines,
                  repoName,
                  prefixToRemoveFromFileUris);

          for (UsageLocation location : usageLocations) {
            String commentBody =
                formatReviewCommentBody(
                    checkDisplayName, resultLevel, resultCheckFailure.failureMessage());
            reviewComments.add(
                new GithubClient.ReviewComment(
                    commentBody, location.getFilePath(), location.getLineNumber()));
          }
        }
      }
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
