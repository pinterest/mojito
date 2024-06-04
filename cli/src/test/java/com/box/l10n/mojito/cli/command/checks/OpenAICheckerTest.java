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

import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.rest.client.OpenAIServiceClient;
import com.box.l10n.mojito.rest.entity.OpenAICheckResponse;
import com.box.l10n.mojito.rest.entity.OpenAICheckResult;
import com.box.l10n.mojito.rest.openai.OpenAIException;
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

public class OpenAICheckerTest {

  @Mock OpenAIServiceClient openAIServiceClient;

  List<AssetExtractionDiff> assetExtractionDiffs;

  OpenAIChecker openAIChecker;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    openAIChecker = new OpenAIChecker();
    openAIChecker.openAIServiceClient = openAIServiceClient;
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

    openAIChecker.setCliCheckerOptions(
        new CliCheckerOptions(
            Sets.newHashSet(),
            Sets.newHashSet(),
            ImmutableMap.<String, String>builder()
                .put(REPOSITORY_NAME.getKey(), "test-repo")
                .put(OPEN_AI_RETRY_ERROR_MSG.getKey(), "Retries exhausted for OpenAI check")
                .build()));
  }

  @Test
  public void testCheckSuccess() {
    OpenAICheckResponse openAICheckResponse = new OpenAICheckResponse();
    openAICheckResponse.setError(false);
    openAICheckResponse.setErrorMessage(null);
    Map<String, List<OpenAICheckResult>> checkResults = new HashMap<>();
    List<OpenAICheckResult> results = new ArrayList<>();

    OpenAICheckResult openAICheckResult = new OpenAICheckResult();
    openAICheckResult.setSuccess(true);
    openAICheckResult.setSuggestedFix("");

    results.add(openAICheckResult);
    checkResults.put("A source string with no errors.", results);
    openAICheckResponse.setResults(checkResults);

    when(openAIServiceClient.executeAIChecks(any())).thenReturn(openAICheckResponse);
    CliCheckResult result = openAIChecker.run(assetExtractionDiffs);

    verify(openAIServiceClient, times(1)).executeAIChecks(any());
    assertTrue(result.isSuccessful());
    assertTrue(result.getNotificationText().isEmpty());
  }

  @Test
  public void testCheckFailure() {
    OpenAICheckResponse openAICheckResponse = new OpenAICheckResponse();
    openAICheckResponse.setError(false);
    openAICheckResponse.setErrorMessage(null);
    Map<String, List<OpenAICheckResult>> checkResults = new HashMap<>();
    List<OpenAICheckResult> results = new ArrayList<>();

    OpenAICheckResult openAICheckResult = new OpenAICheckResult();
    openAICheckResult.setSuccess(false);
    openAICheckResult.setSuggestedFix("Spelling mistake found in the source string.");

    results.add(openAICheckResult);
    checkResults.put("A source string with no errors.", results);
    openAICheckResponse.setResults(checkResults);

    when(openAIServiceClient.executeAIChecks(any())).thenReturn(openAICheckResponse);
    CliCheckResult result = openAIChecker.run(assetExtractionDiffs);

    verify(openAIServiceClient, times(1)).executeAIChecks(any());
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
    openAIChecker.retryConfiguration =
        Retry.backoff(10, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(1));
    when(openAIServiceClient.executeAIChecks(any())).thenThrow(new OpenAIException("Test error"));
    CliCheckResult result = openAIChecker.run(assetExtractionDiffs);
    verify(openAIServiceClient, times(11)).executeAIChecks(any());
    assertFalse(result.isSuccessful());
    assertTrue(result.getNotificationText().contains("Retries exhausted for OpenAI check"));
  }

  @Test
  public void exceptionIfNoRepoProvided() {
    openAIChecker.setCliCheckerOptions(
        new CliCheckerOptions(
            Sets.newHashSet(),
            Sets.newHashSet(),
            ImmutableMap.<String, String>builder()
                .put(OPEN_AI_RETRY_ERROR_MSG.getKey(), "Retries exhausted for OpenAI check")
                .build()));
    Exception ex =
        assertThrows(CommandException.class, () -> openAIChecker.run(assetExtractionDiffs));
    assertEquals(
        "Repository name must be provided in checker options when using OpenAI checks.",
        ex.getMessage());
  }

  @Test
  public void testErrorResultReturnedFromCli() {
    openAIChecker.retryConfiguration =
        Retry.backoff(1, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(1));
    OpenAICheckResponse openAICheckResponse = new OpenAICheckResponse();
    openAICheckResponse.setError(true);
    openAICheckResponse.setErrorMessage("Some error message");
    when(openAIServiceClient.executeAIChecks(any())).thenReturn(openAICheckResponse);
    CliCheckResult result = openAIChecker.run(assetExtractionDiffs);
    assertFalse(result.isSuccessful());
    assertTrue(result.getNotificationText().contains("Retries exhausted for OpenAI check"));
  }
}
