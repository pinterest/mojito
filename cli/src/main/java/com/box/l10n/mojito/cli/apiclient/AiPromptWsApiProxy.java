package com.box.l10n.mojito.cli.apiclient;

import com.box.l10n.mojito.cli.model.AIPromptContextMessageCreateRequest;
import com.box.l10n.mojito.cli.model.AIPromptCreateRequest;
import com.box.l10n.mojito.cli.model.AITranslationLocalePromptOverridesRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiPromptWsApiProxy extends AiPromptWsApi {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(AssetWsApiProxy.class);

  public AiPromptWsApiProxy(ApiClient apiClient) {
    super(apiClient);
  }

  @Override
  public String deleteRepositoryLocalePromptOverrides(
      AITranslationLocalePromptOverridesRequest body) throws ApiException {
    logger.debug("Received request to delete repository locale prompt overrides");
    return super.deleteRepositoryLocalePromptOverrides(body);
  }

  @Override
  public String createOrUpdateRepositoryLocalePromptOverrides(
      AITranslationLocalePromptOverridesRequest body) throws ApiException {
    logger.debug("Received request to create or update repository locale prompt overrides");
    return super.createOrUpdateRepositoryLocalePromptOverrides(body);
  }

  @Override
  public void addPromptToRepository(Long promptId, String repositoryName, String promptType)
      throws ApiException {
    logger.debug("Received request to add prompt id {} to {} repository", promptId, repositoryName);
    super.addPromptToRepository(promptId, repositoryName, promptType);
  }

  @Override
  public Long createPrompt(AIPromptCreateRequest body) throws ApiException {
    logger.debug("Received request to create prompt");
    return super.createPrompt(body);
  }

  @Override
  public Long createPromptMessage(AIPromptContextMessageCreateRequest body) throws ApiException {
    logger.debug("Received request to create prompt context message");
    return super.createPromptMessage(body);
  }

  @Override
  public void deletePrompt(Long promptId) throws ApiException {
    logger.debug("Received request to delete prompt id {}", promptId);
    super.deletePrompt(promptId);
  }

  @Override
  public void deletePromptMessage(Long contextMessageId) throws ApiException {
    logger.debug("Received request to delete prompt message id {}", contextMessageId);
    super.deletePromptMessage(contextMessageId);
  }
}
