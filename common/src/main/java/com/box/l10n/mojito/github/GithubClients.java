package com.box.l10n.mojito.github;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GithubClients {
  private final Map<String, GithubClient> githubOwnerToClientsCache;

  @Autowired
  public GithubClients(
      GithubClientsConfiguration githubClientsConfiguration,
      Optional<MeterRegistry> meterRegistry) {
    githubOwnerToClientsCache = createGithubClients(githubClientsConfiguration, meterRegistry);
  }

  private Map<String, GithubClient> createGithubClients(
      GithubClientsConfiguration githubClientsConfiguration,
      Optional<MeterRegistry> meterRegistry) {

    return githubClientsConfiguration.getGithubClients().entrySet().stream()
        .collect(
            Collectors.toMap(
                e -> e.getValue().getOwner(),
                e ->
                    new GithubClient(
                        e.getValue().getAppId(),
                        e.getValue().getKey(),
                        e.getValue().getOwner(),
                        e.getValue().getTokenTTL(),
                        e.getValue().getEndpoint(),
                        e.getValue().getMaxRetries(),
                        Duration.ofSeconds(e.getValue().getRetryMinBackoffSecs()),
                        Duration.ofSeconds(e.getValue().getRetryMaxBackoffSecs()),
                        meterRegistry != null && meterRegistry.isPresent()
                            ? meterRegistry.get()
                            : null)));
  }

  public GithubClient getClient(String owner) {
    GithubClient githubClient = githubOwnerToClientsCache.get(owner);
    if (githubClient == null) {
      throw new GithubException(
          String.format("Github client for owner '%s' is not configured", owner));
    }
    return githubClient;
  }

  public boolean isClientAvailable(String owner) {
    return githubOwnerToClientsCache.containsKey(owner);
  }
}
