package com.box.l10n.mojito.cli.command.utils;

import com.box.l10n.mojito.cli.GitInfo;
import com.box.l10n.mojito.cli.command.checks.CheckerRuleId;
import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
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

  // File extensions for where the comments are extracted from comments above the translation
  // function call
  // This is useful for adjusting the line number for comment related checks
  private final String[] extractedCommentFileExtensions;

  private final MeterRegistry meterRegistry;

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
      List<AssetExtractionDiff> assetExtractionDiffs,
      Map<String, Set<Integer>> githubModifiedLines) {
    SarifBuilder sarifBuilder = new SarifBuilder();
    Map<String, AssetExtractorTextUnit> nameToAssetTextUnitMap =
        assetExtractionDiffs.stream()
            .flatMap(diff -> diff.getAddedTextunits().stream())
            .collect(Collectors.toMap(AssetExtractorTextUnit::getName, x -> x));
    for (CliCheckResult checkFailure : cliCheckerFailures) {
      ResultLevel resultLevel = checkFailure.isHardFail() ? ResultLevel.ERROR : ResultLevel.WARNING;
      sarifBuilder.addRun(checkFailure.getCheckName(), infoUri, this.gitInfo.getCommit().getId());
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
              convertToTitleCase(checkFailure.getCheckName()),
              resultCheckFailure.failureMessage(),
              getUsageLocations(
                  assetExtractorTextUnit,
                  extractedCommentFileExtensions,
                  githubModifiedLines,
                  ruleId.isCommentRelated()));
        } else {
          sarifBuilder.addResultWithoutLocation(
              ruleId.toString(),
              resultLevel,
              convertToTitleCase(checkFailure.getCheckName()),
              resultCheckFailure.failureMessage());
        }
      }
    }

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
                int startLineNumber = Integer.parseInt(usage.substring(colonIndex + 1));

                if (!isCommentRelatedCheck) {
                  return new Location(fileUri, startLineNumber);
                }

                int fullStopIndex = fileUri.lastIndexOf('.');
                if (fullStopIndex == -1) {
                  return new Location(fileUri, startLineNumber);
                }

                String fileExtension = Files.getFileExtension(fileUri);
                if (Arrays.stream(extractedCommentFileExtensions)
                    .noneMatch(x -> x.equalsIgnoreCase(fileExtension))) {
                  return new Location(fileUri, startLineNumber);
                }

                // If the comment is flagged, the usage only reports the line
                // where the string is added
                // If only a comment is changed, then the line number is off (GitHub only
                // considers the line in PR diff as a valid source
                // for a SARIF / security related comment)

                Set<Integer> modifiedLines = githubModifiedLines.get(fileUri);
                if (modifiedLines == null || modifiedLines.isEmpty()) {
                  return new Location(fileUri, startLineNumber);
                }

                if (modifiedLines.contains(startLineNumber)) {
                  return new Location(fileUri, startLineNumber);
                }

                // Best effort to try find nearest line if the exact line is not available.
                // This could happen if a comment is changed without changing the string, or vice
                // versa. Django always reports the translation line number but GitHub will not
                // accept a line number which was not modified in the PR.
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
                    .counter("SarifFileGenerator.Github.LineNumberMisreported")
                    .increment();
                return new Location(fileUri, startLineNumber);

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

  // Converts a string to title case:
  // i.e EXAMPLE_CHECK_NAME -> Example check name
  static String convertToTitleCase(String input) {
    String result =
        Arrays.stream(input.split("_")).map(String::toLowerCase).collect(Collectors.joining(" "));
    return result.substring(0, 1).toUpperCase() + result.substring(1);
  }
}
