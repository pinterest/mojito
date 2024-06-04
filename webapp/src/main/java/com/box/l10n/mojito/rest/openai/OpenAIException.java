package com.box.l10n.mojito.rest.openai;

public class OpenAIException extends RuntimeException {

  public OpenAIException(String message) {
    super(message);
  }

  public OpenAIException(String message, Exception e) {
    super(message, e);
  }
}
