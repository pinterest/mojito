package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.entity.OpenAICheckRequest;
import com.box.l10n.mojito.rest.entity.OpenAICheckResponse;
import com.box.l10n.mojito.rest.entity.OpenAIPrompt;
import com.box.l10n.mojito.rest.entity.OpenAIPromptCreateRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OpenAIServiceClient extends BaseClient {

  static Logger logger = LoggerFactory.getLogger(OpenAIServiceClient.class);

  @Override
  public String getEntityName() {
    return "openai";
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

  public List<OpenAIPrompt> getAllActivePrompts() {
    logger.debug("Received request to get all active prompts");
    return authenticatedRestTemplate.getForObject(getBasePathForEntity() + "/prompts", List.class);
  }
}
