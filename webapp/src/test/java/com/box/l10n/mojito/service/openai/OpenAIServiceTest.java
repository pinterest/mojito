package com.box.l10n.mojito.service.openai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.box.l10n.mojito.entity.AIPrompt;
import com.box.l10n.mojito.entity.AIPromptType;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryAIPrompt;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.rest.openai.OpenAICheckRequest;
import com.box.l10n.mojito.rest.openai.OpenAICheckResponse;
import com.box.l10n.mojito.rest.openai.OpenAIException;
import com.box.l10n.mojito.rest.openai.OpenAIPromptCreateRequest;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

class OpenAIServiceTest {

  @Mock OpenAIClient openAIClient;

  @Mock RepositoryRepository repositoryRepository;

  @Mock AIStringCheckRepository aiStringCheckRepository;

  @Mock AIPromptRepository aiPromptRepository;

  @Mock AIPromptTypeRepository aiPromptTypeRepository;

  @Mock RepositoryAIPromptRepository repositoryAIPromptRepository;

  @Spy ObjectMapper objectMapper;

  @InjectMocks OpenAIService openAIService;

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
    OpenAICheckRequest openAICheckRequest = new OpenAICheckRequest();
    openAICheckRequest.setRepositoryName("testRepo");
    openAICheckRequest.setTextUnits(textUnits);
    Repository repository = new Repository();
    repository.setId(1L);
    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(aiPromptRepository.findByRepositoryIdAndPromptTypeName(1L, "SOURCE_STRING_CHECKER"))
        .thenReturn(prompts);
    when(aiPromptTypeRepository.findByName("SOURCE_STRING_CHECKER")).thenReturn(null);
    when(aiPromptTypeRepository.save(any())).thenReturn(null);
    when(aiPromptRepository.save(any())).thenReturn(null);
    when(repositoryAIPromptRepository.save(any())).thenReturn(null);
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

    OpenAICheckResponse response = openAIService.executeAIChecks(openAICheckRequest);
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
    OpenAICheckRequest openAICheckRequest = new OpenAICheckRequest();
    openAICheckRequest.setRepositoryName("testRepo");
    openAICheckRequest.setTextUnits(textUnits);
    Repository repository = new Repository();
    repository.setId(1L);
    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(aiPromptRepository.findByRepositoryIdAndPromptTypeName(1L, "SOURCE_STRING_CHECKER"))
        .thenReturn(prompts);
    when(aiPromptTypeRepository.findByName("SOURCE_STRING_CHECKER")).thenReturn(null);
    when(aiPromptTypeRepository.save(any())).thenReturn(null);
    when(aiPromptRepository.save(any())).thenReturn(null);
    when(repositoryAIPromptRepository.save(any())).thenReturn(null);
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

    OpenAICheckResponse response = openAIService.executeAIChecks(openAICheckRequest);
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
    assetExtractorTextUnit.setSource("A tst string");
    assetExtractorTextUnit.setName("A tst string --- A test context");
    assetExtractorTextUnit.setComments("A test comment");
    List<AssetExtractorTextUnit> textUnits = List.of(assetExtractorTextUnit);
    OpenAICheckRequest openAICheckRequest = new OpenAICheckRequest();
    openAICheckRequest.setRepositoryName("testRepo");
    openAICheckRequest.setTextUnits(textUnits);
    Repository repository = new Repository();
    repository.setId(1L);
    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(aiPromptRepository.findByRepositoryIdAndPromptTypeName(1L, "SOURCE_STRING_CHECKER"))
        .thenReturn(prompts);
    when(aiPromptTypeRepository.findByName("SOURCE_STRING_CHECKER")).thenReturn(null);
    when(aiPromptTypeRepository.save(any())).thenReturn(null);
    when(aiPromptRepository.save(any())).thenReturn(null);
    when(repositoryAIPromptRepository.save(any())).thenReturn(null);
    List<OpenAIClient.ChatCompletionsResponse.Choice> choices =
        List.of(
            new OpenAIClient.ChatCompletionsResponse.Choice(
                0,
                new OpenAIClient.ChatCompletionsResponse.Choice.Message(
                    "test",
                    "\"success\": false, \"suggestedFix\": \"The word test is spelt wrong\""),
                null));
    OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
        new OpenAIClient.ChatCompletionsResponse(null, null, null, null, choices, null, null);
    CompletableFuture<OpenAIClient.ChatCompletionsResponse> futureResponse =
        CompletableFuture.completedFuture(chatCompletionsResponse);
    when(openAIClient.getChatCompletions(any(OpenAIClient.ChatCompletionsRequest.class)))
        .thenReturn(futureResponse);

    OpenAIException exception =
        assertThrows(
            OpenAIException.class, () -> openAIService.executeAIChecks(openAICheckRequest));
    assertEquals("Error processing JSON response from OpenAI", exception.getMessage());
  }

  @Test
  void testNoPromptsSuccessResult() {
    List<AIPrompt> prompts = new ArrayList<>();
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setSource("A test string");
    assetExtractorTextUnit.setName("A test string --- A test context");
    assetExtractorTextUnit.setComments("A test comment");
    List<AssetExtractorTextUnit> textUnits = List.of(assetExtractorTextUnit);
    OpenAICheckRequest openAICheckRequest = new OpenAICheckRequest();
    openAICheckRequest.setRepositoryName("testRepo");
    openAICheckRequest.setTextUnits(textUnits);
    Repository repository = new Repository();
    repository.setId(1L);
    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(aiPromptRepository.findByRepositoryIdAndPromptTypeName(1L, "SOURCE_STRING_CHECKER"))
        .thenReturn(prompts);
    when(aiPromptTypeRepository.findByName("SOURCE_STRING_CHECKER")).thenReturn(null);
    when(aiPromptTypeRepository.save(any())).thenReturn(null);
    when(aiPromptRepository.save(any())).thenReturn(null);
    when(repositoryAIPromptRepository.save(any())).thenReturn(null);
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

    OpenAICheckResponse response = openAIService.executeAIChecks(openAICheckRequest);
    assertNotNull(response);
    assertEquals(1, response.getResults().size());
    assertTrue(response.getResults().containsKey("A test string"));
    assertTrue(response.getResults().get("A test string").getFirst().isSuccess());
    assertTrue(
        response
            .getResults()
            .get("A test string")
            .getFirst()
            .getSuggestedFix()
            .equals("No prompts found for repository id: 1, skipping check."));
  }

  @Test
  void testPromptCreation() {
    Repository repository = new Repository();
    repository.setId(1L);
    RepositoryAIPrompt repositoryAIPrompt = new RepositoryAIPrompt();
    repositoryAIPrompt.setId(1L);
    AIPromptType promptType = new AIPromptType();
    promptType.setId(1L);
    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setUserPrompt("Check strings for spelling");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    OpenAIPromptCreateRequest openAIPromptCreateRequest = new OpenAIPromptCreateRequest();
    openAIPromptCreateRequest.setRepositoryName("testRepo");
    openAIPromptCreateRequest.setPromptType("SOURCE_STRING_CHECKER");
    openAIPromptCreateRequest.setUserPrompt("Check strings for spelling");
    openAIPromptCreateRequest.setModelName("gtp-3.5-turbo");
    openAIPromptCreateRequest.setPromptTemperature(0.0F);
    when(aiPromptRepository.save(any())).thenReturn(prompt);
    when(repositoryRepository.findByName("testRepo")).thenReturn(repository);
    when(aiPromptTypeRepository.findByName("SOURCE_STRING_CHECKER")).thenReturn(promptType);
    when(repositoryAIPromptRepository.save(any())).thenReturn(repositoryAIPrompt);

    openAIService.createPrompt(openAIPromptCreateRequest);

    verify(aiPromptTypeRepository, times(1)).findByName("SOURCE_STRING_CHECKER");
    verify(aiPromptRepository, times(1)).save(any());
    verify(repositoryAIPromptRepository, times(1)).save(any());
  }

  @Test
  void testPromptCreationNoPromptType() {

    AIPrompt prompt = new AIPrompt();
    prompt.setId(1L);
    prompt.setUserPrompt("Check strings for spelling");
    prompt.setModelName("gtp-3.5-turbo");
    prompt.setPromptTemperature(0.0F);
    OpenAIPromptCreateRequest openAIPromptCreateRequest = new OpenAIPromptCreateRequest();
    openAIPromptCreateRequest.setRepositoryName("testRepo");
    openAIPromptCreateRequest.setPromptType("SOURCE_STRING_CHECKER");
    openAIPromptCreateRequest.setUserPrompt("Check strings for spelling");
    openAIPromptCreateRequest.setModelName("gtp-3.5-turbo");
    openAIPromptCreateRequest.setPromptTemperature(0.0F);
    when(aiPromptRepository.save(any())).thenReturn(prompt);
    when(repositoryRepository.findByName("testRepo")).thenReturn(new Repository());
    when(aiPromptTypeRepository.findByName("SOURCE_STRING_CHECKER")).thenReturn(null);
    when(repositoryAIPromptRepository.save(any())).thenReturn(1L);

    OpenAIException exception =
        assertThrows(
            OpenAIException.class, () -> openAIService.createPrompt(openAIPromptCreateRequest));
    assertEquals("Prompt type not found: SOURCE_STRING_CHECKER", exception.getMessage());

    verify(aiPromptTypeRepository, times(1)).findByName("SOURCE_STRING_CHECKER");
    verify(aiPromptTypeRepository, times(0)).save(any());
    verify(repositoryAIPromptRepository, times(0)).save(any());
  }

  @Test
  void testPromptDeletion() {
    AIPrompt aiPrompt = new AIPrompt();
    aiPrompt.setId(1L);
    aiPrompt.setDeleted(false);
    when(aiPromptRepository.findById(1L)).thenReturn(Optional.of(aiPrompt));
    openAIService.deletePrompt(1L);
    verify(aiPromptRepository, times(1)).save(aiPrompt);
    assertTrue(aiPrompt.isDeleted());
  }

  @Test
  void testPromptDeletionError() {
    when(aiPromptRepository.findById(1L)).thenReturn(Optional.empty());
    OpenAIException exception =
        assertThrows(OpenAIException.class, () -> openAIService.deletePrompt(1L));
    assertEquals("Prompt not found: 1", exception.getMessage());
  }

  @Test
  void testGetPrompt() {
    AIPrompt aiPrompt = new AIPrompt();
    aiPrompt.setId(1L);
    aiPrompt.setUserPrompt("Check strings for spelling");
    aiPrompt.setModelName("gtp-3.5-turbo");
    aiPrompt.setPromptTemperature(0.0F);
    when(aiPromptRepository.findById(1L)).thenReturn(Optional.of(aiPrompt));
    AIPrompt openAIPrompt = openAIService.getPrompt(1L);
    assertNotNull(openAIPrompt);
    assertEquals("Check strings for spelling", openAIPrompt.getUserPrompt());
    assertEquals("gtp-3.5-turbo", openAIPrompt.getModelName());
    assertEquals(0.0F, openAIPrompt.getPromptTemperature());
  }
}
