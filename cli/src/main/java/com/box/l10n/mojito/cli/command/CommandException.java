package com.box.l10n.mojito.cli.command;

/**
 * Unrecoverable execution error that need to be shown to the user and lead to existing the CLI with
 * an error code.
 *
 * <p>The message will be displayed to the end user so it should be simple and doesn't contain too
 * technical information. More information will be added in the logs.
 *
 * @author jaurambault
 */
public class CommandException extends RuntimeException {

  private boolean sendAlert = true;

  public CommandException(Throwable cause) {
    super(cause);
  }

  public CommandException(String message) {
    super(message);
  }

  public CommandException(String message, Throwable cause) {
    super(message, cause);
  }

  public CommandException(String message, boolean sendAlert) {
    super(message);
    this.sendAlert = sendAlert;
  }

  public boolean isSendAlert() {
    return this.sendAlert;
  }
}
