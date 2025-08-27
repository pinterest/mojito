package com.box.l10n.mojito.service.ratelimiter;

import com.box.l10n.mojito.service.blobstorage.redis.RedisClient;
import com.box.l10n.mojito.service.blobstorage.redis.RedisScript;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.exceptions.JedisException;

public class SlidingWindowRateLimiter {
  static Logger logger = LoggerFactory.getLogger(SlidingWindowRateLimiter.class);

  private final RedisClient redisClient;
  private final String rateLimiterKey;
  private final int maxRequests;
  private final int windowSizeInMillis;

  public SlidingWindowRateLimiter(
      RedisClient redisClient, String rateLimiterKey, int maxRequests, int windowSizeInMillis) {
    this.redisClient = redisClient;
    this.rateLimiterKey = rateLimiterKey;
    this.maxRequests = maxRequests;
    this.windowSizeInMillis = windowSizeInMillis;
  }

  /**
   * Checks if the request is allowed based on the sliding window rate limiting algorithm.
   *
   * @return true if the request is allowed, false if the rate limit has been exceeded
   * @throws JedisException if there is an error executing the Redis script
   */
  public boolean isAllowed() throws JedisException {
    // If Redis is not configured, allow all requests
    if (redisClient != null) {
      return redisClient.executeRateLimitScript(
              RedisScript.SLIDING_WINDOW_RATE_LIMITER,
              List.of(rateLimiterKey),
              List.of(String.valueOf(maxRequests), String.valueOf(windowSizeInMillis)))
          == 1L;
    } else {
      // No rate limiting if Redis is not configured
      logger.debug(
          "Redis is not configured, allowing all requests for rate limiter key: {}",
          rateLimiterKey);
      return true;
    }
  }
}
