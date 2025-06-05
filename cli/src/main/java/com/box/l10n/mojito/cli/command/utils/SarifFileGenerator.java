package com.box.l10n.mojito.cli.command.utils;

import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.sarif.builder.SarifBuilder;
import com.box.l10n.mojito.sarif.model.Location;
import com.box.l10n.mojito.sarif.model.ResultLevel;
import com.box.l10n.mojito.sarif.model.Sarif;
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

  private final String infoUri;

  @Autowired
  SarifFileGenerator(@Value("${l10n.extraction-check.sarif.infoUri:}") String infoUri) {
    this.infoUri = infoUri;
  }

  public Sarif generateSarifFile(
      List<CliCheckResult> cliCheckerFailures, List<AssetExtractionDiff> assetExtractionDiffs) {
    SarifBuilder sarifBuilder = new SarifBuilder();
    Map<String, AssetExtractorTextUnit> sourceToAssetTextUnitMap =
        assetExtractionDiffs.stream()
            .flatMap(diff -> diff.getAddedTextunits().stream())
            .collect(Collectors.toMap(AssetExtractorTextUnit::getSource, x -> x));
    for (CliCheckResult checkFailure : cliCheckerFailures) {
      ResultLevel resultLevel = checkFailure.isHardFail() ? ResultLevel.ERROR : ResultLevel.WARNING;
      sarifBuilder.addRun(checkFailure.getCheckName(), infoUri);
      for (Map.Entry<String, CliCheckResult.CheckFailure> entry :
          checkFailure.getFieldFailuresMap().entrySet()) {
        String source = entry.getKey();
        CliCheckResult.CheckFailure resultCheckFailure = entry.getValue();
        AssetExtractorTextUnit assetExtractorTextUnit = sourceToAssetTextUnitMap.get(source);
        if (hasUsages(assetExtractorTextUnit)) {
          sarifBuilder.addResultWithLocations(
              resultCheckFailure.ruleId().toString(),
              resultLevel,
              resultCheckFailure.failureMessage(),
              getUsageLocations(assetExtractorTextUnit));
        } else {
          sarifBuilder.addResultWithoutLocation(
              resultCheckFailure.ruleId().toString(),
              resultLevel,
              resultCheckFailure.failureMessage());
        }
      }
    }

    return sarifBuilder.build();
  }

  private static boolean hasUsages(AssetExtractorTextUnit assetExtractorTextUnit) {
    return assetExtractorTextUnit != null
        && assetExtractorTextUnit.getUsages() != null
        && !assetExtractorTextUnit.getUsages().isEmpty();
  }

  private static List<Location> getUsageLocations(AssetExtractorTextUnit assetExtractorTextUnit) {
    return assetExtractorTextUnit.getUsages().stream()
        .map(
            usage -> {
              int colonIndex = usage.lastIndexOf(':');
              String fileUri = usage.substring(0, colonIndex);
              try {
                Integer startLineNumber = Integer.parseInt(usage.substring(colonIndex + 1));
                return new Location(fileUri, startLineNumber);
              } catch (NumberFormatException e) {
                logger.info(
                    "SARIF Generation - Unable to parse line number: {}",
                    usage.substring(colonIndex + 1));
                return null;
              }
            })
        .filter(Objects::nonNull)
        .toList();
  }
}
