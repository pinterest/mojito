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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

  @Autowired
  SarifFileGenerator(
      @Value("${l10n.extraction-check.sarif.infoUri:}") String infoUri,
      @Value(
              "#{'${l10n.extraction-check.sarif.extracted-comments.fileExtensions:py,xml}'.split(',')}")
          String[] extractedCommentFileExtensions,
      @Autowired GitInfo gitInfo) {
    this.infoUri = infoUri;
    this.gitInfo = gitInfo;
    this.extractedCommentFileExtensions = extractedCommentFileExtensions;
  }

  public Sarif generateSarifFile(
      List<CliCheckResult> cliCheckerFailures, List<AssetExtractionDiff> assetExtractionDiffs) {
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

  static List<Location> getUsageLocations(
      AssetExtractorTextUnit assetExtractorTextUnit,
      String[] extractedCommentFileExtensions,
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
                return new Location(fileUri, startLineNumber - 1);

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
