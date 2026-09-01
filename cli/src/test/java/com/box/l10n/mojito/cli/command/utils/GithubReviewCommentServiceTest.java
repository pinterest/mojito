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

  private static final String[] COMMENT_FILE_EXTENSIONS = new String[] {"py", "xml"};
  private static final int DEFAULT_LINE_ERROR_ALLOWANCE = 2;

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
    List<GithubClient.ReviewComment> orderedReviewComments =
        reviewComments.stream()
            .sorted(Comparator.comparing(GithubClient.ReviewComment::getPath))
            .toList();
    assertThat(orderedReviewComments).hasSize(2);
    assertThat(orderedReviewComments.getFirst().getPath()).isEqualTo("file1.java");
    assertThat(orderedReviewComments.getFirst().getLine()).isEqualTo(10);
    assertThat(orderedReviewComments.getFirst().getBody()).contains("I18N_TestCheck");
    assertThat(orderedReviewComments.getFirst().getBody()).contains("Error");
    assertThat(orderedReviewComments.getFirst().getBody()).contains("Failure message 1");

    assertThat(orderedReviewComments.get(1).getPath()).isEqualTo("file2.java");
    assertThat(orderedReviewComments.get(1).getLine()).isEqualTo(20);
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
  void generateReviewComments_errorLevel() {
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
                CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Error message"));

    CliCheckResult checkResult = createCliCheckResult(true, "ErrorCheck", fieldFailures);

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), new HashMap<>(), "repoName", "");

    // Assert
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.getFirst().getBody()).contains("I18N_ErrorCheck");
    assertThat(reviewComments.getFirst().getBody()).contains("Error");
    assertThat(reviewComments.getFirst().getBody()).contains("Error message");
    assertThat(reviewComments.getFirst().getBody()).doesNotContain("Warning");
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
    assertThat(reviewComments.getFirst().getBody()).contains("Warning");
    assertThat(reviewComments.getFirst().getBody()).contains("⚠️");
    assertThat(reviewComments.getFirst().getBody()).doesNotContain("Error");
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
    assertThat(reviewComments.getFirst().getPath()).isEqualTo("src/file1.java");
    assertThat(reviewComments.getFirst().getLine()).isEqualTo(10);
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
    assertThat(reviewComments.getFirst().getPath()).isEqualTo("file1.py");
    assertThat(reviewComments.getFirst().getLine()).isEqualTo(11); // Adjusted from 10 to 11
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

  /**
   * Test group for comment-related check logic (lines 148-150).
   *
   * <p>These tests verify the behavior when isCommentRelatedCheck is true/false, which determines
   * whether line number estimation should occur.
   */
  @Test
  void generateReviewComments_isCommentRelatedCheck_false_returnsOriginalLineNumber() {
    // Arrange
    // When isCommentRelatedCheck is false, the method should return the original line number
    // without attempting to estimate/adjust it (lines 148-150).
    GithubReviewCommentService service =
        new GithubReviewCommentService(
            COMMENT_FILE_EXTENSIONS, DEFAULT_LINE_ERROR_ALLOWANCE, new SimpleMeterRegistry());

    String fileUri = "src/main/python/helper.py";
    int originalLineNumber = 10;
    AssetExtractorTextUnit textUnit =
        createAssetExtractorTextUnit("pythonSource", Set.of(fileUri + ":" + originalLineNumber));

    AssetExtractionDiff diff = new AssetExtractionDiff();
    diff.setAddedTextunits(List.of(textUnit));

    Map<String, CliCheckResult.CheckFailure> fieldFailures =
        Map.of(
            "pythonSource",
            new CliCheckResult.CheckFailure(
                CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Test failure"));

    // Use a rule ID that is NOT comment-related (isCommentRelated() returns false)
    CliCheckResult checkResult = createCliCheckResult(true, "TestCheck", fieldFailures);

    Map<String, Set<Integer>> githubModifiedLines = new HashMap<>();
    githubModifiedLines.put(fileUri, Set.of(8, 9, 10, 11, 12));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), githubModifiedLines, "test-repo", "");

    // Assert - should return original line number, not estimated one
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.getFirst().getLine()).isEqualTo(originalLineNumber);
    assertThat(reviewComments.getFirst().getPath()).isEqualTo(fileUri);
  }

  @Test
  void generateReviewComments_isCommentRelatedCheck_false_returnsOriginalLineEvenIfNotModified() {
    // Arrange
    // When isCommentRelatedCheck is false, should return original line even if it's not in
    // modified lines
    GithubReviewCommentService service =
        new GithubReviewCommentService(
            COMMENT_FILE_EXTENSIONS, DEFAULT_LINE_ERROR_ALLOWANCE, new SimpleMeterRegistry());

    String fileUri = "src/main/resources/config.xml";
    int originalLineNumber = 25;
    AssetExtractorTextUnit textUnit =
        createAssetExtractorTextUnit("xmlConfig", Set.of(fileUri + ":" + originalLineNumber));

    AssetExtractionDiff diff = new AssetExtractionDiff();
    diff.setAddedTextunits(List.of(textUnit));

    Map<String, CliCheckResult.CheckFailure> fieldFailures =
        Map.of(
            "xmlConfig",
            new CliCheckResult.CheckFailure(CheckerRuleId.EMPTY_COMMENT_STRING, "Test failure"));

    CliCheckResult checkResult = createCliCheckResult(false, "TestCheck", fieldFailures);

    Map<String, Set<Integer>> githubModifiedLines = new HashMap<>();
    githubModifiedLines.put(fileUri, Set.of(1, 2, 3, 4, 5)); // Line 25 is NOT modified

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), githubModifiedLines, "test-repo", "");

    // Assert - should return original line number (25), not attempt estimation
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.getFirst().getLine()).isEqualTo(originalLineNumber);
  }

  @Test
  void generateReviewComments_isCommentRelatedCheck_true_estimatesLineNumber() {
    // Arrange
    // When isCommentRelatedCheck is true and the original line is not modified,
    // the method should attempt to estimate a nearby modified line
    GithubReviewCommentService service =
        new GithubReviewCommentService(
            COMMENT_FILE_EXTENSIONS, DEFAULT_LINE_ERROR_ALLOWANCE, new SimpleMeterRegistry());

    String fileUri = "src/main/python/messages.py";
    int originalLineNumber = 20;
    AssetExtractorTextUnit textUnit =
        createAssetExtractorTextUnit("pythonSource", Set.of(fileUri + ":" + originalLineNumber));

    AssetExtractionDiff diff = new AssetExtractionDiff();
    diff.setAddedTextunits(List.of(textUnit));

    Map<String, CliCheckResult.CheckFailure> fieldFailures =
        Map.of(
            "pythonSource",
            new CliCheckResult.CheckFailure(
                CheckerRuleId.MISSING_DESCRIPTION_FOR_NUMERIC_PLACEHOLDER, "Test failure"));

    CliCheckResult checkResult = createCliCheckResult(true, "TestCheck", fieldFailures);

    Map<String, Set<Integer>> githubModifiedLines = new HashMap<>();
    // Original line 20 is NOT modified, but line 21 is (within allowance of 2)
    githubModifiedLines.put(fileUri, Set.of(8, 9, 10, 21, 22, 30));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), githubModifiedLines, "test-repo", "");

    // Assert - should estimate and use line 21 instead of original 20
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.getFirst().getLine()).isEqualTo(21);
  }

  @Test
  void generateReviewComments_isCommentRelatedCheck_true_noModifiedLinesReturnsOriginal() {
    // Arrange
    // When isCommentRelatedCheck is true but no nearby modified lines exist,
    // should return original line number
    GithubReviewCommentService service =
        new GithubReviewCommentService(
            COMMENT_FILE_EXTENSIONS, DEFAULT_LINE_ERROR_ALLOWANCE, new SimpleMeterRegistry());

    String fileUri = "src/main/resources/data.xml";
    int originalLineNumber = 50;
    AssetExtractorTextUnit textUnit =
        createAssetExtractorTextUnit("xmlData", Set.of(fileUri + ":" + originalLineNumber));

    AssetExtractionDiff diff = new AssetExtractionDiff();
    diff.setAddedTextunits(List.of(textUnit));

    Map<String, CliCheckResult.CheckFailure> fieldFailures =
        Map.of(
            "xmlData",
            new CliCheckResult.CheckFailure(
                CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Test failure"));

    CliCheckResult checkResult = createCliCheckResult(true, "TestCheck", fieldFailures);

    Map<String, Set<Integer>> githubModifiedLines = new HashMap<>();
    // No nearby lines are modified (50 ± 2 range is empty)
    githubModifiedLines.put(fileUri, Set.of(1, 2, 3, 100));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), githubModifiedLines, "test-repo", "");

    // Assert - should return original line when estimation fails
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.getFirst().getLine()).isEqualTo(originalLineNumber);
  }

  @Test
  void generateReviewComments_isCommentRelatedCheck_skippedForNonCommentFileExtensions() {
    // Arrange
    // When isCommentRelatedCheck is true but file extension is not in
    // extractedCommentFileExtensions,
    // the estimation logic should be skipped
    GithubReviewCommentService service =
        new GithubReviewCommentService(
            COMMENT_FILE_EXTENSIONS, DEFAULT_LINE_ERROR_ALLOWANCE, new SimpleMeterRegistry());

    String fileUri = "src/main/java/Service.java"; // .java is NOT in comment file extensions
    int originalLineNumber = 50;
    AssetExtractorTextUnit textUnit =
        createAssetExtractorTextUnit("javaSource", Set.of(fileUri + ":" + originalLineNumber));

    AssetExtractionDiff diff = new AssetExtractionDiff();
    diff.setAddedTextunits(List.of(textUnit));

    Map<String, CliCheckResult.CheckFailure> fieldFailures =
        Map.of(
            "javaSource",
            new CliCheckResult.CheckFailure(
                CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Test failure"));

    CliCheckResult checkResult = createCliCheckResult(true, "TestCheck", fieldFailures);

    Map<String, Set<Integer>> githubModifiedLines = new HashMap<>();
    githubModifiedLines.put(fileUri, Set.of(1, 2, 3, 100)); // Line 50 is NOT modified

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), githubModifiedLines, "test-repo", "");

    // Assert - should return original line, not attempt estimation for .java files
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.getFirst().getLine()).isEqualTo(originalLineNumber);
  }

  @Test
  void generateReviewComments_isCommentRelatedCheck_true_usesCorrectLineWhenModified() {
    // Arrange
    // When isCommentRelatedCheck is true but the original line IS in modified lines,
    // should return original line without estimation
    GithubReviewCommentService service =
        new GithubReviewCommentService(
            COMMENT_FILE_EXTENSIONS, DEFAULT_LINE_ERROR_ALLOWANCE, new SimpleMeterRegistry());

    String fileUri = "src/main/resources/strings.xml";
    int originalLineNumber = 15;
    AssetExtractorTextUnit textUnit =
        createAssetExtractorTextUnit("stringsConfig", Set.of(fileUri + ":" + originalLineNumber));

    AssetExtractionDiff diff = new AssetExtractionDiff();
    diff.setAddedTextunits(List.of(textUnit));

    Map<String, CliCheckResult.CheckFailure> fieldFailures =
        Map.of(
            "stringsConfig",
            new CliCheckResult.CheckFailure(
                CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Test failure"));

    CliCheckResult checkResult = createCliCheckResult(true, "TestCheck", fieldFailures);

    Map<String, Set<Integer>> githubModifiedLines = new HashMap<>();
    githubModifiedLines.put(fileUri, Set.of(14, 15, 16)); // Line 15 IS modified

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), githubModifiedLines, "test-repo", "");

    // Assert - should use original line since it's already in modified lines
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.getFirst().getLine()).isEqualTo(originalLineNumber);
  }
}
