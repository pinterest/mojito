package com.box.l10n.mojito.rest.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAICheckResponse {

  boolean error;

  String errorMessage;

  Map<String, List<OpenAICheckResult>> results;

  public Map<String, List<OpenAICheckResult>> getResults() {
    return results;
  }

  public void setResults(Map<String, List<OpenAICheckResult>> results) {
    this.results = results;
  }

  public boolean isError() {
    return error;
  }

  public void setError(boolean error) {
    this.error = error;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
