package com.box.l10n.mojito.pagerduty;

import java.util.Map;

public class PagerDutyPayload {
  private String summary;
  private String source;
  private PagerDutySeverity severity;
  private Map<String, String> customDetails;

  public PagerDutyPayload(Builder builder) {
    this.summary = builder.summary;
    this.source = builder.source;
    this.severity = builder.severity;
    this.customDetails = builder.customDetails;
  }

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

  public static class Builder {
    private String summary;
    private String source;
    private PagerDutySeverity severity;
    private Map<String, String> customDetails;

    public static Builder newBuilder() {
      return new Builder();
    }

    public Builder summary(String summary) {
      this.summary = summary;
      return this;
    }

    public Builder source(String source) {
      this.source = source;
      return this;
    }

    public Builder severity(PagerDutySeverity severity) {
      this.severity = severity;
      return this;
    }

    public Builder customDetails(Map<String, String> customDetails) {
      this.customDetails = customDetails;
      return this;
    }

    public PagerDutyPayload build() {
      return new PagerDutyPayload(this);
    }
  }
}
