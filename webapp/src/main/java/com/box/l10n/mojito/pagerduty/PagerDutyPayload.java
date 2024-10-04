package com.box.l10n.mojito.pagerduty;

import java.util.Map;

public class PagerDutyPayload {
  private String summary;
  private String source;
  private PagerDutySeverity severity;
  private Map<String, String> customDetails;

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public PagerDutySeverity getSeverity() {
    return severity;
  }

  public void setSeverity(PagerDutySeverity severity) {
    this.severity = severity;
  }

  public Map<String, String> getCustomDetails() {
    return customDetails;
  }

  public void setCustomDetails(Map<String, String> customDetails) {
    this.customDetails = customDetails;
  }
}
