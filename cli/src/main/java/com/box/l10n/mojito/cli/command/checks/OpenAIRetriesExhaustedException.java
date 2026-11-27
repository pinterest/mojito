package com.box.l10n.mojito.cli.command.checks;

/** The AI checker retried the OpenAI API requests and all the retries failed. */
public class OpenAIRetriesExhaustedException extends RuntimeException {

  public OpenAIRetriesExhaustedException(String message, Throwable cause) {
    super(message, cause);
  }
}
