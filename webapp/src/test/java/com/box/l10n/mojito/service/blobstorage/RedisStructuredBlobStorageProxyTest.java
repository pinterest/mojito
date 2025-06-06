package com.box.l10n.mojito.service.blobstorage;

import static com.box.l10n.mojito.service.blobstorage.Retention.MIN_1_DAY;
import static com.box.l10n.mojito.service.blobstorage.Retention.PERMANENT;
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

import com.box.l10n.mojito.service.blobstorage.redis.RedisClient;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

public class RedisStructuredBlobStorageProxyTest {
  static final String FILE_NAME = "file.txt";

  static final String KEY = TEXT_UNIT_DTOS_CACHE.toString().toLowerCase() + "/" + FILE_NAME;

  static final byte[] KEY_BYTES = KEY.getBytes(StandardCharsets.UTF_8);

  BlobStorage blobStorageMock;

  RedisClient redisClientMock;

  ArgumentCaptor<Runnable> runnableArgumentCaptorCaptor = ArgumentCaptor.forClass(Runnable.class);

  @BeforeEach
  public void before() {
    this.redisClientMock = mock(RedisClient.class);
    this.blobStorageMock = mock(BlobStorage.class);
  }

  @Test
  public void testGetString_GetsValueFromBlobStorage() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String value = "s3Value";
      when(this.blobStorageMock.getString(anyString())).thenReturn(of(value));
      RedisStructuredBlobStorageProxy redisStructuredBlobStorageProxy =
          new RedisStructuredBlobStorageProxy(this.blobStorageMock, null, 1);

      Optional<String> result =
          redisStructuredBlobStorageProxy.getString(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(value, result.get());
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
      verify(this.blobStorageMock, times(0)).getRetention(anyString());
      verify(this.redisClientMock, times(0)).put(anyString(), anyString(), any(Retention.class));
    }
  }

  @Test
  public void testGetString_GetsValueFromRedisPoolManager() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String value = "redisValue";
      when(redisClientMock.get(anyString())).thenReturn(of(value));
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(this.blobStorageMock, this.redisClientMock, 1);

      Optional<String> result =
          redisStructuredBlobStorage.getString(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(value, result.get());
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
      verify(this.blobStorageMock, times(0)).getString(anyString());
      verify(this.blobStorageMock, times(0)).getRetention(anyString());
      verify(this.redisClientMock, times(0)).put(anyString(), anyString(), any(Retention.class));
    }
  }

  @Test
  public void testGetString_GetsValueFromBlobStorageInsteadOfRedisPoolManagerFor1DayRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String value = "s3Value";
      when(this.blobStorageMock.getString(anyString())).thenReturn(of(value));
      when(this.blobStorageMock.getRetention(anyString())).thenReturn(MIN_1_DAY);
      when(this.redisClientMock.get(anyString())).thenReturn(empty());
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(this.blobStorageMock, this.redisClientMock, 1);

      Optional<String> result =
          redisStructuredBlobStorage.getString(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(value, result.get());
      verify(this.redisClientMock, times(1)).get(eq(KEY));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.blobStorageMock).getRetention(eq(KEY));
      verify(this.redisClientMock, times(1)).put(eq(KEY), eq(value), eq(MIN_1_DAY));
    }
  }

  @Test
  public void
      testGetString_GetsValueFromBlobStorageInsteadOfRedisPoolManagerForPermanentRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String value = "s3Value";
      when(this.blobStorageMock.getString(anyString())).thenReturn(of(value));
      when(this.blobStorageMock.getRetention(anyString())).thenReturn(PERMANENT);
      when(this.redisClientMock.get(anyString())).thenReturn(empty());
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(this.blobStorageMock, this.redisClientMock, 1);

      Optional<String> result =
          redisStructuredBlobStorage.getString(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(value, result.get());
      verify(this.redisClientMock, times(1)).get(eq(KEY));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.blobStorageMock).getRetention(eq(KEY));
      verify(this.redisClientMock, times(1)).put(eq(KEY), eq(value), eq(PERMANENT));
    }
  }

  @Test
  public void testGetString_GetsNoValue() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      when(this.blobStorageMock.getString(anyString())).thenReturn(empty());
      when(this.redisClientMock.get(anyString())).thenReturn(empty());
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(this.blobStorageMock, this.redisClientMock, 1);

      Optional<String> result =
          redisStructuredBlobStorage.getString(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isEmpty());
      verify(this.redisClientMock, times(1)).get(eq(KEY));
      verify(this.blobStorageMock, times(1)).getString(eq(KEY));
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
      verify(this.blobStorageMock, times(0)).getRetention(anyString());
      verify(this.redisClientMock, times(0)).put(anyString(), anyString(), any(Retention.class));
    }
  }

  @Test
  public void testPut_SavesValueToBlobStorage() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String value = "s3Value";
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(this.blobStorageMock, null, 1);

      redisStructuredBlobStorage.put(TEXT_UNIT_DTOS_CACHE, FILE_NAME, value, Retention.MIN_1_DAY);

      verify(this.blobStorageMock).put(eq(KEY), eq(value), eq(Retention.MIN_1_DAY));
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
    }
  }

  @Test
  public void testPut_SavesValueToRedisPoolManagerFor1DayRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String value = "redisValue";
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(this.blobStorageMock, this.redisClientMock, 1);

      redisStructuredBlobStorage.put(TEXT_UNIT_DTOS_CACHE, FILE_NAME, value, MIN_1_DAY);

      verify(this.redisClientMock).put(eq(KEY), eq(value), eq(MIN_1_DAY));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.blobStorageMock, times(1)).put(eq(KEY), eq(value), eq(MIN_1_DAY));
    }
  }

  @Test
  public void testPut_SavesValueToRedisPoolManagerForPermanentRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final String value = "redisValue";
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(this.blobStorageMock, this.redisClientMock, 1);

      redisStructuredBlobStorage.put(TEXT_UNIT_DTOS_CACHE, FILE_NAME, value, PERMANENT);

      verify(this.redisClientMock).put(eq(KEY), eq(value), eq(PERMANENT));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.blobStorageMock, times(1)).put(eq(KEY), eq(value), eq(PERMANENT));
    }
  }

  @Test
  public void testDelete_DeletesFromBlobStorage() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(this.blobStorageMock, null, 1);

      redisStructuredBlobStorage.delete(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      verify(this.blobStorageMock).delete(eq(KEY));
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
      verify(this.redisClientMock, times(0)).delete(anyString());
    }
  }

  @Test
  public void testDelete_DeletesFromRedisPoolManager() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(this.blobStorageMock, this.redisClientMock, 1);

      redisStructuredBlobStorage.delete(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      verify(this.redisClientMock).delete(eq(KEY));
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
      final byte[] value = "s3Value".getBytes(StandardCharsets.UTF_8);
      when(this.blobStorageMock.getBytes(anyString())).thenReturn(of(value));
      RedisStructuredBlobStorageProxy redisStructuredBlobStorageProxy =
          new RedisStructuredBlobStorageProxy(this.blobStorageMock, null, 1);

      Optional<byte[]> result =
          redisStructuredBlobStorageProxy.getBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(value, result.get());
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
      verify(this.blobStorageMock, times(0)).getRetention(anyString());
      verify(this.redisClientMock, times(0))
          .put(anyString(), any(byte[].class), any(Retention.class));
    }
  }

  @Test
  public void testGetBytes_GetsValueFromRedisPoolManager() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final byte[] value = "redisValue".getBytes(StandardCharsets.UTF_8);
      when(redisClientMock.getBytes(anyString())).thenReturn(of(value));
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(this.blobStorageMock, this.redisClientMock, 1);

      Optional<byte[]> result =
          redisStructuredBlobStorage.getBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(value, result.get());
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
      verify(this.blobStorageMock, times(0)).getBytes(anyString());
      verify(this.blobStorageMock, times(0)).getRetention(anyString());
      verify(this.redisClientMock, times(0))
          .put(anyString(), any(byte[].class), any(Retention.class));
    }
  }

  @Test
  public void testGetBytes_GetsValueFromBlobStorageInsteadOfRedisPoolManagerFor1DayRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final byte[] value = "s3Value".getBytes(StandardCharsets.UTF_8);
      when(this.blobStorageMock.getBytes(anyString())).thenReturn(of(value));
      when(this.blobStorageMock.getRetention(anyString())).thenReturn(MIN_1_DAY);
      when(this.redisClientMock.getBytes(anyString())).thenReturn(empty());
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(this.blobStorageMock, this.redisClientMock, 1);

      Optional<byte[]> result =
          redisStructuredBlobStorage.getBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(value, result.get());
      verify(this.redisClientMock, times(1)).getBytes(eq(KEY));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.blobStorageMock).getRetention(eq(KEY));
      verify(this.redisClientMock, times(1)).put(eq(KEY), eq(value), eq(MIN_1_DAY));
    }
  }

  @Test
  public void
      testGetBytes_GetsValueFromBlobStorageInsteadOfRedisPoolManagerForPermanentRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final byte[] value = "s3Value".getBytes(StandardCharsets.UTF_8);
      when(this.blobStorageMock.getBytes(anyString())).thenReturn(of(value));
      when(this.blobStorageMock.getRetention(anyString())).thenReturn(PERMANENT);
      when(this.redisClientMock.getBytes(anyString())).thenReturn(empty());
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(this.blobStorageMock, this.redisClientMock, 1);

      Optional<byte[]> result =
          redisStructuredBlobStorage.getBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isPresent());
      assertEquals(value, result.get());
      verify(this.redisClientMock, times(1)).getBytes(eq(KEY));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.blobStorageMock).getRetention(eq(KEY));
      verify(this.redisClientMock, times(1)).put(eq(KEY), eq(value), eq(PERMANENT));
    }
  }

  @Test
  public void testGetBytes_GetsNoValue() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      when(this.blobStorageMock.getBytes(anyString())).thenReturn(empty());
      when(this.redisClientMock.getBytes(anyString())).thenReturn(empty());
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(this.blobStorageMock, this.redisClientMock, 1);

      Optional<byte[]> result =
          redisStructuredBlobStorage.getBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME);

      assertTrue(result.isEmpty());
      verify(this.redisClientMock, times(1)).getBytes(eq(KEY));
      verify(this.blobStorageMock, times(1)).getBytes(eq(KEY));
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
      verify(this.blobStorageMock, times(0)).getRetention(anyString());
      verify(this.redisClientMock, times(0))
          .put(anyString(), any(byte[].class), any(Retention.class));
    }
  }

  @Test
  public void testPutBytes_SavesValueToBlobStorage() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final byte[] value = "s3Value".getBytes(StandardCharsets.UTF_8);
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(this.blobStorageMock, null, 1);

      redisStructuredBlobStorage.putBytes(
          TEXT_UNIT_DTOS_CACHE, FILE_NAME, value, Retention.MIN_1_DAY);

      verify(this.blobStorageMock).put(eq(KEY), eq(value), eq(Retention.MIN_1_DAY));
      mocked.verify(
          () -> CompletableFuture.runAsync(any(Runnable.class), any(ExecutorService.class)),
          times(0));
    }
  }

  @Test
  public void testPutBytes_SavesValueToRedisPoolManagerFor1DayRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final byte[] value = "redisValue".getBytes(StandardCharsets.UTF_8);
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(this.blobStorageMock, this.redisClientMock, 1);

      redisStructuredBlobStorage.putBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME, value, MIN_1_DAY);

      verify(this.redisClientMock).put(eq(KEY), eq(value), eq(MIN_1_DAY));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.blobStorageMock, times(1)).put(eq(KEY), eq(value), eq(MIN_1_DAY));
    }
  }

  @Test
  public void testPutBytes_SavesValueToRedisPoolManagerForPermanentRetention() {
    try (MockedStatic<CompletableFuture> mocked = mockStatic(CompletableFuture.class)) {
      final byte[] value = "redisValue".getBytes(StandardCharsets.UTF_8);
      RedisStructuredBlobStorageProxy redisStructuredBlobStorage =
          new RedisStructuredBlobStorageProxy(this.blobStorageMock, this.redisClientMock, 1);

      redisStructuredBlobStorage.putBytes(TEXT_UNIT_DTOS_CACHE, FILE_NAME, value, PERMANENT);

      verify(this.redisClientMock).put(eq(KEY), eq(value), eq(PERMANENT));
      mocked.verify(
          () ->
              CompletableFuture.runAsync(
                  this.runnableArgumentCaptorCaptor.capture(), any(ExecutorService.class)),
          times(1));
      this.runnableArgumentCaptorCaptor.getValue().run();
      verify(this.blobStorageMock, times(1)).put(eq(KEY), eq(value), eq(PERMANENT));
    }
  }
}
