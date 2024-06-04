package com.box.l10n.mojito.rest.openai;

import com.box.l10n.mojito.entity.AIPrompt;
import com.box.l10n.mojito.service.openai.OpenAIService;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(value = "l10n.openai.enabled", havingValue = "true")
public class OpenAIWS {

  static Logger logger = LoggerFactory.getLogger(OpenAIWS.class);

  @Autowired OpenAIService openAIService;

  @RequestMapping(value = "/api/openai/checks", method = RequestMethod.POST)
  @Timed("OpenAIWS.executeAIChecks")
  public OpenAICheckResponse executeAIChecks(@RequestBody OpenAICheckRequest openAICheckRequest) {
    logger.debug("Received request to execute AI checks");
    OpenAICheckResponse response;

    try {
      response = openAIService.executeAIChecks(openAICheckRequest);
    } catch (OpenAIException e) {
      response = buildErrorInCheckResponse(e);
    }

    return response;
  }

  @RequestMapping(value = "/api/openai/prompts", method = RequestMethod.POST)
  @Timed("OpenAIWS.createPrompt")
  public long createPrompt(@RequestBody OpenAIPromptCreateRequest openAIPromptCreateRequest) {
    logger.debug("Received request to create prompt");
    return openAIService.createPrompt(openAIPromptCreateRequest);
  }

  @RequestMapping(value = "/api/openai/prompts/{prompt_id}", method = RequestMethod.DELETE)
  @Timed("OpenAIWS.deletePrompt")
  public void deletePrompt(@PathVariable("prompt_id") Long promptId) {
    logger.debug("Received request to delete prompt id {}", promptId);
    openAIService.deletePrompt(promptId);
  }

  @RequestMapping(value = "/api/openai/prompts/{prompt_id}", method = RequestMethod.GET)
  @Timed("OpenAIWS.getPrompt")
  public OpenAIPrompt getPrompt(@PathVariable("prompt_id") Long promptId) {
    logger.debug("Received request to get prompt id {}", promptId);
    return buildOpenAIPromptDTO(openAIService.getPrompt(promptId));
  }

  @RequestMapping(value = "/api/openai/prompts", method = RequestMethod.GET)
  @Timed("OpenAIWS.getAllActivePrompts")
  public List<OpenAIPrompt> getAllActivePrompts() {
    logger.debug("Received request to get prompts");
    return openAIService.getAllActivePrompts().stream()
        .map(OpenAIWS::buildOpenAIPromptDTO)
        .collect(Collectors.toList());
  }

  @RequestMapping(value = "/api/openai/prompts/{repository_name}", method = RequestMethod.GET)
  @Timed("OpenAIWS.getAllActivePrompts")
  public List<OpenAIPrompt> getAllActivePromptsForRepository(
      @PathVariable("repository_name") String repositoryName) {
    logger.debug("Received request to get prompts");
    return openAIService.getAllActivePromptsForRepository(repositoryName).stream()
        .map(OpenAIWS::buildOpenAIPromptDTO)
        .collect(Collectors.toList());
  }

  private static OpenAICheckResponse buildErrorInCheckResponse(OpenAIException e) {
    OpenAICheckResponse response;
    logger.error("Error executing AI checks", e);
    response = new OpenAICheckResponse();
    response.setError(true);
    response.setErrorMessage(e.getMessage());
    return response;
  }

  private static OpenAIPrompt buildOpenAIPromptDTO(AIPrompt prompt) {
    OpenAIPrompt openAIPrompt = new OpenAIPrompt();
    openAIPrompt.setSystemPrompt(prompt.getSystemPrompt());
    openAIPrompt.setUserPrompt(prompt.getUserPrompt());
    openAIPrompt.setModelName(prompt.getModelName());
    openAIPrompt.setPromptTemperature(prompt.getPromptTemperature());
    openAIPrompt.setDeleted(prompt.isDeleted());
    return openAIPrompt;
  }
}
