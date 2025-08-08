package com.box.l10n.mojito.service.blobstorage.redis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

/**
 * Reads and loads all lua scripts defined in the {@link RedisScript} enum into Redis and stores
 * their SHA hashes for later execution.
 *
 * <p>Scripts are loaded at application startup and can be executed using their enum representation.
 * To execute a script, use {@link RedisClient#executeScript(RedisScript, List, List)}.
 *
 * @author mwilshire
 */
@Component
@ConditionalOnProperty("l10n.redis.connection.endpoint")
public class RedisScriptManager {
  static Logger logger = LoggerFactory.getLogger(RedisScriptManager.class);

  private final String SCRIPTS_PATH = "redis/scripts";
  private final ResourceLoader resourceLoader;
  private final RedisPoolManager redisPoolManager;

  private final Map<RedisScript, String> scriptToSHA = new HashMap<>();

  public RedisScriptManager(ResourceLoader resourceLoader, RedisPoolManager redisPoolManager) {
    this.resourceLoader = resourceLoader;
    this.redisPoolManager = redisPoolManager;
    Arrays.stream(RedisScript.values()).forEach(this::loadScript);
  }

  public void loadScript(RedisScript script) {
    try {
      String scriptContent = loadScriptContent(script);

      try (Jedis jedis = redisPoolManager.getJedis()) {
        String scriptSha = jedis.scriptLoad(scriptContent);
        scriptToSHA.put(script, scriptSha);
        logger.info(
            "Script '{}' loaded into Redis with SHA: {}", script.getScriptName(), scriptSha);
      }
    } catch (Exception e) {
      logger.error("Failed to load script {}: {}", script.getScriptName(), e.getMessage(), e);
    }
  }

  private String loadScriptContent(RedisScript script) throws IOException {
    String resourcePath = "classpath:" + SCRIPTS_PATH + "/" + script.getScriptName();
    Resource resource = resourceLoader.getResource(resourcePath);

    if (!resource.exists()) {
      throw new IllegalArgumentException(
          String.format("Script file not found: '%s' for enum '%s'", resourcePath, script.name()));
    }

    return resource.getContentAsString(StandardCharsets.UTF_8);
  }

  public String getScriptSHA(RedisScript script) {
    String sha = scriptToSHA.get(script);
    if (sha == null) {
      throw new IllegalStateException(
          String.format("Script SHA not found for script: %s", script.getScriptName()));
    }
    return sha;
  }

  public boolean isScriptLoaded(RedisScript script) {
    return scriptToSHA.containsKey(script);
  }
}
