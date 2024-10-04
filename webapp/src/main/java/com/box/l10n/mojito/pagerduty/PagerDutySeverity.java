package com.box.l10n.mojito.pagerduty;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PagerDutySeverity {
  CRITICAL,
  ERROR,
  WARNING,
  INFO,
  UNKNOWN;

  @JsonValue
  public String toLowerCase() {
    return this.name().toLowerCase();
  }
}
