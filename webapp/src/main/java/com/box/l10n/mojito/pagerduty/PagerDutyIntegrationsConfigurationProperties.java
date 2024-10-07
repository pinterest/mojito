package com.box.l10n.mojito.pagerduty;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("l10n")
public class PagerDutyIntegrationsConfigurationProperties {

  private Map<String, String> pagerDutyIntegrations;

  public Map<String, String> getPagerDutyIntegrations() {
    if (pagerDutyIntegrations == null) {
      pagerDutyIntegrations = new HashMap<>();
    }
    return pagerDutyIntegrations;
  }

  public void setPagerDutyIntegrations(Map<String, String> pagerDutyIntegrations) {
    this.pagerDutyIntegrations = pagerDutyIntegrations;
  }
}
