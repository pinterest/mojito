package com.box.l10n.mojito.service.tm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.integrity-check-failure")
public class IntegrityCheckFailureConfigurationProperties {
  private String pagerDutyIncidentSummary =
      "[Failed] Text Unit Variants have failed Integrity Checks";

  private String pagerDutyDedupKey = "f1f3ac7b-e183-4f2b-b7c6-e79ee709cf90";

  private String thirdPartyTextUnitIdPlaceholder = "${thirdPartyTextUnitId}";

  private String jobCronExpression = "0 20 * * * ?";

  public String getPagerDutyIncidentSummary() {
    return pagerDutyIncidentSummary;
  }

  public void setPagerDutyIncidentSummary(String pagerDutyIncidentSummary) {
    this.pagerDutyIncidentSummary = pagerDutyIncidentSummary;
  }

  public String getPagerDutyDedupKey() {
    return pagerDutyDedupKey;
  }

  public void setPagerDutyDedupKey(String pagerDutyDedupKey) {
    this.pagerDutyDedupKey = pagerDutyDedupKey;
  }

  public String getThirdPartyTextUnitIdPlaceholder() {
    return thirdPartyTextUnitIdPlaceholder;
  }

  public void setThirdPartyTextUnitIdPlaceholder(String thirdPartyTextUnitIdPlaceholder) {
    this.thirdPartyTextUnitIdPlaceholder = thirdPartyTextUnitIdPlaceholder;
  }

  public String getJobCronExpression() {
    return jobCronExpression;
  }

  public void setJobCronExpression(String jobCronExpression) {
    this.jobCronExpression = jobCronExpression;
  }
}
