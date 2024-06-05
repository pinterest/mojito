package com.box.l10n.mojito.service.openai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.box.l10n.mojito.entity.AIPrompt;
import com.box.l10n.mojito.entity.PromptType;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.rest.ai.AICheckRequest;
import com.box.l10n.mojito.rest.ai.AICheckResponse;
import com.box.l10n.mojito.service.ai.openai.AIStringCheckRepository;
import com.box.l10n.mojito.service.ai.openai.OpenAILLMService;
import com.box.l10n.mojito.service.ai.openai.OpenAIPromptService;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

class OpenAILLMServiceTest {

  @Mock OpenAIClient openAIClient;

  @Mock RepositoryRepository repositoryRepository;

  @Mock AIStringCheckRepository aiStringCheckRepository;

  @Mock OpenAIPromptService promptService;

  @Spy ObjectMapper objectMapper;

  @InjectMocks OpenAILLMService openAILLMService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void executeAIChecksSuccessTest() {
    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setUserPrompt("Check strings for spelling");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    List<AIPrompt> prompts = List.of(prompt);
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A test string");
    assetExtractorTextUnit.setName("A test string --- A test context");
    assetExtractorTextUnit.setComments("A test comment");
    List<AssetExtractorTextUnit> textUnits = List.of(assetExtractorTextUnit);
    AICheckRequest AICheckRequest = new AICheckRequest();
    AICheckRequest.setRepositoryName("testRepo");
    AICheckRequest.setTextUnits(textUnits);
    Repository repository = new Repository();
    repository.setId(1L);
    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(promptService.getPromptsByRepositoryAndPromptType(
            repository, PromptType.SOURCE_STRING_CHECKER))
        .thenReturn(prompts);
    List<OpenAIClient.ChatCompletionsResponse.Choice> choices =
        List.of(
            new OpenAIClient.ChatCompletionsResponse.Choice(
                0,
                new OpenAIClient.ChatCompletionsResponse.Choice.Message(
                    "test", "{\"success\": true, \"suggestedFix\": \"\"}"),
                null));
    OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
        new OpenAIClient.ChatCompletionsResponse(null, null, null, null, choices, null, null);
    CompletableFuture<OpenAIClient.ChatCompletionsResponse> futureResponse =
        CompletableFuture.completedFuture(chatCompletionsResponse);
    when(openAIClient.getChatCompletions(any(OpenAIClient.ChatCompletionsRequest.class)))
        .thenReturn(futureResponse);

    AICheckResponse response = openAILLMService.executeAIChecks(AICheckRequest);
    assertNotNull(response);
    assertEquals(1, response.getResults().size());
    assertTrue(response.getResults().containsKey("A test string"));
    assertTrue(response.getResults().get("A test string").get(0).isSuccess());
  }

  @Test
  void executeAIChecksFailureTest() {
    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setUserPrompt("Check strings for spelling");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    List<AIPrompt> prompts = List.of(prompt);
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A tst string");
    assetExtractorTextUnit.setName("A tst string --- A test context");
    assetExtractorTextUnit.setComments("A test comment");
    List<AssetExtractorTextUnit> textUnits = List.of(assetExtractorTextUnit);
    AICheckRequest AICheckRequest = new AICheckRequest();
    AICheckRequest.setRepositoryName("testRepo");
    AICheckRequest.setTextUnits(textUnits);
    Repository repository = new Repository();
    repository.setId(1L);
    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(promptService.getPromptsByRepositoryAndPromptType(
            repository, PromptType.SOURCE_STRING_CHECKER))
        .thenReturn(prompts);
    List<OpenAIClient.ChatCompletionsResponse.Choice> choices =
        List.of(
            new OpenAIClient.ChatCompletionsResponse.Choice(
                0,
                new OpenAIClient.ChatCompletionsResponse.Choice.Message(
                    "test",
                    "{\"success\": false, \"suggestedFix\": \"The word test is spelt wrong\"}"),
                null));
    OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
        new OpenAIClient.ChatCompletionsResponse(null, null, null, null, choices, null, null);
    CompletableFuture<OpenAIClient.ChatCompletionsResponse> futureResponse =
        CompletableFuture.completedFuture(chatCompletionsResponse);
    when(openAIClient.getChatCompletions(any(OpenAIClient.ChatCompletionsRequest.class)))
        .thenReturn(futureResponse);

    AICheckResponse response = openAILLMService.executeAIChecks(AICheckRequest);
    assertNotNull(response);
    assertEquals(1, response.getResults().size());
    assertTrue(response.getResults().containsKey("A tst string"));
    assertFalse(response.getResults().get("A tst string").get(0).isSuccess());
    assertEquals(
        "The word test is spelt wrong",
        response.getResults().get("A tst string").get(0).getSuggestedFix());
  }

  @Test
  void testJsonSerializationError() {
    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setUserPrompt("Check strings for spelling");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    List<AIPrompt> prompts = List.of(prompt);
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A test string");
    assetExtractorTextUnit.setName("A test string --- A test context");
    assetExtractorTextUnit.setComments("A test comment");
    List<AssetExtractorTextUnit> textUnits = List.of(assetExtractorTextUnit);
    AICheckRequest AICheckRequest = new AICheckRequest();
    AICheckRequest.setRepositoryName("testRepo");
    AICheckRequest.setTextUnits(textUnits);
    Repository repository = new Repository();
    repository.setId(1L);
    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(promptService.getPromptsByRepositoryAndPromptType(
            repository, PromptType.SOURCE_STRING_CHECKER))
        .thenReturn(prompts);
    List<OpenAIClient.ChatCompletionsResponse.Choice> choices =
        List.of(
            new OpenAIClient.ChatCompletionsResponse.Choice(
                0,
                new OpenAIClient.ChatCompletionsResponse.Choice.Message(
                    "test", "\"success\": true, \"suggestedFix\": \"\""),
                null));
    OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
        new OpenAIClient.ChatCompletionsResponse(null, null, null, null, choices, null, null);
    CompletableFuture<OpenAIClient.ChatCompletionsResponse> futureResponse =
        CompletableFuture.completedFuture(chatCompletionsResponse);
    when(openAIClient.getChatCompletions(any(OpenAIClient.ChatCompletionsRequest.class)))
        .thenReturn(futureResponse);

    AICheckResponse result = openAILLMService.executeAIChecks(AICheckRequest);

    assertTrue(result.getResults().get("A test string").getFirst().isSuccess());
    assertEquals(
        result.getResults().get("A test string").getFirst().getSuggestedFix(),
        "Check skipped as error parsing response from OpenAI.");
  }

  @Test
  void testNoPromptsSuccessResult() {
    List<AIPrompt> prompts = new ArrayList<>();
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A test string");
    assetExtractorTextUnit.setName("A test string --- A test context");
    assetExtractorTextUnit.setComments("A test comment");
    List<AssetExtractorTextUnit> textUnits = List.of(assetExtractorTextUnit);
    AICheckRequest AICheckRequest = new AICheckRequest();
    AICheckRequest.setRepositoryName("testRepo");
    AICheckRequest.setTextUnits(textUnits);
    Repository repository = new Repository();
    repository.setId(1L);
    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(promptService.getPromptsByRepositoryAndPromptType(
            repository, PromptType.SOURCE_STRING_CHECKER))
        .thenReturn(prompts);
    List<OpenAIClient.ChatCompletionsResponse.Choice> choices =
        List.of(
            new OpenAIClient.ChatCompletionsResponse.Choice(
                0,
                new OpenAIClient.ChatCompletionsResponse.Choice.Message(
                    "test", "{\"success\": true, \"suggestedFix\": \"\"}"),
                null));
    OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
        new OpenAIClient.ChatCompletionsResponse(null, null, null, null, choices, null, null);
    CompletableFuture<OpenAIClient.ChatCompletionsResponse> futureResponse =
        CompletableFuture.completedFuture(chatCompletionsResponse);
    when(openAIClient.getChatCompletions(any(OpenAIClient.ChatCompletionsRequest.class)))
        .thenReturn(futureResponse);

    AICheckResponse response = openAILLMService.executeAIChecks(AICheckRequest);
    assertNotNull(response);
    assertEquals(1, response.getResults().size());
    assertTrue(response.getResults().containsKey("A test string"));
    assertTrue(response.getResults().get("A test string").getFirst().isSuccess());
    assertEquals(
        "No prompts found for repository id: 1, skipping check.",
        response.getResults().get("A test string").getFirst().getSuggestedFix());
  }
}
