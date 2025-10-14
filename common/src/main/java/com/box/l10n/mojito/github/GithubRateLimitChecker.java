package com.box.l10n.mojito.github;

import io.micrometer.core.instrument.MeterRegistry;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.RateLimitChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GithubRateLimitChecker extends RateLimitChecker {
  private final Logger logger = LoggerFactory.getLogger(GithubRateLimitChecker.class);
  private final MeterRegistry meterRegistry;

  public GithubRateLimitChecker(MeterRegistry registry) {
    this.meterRegistry = registry;
  }

  protected boolean checkRateLimit(GHRateLimit.Record record, long count) {
    logger.debug("Checking rate-limit for GHRateLimit record {}", record);
    int remainingRequests = record.getRemaining();
    int limit = record.getLimit();
    int currentlyUsed = limit - remainingRequests;
    meterRegistry.gauge("GithubRateLimitChecker.RateLimit.Used", currentlyUsed);
    meterRegistry.gauge("GithubRateLimitChecker.RateLimit.Limit", record.getLimit());
    logger.debug("Rate limit: {} / {}, remaining: {}", currentlyUsed, limit, remainingRequests);

    return remainingRequests > 0;
  }
}
