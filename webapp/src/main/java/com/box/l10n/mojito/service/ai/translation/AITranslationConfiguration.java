package com.box.l10n.mojito.service.ai.translation;

import com.google.common.collect.Maps;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.ai.translation")
public class AITranslationConfiguration {

  private boolean enabled = false;
  private int batchSize = 1000;

  /**
   * Duration after which a pending MT is considered expired and will not be processed in AI
   * translation (as it will be eligible for third party syncs once the entity is older than the
   * expiry period).
   *
   * <p>If the pending MT is expired, it will be deleted which will remove it from AI translation
   * flow.
   */
  private Duration expiryDuration = Duration.ofHours(3);

  private String cron = "0 0/10 * * * ?";

  private Map<String, RepositorySettings> repositorySettings = Maps.newHashMap();

  public static class RepositorySettings {
    private boolean reuseSourceOnLanguageMatch = false;

    public Boolean isReuseSourceOnLanguageMatch() {
      return reuseSourceOnLanguageMatch;
    }

    public void setReuseSourceOnLanguageMatch(Boolean reuseSourceOnLanguageMatch) {
      this.reuseSourceOnLanguageMatch = reuseSourceOnLanguageMatch;
    }
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public Duration getExpiryDuration() {
    return expiryDuration;
  }

  public void setExpiryDuration(Duration expiryDuration) {
    this.expiryDuration = expiryDuration;
  }

  public String getCron() {
    return cron;
  }

  public void setCron(String cron) {
    this.cron = cron;
  }

  public Map<String, RepositorySettings> getRepositorySettings() {
    return repositorySettings;
  }

  public void setRepositorySettings(Map<String, RepositorySettings> repositorySettings) {
    this.repositorySettings = repositorySettings;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public RepositorySettings getRepositorySettings(String repositoryName) {
    return repositorySettings.get(repositoryName);
  }
}
