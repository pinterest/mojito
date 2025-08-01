package com.box.l10n.mojito.cli.command.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.box.l10n.mojito.cli.command.checks.CheckerRuleId;
import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.sarif.model.Location;
import com.box.l10n.mojito.sarif.model.Result;
import com.box.l10n.mojito.sarif.model.ResultLevel;
import com.box.l10n.mojito.sarif.model.Run;
import com.box.l10n.mojito.sarif.model.Sarif;
import java.util.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SarifFileGeneratorTest {

  private CliCheckResult createCliCheckResult(
      boolean isError, String checkName, Map<String, CliCheckResult.CheckFailure> fieldFailures) {
    CliCheckResult checkResult = new CliCheckResult(isError, checkName);
    checkResult.appendToFailuresMap(fieldFailures);
    return checkResult;
  }

  private AssetExtractorTextUnit createAssetExtractorTextUnit(String source, Set<String> usages) {
    AssetExtractorTextUnit textUnit = new AssetExtractorTextUnit();
    textUnit.setName(source);
    textUnit.setSource(source);
    textUnit.setUsages(usages);
    return textUnit;
  }

  @Test
  void generateSarifFile_withUsagesAndNoUsages() {
    // Arrange
    SarifFileGenerator generator = new SarifFileGenerator("infoUri");
    AssetExtractorTextUnit textUnitWithUsage =
        createAssetExtractorTextUnit("source1", Set.of("file1.java:10", "file2.java:20"));
    AssetExtractorTextUnit textUnitNoUsage =
        createAssetExtractorTextUnit("source2", Collections.emptySet());
    AssetExtractorTextUnit textUnitNoUsage2 =
        createAssetExtractorTextUnit("source3", Collections.emptySet());
    AssetExtractorTextUnit textUnitNoUsage3 =
        createAssetExtractorTextUnit("source4", Collections.emptySet());

    AssetExtractionDiff diff = new AssetExtractionDiff();
    diff.setAddedTextunits(List.of(textUnitWithUsage, textUnitNoUsage));
    Map<String, CliCheckResult.CheckFailure> fieldFailures =
        Map.of(
            "source1",
                new CliCheckResult.CheckFailure(
                    CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Failure message 1"),
            "source2",
                new CliCheckResult.CheckFailure(
                    CheckerRuleId.EMPTY_COMMENT_STRING, "Failure message 2"),
            "source4",
                new CliCheckResult.CheckFailure(
                    CheckerRuleId.EMPTY_CONTEXT_AND_COMMENT_STRINGS, "Failure message 3"));

    CliCheckResult checkResult = createCliCheckResult(true, "TestCheck", fieldFailures);

    // Act
    Sarif sarif = generator.generateSarifFile(List.of(checkResult), List.of(diff));

    // Assert
    assertThat(sarif.getRuns()).hasSize(1);
    List<Run> runs = sarif.getRuns();
    Run run = runs.getFirst();
    assertThat(run.getTool().getDriver().getName()).isEqualTo("TestCheck");
    assertThat(run.getResults()).hasSize(3);

    // Check that source3 is not reported [because it had no failures]
    Assertions.assertFalse(
        run.getResults().stream().anyMatch(x -> x.getRuleId().contains("source3")));

    // Check result with usages
    Result resultWithUsage =
        run.getResults().stream()
            .filter(r -> CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT.toString().equals(r.getRuleId()))
            .findFirst()
            .orElseThrow();
    assertThat(resultWithUsage.getLevel()).isEqualTo(ResultLevel.ERROR);
    assertThat(resultWithUsage.getMessage().getText()).isEqualTo("Failure message 1");
    assertThat(resultWithUsage.getLocations()).hasSize(2);
    assertThat(resultWithUsage.getLocations())
        .extracting(Location::getPhysicalLocation)
        .anyMatch(
            loc ->
                loc.getArtifactLocation().getUri().equals("file1.java")
                    && loc.getRegion().getStartLine() == 10);

    // Check result without usages (aggregate)
    List<Result> resultNoUsage =
        run.getResults().stream()
            .filter(r -> r.getLocations() == null || r.getLocations().isEmpty())
            .toList();
    Assertions.assertTrue(
        resultNoUsage.stream()
            .allMatch(
                x ->
                    x.getRuleId()
                            .contains(CheckerRuleId.EMPTY_CONTEXT_AND_COMMENT_STRINGS.toString())
                        || x.getRuleId().contains(CheckerRuleId.EMPTY_COMMENT_STRING.toString())));
    Assertions.assertTrue(
        resultNoUsage.stream()
            .allMatch(
                x ->
                    x.getRuleId()
                            .contains(CheckerRuleId.EMPTY_CONTEXT_AND_COMMENT_STRINGS.toString())
                        || x.getRuleId().contains(CheckerRuleId.EMPTY_COMMENT_STRING.toString())));
  }

  @Test
  void generateSarifFile_warningLevel() {
    SarifFileGenerator generator = new SarifFileGenerator("infoUri");

    AssetExtractorTextUnit textUnit =
        createAssetExtractorTextUnit("sourceX", Set.of("fileX.java:42"));

    AssetExtractionDiff diff = new AssetExtractionDiff();
    diff.setAddedTextunits(List.of(textUnit));

    Map<String, CliCheckResult.CheckFailure> fieldFailures =
        Map.of(
            "sourceX",
            new CliCheckResult.CheckFailure(
                CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Warn message"));

    CliCheckResult checkResult = createCliCheckResult(false, "WarnCheck", fieldFailures);

    Sarif sarif = generator.generateSarifFile(List.of(checkResult), List.of(diff));

    assertThat(sarif.getRuns()).hasSize(1);
    List<Run> runs = sarif.getRuns();
    Run run = runs.getFirst();
    assertThat(run.getResults()).hasSize(1);
    Result result = run.getResults().getFirst();
    assertThat(result.getLevel()).isEqualTo(ResultLevel.WARNING);
    assertThat(result.getRuleId()).isEqualTo(CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT.toString());
    assertThat(result.getMessage().getText()).isEqualTo("Warn message");
  }

  @Test
  void getUsageLocations_handlesInvalidLineNumber() {
    SarifFileGenerator generator = new SarifFileGenerator("infoUri");

    AssetExtractorTextUnit textUnit =
        createAssetExtractorTextUnit(
            "badSource",
            Set.of(
                "badfile.java:notanumber",
                "goodfile.java:5",
                "badUsage.java",
                "noColon",
                "badUsage2.java",
                ""));

    AssetExtractionDiff diff = new AssetExtractionDiff();
    diff.setAddedTextunits(List.of(textUnit));

    Map<String, CliCheckResult.CheckFailure> fieldFailures =
        Map.of(
            "badSource",
            new CliCheckResult.CheckFailure(
                CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Some failure"));

    CliCheckResult checkResult = createCliCheckResult(true, "BadCheck", fieldFailures);

    Sarif sarif = generator.generateSarifFile(List.of(checkResult), List.of(diff));

    List<Run> runs = sarif.getRuns();
    Run run = runs.getFirst();
    Result result = run.getResults().getFirst();
    assertThat(result.getLocations()).hasSize(1);
    assertThat(
            result.getLocations().getFirst().getPhysicalLocation().getArtifactLocation().getUri())
        .isEqualTo("goodfile.java");
    assertThat(result.getLocations().getFirst().getPhysicalLocation().getRegion().getStartLine())
        .isEqualTo(5);
  }
}
