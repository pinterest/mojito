package com.box.l10n.mojito.service.ai.translation;

import com.box.l10n.mojito.service.blobstorage.redis.RedisClient;
import com.box.l10n.mojito.service.ratelimiter.SlidingWindowRateLimiter;
import com.google.common.collect.Maps;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.ai.translation")
public class AITranslationConfiguration {

  private boolean enabled = false;
  private int batchSize = 10;

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

  private Duration timeout = Duration.ofMinutes(30);

  private Map<String, RepositorySettings> repositorySettings = Maps.newHashMap();

  public static class RepositorySettings {
    /**
     * If true, reuse the source text if the language of the source text matches the target
     * language.
     *
     * <p>Uses the language piece of the BCP47 tag to determine if the language matches. For
     * example, en-US and en-GB would match.
     *
     * <p>If a match is found, the source text will be used as the translation for the target locale
     * and no AI translation will be requested for the target locale.
     */
    private boolean reuseSourceOnLanguageMatch = false;

    private boolean injectGlossaryMatches = false;

    public Boolean isReuseSourceOnLanguageMatch() {
      return reuseSourceOnLanguageMatch;
    }

    public void setReuseSourceOnLanguageMatch(Boolean reuseSourceOnLanguageMatch) {
      this.reuseSourceOnLanguageMatch = reuseSourceOnLanguageMatch;
    }

    public Boolean isInjectGlossaryMatches() {
      return injectGlossaryMatches;
    }

    public void setInjectGlossaryMatches(Boolean injectGlossaryMatches) {
      this.injectGlossaryMatches = injectGlossaryMatches;
    }
  }

  private RateLimitConfiguration rateLimit;

  public static class RateLimitConfiguration {
    private boolean enabled = false;
    private int maxRequests;
    private Duration windowSize;
    private Duration minPollInterval;
    private Duration maxPollInterval;
    private Duration minJitter = Duration.ofMillis(50);
    private Duration maxJitter = Duration.ofMillis(200);

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getMaxRequests() {
      return maxRequests;
    }

    public void setMaxRequests(int maxRequests) {
      this.maxRequests = maxRequests;
    }

    public Duration getWindowSize() {
      return windowSize;
    }

    public void setWindowSize(Duration windowSize) {
      this.windowSize = windowSize;
    }

    public Duration getMinPollInterval() {
      return minPollInterval;
    }

    public void setMinPollInterval(Duration minPollInterval) {
      this.minPollInterval = minPollInterval;
    }

    public Duration getMaxPollInterval() {
      return maxPollInterval;
    }

    public void setMaxPollInterval(Duration maxPollInterval) {
      this.maxPollInterval = maxPollInterval;
    }

    public Duration getMinJitter() {
      return minJitter;
    }

    public void setMinJitter(Duration minJitter) {
      this.minJitter = minJitter;
    }

    public Duration getMaxJitter() {
      return maxJitter;
    }

    public void setMaxJitter(Duration maxJitter) {
      this.maxJitter = maxJitter;
    }
  }

  @Bean
  @ConditionalOnBean(RedisClient.class)
  @ConditionalOnProperty(value = "l10n.ai.translation.rateLimit.enabled", havingValue = "true")
  public SlidingWindowRateLimiter aiTranslationRateLimiter(@Autowired RedisClient redisClient) {
    return new SlidingWindowRateLimiter(
        redisClient,
        "ai_translation",
        rateLimit.getMaxRequests(),
        rateLimit.getWindowSize().toMillis());
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

  public RateLimitConfiguration getRateLimit() {
    return rateLimit;
  }

  public void setRateLimit(RateLimitConfiguration rateLimit) {
    this.rateLimit = rateLimit;
  }

  public Duration getTimeout() {
    return timeout;
  }

  public void setTimeout(Duration timeout) {
    this.timeout = timeout;
  }
}
