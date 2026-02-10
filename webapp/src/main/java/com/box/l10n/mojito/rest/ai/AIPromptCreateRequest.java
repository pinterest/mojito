package com.box.l10n.mojito.rest.ai;

import com.box.l10n.mojito.entity.PromptTextUnitTypeChecker;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AIPromptCreateRequest {

  private String systemPrompt;
  private String userPrompt;
  private String modelName;
  private float promptTemperature;
  private boolean deleted;
  private String repositoryName;
  private String promptType;
  private boolean isJsonResponse;
  private String jsonResponseKey;
  private PromptTextUnitTypeChecker promptTextUnitTypeChecker;

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  public float getPromptTemperature() {
    return promptTemperature;
  }

  public void setPromptTemperature(float promptTemperature) {
    this.promptTemperature = promptTemperature;
  }

  public String getSystemPrompt() {
    return systemPrompt;
  }

  public void setSystemPrompt(String systemPrompt) {
    this.systemPrompt = systemPrompt;
  }

  public String getUserPrompt() {
    return userPrompt;
  }

  public void setUserPrompt(String userPrompt) {
    this.userPrompt = userPrompt;
  }

  public String getPromptType() {
    return promptType;
  }

  public void setPromptType(String promptType) {
    this.promptType = promptType;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public boolean isJsonResponse() {
    return isJsonResponse;
  }

  public void setJsonResponse(boolean jsonResponse) {
    isJsonResponse = jsonResponse;
  }

  public String getJsonResponseKey() {
    return jsonResponseKey;
  }

  public void setJsonResponseKey(String jsonResponseKey) {
    this.jsonResponseKey = jsonResponseKey;
  }

  public PromptTextUnitTypeChecker getPromptTexUnitTypeChecker() {
    return promptTextUnitTypeChecker;
  }

  public void setPromptTexUnitTypeChecker(PromptTextUnitTypeChecker promptTextUnitTypeChecker) {
    this.promptTextUnitTypeChecker = promptTextUnitTypeChecker;
  }
}
