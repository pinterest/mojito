package com.box.l10n.mojito.pagerduty;

import java.net.http.HttpClient;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PagerDutyIntegrationService {

  private final PagerDutyIntegrationConfiguration pagerDutyIntegrationConfiguration;

  private Map<String, PagerDutyClient> pagerDutyClients;

  @Autowired
  public PagerDutyIntegrationService(
      PagerDutyIntegrationConfiguration pagerDutyIntegrationsConfiguration) {
    this.pagerDutyIntegrationConfiguration = pagerDutyIntegrationsConfiguration;

    createClientsFromConfiguration();
  }

  public Optional<PagerDutyClient> getPagerDutyClient(String integration) {
    return pagerDutyClients.containsKey(integration)
        ? Optional.of(pagerDutyClients.get(integration))
        : Optional.empty();
  }

  public Optional<PagerDutyClient> getDefaultPagerDutyClient() {
    return getPagerDutyClient("default");
  }

  private void createClientsFromConfiguration() {
    pagerDutyClients =
        pagerDutyIntegrationConfiguration.getPagerDutyIntegrations().entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> new PagerDutyClient(e.getValue(), HttpClient.newHttpClient())));
  }
}
