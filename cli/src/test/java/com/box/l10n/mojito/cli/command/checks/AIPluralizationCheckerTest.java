package com.box.l10n.mojito.cli.command.checks;

import static com.box.l10n.mojito.cli.command.checks.AIPluralizationChecker.PLURALIZATION_SUGGESTED_FIX_PLACEHOLDER;
import static com.box.l10n.mojito.cli.command.checks.CliCheckerParameters.PLURALIZATION_SUGGESTED_FIX;
import static com.box.l10n.mojito.cli.command.checks.CliCheckerParameters.REPOSITORY_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.apiclient.AIServiceClient;
import com.box.l10n.mojito.apiclient.model.AICheckRequest;
import com.box.l10n.mojito.apiclient.model.AICheckResponse;
import com.box.l10n.mojito.apiclient.model.AICheckResult;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AIPluralizationCheckerTest {
  @Mock private AIServiceClient aiServiceClient;

  AIChecker aiChecker;

  AIPluralizationChecker aiPluralizationChecker;

  @Captor ArgumentCaptor<AICheckRequest> aiCheckRequestArgumentCaptor;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    AICheckResponse aiCheckResponse = new AICheckResponse();
    aiCheckResponse.setError(false);
    AICheckResult aiCheckResult = new AICheckResult();
    aiCheckResult.setSuccess(false);
    aiCheckResult.setSuggestedFix(
        String.format("Current suggested fix %s", PLURALIZATION_SUGGESTED_FIX_PLACEHOLDER));
    aiCheckResponse.setResults(Map.of("", List.of(aiCheckResult)));
    when(this.aiServiceClient.executeAIChecks(any(AICheckRequest.class)))
        .thenReturn(aiCheckResponse);
    this.aiPluralizationChecker = new AIPluralizationChecker();
    this.aiChecker = this.aiPluralizationChecker.getAiChecker();
    this.aiChecker.setAiServiceClient(this.aiServiceClient);
    this.aiPluralizationChecker.setCliCheckerOptions(
        new CliCheckerOptions(
            Sets.newHashSet(),
            Sets.newHashSet(),
            ImmutableMap.<String, String>builder()
                .put(REPOSITORY_NAME.getKey(), "test-repo")
                .build()));
  }

  private static AssetExtractorTextUnit buildTextUnit(
      String id, String source, String comment, String pluralForm) {
    AssetExtractorTextUnit tu1 = new AssetExtractorTextUnit();
    tu1.setName(id);
    tu1.setSource(source);
    tu1.setComments(comment);
    tu1.setPluralForm(pluralForm);
    return tu1;
  }

  @Test
  public void testRun_ExecutesAIChecksForNonPluralizedTextUnit() {
    List<AssetExtractorTextUnit> addedTUs =
        List.of(
            buildTextUnit("ID1 --- Context1", "Source string 1", "Comment1", "one"),
            buildTextUnit("ID2 --- Context2", "Source string 2", "Comment2", "other"),
            buildTextUnit("ID3 --- Context3", "Source string 3", "Comment3", null));

    AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
    assetExtractionDiff.setAddedTextunits(addedTUs);
    List<AssetExtractionDiff> assetExtractionDiffs = List.of(assetExtractionDiff);

    CliCheckResult cliCheckResult = this.aiPluralizationChecker.run(assetExtractionDiffs);

    verify(this.aiServiceClient).executeAIChecks(this.aiCheckRequestArgumentCaptor.capture());
    AICheckRequest aiCheckRequest = this.aiCheckRequestArgumentCaptor.getValue();
    List<com.box.l10n.mojito.apiclient.model.AssetExtractorTextUnit> textUnits =
        aiCheckRequest.getTextUnits();
    assertEquals(1, textUnits.size());
    assertEquals("ID3 --- Context3", textUnits.getFirst().getName());
    assertTrue(cliCheckResult.getNotificationText().contains("Current suggested fix"));
    assertFalse(cliCheckResult.getNotificationText().contains("New suggested fix"));
  }

  @Test
  public void testRun_UpdatesNotificationText() {
    this.aiPluralizationChecker.setCliCheckerOptions(
        new CliCheckerOptions(
            Sets.newHashSet(),
            Sets.newHashSet(),
            ImmutableMap.<String, String>builder()
                .put(REPOSITORY_NAME.getKey(), "test-repo")
                .put(PLURALIZATION_SUGGESTED_FIX.getKey(), "New suggested fix")
                .build()));
    List<AssetExtractorTextUnit> addedTUs =
        List.of(buildTextUnit("ID1 --- Context1", "Source string 1", "Comment1", null));

    AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
    assetExtractionDiff.setAddedTextunits(addedTUs);
    List<AssetExtractionDiff> assetExtractionDiffs = List.of(assetExtractionDiff);

    CliCheckResult cliCheckResult = this.aiPluralizationChecker.run(assetExtractionDiffs);

    assertTrue(cliCheckResult.getNotificationText().contains("Current suggested fix"));
    assertTrue(cliCheckResult.getNotificationText().contains("New suggested fix"));
  }
}
