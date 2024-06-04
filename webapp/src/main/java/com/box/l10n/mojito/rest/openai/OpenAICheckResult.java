package com.box.l10n.mojito.rest.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAICheckResult {

  boolean success;
  String suggestedFix;

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getSuggestedFix() {
    return suggestedFix;
  }

  public void setSuggestedFix(String suggestedFix) {
    this.suggestedFix = suggestedFix;
  }
}
