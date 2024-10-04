package com.box.l10n.mojito.pagerduty;

public class PagerDutyException extends Exception {

  public PagerDutyException(String message) {
    super(message);
  }

  public PagerDutyException(String message, Throwable cause) {
    super(message, cause);
  }
}
