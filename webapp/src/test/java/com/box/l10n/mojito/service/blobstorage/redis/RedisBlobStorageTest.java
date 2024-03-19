package com.box.l10n.mojito.service.blobstorage.redis;

import com.box.l10n.mojito.service.blobstorage.Retention;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RedisBlobStorageTest {

    @Mock
    private JedisPool jedisPool;

    @Mock
    private Jedis jedis;

    private RedisBlobStorage redisBlobStorage;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(jedisPool.getResource()).thenReturn(jedis);
        redisBlobStorage = new RedisBlobStorage(jedisPool);
    }

    @Test
    public void getBytes_returnsOptionalOfBytes_whenKeyExists() {
        String key = "key";
        byte[] value = "value".getBytes();
        when(jedis.get(key.getBytes())).thenReturn(value);

        Optional<byte[]> result = redisBlobStorage.getBytes(key);

        assertTrue(result.isPresent());
        assertArrayEquals(value, result.get());
    }

    @Test
    public void getBytes_returnsEmptyOptional_whenKeyDoesNotExist() {
        String key = "key";
        when(jedis.get(key.getBytes())).thenReturn(null);

        Optional<byte[]> result = redisBlobStorage.getBytes(key);

        assertFalse(result.isPresent());
    }

    @Test
    public void put_storesBytesInRedis_withCorrectKeyAndRetention() {
        String key = "key";
        byte[] value = "value".getBytes();
        Retention retention = Retention.MIN_1_DAY;

        redisBlobStorage.put(key, value, retention);

        verify(jedis).set(eq(key.getBytes()), eq(value), any());
    }

    @Test
    public void delete_removesKeyFromRedis() {
        String key = "key";

        redisBlobStorage.delete(key);

        verify(jedis).del(key.getBytes());
    }

    @Test
    public void exists_returnsTrue_whenKeyExists() {
        String key = "key";
        when(jedis.exists(key)).thenReturn(true);

        boolean result = redisBlobStorage.exists(key);

        assertTrue(result);
    }

    @Test
    public void exists_returnsFalse_whenKeyDoesNotExist() {
        String key = "key";
        when(jedis.exists(key)).thenReturn(false);

        boolean result = redisBlobStorage.exists(key);

        assertFalse(result);
    }
}
