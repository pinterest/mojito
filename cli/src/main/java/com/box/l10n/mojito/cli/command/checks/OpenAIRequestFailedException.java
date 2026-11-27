package com.box.l10n.mojito.cli.command.checks;

/** The OpenAI API requests failed on the server. The AI checker retries only this error. */
public class OpenAIRequestFailedException extends RuntimeException {

  public OpenAIRequestFailedException(String message) {
    super(message);
  }
}
