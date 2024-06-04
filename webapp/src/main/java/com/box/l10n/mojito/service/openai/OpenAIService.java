package com.box.l10n.mojito.service.openai;

import static com.box.l10n.mojito.entity.PromptType.SOURCE_STRING_CHECKER;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.SystemMessage.systemMessageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.chatCompletionsRequest;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.AIPrompt;
import com.box.l10n.mojito.entity.AIPromptType;
import com.box.l10n.mojito.entity.AIStringCheck;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryAIPrompt;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.rest.openai.OpenAICheckRequest;
import com.box.l10n.mojito.rest.openai.OpenAICheckResponse;
import com.box.l10n.mojito.rest.openai.OpenAICheckResult;
import com.box.l10n.mojito.rest.openai.OpenAIException;
import com.box.l10n.mojito.rest.openai.OpenAIPromptCreateRequest;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import io.micrometer.core.annotation.Timed;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "l10n.openai.enabled", havingValue = "true")
public class OpenAIService {

  private static final String SOURCE_STRING_PLACEHOLDER = "[mojito_source_string]";
  private static final String COMMENT_STRING_PLACEHOLDER = "[mojito_comment_string]";
  private static final String CONTEXT_STRING_PLACEHOLDER = "[mojito_context_string]";

  static Logger logger = LoggerFactory.getLogger(OpenAIService.class);

  @Autowired OpenAIClient openAIClient;

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired AIStringCheckRepository aiStringCheckRepository;

  @Autowired AIPromptRepository aiPromptRepository;

  @Autowired AIPromptTypeRepository aiPromptTypeRepository;

  @Autowired RepositoryAIPromptRepository repositoryAIPromptRepository;

  @Autowired ObjectMapper objectMapper;

  /**
   * Executes AI checks on the provided text units.
   *
   * <p>Prompt must include a placeholder for the string to be checked.
   *
   * <p>The placeholder for the string is [mojito_source_string] and must be included in the prompt.
   *
   * <p>Example: "Is the following string correct? [mojito_source_string]"
   *
   * <p>A placeholder [mojito_comment_string] can also be used to include the comment in the prompt,
   * but it is not mandatory.
   *
   * <p>Your prompt must only return responses that align with the {@link OpenAICheckResult} class,
   * otherwise JSON processing will fail and return an error to the caller.
   *
   * <p>e.g. '{"success": true, "suggestedFix": ""}' or '{"success": false, "suggestedFix": "It
   * looks like the word 'tpyo' is spelt incorrectly. It should be 'typo'."}'
   */
  @Timed("OpenAIService.executeAIChecks")
  public OpenAICheckResponse executeAIChecks(OpenAICheckRequest openAICheckRequest) {

    logger.debug("Executing OpenAI string checks.");
    Repository repository = repositoryRepository.findByName(openAICheckRequest.getRepositoryName());

    if (repository == null) {
      logger.error("Repository not found: {}", openAICheckRequest.getRepositoryName());
      throw new OpenAIException("Repository not found: " + openAICheckRequest.getRepositoryName());
    }

    List<AIPrompt> prompts =
        aiPromptRepository.findByRepositoryIdAndPromptTypeName(
            repository.getId(), SOURCE_STRING_CHECKER.name());

    Map<String, List<OpenAICheckResult>> results = new HashMap<>();
    openAICheckRequest.getTextUnits().stream()
        .forEach(
            textUnit -> {
              List<OpenAICheckResult> openAICheckResults =
                  checkString(textUnit, prompts, repository.getId());
              results.put(textUnit.getSource(), openAICheckResults);
            });

    OpenAICheckResponse openAICheckResponse = new OpenAICheckResponse();
    openAICheckResponse.setResults(results);

    return openAICheckResponse;
  }

  @Timed("OpenAIService.createPrompt")
  @Transactional
  public Long createPrompt(OpenAIPromptCreateRequest openAIPromptCreateRequest) {

    Repository repository =
        repositoryRepository.findByName(openAIPromptCreateRequest.getRepositoryName());

    if (repository == null) {
      logger.error("Repository not found: {}", openAIPromptCreateRequest.getRepositoryName());
      throw new OpenAIException(
          "Repository not found: " + openAIPromptCreateRequest.getRepositoryName());
    }

    AIPromptType aiPromptType =
        aiPromptTypeRepository.findByName(openAIPromptCreateRequest.getPromptType());
    if (aiPromptType == null) {
      logger.error("Prompt type not found: {}", openAIPromptCreateRequest.getPromptType());
      throw new OpenAIException(
          "Prompt type not found: " + openAIPromptCreateRequest.getPromptType());
    }

    AIPrompt aiPrompt = new AIPrompt();
    aiPrompt.setSystemPrompt(openAIPromptCreateRequest.getSystemPrompt());
    aiPrompt.setUserPrompt(openAIPromptCreateRequest.getUserPrompt());
    aiPrompt.setPromptTemperature(openAIPromptCreateRequest.getPromptTemperature());
    aiPrompt.setModelName(openAIPromptCreateRequest.getModelName());
    aiPrompt.setCreatedDate(JSR310Migration.dateTimeNow());
    aiPromptRepository.save(aiPrompt);
    logger.debug("Created prompt with id: {}", aiPrompt.getId());

    RepositoryAIPrompt repositoryAIPrompt = new RepositoryAIPrompt();
    repositoryAIPrompt.setRepositoryId(repository.getId());
    repositoryAIPrompt.setAiPromptId(aiPrompt.getId());
    repositoryAIPrompt.setPromptTypeId(aiPromptType.getId());
    repositoryAIPromptRepository.save(repositoryAIPrompt);
    logger.debug("Created repository prompt with id: {}", repositoryAIPrompt.getId());

    return aiPrompt.getId();
  }

  @Timed("OpenAIService.deletePrompt")
  public void deletePrompt(Long promptId) {
    AIPrompt aiPrompt =
        aiPromptRepository
            .findById(promptId)
            .orElseThrow(() -> new OpenAIException("Prompt not found: " + promptId));
    aiPrompt.setDeleted(true);
    aiPromptRepository.save(aiPrompt);
  }

  @Timed("OpenAIService.getPrompt")
  public AIPrompt getPrompt(Long promptId) {
    return aiPromptRepository
        .findById(promptId)
        .orElseThrow(() -> new OpenAIException("Prompt not found: " + promptId));
  }

  @Timed("OpenAIService.getAllActivePrompts")
  public List<AIPrompt> getAllActivePrompts() {
    return aiPromptRepository.findByDeletedFalse();
  }

  @Timed("OpenAIService.getAllActivePromptsForRepository")
  public List<AIPrompt> getAllActivePromptsForRepository(String repositoryName) {
    Repository repository = repositoryRepository.findByName(repositoryName);
    if (repository == null) {
      logger.error("Repository not found: {}", repositoryName);
      throw new OpenAIException("Repository not found: " + repositoryName);
    }
    return aiPromptRepository.findByRepositoryIdAndDeletedFalse(repository.getId());
  }

  @Timed("OpenAIService.checkString")
  private List<OpenAICheckResult> checkString(
      AssetExtractorTextUnit textUnit, List<AIPrompt> prompts, long repositoryId) {
    try {
      List<OpenAICheckResult> results = new ArrayList<>();
      String sourceString = textUnit.getSource();
      String comment = textUnit.getComments();
      String[] nameSplit = textUnit.getName().split(" --- ");

      if (!prompts.isEmpty()) {
        executePromptChecks(
            textUnit, prompts, repositoryId, sourceString, comment, nameSplit, results);
      } else {
        logger.warn("No prompts found for repository id: {}", repositoryId);
        OpenAICheckResult result = new OpenAICheckResult();
        result.setSuccess(true);
        result.setSuggestedFix(
            "No prompts found for repository id: " + repositoryId + ", skipping check.");
        results.add(result);
      }

      return results;
    } catch (JsonProcessingException e) {
      logger.error("Error processing JSON response from OpenAI", e);
      throw new OpenAIException("Error processing JSON response from OpenAI", e);
    }
  }

  private void executePromptChecks(
      AssetExtractorTextUnit textUnit,
      List<AIPrompt> prompts,
      long repositoryId,
      String sourceString,
      String comment,
      String[] nameSplit,
      List<OpenAICheckResult> results)
      throws JsonProcessingException {
    for (AIPrompt prompt : prompts) {
      if ((!Strings.isNullOrEmpty(prompt.getSystemPrompt())
              && !prompt.getSystemPrompt().contains(SOURCE_STRING_PLACEHOLDER))
          && (!Strings.isNullOrEmpty(prompt.getUserPrompt())
              && !prompt.getUserPrompt().contains(SOURCE_STRING_PLACEHOLDER))) {
        logger.error(
            "Source string placeholder is missing in both system and user prompts for prompt id {}. Skipping check.",
            prompt.getId());
        continue;
      }
      String systemPrompt = getFormattedPrompt(prompt.getSystemPrompt(), sourceString, comment);

      String userPrompt = getFormattedPrompt(prompt.getUserPrompt(), sourceString, comment);

      if (nameSplit.length > 1
          && (systemPrompt.contains(CONTEXT_STRING_PLACEHOLDER)
              || userPrompt.contains(CONTEXT_STRING_PLACEHOLDER))) {
        systemPrompt = systemPrompt.replace(CONTEXT_STRING_PLACEHOLDER, nameSplit[1]);
        userPrompt = userPrompt.replace(CONTEXT_STRING_PLACEHOLDER, nameSplit[1]);
      }

      OpenAIClient.ChatCompletionsRequest chatCompletionsRequest =
          buildChatCompletionsRequest(prompt, systemPrompt, userPrompt);

      OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
          openAIClient.getChatCompletions(chatCompletionsRequest).join();
      persistCheckResult(textUnit, repositoryId, prompt, chatCompletionsResponse);
      OpenAICheckResult result =
          objectMapper.readValue(
              chatCompletionsResponse.choices().getFirst().message().content(),
              OpenAICheckResult.class);
      results.add(result);
    }
  }

  private static String getFormattedPrompt(String prompt, String sourceString, String comment) {
    String systemPrompt = "";
    if (prompt != null) {
      systemPrompt =
          prompt
              .replace(SOURCE_STRING_PLACEHOLDER, sourceString)
              .replace(COMMENT_STRING_PLACEHOLDER, comment);
    }
    return systemPrompt;
  }

  protected void persistCheckResult(
      AssetExtractorTextUnit textUnit,
      long repositoryId,
      AIPrompt prompt,
      OpenAIClient.ChatCompletionsResponse chatCompletionsResponse) {
    AIStringCheck aiStringCheck = new AIStringCheck();
    aiStringCheck.setRepositoryId(repositoryId);
    aiStringCheck.setAiPromptId(prompt.getId());
    aiStringCheck.setContent(textUnit.getSource());
    aiStringCheck.setComment(textUnit.getComments());
    aiStringCheck.setPromptOutput(chatCompletionsResponse.choices().get(0).message().content());
    aiStringCheck.setCreatedDate(JSR310Migration.dateTimeNow());
    aiStringCheckRepository.save(aiStringCheck);
  }

  private static OpenAIClient.ChatCompletionsRequest buildChatCompletionsRequest(
      AIPrompt prompt, String systemPrompt, String userPrompt) {
    OpenAIClient.ChatCompletionsRequest.Builder chatCompletionsRequestBuilder =
        chatCompletionsRequest()
            .temperature(prompt.getPromptTemperature())
            .model(prompt.getModelName());

    if (!Strings.isNullOrEmpty(systemPrompt)) {
      chatCompletionsRequestBuilder =
          chatCompletionsRequestBuilder.messages(
              List.of(systemMessageBuilder().content(systemPrompt).build()));
    }

    if (!Strings.isNullOrEmpty(userPrompt)) {
      chatCompletionsRequestBuilder =
          chatCompletionsRequestBuilder.messages(
              List.of(systemMessageBuilder().content(userPrompt).build()));
    }

    OpenAIClient.ChatCompletionsRequest chatCompletionsRequest =
        chatCompletionsRequestBuilder.build();
    return chatCompletionsRequest;
  }
}
