package com.box.l10n.mojito.service.blobstorage.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import redis.clients.jedis.Jedis;

@ExtendWith(MockitoExtension.class)
public class RedisScriptManagerTest {

  @Mock private ResourceLoader resourceLoader;

  @Mock private RedisPoolManager redisPoolManager;

  @Mock private Jedis jedis;

  @Mock private Resource resource;

  private RedisScriptManager redisScriptManager;

  @BeforeEach
  public void setUp() {
    when(redisPoolManager.getJedis()).thenReturn(jedis);
  }

  @Test
  public void constructorShouldLoadAllScriptsFromEnum() throws IOException {
    String scriptContent = "return redis.call('SET', KEYS[1], ARGV[1])";
    String expectedSHA = "abc123def456";

    when(resourceLoader.getResource(anyString())).thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getContentAsString(StandardCharsets.UTF_8)).thenReturn(scriptContent);
    when(jedis.scriptLoad(scriptContent)).thenReturn(expectedSHA);

    redisScriptManager = new RedisScriptManager(resourceLoader, redisPoolManager);

    verify(resourceLoader).getResource("classpath:redis/scripts/sliding-window-rate-limiter.lua");
    verify(jedis).scriptLoad(scriptContent);
    assertThat(redisScriptManager.isScriptLoaded(RedisScript.SLIDING_WINDOW_RATE_LIMITER)).isTrue();
    assertThat(redisScriptManager.getScriptSHA(RedisScript.SLIDING_WINDOW_RATE_LIMITER))
        .isEqualTo(expectedSHA);
  }

  @Test
  public void validScriptShouldLoadSuccessfully() throws IOException {
    String scriptContent = "return redis.call('GET', KEYS[1])";
    String expectedSHA = "def789ghi012";

    when(resourceLoader.getResource(anyString())).thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getContentAsString(StandardCharsets.UTF_8)).thenReturn(scriptContent);
    when(jedis.scriptLoad(scriptContent)).thenReturn(expectedSHA);

    redisScriptManager = new RedisScriptManager(resourceLoader, redisPoolManager);

    verify(jedis).scriptLoad(scriptContent);
    assertThat(redisScriptManager.getScriptSHA(RedisScript.SLIDING_WINDOW_RATE_LIMITER))
        .isEqualTo(expectedSHA);
  }

  @Test
  public void loadScriptWithRedisExceptionShouldHandleGracefully() throws IOException {
    String scriptContent = "return redis.call('SET', KEYS[1], ARGV[1])";

    when(resourceLoader.getResource(anyString())).thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getContentAsString(StandardCharsets.UTF_8)).thenReturn(scriptContent);
    doThrow(new RuntimeException("Redis connection error")).when(jedis).scriptLoad(scriptContent);

    redisScriptManager = new RedisScriptManager(resourceLoader, redisPoolManager);

    verify(jedis).scriptLoad(scriptContent);
    assertThat(redisScriptManager.isScriptLoaded(RedisScript.SLIDING_WINDOW_RATE_LIMITER))
        .isFalse();
  }

  @Test
  public void getScriptSHAShouldReturnSHA() throws IOException {
    String scriptContent = "return redis.call('SET', KEYS[1], ARGV[1])";
    String expectedSHA = "abc123def456";

    when(resourceLoader.getResource(anyString())).thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getContentAsString(StandardCharsets.UTF_8)).thenReturn(scriptContent);
    when(jedis.scriptLoad(scriptContent)).thenReturn(expectedSHA);

    redisScriptManager = new RedisScriptManager(resourceLoader, redisPoolManager);
    String actualSHA = redisScriptManager.getScriptSHA(RedisScript.SLIDING_WINDOW_RATE_LIMITER);
    assertThat(actualSHA).isEqualTo(expectedSHA);
  }

  @Test
  public void isScriptLoadedShouldReturnTrue() throws IOException {
    String scriptContent = "return redis.call('SET', KEYS[1], ARGV[1])";
    String expectedSHA = "abc123def456";

    when(resourceLoader.getResource(anyString())).thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getContentAsString(StandardCharsets.UTF_8)).thenReturn(scriptContent);
    when(jedis.scriptLoad(scriptContent)).thenReturn(expectedSHA);

    redisScriptManager = new RedisScriptManager(resourceLoader, redisPoolManager);
    assertThat(redisScriptManager.isScriptLoaded(RedisScript.SLIDING_WINDOW_RATE_LIMITER)).isTrue();
  }

  @Test
  public void constructorShouldCloseJedisConnectionAfterScriptLoad() throws IOException {
    // IMPORTANT: if this fails, it means the Jedis connection is not closed properly and could
    // lead to memory leaks
    when(redisPoolManager.getJedis()).thenReturn(jedis);
    String scriptContent = "return redis.call('SET', KEYS[1], ARGV[1])";
    when(resourceLoader.getResource(anyString())).thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getContentAsString(StandardCharsets.UTF_8)).thenReturn(scriptContent);
    when(jedis.scriptLoad(scriptContent)).thenReturn("sha");
    redisScriptManager = new RedisScriptManager(resourceLoader, redisPoolManager);
    verify(jedis).close();
  }
}
