package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.entity.OpenAICheckRequest;
import com.box.l10n.mojito.rest.entity.OpenAICheckResponse;
import com.box.l10n.mojito.rest.entity.OpenAIPrompt;
import com.box.l10n.mojito.rest.entity.OpenAIPromptContextMessageCreateRequest;
import com.box.l10n.mojito.rest.entity.OpenAIPromptCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AIServiceClient extends BaseClient {

  static Logger logger = LoggerFactory.getLogger(AIServiceClient.class);

  @Override
  public String getEntityName() {
    return "ai";
  }

  public OpenAICheckResponse executeAIChecks(OpenAICheckRequest openAICheckRequest) {
    logger.debug("Received request to execute AI checks");
    return authenticatedRestTemplate.postForObject(
        getBasePathForEntity() + "/checks", openAICheckRequest, OpenAICheckResponse.class);
  }

  public Long createPrompt(OpenAIPromptCreateRequest openAIPromptCreateRequest) {
    logger.debug("Received request to create prompt");
    return authenticatedRestTemplate.postForObject(
        getBasePathForEntity() + "/prompts", openAIPromptCreateRequest, Long.class);
  }

  public void deletePrompt(Long promptId) {
    logger.debug("Received request to delete prompt id {}", promptId);
    authenticatedRestTemplate.delete(getBasePathForEntity() + "/prompts/" + promptId);
  }

  public OpenAIPrompt getPrompt(Long promptId) {
    logger.debug("Received request to get prompt id {}", promptId);
    return authenticatedRestTemplate.getForObject(
        getBasePathForEntity() + "/prompts/" + promptId, OpenAIPrompt.class);
  }

  public Long createPromptContextMessage(
      OpenAIPromptContextMessageCreateRequest openAIPromptContextMessageCreateRequest) {
    logger.debug("Received request to create prompt context message");
    return authenticatedRestTemplate.postForObject(
        getBasePathForEntity() + "/prompts/contextMessage",
        openAIPromptContextMessageCreateRequest,
        Long.class);
  }

  public void deletePromptContextMessage(Long contextMessageId) {
    logger.debug("Received request to delete prompt message id {}", contextMessageId);
    authenticatedRestTemplate.delete(
        getBasePathForEntity() + "/prompts/contextMessage/" + contextMessageId);
  }
}
