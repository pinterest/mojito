package com.box.l10n.mojito.cli.command.checks;

import static com.box.l10n.mojito.cli.command.checks.CliCheckerParameters.OPEN_AI_RETRY_ERROR_MSG;
import static com.box.l10n.mojito.cli.command.checks.CliCheckerParameters.REPOSITORY_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.apiclient.AIServiceClient;
import com.box.l10n.mojito.apiclient.model.AICheckResponse;
import com.box.l10n.mojito.apiclient.model.AICheckResult;
import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.rest.ai.AIException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.util.retry.Retry;

public class AICheckerTest {

  @Mock AIServiceClient aiServiceClient;

  List<AssetExtractionDiff> assetExtractionDiffs;

  AIChecker AIChecker;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    AIChecker = new AIChecker();
    AIChecker.aiServiceClient = aiServiceClient;
    List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setName("Some string id --- Test context");
    assetExtractorTextUnit.setSource("A source string with no errors.");
    assetExtractorTextUnit.setComments("Test comment");
    addedTUs.add(assetExtractorTextUnit);
    assetExtractionDiffs = new ArrayList<>();
    AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
    assetExtractionDiff.setAddedTextunits(addedTUs);
    assetExtractionDiffs.add(assetExtractionDiff);

    AIChecker.setCliCheckerOptions(
        new CliCheckerOptions(
            Sets.newHashSet(),
            Sets.newHashSet(),
            ImmutableMap.<String, String>builder()
                .put(REPOSITORY_NAME.getKey(), "test-repo")
                .put(OPEN_AI_RETRY_ERROR_MSG.getKey(), "Retries exhausted for OpenAI check")
                .build()));
  }

  private static AssetExtractorTextUnit buildTextUnit(String id, String source, String comment) {
    AssetExtractorTextUnit tu1 = new AssetExtractorTextUnit();
    tu1.setName(id);
    tu1.setSource(source);
    tu1.setComments(comment);
    return tu1;
  }

  @Test
  public void testCheckSuccess() {
    AICheckResponse aiCheckResponse = new AICheckResponse();
    aiCheckResponse.setError(false);
    aiCheckResponse.setErrorMessage(null);
    Map<String, List<AICheckResult>> checkResults = new HashMap<>();
    List<AICheckResult> results = new ArrayList<>();

    AICheckResult aiCheckResult = new AICheckResult();
    aiCheckResult.setSuccess(true);
    aiCheckResult.setSuggestedFix("");

    results.add(aiCheckResult);
    checkResults.put("A source string with no errors.", results);
    aiCheckResponse.setResults(checkResults);

    when(aiServiceClient.executeAIChecks(any())).thenReturn(aiCheckResponse);
    CliCheckResult result = AIChecker.run(assetExtractionDiffs);

    verify(aiServiceClient, times(1)).executeAIChecks(any());
    assertTrue(result.isSuccessful());
    assertTrue(result.getNotificationText().isEmpty());
  }

  @Test
  public void testTextUnitSourceToNameMapping() {
    List<AssetExtractorTextUnit> addedTUs =
        List.of(
            buildTextUnit("ID1 --- Context1", "Source string 1", "Comment1"),
            buildTextUnit("ID2 --- Context2", "Source string 2", "Comment2"));

    AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
    assetExtractionDiff.setAddedTextunits(addedTUs);
    List<AssetExtractionDiff> diffs = List.of(assetExtractionDiff);

    // Prepare AI check response with failures for both sources
    AICheckResponse aiCheckResponse = new AICheckResponse();
    aiCheckResponse.setError(false);
    aiCheckResponse.setErrorMessage(null);
    Map<String, List<AICheckResult>> checkResults = new HashMap<>();

    AICheckResult fail1 = new AICheckResult();
    fail1.setSuccess(false);
    fail1.setSuggestedFix("Issue in string 1");
    checkResults.put("Source string 1", List.of(fail1));

    AICheckResult fail2 = new AICheckResult();
    fail2.setSuccess(false);
    fail2.setSuggestedFix("Issue in string 2");
    checkResults.put("Source string 2", List.of(fail2));

    aiCheckResponse.setResults(checkResults);
    when(aiServiceClient.executeAIChecks(any())).thenReturn(aiCheckResponse);

    CliCheckResult result = AIChecker.run(diffs);

    assertFalse(result.getNameToFailuresMap().containsKey("Source string 1"));
    assertFalse(result.getNameToFailuresMap().containsKey("Source string 2"));
    assertTrue(result.getNameToFailuresMap().containsKey("ID1 --- Context1"));
    assertTrue(result.getNameToFailuresMap().containsKey("ID2 --- Context2"));
  }

  @Test
  public void testCheckFailure() {
    AICheckResponse AICheckResponse = new AICheckResponse();
    AICheckResponse.setError(false);
    AICheckResponse.setErrorMessage(null);
    Map<String, List<AICheckResult>> checkResults = new HashMap<>();
    List<AICheckResult> results = new ArrayList<>();

    AICheckResult AICheckResult = new AICheckResult();
    AICheckResult.setSuccess(false);
    AICheckResult.setSuggestedFix("Spelling mistake found in the source string.");

    results.add(AICheckResult);
    checkResults.put("A source string with no errors.", results);
    AICheckResponse.setResults(checkResults);

    when(aiServiceClient.executeAIChecks(any())).thenReturn(AICheckResponse);
    CliCheckResult result = AIChecker.run(assetExtractionDiffs);

    verify(aiServiceClient, times(1)).executeAIChecks(any());
    assertFalse(result.isSuccessful());
    assertFalse(result.getNotificationText().isEmpty());
    assertTrue(
        result
            .getNotificationText()
            .contains("The string `A source string with no errors.` has the following issues:"));
    assertTrue(
        result.getNotificationText().contains("Spelling mistake found in the source string."));
  }

  @Test
  public void testRetryExhausted() {
    AIChecker.retryConfiguration =
        Retry.backoff(10, Duration.ofMillis(1)).maxBackoff(Duration.ofMillis(1));
    when(aiServiceClient.executeAIChecks(any())).thenThrow(new AIException("Test error"));
    CliCheckResult result = AIChecker.run(assetExtractionDiffs);
    verify(aiServiceClient, times(11)).executeAIChecks(any());
    assertFalse(result.isSuccessful());
    assertTrue(result.getNotificationText().contains("Retries exhausted for OpenAI check"));
  }

  @Test
  public void exceptionIfNoRepoProvided() {
    AIChecker.setCliCheckerOptions(
        new CliCheckerOptions(
            Sets.newHashSet(),
            Sets.newHashSet(),
            ImmutableMap.<String, String>builder()
                .put(OPEN_AI_RETRY_ERROR_MSG.getKey(), "Retries exhausted for OpenAI check")
                .build()));
    Exception ex = assertThrows(CommandException.class, () -> AIChecker.run(assetExtractionDiffs));
    assertEquals(
        "Repository name must be provided in checker options when using OpenAI checks.",
        ex.getMessage());
  }

  @Test
  public void testErrorResultReturnedFromCli() {
    AIChecker.retryConfiguration =
        Retry.backoff(1, Duration.ofMillis(1)).maxBackoff(Duration.ofMillis(1));
    AICheckResponse AICheckResponse = new AICheckResponse();
    AICheckResponse.setError(true);
    AICheckResponse.setErrorMessage("Some error message");
    when(aiServiceClient.executeAIChecks(any())).thenReturn(AICheckResponse);
    CliCheckResult result = AIChecker.run(assetExtractionDiffs);
    assertFalse(result.isSuccessful());
    assertTrue(result.getNotificationText().contains("Retries exhausted for OpenAI check"));
  }
}
