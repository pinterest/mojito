package com.box.l10n.mojito.cli.apiclient.exceptions;

public class PollableTaskTimeoutException extends PollableTaskException {
  public PollableTaskTimeoutException(String message) {
    super(message);
  }
}
