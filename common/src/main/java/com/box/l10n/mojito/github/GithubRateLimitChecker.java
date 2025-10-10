package com.box.l10n.mojito.github;

import io.micrometer.core.instrument.MeterRegistry;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.RateLimitChecker;

public class GithubRateLimitChecker extends RateLimitChecker {
  private final MeterRegistry meterRegistry;

  public GithubRateLimitChecker(MeterRegistry registry) {
    this.meterRegistry = registry;
  }

  protected boolean checkRateLimit(GHRateLimit.Record record, long count) {
    int remainingRequests = record.getRemaining();
    int limit = record.getLimit();
    int currentlyUsed = limit - remainingRequests;
    meterRegistry.gauge("GithubRateLimitChecker.RateLimit.Used", currentlyUsed);
    meterRegistry.gauge("GithubRateLimitChecker.RateLimit.Limit", record.getLimit());

    return remainingRequests > 0;
  }
}
