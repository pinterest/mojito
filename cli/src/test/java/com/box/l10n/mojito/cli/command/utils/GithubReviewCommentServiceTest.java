package com.box.l10n.mojito.cli.command.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.box.l10n.mojito.cli.command.checks.CheckerRuleId;
import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.*;
import org.junit.jupiter.api.Test;

class GithubReviewCommentServiceTest {

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
  void generateReviewComments_withUsages() {
    // Arrange
    GithubReviewCommentService service =
        new GithubReviewCommentService(new String[] {}, 1, new SimpleMeterRegistry());

    AssetExtractorTextUnit textUnitWithUsage =
        createAssetExtractorTextUnit("source1", Set.of("file1.java:10", "file2.java:20"));

    AssetExtractionDiff diff = new AssetExtractionDiff();
    diff.setAddedTextunits(List.of(textUnitWithUsage));

    Map<String, CliCheckResult.CheckFailure> fieldFailures =
        Map.of(
            "source1",
            new CliCheckResult.CheckFailure(
                CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Failure message 1"));

    CliCheckResult checkResult = createCliCheckResult(true, "TestCheck", fieldFailures);

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), new HashMap<>(), "repoName", "");

    // Assert
    assertThat(reviewComments).hasSize(2);
    assertThat(reviewComments.get(0).getPath()).isEqualTo("file1.java");
    assertThat(reviewComments.get(0).getLine()).isEqualTo(10);
    assertThat(reviewComments.get(0).getBody()).contains("I18N_TestCheck");
    assertThat(reviewComments.get(0).getBody()).contains("Error");
    assertThat(reviewComments.get(0).getBody()).contains("Failure message 1");
    assertThat(reviewComments.get(0).getBody()).contains("EMPTY_PLACEHOLDER_COMMENT");

    assertThat(reviewComments.get(1).getPath()).isEqualTo("file2.java");
    assertThat(reviewComments.get(1).getLine()).isEqualTo(20);
  }

  @Test
  void generateReviewComments_withNoUsages() {
    // Arrange
    GithubReviewCommentService service =
        new GithubReviewCommentService(new String[] {}, 1, new SimpleMeterRegistry());

    AssetExtractorTextUnit textUnitNoUsage =
        createAssetExtractorTextUnit("source1", Collections.emptySet());

    AssetExtractionDiff diff = new AssetExtractionDiff();
    diff.setAddedTextunits(List.of(textUnitNoUsage));

    Map<String, CliCheckResult.CheckFailure> fieldFailures =
        Map.of(
            "source1",
            new CliCheckResult.CheckFailure(CheckerRuleId.EMPTY_COMMENT_STRING, "Failure message"));

    CliCheckResult checkResult = createCliCheckResult(false, "TestCheck", fieldFailures);

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), new HashMap<>(), "repoName", "");

    // Assert - no review comments should be generated for text units without usages
    assertThat(reviewComments).isEmpty();
  }

  @Test
  void generateReviewComments_warningLevel() {
    // Arrange
    GithubReviewCommentService service =
        new GithubReviewCommentService(new String[] {}, 1, new SimpleMeterRegistry());

    AssetExtractorTextUnit textUnitWithUsage =
        createAssetExtractorTextUnit("source1", Set.of("file1.java:15"));

    AssetExtractionDiff diff = new AssetExtractionDiff();
    diff.setAddedTextunits(List.of(textUnitWithUsage));

    Map<String, CliCheckResult.CheckFailure> fieldFailures =
        Map.of(
            "source1",
            new CliCheckResult.CheckFailure(
                CheckerRuleId.EMPTY_CONTEXT_AND_COMMENT_STRINGS, "Warning message"));

    // Create warning (not error) check result
    CliCheckResult checkResult = createCliCheckResult(false, "WarningCheck", fieldFailures);

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), new HashMap<>(), "repoName", "");

    // Assert
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.get(0).getBody()).contains("Warning");
    assertThat(reviewComments.get(0).getBody()).contains("⚠️");
    assertThat(reviewComments.get(0).getBody()).doesNotContain("Error");
  }

  @Test
  void generateReviewComments_withFileUriPrefix() {
    // Arrange
    GithubReviewCommentService service =
        new GithubReviewCommentService(new String[] {}, 1, new SimpleMeterRegistry());

    AssetExtractorTextUnit textUnitWithUsage =
        createAssetExtractorTextUnit("source1", Set.of("/project/src/file1.java:10"));

    AssetExtractionDiff diff = new AssetExtractionDiff();
    diff.setAddedTextunits(List.of(textUnitWithUsage));

    Map<String, CliCheckResult.CheckFailure> fieldFailures =
        Map.of(
            "source1",
            new CliCheckResult.CheckFailure(CheckerRuleId.CONTROL_CHARACTER_DETECTED, "Message"));

    CliCheckResult checkResult = createCliCheckResult(true, "TestCheck", fieldFailures);

    // Act - remove prefix "/project/"
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), new HashMap<>(), "repoName", "/project/");

    // Assert
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.get(0).getPath()).isEqualTo("src/file1.java");
    assertThat(reviewComments.get(0).getLine()).isEqualTo(10);
  }

  @Test
  void generateReviewComments_withModifiedLinesAdjustment() {
    // Arrange
    GithubReviewCommentService service =
        new GithubReviewCommentService(new String[] {"py", "xml"}, 2, new SimpleMeterRegistry());

    AssetExtractorTextUnit textUnitWithUsage =
        createAssetExtractorTextUnit("source1", Set.of("file1.py:10"));

    AssetExtractionDiff diff = new AssetExtractionDiff();
    diff.setAddedTextunits(List.of(textUnitWithUsage));

    Map<String, CliCheckResult.CheckFailure> fieldFailures =
        Map.of(
            "source1",
            new CliCheckResult.CheckFailure(
                CheckerRuleId.MISSING_DESCRIPTION_FOR_NUMERIC_PLACEHOLDER, "Message"));

    CliCheckResult checkResult = createCliCheckResult(true, "TestCheck", fieldFailures);

    // Modified lines: line 10 is not modified, but line 11 is
    Map<String, Set<Integer>> modifiedLines = Map.of("file1.py", Set.of(11, 12, 13));

    // Act - should adjust line number from 10 to 11 for comment-related check
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), modifiedLines, "repoName", "");

    // Assert
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.get(0).getPath()).isEqualTo("file1.py");
    assertThat(reviewComments.get(0).getLine()).isEqualTo(11); // Adjusted from 10 to 11
  }

  @Test
  void generateReviewComments_multipleCheckFailures() {
    // Arrange
    GithubReviewCommentService service =
        new GithubReviewCommentService(new String[] {}, 1, new SimpleMeterRegistry());

    AssetExtractorTextUnit textUnit1 =
        createAssetExtractorTextUnit("source1", Set.of("file1.java:10"));
    AssetExtractorTextUnit textUnit2 =
        createAssetExtractorTextUnit("source2", Set.of("file2.java:20"));

    AssetExtractionDiff diff = new AssetExtractionDiff();
    diff.setAddedTextunits(List.of(textUnit1, textUnit2));

    Map<String, CliCheckResult.CheckFailure> fieldFailures1 =
        Map.of(
            "source1",
            new CliCheckResult.CheckFailure(CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Failure 1"));

    Map<String, CliCheckResult.CheckFailure> fieldFailures2 =
        Map.of(
            "source2",
            new CliCheckResult.CheckFailure(CheckerRuleId.EMPTY_COMMENT_STRING, "Failure 2"));

    CliCheckResult checkResult1 = createCliCheckResult(true, "Check1", fieldFailures1);
    CliCheckResult checkResult2 = createCliCheckResult(false, "Check2", fieldFailures2);

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult1, checkResult2), List.of(diff), new HashMap<>(), "repoName", "");

    // Assert
    assertThat(reviewComments).hasSize(2);
    assertThat(reviewComments.get(0).getBody()).contains("I18N_Check1");
    assertThat(reviewComments.get(0).getBody()).contains("Error");
    assertThat(reviewComments.get(1).getBody()).contains("I18N_Check2");
    assertThat(reviewComments.get(1).getBody()).contains("Warning");
  }

  @Test
  void generateReviewComments_invalidUsageFormat() {
    // Arrange
    GithubReviewCommentService service =
        new GithubReviewCommentService(new String[] {}, 1, new SimpleMeterRegistry());

    AssetExtractorTextUnit textUnitWithInvalidUsage =
        createAssetExtractorTextUnit("source1", Set.of("invalid_format", "file1.java:notanumber"));

    AssetExtractionDiff diff = new AssetExtractionDiff();
    diff.setAddedTextunits(List.of(textUnitWithInvalidUsage));

    Map<String, CliCheckResult.CheckFailure> fieldFailures =
        Map.of(
            "source1",
            new CliCheckResult.CheckFailure(CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Message"));

    CliCheckResult checkResult = createCliCheckResult(true, "TestCheck", fieldFailures);

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), new HashMap<>(), "repoName", "");

    // Assert - invalid usages should be filtered out
    assertThat(reviewComments).isEmpty();
  }
}
