package com.box.l10n.mojito.service.blobstorage.redis;

/**
 * Enum representing Redis scripts - scripts must be defined here to be loaded.
 *
 * <p>Redis scripts are executed using their enum. All scripts should be placed in the
 * `redis/scripts` directory and are loaded into Redis at application startup by the {@link
 * RedisScriptManager}.
 *
 * @author mattwilshire
 */
public enum RedisScript {
  SLIDING_WINDOW_RATE_LIMITER("sliding-window-rate-limiter.lua");

  private final String scriptName;

  RedisScript(String scriptName) {
    this.scriptName = scriptName;
  }

  public String getScriptName() {
    return scriptName;
  }
}
