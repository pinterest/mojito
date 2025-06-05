package com.box.l10n.mojito.service.blobstorage;

import static com.box.l10n.mojito.service.blobstorage.RedisStructuredBlobStorage.ONE_DAY_IN_SECONDS;
import static com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage.Prefix.TEXT_UNIT_DTOS_CACHE;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.service.blobstorage.redis.RedisPoolManager;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import redis.clients.jedis.Jedis;

public class RedisStructuredBlobStorageTest {
  static final String FILE_NAME = "file.txt";

  static final String KEY = TEXT_UNIT_DTOS_CACHE.toString().toLowerCase() + "/" + FILE_NAME;

  static final byte[] KEY_BYTES = KEY.getBytes(StandardCharsets.UTF_8);

  BlobStorage blobStorageMock;

  RedisPoolManager redisPoolManagerMock;

  ArgumentCaptor<Runnable> runnableArgumentCaptorCaptor = ArgumentCaptor.forClass(Runnable.class);

  @BeforeEach
  public void before() {
    this.redisPoolManagerMock = mock(RedisPoolManager.class);
    this.blobStorageMock = mock(BlobStorage.class);
  }

  @Test
  public void testGetString_GetsValueFromBlobStorage() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String content = "s3Content";
      when(this.blobStorageMock.getString(anyString())).thenReturn(of(content));
      RedisStructuredBlobStorage redisStructuredBlobStorage =
          new RedisStructuredBlobStorage(this.blobStorageMock, null, 1);

      Optional<String> result =
          redisStructuredBlobStorage.getString(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(content, result.get());
      verify(this.blobStorageMock, times(0)).getRetention(eq(KEY));
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
    }
  }

  @Test
  public void testGetString_GetsValueFromRedisPoolManager() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String content = "redisContent";
      this.blobStorageMock = mock(BlobStorage.class);
      Jedis jedis = mock(Jedis.class);
      when(jedis.get(anyString())).thenReturn(content);
      when(jedis.exists(anyString())).thenReturn(true);
      when(this.redisPoolManagerMock.getJedis()).thenReturn(jedis);
      RedisStructuredBlobStorage redisStructuredBlobStorage =
          new RedisStructuredBlobStorage(this.blobStorageMock, this.redisPoolManagerMock, 1);

      Optional<String> result =
          redisStructuredBlobStorage.getString(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(content, result.get());
      verify(jedis, times(1)).close();
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
    }
  }

  @Test
  public void testGetString_GetsValueFromBlobStorageInsteadOfRedisPoolManagerFor1DayRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String content = "s3Content";
      Jedis jedis = mock(Jedis.class);
      when(jedis.exists(anyString())).thenReturn(false);
      Jedis asyncJedis = mock(Jedis.class);
      when(this.blobStorageMock.getString(anyString())).thenReturn(of(content));
      when(this.blobStorageMock.getRetention(anyString())).thenReturn(of(Retention.MIN_1_DAY));
      when(this.redisPoolManagerMock.getJedis()).thenReturn(jedis).thenReturn(asyncJedis);
      RedisStructuredBlobStorage redisStructuredBlobStorage =
          new RedisStructuredBlobStorage(this.blobStorageMock, this.redisPoolManagerMock, 1);

      Optional<String> result =
          redisStructuredBlobStorage.getString(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(content, result.get());
      verify(jedis, times(1)).close();
      verify(this.blobStorageMock).getRetention(eq(KEY));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(asyncJedis, times(1)).close();
      verify(asyncJedis, times(1)).setex(eq(KEY), eq((long) ONE_DAY_IN_SECONDS), eq(content));
    }
  }

  @Test
  public void
      testGetString_GetsValueFromBlobStorageInsteadOfRedisPoolManagerForPermanentRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String content = "s3Content";
      Jedis jedis = mock(Jedis.class);
      when(jedis.exists(anyString())).thenReturn(false);
      Jedis asyncJedis = mock(Jedis.class);
      when(this.blobStorageMock.getString(anyString())).thenReturn(of(content));
      when(this.blobStorageMock.getRetention(anyString())).thenReturn(of(Retention.PERMANENT));
      when(this.redisPoolManagerMock.getJedis()).thenReturn(jedis).thenReturn(asyncJedis);
      RedisStructuredBlobStorage redisStructuredBlobStorage =
          new RedisStructuredBlobStorage(this.blobStorageMock, this.redisPoolManagerMock, 1);

      Optional<String> result =
          redisStructuredBlobStorage.getString(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(content, result.get());
      verify(jedis, times(1)).close();
      verify(this.blobStorageMock).getRetention(eq(KEY));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(asyncJedis, times(1)).close();
      verify(asyncJedis, times(1)).set(eq(KEY), eq(content));
    }
  }

  @Test
  public void testGetString_GetsNoValue() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      Jedis jedis = mock(Jedis.class);
      when(jedis.exists(anyString())).thenReturn(false);
      when(this.blobStorageMock.getString(anyString())).thenReturn(empty());
      when(this.redisPoolManagerMock.getJedis()).thenReturn(jedis);
      RedisStructuredBlobStorage redisStructuredBlobStorage =
          new RedisStructuredBlobStorage(this.blobStorageMock, this.redisPoolManagerMock, 1);

      Optional<String> result =
          redisStructuredBlobStorage.getString(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isEmpty());
      verify(jedis, times(1)).close();
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
    }
  }

  @Test
  public void testPut_SavesValueToBlobStorage() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String content = "s3Content";
      RedisStructuredBlobStorage redisStructuredBlobStorage =
          new RedisStructuredBlobStorage(this.blobStorageMock, null, 1);

      redisStructuredBlobStorage.put(TEXT_UNIT_DTOS_CACHE, FILE_NAME, content, Retention.MIN_1_DAY);

      verify(this.blobStorageMock).put(eq(KEY), eq(content), eq(Retention.MIN_1_DAY));
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
    }
  }

  @Test
  public void testPut_SavesValueToRedisPoolManagerFor1DayRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String content = "redisContent";
      this.blobStorageMock = mock(BlobStorage.class);
      Jedis jedis = mock(Jedis.class);
      when(this.redisPoolManagerMock.getJedis()).thenReturn(jedis);
      RedisStructuredBlobStorage redisStructuredBlobStorage =
          new RedisStructuredBlobStorage(this.blobStorageMock, this.redisPoolManagerMock, 1);

      redisStructuredBlobStorage.put(TEXT_UNIT_DTOS_CACHE, FILE_NAME, content, Retention.MIN_1_DAY);

      verify(jedis).setex(eq(KEY), eq((long) ONE_DAY_IN_SECONDS), eq(content));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.blobStorageMock, times(1)).put(eq(KEY), eq(content), eq(Retention.MIN_1_DAY));
    }
  }

  @Test
  public void testPut_SavesValueToRedisPoolManagerForPermanentRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String content = "redisContent";
      this.blobStorageMock = mock(BlobStorage.class);
      Jedis jedis = mock(Jedis.class);
      when(this.redisPoolManagerMock.getJedis()).thenReturn(jedis);
      RedisStructuredBlobStorage redisStructuredBlobStorage =
          new RedisStructuredBlobStorage(this.blobStorageMock, this.redisPoolManagerMock, 1);

      redisStructuredBlobStorage.put(TEXT_UNIT_DTOS_CACHE, FILE_NAME, content, Retention.PERMANENT);

      verify(jedis).set(eq(KEY), eq(content));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.blobStorageMock, times(1)).put(eq(KEY), eq(content), eq(Retention.PERMANENT));
    }
  }

  @Test
  public void testDelete_DeletesFromBlobStorage() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      RedisStructuredBlobStorage redisStructuredBlobStorage =
          new RedisStructuredBlobStorage(this.blobStorageMock, null, 1);

      redisStructuredBlobStorage.delete(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      verify(this.blobStorageMock).delete(eq(KEY));
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
    }
  }

  @Test
  public void testDelete_DeletesFromRedisPoolManager() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      this.blobStorageMock = mock(BlobStorage.class);
      Jedis jedis = mock(Jedis.class);
      when(this.redisPoolManagerMock.getJedis()).thenReturn(jedis);
      RedisStructuredBlobStorage redisStructuredBlobStorage =
          new RedisStructuredBlobStorage(this.blobStorageMock, this.redisPoolManagerMock, 1);

      redisStructuredBlobStorage.delete(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      verify(jedis).del(eq(KEY));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.blobStorageMock, times(1)).delete(eq(KEY));
    }
  }

  @Test
  public void testGetBytes_GetsValueFromBlobStorage() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      byte[] content = "s3Content".getBytes(StandardCharsets.UTF_8);
      when(this.blobStorageMock.getBytes(anyString())).thenReturn(of(content));
      RedisStructuredBlobStorage redisStructuredBlobStorage =
          new RedisStructuredBlobStorage(this.blobStorageMock, null, 1);

      Optional<byte[]> result =
          redisStructuredBlobStorage.getBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(content, result.get());
      verify(this.blobStorageMock, times(0)).getRetention(eq(KEY));
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
    }
  }

  @Test
  public void testGetBytes_GetsValueFromRedisPoolManager() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      byte[] content = "redisContent".getBytes(StandardCharsets.UTF_8);
      this.blobStorageMock = mock(BlobStorage.class);
      Jedis jedis = mock(Jedis.class);
      when(jedis.get(any(byte[].class))).thenReturn(content);
      when(jedis.exists(anyString())).thenReturn(true);
      when(this.redisPoolManagerMock.getJedis()).thenReturn(jedis);
      RedisStructuredBlobStorage redisStructuredBlobStorage =
          new RedisStructuredBlobStorage(this.blobStorageMock, this.redisPoolManagerMock, 1);

      Optional<byte[]> result =
          redisStructuredBlobStorage.getBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(content, result.get());
      verify(jedis, times(1)).close();
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
    }
  }

  @Test
  public void testGetBytes_GetsValueFromBlobStorageInsteadOfRedisPoolManagerFor1DayRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      byte[] content = "s3Content".getBytes(StandardCharsets.UTF_8);
      Jedis jedis = mock(Jedis.class);
      when(jedis.exists(anyString())).thenReturn(false);
      Jedis asyncJedis = mock(Jedis.class);
      when(this.blobStorageMock.getBytes(anyString())).thenReturn(of(content));
      when(this.blobStorageMock.getRetention(anyString())).thenReturn(of(Retention.MIN_1_DAY));
      when(this.redisPoolManagerMock.getJedis()).thenReturn(jedis).thenReturn(asyncJedis);
      RedisStructuredBlobStorage redisStructuredBlobStorage =
          new RedisStructuredBlobStorage(this.blobStorageMock, this.redisPoolManagerMock, 1);

      Optional<byte[]> result =
          redisStructuredBlobStorage.getBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(content, result.get());
      verify(jedis, times(1)).close();
      verify(this.blobStorageMock).getRetention(eq(KEY));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(asyncJedis, times(1)).close();
      verify(asyncJedis, times(1)).setex(eq(KEY_BYTES), eq((long) ONE_DAY_IN_SECONDS), eq(content));
    }
  }

  @Test
  public void
      testGetBytes_GetsValueFromBlobStorageInsteadOfRedisPoolManagerForPermanentRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      byte[] content = "s3Content".getBytes(StandardCharsets.UTF_8);
      Jedis jedis = mock(Jedis.class);
      when(jedis.exists(anyString())).thenReturn(false);
      Jedis asyncJedis = mock(Jedis.class);
      when(this.blobStorageMock.getBytes(anyString())).thenReturn(of(content));
      when(this.blobStorageMock.getRetention(anyString())).thenReturn(of(Retention.PERMANENT));
      when(this.redisPoolManagerMock.getJedis()).thenReturn(jedis).thenReturn(asyncJedis);
      RedisStructuredBlobStorage redisStructuredBlobStorage =
          new RedisStructuredBlobStorage(this.blobStorageMock, this.redisPoolManagerMock, 1);

      Optional<byte[]> result =
          redisStructuredBlobStorage.getBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(content, result.get());
      verify(jedis, times(1)).close();
      verify(this.blobStorageMock).getRetention(eq(KEY));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(asyncJedis, times(1)).close();
      verify(asyncJedis, times(1)).set(eq(KEY_BYTES), eq(content));
    }
  }

  @Test
  public void testGetBytes_GetsNoValue() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      Jedis jedis = mock(Jedis.class);
      when(jedis.exists(anyString())).thenReturn(false);
      when(this.blobStorageMock.getBytes(anyString())).thenReturn(empty());
      when(this.redisPoolManagerMock.getJedis()).thenReturn(jedis);
      RedisStructuredBlobStorage redisStructuredBlobStorage =
          new RedisStructuredBlobStorage(this.blobStorageMock, this.redisPoolManagerMock, 1);

      Optional<byte[]> result =
          redisStructuredBlobStorage.getBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isEmpty());
      verify(jedis, times(1)).close();
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
    }
  }

  @Test
  public void testPutBytes_SavesValueToBlobStorage() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final byte[] content = "s3Content".getBytes(StandardCharsets.UTF_8);
      RedisStructuredBlobStorage redisStructuredBlobStorage =
          new RedisStructuredBlobStorage(this.blobStorageMock, null, 1);

      redisStructuredBlobStorage.putBytes(
          TEXT_UNIT_DTOS_CACHE, FILE_NAME, content, Retention.MIN_1_DAY);

      verify(this.blobStorageMock).put(eq(KEY), eq(content), eq(Retention.MIN_1_DAY));
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
    }
  }

  @Test
  public void testPutBytes_SavesValueToRedisPoolManagerFor1DayRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final byte[] content = "redisContent".getBytes(StandardCharsets.UTF_8);
      this.blobStorageMock = mock(BlobStorage.class);
      Jedis jedis = mock(Jedis.class);
      when(this.redisPoolManagerMock.getJedis()).thenReturn(jedis);
      RedisStructuredBlobStorage redisStructuredBlobStorage =
          new RedisStructuredBlobStorage(this.blobStorageMock, this.redisPoolManagerMock, 1);

      redisStructuredBlobStorage.putBytes(
          TEXT_UNIT_DTOS_CACHE, FILE_NAME, content, Retention.MIN_1_DAY);

      verify(jedis).setex(eq(KEY_BYTES), eq((long) ONE_DAY_IN_SECONDS), eq(content));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.blobStorageMock, times(1)).put(eq(KEY), eq(content), eq(Retention.MIN_1_DAY));
    }
  }

  @Test
  public void testPutBytes_SavesValueToRedisPoolManagerForPermanentRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final byte[] content = "redisContent".getBytes(StandardCharsets.UTF_8);
      this.blobStorageMock = mock(BlobStorage.class);
      Jedis jedis = mock(Jedis.class);
      when(this.redisPoolManagerMock.getJedis()).thenReturn(jedis);
      RedisStructuredBlobStorage redisStructuredBlobStorage =
          new RedisStructuredBlobStorage(this.blobStorageMock, this.redisPoolManagerMock, 1);

      redisStructuredBlobStorage.putBytes(
          TEXT_UNIT_DTOS_CACHE, FILE_NAME, content, Retention.PERMANENT);

      verify(jedis).set(eq(KEY_BYTES), eq(content));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.blobStorageMock, times(1)).put(eq(KEY), eq(content), eq(Retention.PERMANENT));
    }
  }
}
