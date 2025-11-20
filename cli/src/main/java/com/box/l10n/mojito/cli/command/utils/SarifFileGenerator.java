package com.box.l10n.mojito.cli.command.utils;

import com.box.l10n.mojito.cli.GitInfo;
import com.box.l10n.mojito.cli.command.checks.CheckerRuleId;
import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.cli.command.checks.CliCheckerType;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.sarif.builder.SarifBuilder;
import com.box.l10n.mojito.sarif.model.Location;
import com.box.l10n.mojito.sarif.model.ResultLevel;
import com.box.l10n.mojito.sarif.model.Sarif;
import com.google.common.io.Files;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SarifFileGenerator {

  static Logger logger = LoggerFactory.getLogger(SarifFileGenerator.class);

  private final GitInfo gitInfo;

  private final String infoUri;

  private final MeterRegistry meterRegistry;

  // File extensions for where the comments are extracted from comments above the translation
  // function call
  // This is useful for adjusting the line number for comment related checks
  private final String[] extractedCommentFileExtensions;

  @Autowired
  SarifFileGenerator(
      @Value("${l10n.extraction-check.sarif.infoUri:}") String infoUri,
      @Value(
              "#{'${l10n.extraction-check.sarif.extracted-comments.fileExtensions:py,xml}'.split(',')}")
          String[] extractedCommentFileExtensions,
      GitInfo gitInfo,
      MeterRegistry meterRegistry) {
    this.infoUri = infoUri;
    this.gitInfo = gitInfo;
    this.extractedCommentFileExtensions = extractedCommentFileExtensions;
    this.meterRegistry = meterRegistry;
  }

  public Sarif generateSarifFile(
      List<CliCheckResult> cliCheckerFailures,
      List<CliCheckerType> configuredCheckerTypes,
      List<AssetExtractionDiff> assetExtractionDiffs,
      Map<String, Set<Integer>> githubModifiedLines,
      String repoName,
      String prefixToRemoveFromFileUris) {
    SarifBuilder sarifBuilder = new SarifBuilder();
    Map<String, AssetExtractorTextUnit> nameToAssetTextUnitMap =
        assetExtractionDiffs.stream()
            .flatMap(diff -> diff.getAddedTextunits().stream())
            .collect(Collectors.toMap(AssetExtractorTextUnit::getName, x -> x));

    var failedCheckNames =
        cliCheckerFailures.stream().map(CliCheckResult::getCheckName).collect(Collectors.toSet());
    var configuredCheckNames =
        configuredCheckerTypes.stream().map(Enum::name).collect(Collectors.toSet());
    var passedChecks =
        configuredCheckNames.stream()
            .filter(x -> !failedCheckNames.contains(x))
            .collect(Collectors.toSet());

    for (CliCheckResult checkFailure : cliCheckerFailures) {
      ResultLevel resultLevel = checkFailure.isHardFail() ? ResultLevel.ERROR : ResultLevel.WARNING;
      sarifBuilder.addRun(
          buildCheckDisplayName(checkFailure), infoUri, this.gitInfo.getCommit().getId());
      for (Map.Entry<String, CliCheckResult.CheckFailure> entry :
          checkFailure.getNameToFailuresMap().entrySet()) {

        String source = entry.getKey();
        CliCheckResult.CheckFailure resultCheckFailure = entry.getValue();
        AssetExtractorTextUnit assetExtractorTextUnit = nameToAssetTextUnitMap.get(source);
        CheckerRuleId ruleId = resultCheckFailure.ruleId();
        if (hasUsages(assetExtractorTextUnit)) {
          sarifBuilder.addResultWithLocations(
              ruleId.toString(),
              resultLevel,
              resultCheckFailure.failureMessage(),
              resultCheckFailure.failureMessage(),
              getUsageLocations(
                  assetExtractorTextUnit,
                  extractedCommentFileExtensions,
                  githubModifiedLines,
                  repoName,
                  prefixToRemoveFromFileUris,
                  ruleId.isCommentRelated()));
        } else {
          sarifBuilder.addResultWithoutLocation(
              ruleId.toString(),
              resultLevel,
              resultCheckFailure.failureMessage(),
              resultCheckFailure.failureMessage());
        }
      }
    }

    passedChecks.forEach(
        checkName -> {
          sarifBuilder.addRun(
              buildCheckDisplayName(checkName), infoUri, this.gitInfo.getCommit().getId());
        });

    return sarifBuilder.build();
  }

  static boolean hasUsages(AssetExtractorTextUnit assetExtractorTextUnit) {
    return assetExtractorTextUnit != null
        && assetExtractorTextUnit.getUsages() != null
        && !assetExtractorTextUnit.getUsages().isEmpty();
  }

  List<Location> getUsageLocations(
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
                  return new Location(fileUri, startLineNumber);
                }

                if (!modifiedLines.contains(startLineNumber)) {
                  meterRegistry
                      .counter(
                          "SarifFileGenerator.Github.LineNumberIncorrect", "repository", repoName)
                      .increment();
                }

                if (!isCommentRelatedCheck) {
                  return new Location(fileUri, startLineNumber);
                }

                return estimateLocationLineNumber(
                    modifiedLines,
                    extractedCommentFileExtensions,
                    repoName,
                    fileUri,
                    startLineNumber);

              } catch (NumberFormatException e) {
                logger.warn(
                    "SARIF Generation - Unable to parse line number: {}",
                    usage.substring(colonIndex + 1));
                return null;
              }
            })
        .filter(Objects::nonNull)
        .toList();
  }

  /***
   * Github only accepts line numbers which were modified in the PR: all other lines are ignored.
   * If a comment is flagged by the checker, the usage reported is the line where the string was added.
   * Hence, if only a comment is changed, then the line number will be wrong.
   * We try to add or subtract one to find a valid modified line (before or after the line) to
   * get a line number Github will accept
   */
  private Location estimateLocationLineNumber(
      Set<Integer> modifiedLines,
      String[] extractedCommentFileExtensions,
      String repoName,
      String fileUri,
      int startLineNumber) {

    int fullStopIndex = fileUri.lastIndexOf('.');
    if (fullStopIndex == -1) {
      return new Location(fileUri, startLineNumber);
    }

    String fileExtension = Files.getFileExtension(fileUri);
    if (Arrays.stream(extractedCommentFileExtensions)
        .noneMatch(x -> x.equalsIgnoreCase(fileExtension))) {
      return new Location(fileUri, startLineNumber);
    }

    if (modifiedLines.contains(startLineNumber)) {
      return new Location(fileUri, startLineNumber);
    }

    Integer lineNumber = startLineNumber - 1;
    if (modifiedLines.contains(lineNumber)) {
      return new Location(fileUri, lineNumber);
    } else {
      lineNumber = startLineNumber + 1;
      if (modifiedLines.contains(lineNumber)) {
        return new Location(fileUri, lineNumber);
      }
    }

    meterRegistry
        .counter("SarifFileGenerator.Github.LineNumberVariationNotFound", "repository", repoName)
        .increment();

    return new Location(fileUri, startLineNumber);
  }

  private String buildCheckDisplayName(String checkName) {
    return "I18N_" + checkName;
  }

  private String buildCheckDisplayName(CliCheckResult checkResult) {
    return buildCheckDisplayName(checkResult.getCheckName());
  }
}
