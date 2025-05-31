package com.box.l10n.mojito.service.blobstorage.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public class RedisPoolManagerTest {

  private RedisConfigurationProperties redisProps;

  private ScheduledExecutorService scheduler;

  private RedisPoolManager redisPoolManager;

  @SystemStub EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @BeforeEach
  public void setUp() {
    scheduler = mock(ScheduledExecutorService.class);
    redisProps = new RedisConfigurationProperties();
    redisProps.setAccessKey("access");
    redisProps.setSecretKey("secret");
    environmentVariables.set("AWS_ACCESS_KEY_ID", "env-access-key");
    environmentVariables.set("AWS_SECRET_ACCESS_KEY", "env-secret-key");
    redisProps.setRedisUserId("user");
    redisProps.setReplicationGroupId("group");
    redisProps.setRegion("region");
    redisProps.setRedisEndpoint("localhost");
    try (MockedStatic<Executors> mocked = mockStatic(Executors.class)) {
      mocked.when(() -> Executors.newScheduledThreadPool(anyInt())).thenReturn(this.scheduler);
      redisPoolManager =
          spy(new RedisPoolManager(this.redisProps, new RedisPoolConfigurationProperties()));
    }
  }

  @Test
  public void testGetAwsCredentials_UsesCustomProperties() {
    AwsCredentials creds = this.redisPoolManager.getAwsCredentials();

    assertThat(creds.accessKeyId()).isEqualTo("access");
    assertThat(creds.secretAccessKey()).isEqualTo("secret");
  }

  @Test
  public void testGetAwsCredentials_UsesDefaultProperties() {
    this.redisProps.setAccessKey(null);
    this.redisProps.setSecretKey(null);

    AwsCredentials creds = this.redisPoolManager.getAwsCredentials();

    assertThat(creds.accessKeyId()).isEqualTo("env-access-key");
    assertThat(creds.secretAccessKey()).isEqualTo("env-secret-key");
  }

  @Test
  void testInit_RefreshesPoolAndSchedulesTask() {
    this.redisPoolManager.init();

    verify(this.redisPoolManager, times(1)).refreshJedisPool();
    assertThat(this.redisPoolManager.getJedisPool()).isNotNull();
    verify(this.scheduler, times(1))
        .scheduleAtFixedRate(any(Runnable.class), eq(10L), eq(10L), eq(TimeUnit.MINUTES));
  }

  @Test
  void testShutdown_ClosesScheduler() {
    redisPoolManager.shutdown();

    assertThat(redisPoolManager.getJedisPool()).isNull();
    verify(this.scheduler, times(1)).shutdown();
  }

  @Test
  void testShutdown_ClosesPoolAndScheduler() {
    this.redisPoolManager.refreshJedisPool();
    this.redisPoolManager.shutdown();

    assertThat(this.redisPoolManager.getJedisPool()).isNotNull();
    assertThat(this.redisPoolManager.getJedisPool().isClosed()).isTrue();
    verify(this.scheduler, times(1)).shutdown();
  }
}
