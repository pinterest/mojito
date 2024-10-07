package com.box.l10n.mojito.pagerduty;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("pagerDutyIntegrationsConfiguration")
public class PagerDutyIntegrations {

  private final PagerDutyIntegrationsConfiguration pagerDutyIntegrationsConfigurationProperties;

  private Map<String, PagerDutyClient> pagerDutyClients;

  @Autowired
  public PagerDutyIntegrations(
      PagerDutyIntegrationsConfiguration pagerDutyIntegrationsConfigurationProperties) {
    this.pagerDutyIntegrationsConfigurationProperties =
        pagerDutyIntegrationsConfigurationProperties;

    setPagerDutyClients();
  }

  public Optional<PagerDutyClient> getPagerDutyClient(String integration) {
    return pagerDutyClients.containsKey(integration)
        ? Optional.of(pagerDutyClients.get(integration))
        : Optional.empty();
  }

  public Optional<PagerDutyClient> getDefaultPagerDutyClient() {
    return getPagerDutyClient("default");
  }

  public void setPagerDutyClients() {
    pagerDutyClients =
        pagerDutyIntegrationsConfigurationProperties.getPagerDutyIntegrations().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> new PagerDutyClient(e.getValue())));
  }
}
