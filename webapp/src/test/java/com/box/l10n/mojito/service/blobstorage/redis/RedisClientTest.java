package com.box.l10n.mojito.service.blobstorage.redis;

import static com.box.l10n.mojito.service.blobstorage.Retention.MIN_1_DAY;
import static com.box.l10n.mojito.service.blobstorage.Retention.PERMANENT;
import static com.box.l10n.mojito.service.blobstorage.redis.RedisClient.ONE_DAY_IN_SECONDS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

public class RedisClientTest {
  static final String KEY = "key";

  static final String VALUE = "value";

  static final byte[] KEY_BYTES = KEY.getBytes(StandardCharsets.UTF_8);

  static final byte[] VALUE_BYTES = VALUE.getBytes(StandardCharsets.UTF_8);

  Jedis jedisMock;

  RedisPoolManager redisPoolManagerMock;

  RedisClient redisClient;

  RedisScriptManager redisScriptManagerMock;

  @BeforeEach
  public void setUp() {
    this.redisPoolManagerMock = mock(RedisPoolManager.class);
    this.jedisMock = mock(Jedis.class);
    this.redisScriptManagerMock = mock(RedisScriptManager.class);
    when(this.redisPoolManagerMock.getJedis()).thenReturn(this.jedisMock);
    this.redisClient = new RedisClient(redisPoolManagerMock, redisScriptManagerMock);
  }

  @Test
  public void testGet_ReturnsNonEmptyValue() {
    when(this.jedisMock.get(anyString())).thenReturn(VALUE);

    Optional<String> value = this.redisClient.get(KEY);

    assertTrue(value.isPresent());
    assertEquals(VALUE, value.get());
  }

  @Test
  public void testGet_ReturnsEmptyValue() {
    when(this.jedisMock.get(anyString())).thenReturn(null);

    Optional<String> value = this.redisClient.get(KEY);

    assertTrue(value.isEmpty());
  }

  @Test
  public void testGet_ReturnsEmptyValueWhenThrowingException() {
    when(this.redisPoolManagerMock.getJedis())
        .thenThrow(new JedisException("jedis pool exception"));

    Optional<String> value = this.redisClient.get(KEY);

    assertTrue(value.isEmpty());

    reset(this.redisPoolManagerMock);
    when(this.redisPoolManagerMock.getJedis()).thenReturn(this.jedisMock);
    when(this.jedisMock.get(anyString())).thenThrow(new JedisException("jedis client exception"));

    value = this.redisClient.get(KEY);

    assertTrue(value.isEmpty());
  }

  @Test
  public void testPut_DoesNotThrowException() {
    this.redisClient.put(KEY, VALUE, MIN_1_DAY);

    verify(this.jedisMock).setex(KEY, ONE_DAY_IN_SECONDS, VALUE);

    this.redisClient.put(KEY, VALUE, PERMANENT);

    verify(this.jedisMock).set(KEY, VALUE);
  }

  @Test
  public void testPut_ThrowsException() {
    when(this.redisPoolManagerMock.getJedis())
        .thenThrow(new JedisException("jedis pool exception"));

    this.redisClient.put(KEY, VALUE, MIN_1_DAY);

    reset(this.redisPoolManagerMock);
    when(this.redisPoolManagerMock.getJedis()).thenReturn(this.jedisMock);
    when(this.jedisMock.setex(anyString(), anyLong(), anyString()))
        .thenThrow(new JedisException("jedis client exception"));

    this.redisClient.put(KEY, VALUE, MIN_1_DAY);

    reset(this.jedisMock);
    when(this.jedisMock.set(anyString(), anyString()))
        .thenThrow(new JedisException("jedis client exception"));

    this.redisClient.put(KEY, VALUE, PERMANENT);
  }

  @Test
  public void testDelete_DoesNotThrowException() {
    this.redisClient.delete(KEY);

    verify(this.jedisMock).del(KEY);
  }

  @Test
  public void testDelete_ThrowsException() {
    when(this.redisPoolManagerMock.getJedis())
        .thenThrow(new JedisException("jedis pool exception"));

    this.redisClient.delete(KEY);

    reset(this.redisPoolManagerMock);
    when(this.redisPoolManagerMock.getJedis()).thenReturn(this.jedisMock);
    when(this.jedisMock.del(anyString())).thenThrow(new JedisException("jedis client exception"));

    this.redisClient.delete(KEY);
  }

  @Test
  public void testGetBytes_ReturnsNonEmptyValue() {
    when(this.jedisMock.get(any(byte[].class)))
        .thenReturn("value".getBytes(StandardCharsets.UTF_8));

    Optional<byte[]> value = this.redisClient.getBytes(KEY);

    assertTrue(value.isPresent());
    assertArrayEquals(VALUE_BYTES, value.get());
  }

  @Test
  public void testGetBytes_ReturnsEmptyValue() {
    when(this.jedisMock.get(any(byte[].class))).thenReturn(null);

    Optional<String> value = this.redisClient.get(KEY);

    assertTrue(value.isEmpty());
  }

  @Test
  public void testGetBytes_ReturnsEmptyValueWhenThrowingException() {
    when(this.redisPoolManagerMock.getJedis())
        .thenThrow(new JedisException("jedis pool exception"));

    Optional<byte[]> value = this.redisClient.getBytes(KEY);

    assertTrue(value.isEmpty());

    reset(this.redisPoolManagerMock);
    when(this.redisPoolManagerMock.getJedis()).thenReturn(this.jedisMock);
    when(this.jedisMock.get(any(byte[].class)))
        .thenThrow(new JedisException("jedis client exception"));

    value = this.redisClient.getBytes(KEY);

    assertTrue(value.isEmpty());
  }

  @Test
  public void testPutBytes_DoesNotThrowException() {
    this.redisClient.put(KEY, VALUE_BYTES, MIN_1_DAY);

    verify(this.jedisMock).setex(KEY_BYTES, ONE_DAY_IN_SECONDS, VALUE_BYTES);

    this.redisClient.put(KEY, VALUE_BYTES, PERMANENT);

    verify(this.jedisMock).set(KEY_BYTES, VALUE_BYTES);
  }

  @Test
  public void testPutBytes_ThrowsException() {
    when(this.redisPoolManagerMock.getJedis())
        .thenThrow(new JedisException("jedis pool exception"));

    this.redisClient.put(KEY, VALUE_BYTES, MIN_1_DAY);

    reset(this.redisPoolManagerMock);
    when(this.redisPoolManagerMock.getJedis()).thenReturn(this.jedisMock);
    when(this.jedisMock.setex(any(byte[].class), anyLong(), any(byte[].class)))
        .thenThrow(new JedisException("jedis client exception"));

    this.redisClient.put(KEY, VALUE_BYTES, MIN_1_DAY);

    reset(this.jedisMock);
    when(this.jedisMock.set(any(byte[].class), any(byte[].class)))
        .thenThrow(new JedisException("jedis client exception"));

    this.redisClient.put(KEY, VALUE_BYTES, PERMANENT);
  }

  @Test
  public void testExecuteScript_ScriptAlreadyLoaded_ReturnsResult() {
    RedisScript script = RedisScript.SLIDING_WINDOW_RATE_LIMITER;
    List<String> keys = Arrays.asList("key1", "key2");
    List<String> args = Arrays.asList("arg1", "arg2");
    String scriptSHA = "test-sha-hash";
    Object expectedResult = 1L;

    when(this.redisScriptManagerMock.isScriptLoaded(script)).thenReturn(true);
    when(this.redisScriptManagerMock.getScriptSHA(script)).thenReturn(scriptSHA);
    when(this.jedisMock.evalsha(scriptSHA, keys, args)).thenReturn(expectedResult);

    Object result = this.redisClient.executeScript(script, keys, args);

    assertEquals(expectedResult, result);
    verify(this.redisScriptManagerMock).isScriptLoaded(script);
    verify(this.redisScriptManagerMock).getScriptSHA(script);
    verify(this.jedisMock).evalsha(scriptSHA, keys, args);
  }

  @Test
  public void testExecuteScript_ScriptNotLoaded_LoadsScriptThenExecutes() {
    RedisScript script = RedisScript.SLIDING_WINDOW_RATE_LIMITER;
    List<String> keys = Arrays.asList("key1");
    List<String> args = Arrays.asList("arg1");
    String scriptSHA = "test-sha-hash";
    Object expectedResult = 0L;

    when(this.redisScriptManagerMock.isScriptLoaded(script)).thenReturn(false);
    when(this.redisScriptManagerMock.getScriptSHA(script)).thenReturn(scriptSHA);
    when(this.jedisMock.evalsha(scriptSHA, keys, args)).thenReturn(expectedResult);

    Object result = this.redisClient.executeScript(script, keys, args);

    assertEquals(expectedResult, result);
    verify(this.redisScriptManagerMock).isScriptLoaded(script);
    verify(this.redisScriptManagerMock).loadScript(script);
    verify(this.redisScriptManagerMock).getScriptSHA(script);
    verify(this.jedisMock).evalsha(scriptSHA, keys, args);
  }

  @Test
  public void testExecuteScript_EmptyKeysAndArgs_ExecutesSuccessfully() {
    RedisScript script = RedisScript.SLIDING_WINDOW_RATE_LIMITER;
    List<String> keys = Arrays.asList();
    List<String> args = Arrays.asList();
    String scriptSHA = "test-sha-hash";
    Object expectedResult = 1L;

    when(this.redisScriptManagerMock.isScriptLoaded(script)).thenReturn(true);
    when(this.redisScriptManagerMock.getScriptSHA(script)).thenReturn(scriptSHA);
    when(this.jedisMock.evalsha(scriptSHA, keys, args)).thenReturn(expectedResult);

    Object result = this.redisClient.executeScript(script, keys, args);

    assertEquals(expectedResult, result);
  }

  @Test
  public void testExecuteScript_JedisPoolException_ThrowsException() {
    RedisScript script = RedisScript.SLIDING_WINDOW_RATE_LIMITER;
    List<String> keys = Arrays.asList("key1");
    List<String> args = Arrays.asList("arg1");

    when(this.redisPoolManagerMock.getJedis())
        .thenThrow(new JedisException("jedis pool exception"));

    assertThrows(
        JedisException.class,
        () -> {
          this.redisClient.executeScript(script, keys, args);
        });
  }

  @Test
  public void testExecuteScript_JedisEvalshaException_ThrowsException() {
    RedisScript script = RedisScript.SLIDING_WINDOW_RATE_LIMITER;
    List<String> keys = Arrays.asList("key1");
    List<String> args = Arrays.asList("arg1");
    String scriptSHA = "test-sha-hash";

    when(this.redisScriptManagerMock.isScriptLoaded(script)).thenReturn(true);
    when(this.redisScriptManagerMock.getScriptSHA(script)).thenReturn(scriptSHA);
    when(this.jedisMock.evalsha(scriptSHA, keys, args))
        .thenThrow(new JedisException("evalsha execution failed"));

    assertThrows(
        JedisException.class,
        () -> {
          this.redisClient.executeScript(script, keys, args);
        });
  }

  @Test
  public void testExecuteScript_ScriptManagerGetSHAException_ThrowsException() {
    RedisScript script = RedisScript.SLIDING_WINDOW_RATE_LIMITER;
    List<String> keys = Arrays.asList("key1");
    List<String> args = Arrays.asList("arg1");

    when(this.redisScriptManagerMock.isScriptLoaded(script)).thenReturn(true);
    when(this.redisScriptManagerMock.getScriptSHA(script))
        .thenThrow(new IllegalStateException("Script SHA not found"));

    assertThrows(
        IllegalStateException.class,
        () -> {
          this.redisClient.executeScript(script, keys, args);
        });
  }
}
