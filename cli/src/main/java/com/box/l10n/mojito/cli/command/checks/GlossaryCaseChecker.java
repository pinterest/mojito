package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GlossaryCaseChecker extends AbstractCliChecker {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public CliCheckResult run(List<AssetExtractionDiff> assetExtractionDiffs) {
    CliCheckResult cliCheckResult = createCliCheckerResult();

    // TEMPORARY, DO NOT MERGE: fail on every added text unit to exercise the PR review comments
    return failEveryAddedTextUnit(cliCheckResult, assetExtractionDiffs);
  }

  /**
   * Reports a failure for every added text unit, regardless of the glossary terms, so that a pull
   * request always gets review comments for the usages of the strings it adds.
   */
  private CliCheckResult failEveryAddedTextUnit(
      CliCheckResult cliCheckResult, List<AssetExtractionDiff> assetExtractionDiffs) {
    cliCheckResult.setSuccessful(false);

    Map<String, CliCheckResult.CheckFailure> failedFeatureMap =
        getAddedTextUnitsExcludingInconsistentComments(assetExtractionDiffs).stream()
            .collect(
                Collectors.toMap(
                    AssetExtractorTextUnit::getName,
                    assetExtractorTextUnit ->
                        new CliCheckResult.CheckFailure(
                            CheckerRuleId.AGGREGATE_GLOSSARY_CASE_CHECKER_RESULTS,
                            "Forced glossary case check failure"),
                    (first, second) -> first));
    cliCheckResult.appendToFailuresMap(failedFeatureMap);
    cliCheckResult.setNotificationText(
        "Glossary check failures:"
            + System.lineSeparator()
            + BULLET_POINT
            + "Forced glossary case check failure");

    return cliCheckResult;
  }

  private CliCheckResult runGlossaryCaseCheck(List<AssetExtractionDiff> assetExtractionDiffs) {
    CliCheckResult cliCheckResult = createCliCheckerResult();
    try {
      GlossaryTermCaseCheckerTrie glossaryTermCaseCheckerTrie = getGlossaryTermTrie();
      List<GlossaryCaseCheckerSearchResult> failures =
          getGlossarySearchResults(glossaryTermCaseCheckerTrie, assetExtractionDiffs);
      if (!failures.isEmpty()) {
        if (failures.stream().anyMatch(GlossaryCaseCheckerSearchResult::isMajorFailure)) {
          cliCheckResult.setSuccessful(false);
        }
        Map<String, CliCheckResult.CheckFailure> failedFeatureMap =
            failures.stream()
                .collect(
                    Collectors.toMap(
                        GlossaryCaseCheckerSearchResult::getName,
                        searchResult ->
                            new CliCheckResult.CheckFailure(
                                CheckerRuleId.AGGREGATE_GLOSSARY_CASE_CHECKER_RESULTS,
                                String.join("\n ", searchResult.getFailures()))));
        cliCheckResult.appendToFailuresMap(failedFeatureMap);
        cliCheckResult.setNotificationText(buildNotificationText(failures).toString());
      }
    } catch (IOException e) {
      throw new CommandException(
          String.format(
              "Error retrieving glossary terms from file path %s: %s",
              cliCheckerOptions.getGlossaryFilePath(), e.getMessage()));
    }

    return cliCheckResult;
  }

  private List<GlossaryCaseCheckerSearchResult> getGlossarySearchResults(
      GlossaryTermCaseCheckerTrie glossaryTermCaseCheckerTrie,
      List<AssetExtractionDiff> assetExtractionDiffs) {
    return getAddedTextUnitsExcludingInconsistentComments(assetExtractionDiffs).stream()
        .map(
            assetExtractorTextUnit ->
                glossaryTermCaseCheckerTrie.runGlossaryCaseCheck(
                    assetExtractorTextUnit.getName(),
                    assetExtractorTextUnit.getSource(),
                    this.cliCheckerOptions.getParameterRegexSet()))
        .filter(result -> !result.isSuccess())
        .collect(Collectors.toList());
  }

  private GlossaryTermCaseCheckerTrie getGlossaryTermTrie() throws IOException {
    List<GlossaryTerm> terms =
        Arrays.asList(
            objectMapper.readValue(
                Paths.get(cliCheckerOptions.getGlossaryFilePath()).toFile(), GlossaryTerm[].class));
    return new GlossaryTermCaseCheckerTrie(terms);
  }

  private StringBuilder buildNotificationText(List<GlossaryCaseCheckerSearchResult> failures) {
    StringBuilder builder = new StringBuilder();
    builder.append("Glossary check failures:");
    builder.append(System.lineSeparator());
    builder.append(
        failures.stream()
            .map(this::getFailureText)
            .collect(Collectors.joining(System.lineSeparator())));
    return builder;
  }

  private String getFailureText(GlossaryCaseCheckerSearchResult failure) {
    StringBuilder sb = new StringBuilder();
    sb.append(
        failure.getFailures().stream()
            .map(failureText -> BULLET_POINT + failureText)
            .collect(Collectors.joining(System.lineSeparator())));
    return sb.toString();
  }
}
