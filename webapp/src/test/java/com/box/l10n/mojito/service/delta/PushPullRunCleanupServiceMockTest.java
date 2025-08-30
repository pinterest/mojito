package com.box.l10n.mojito.service.delta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.box.l10n.mojito.service.pullrun.PullRunService;
import com.box.l10n.mojito.service.pushrun.PushRunService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PushPullRunCleanupServiceMockTest {
  @Mock PushRunService pushRunServiceMock;

  @Mock PullRunService pullRunServiceMock;

  PushPullRunCleanupServiceTestImpl pushPullRunCleanupService;

  @Captor ArgumentCaptor<ZonedDateTime> startDateTimeCaptor;

  @Captor ArgumentCaptor<ZonedDateTime> endDateTimeCaptor;

  AutoCloseable mocks;

  private PushPullRunCleanupConfigurationProperties configurationProperties;

  @BeforeEach
  public void setUp() {
    this.mocks = MockitoAnnotations.openMocks(this);

    this.configurationProperties = new PushPullRunCleanupConfigurationProperties();
    this.configurationProperties.setRetentionDuration(Duration.ofDays(15));
    this.configurationProperties.setDeleteBatchSize(1);
    this.configurationProperties.setMaxNumberOfBatches(1);
    this.configurationProperties.setExtraNumberOfWeeksToRetain(2);
  }

  @Test
  public void testCleanOldPushPullData_Uses2025_7_1AsCurrentDate() {
    LocalDate date = LocalDate.of(2025, 7, 1);
    ZonedDateTime currentDateTime = date.atStartOfDay(ZoneId.systemDefault());
    this.pushPullRunCleanupService =
        new PushPullRunCleanupServiceTestImpl(
            this.pushRunServiceMock, this.pullRunServiceMock, currentDateTime);

    this.pushPullRunCleanupService.cleanOldPushPullData(this.configurationProperties);

    verify(this.pushRunServiceMock)
        .deleteAllPushEntitiesOlderThan(
            eq(LocalDate.of(2025, 6, 2).atStartOfDay(ZoneId.systemDefault())), eq(1));
    verify(this.pullRunServiceMock)
        .deleteAllPullEntitiesOlderThan(
            eq(LocalDate.of(2025, 6, 2).atStartOfDay(ZoneId.systemDefault())), eq(1));
    verify(this.pushRunServiceMock, times(1))
        .deletePushRunsByAsset(
            this.startDateTimeCaptor.capture(),
            this.endDateTimeCaptor.capture(),
            anyInt(),
            anyInt());

    List<ZonedDateTime> expectedStartDates =
        List.of(LocalDate.of(2025, 6, 2).atStartOfDay(ZoneId.systemDefault()));
    assertEquals(expectedStartDates, startDateTimeCaptor.getAllValues());
    List<ZonedDateTime> expectedEndDates =
        List.of(LocalDate.of(2025, 6, 16).atStartOfDay(ZoneId.systemDefault()));
    assertEquals(expectedEndDates, endDateTimeCaptor.getAllValues());
  }

  @Test
  public void testCleanOldPushPullData_Uses2025_7_31AsCurrentDate() {
    LocalDate date = LocalDate.of(2025, 7, 31);
    ZonedDateTime currentDateTime = date.atStartOfDay(ZoneId.systemDefault());
    this.pushPullRunCleanupService =
        new PushPullRunCleanupServiceTestImpl(
            this.pushRunServiceMock, this.pullRunServiceMock, currentDateTime);

    this.pushPullRunCleanupService.cleanOldPushPullData(this.configurationProperties);

    verify(this.pushRunServiceMock)
        .deleteAllPushEntitiesOlderThan(
            eq(LocalDate.of(2025, 7, 2).atStartOfDay(ZoneId.systemDefault())), eq(1));
    verify(this.pullRunServiceMock)
        .deleteAllPullEntitiesOlderThan(
            eq(LocalDate.of(2025, 7, 2).atStartOfDay(ZoneId.systemDefault())), eq(1));
    verify(this.pushRunServiceMock, times(1))
        .deletePushRunsByAsset(
            this.startDateTimeCaptor.capture(),
            this.endDateTimeCaptor.capture(),
            anyInt(),
            anyInt());

    List<ZonedDateTime> expectedStartDates =
        List.of(LocalDate.of(2025, 7, 2).atStartOfDay(ZoneId.systemDefault()));
    assertEquals(expectedStartDates, startDateTimeCaptor.getAllValues());
    List<ZonedDateTime> expectedEndDates =
        List.of(LocalDate.of(2025, 7, 16).atStartOfDay(ZoneId.systemDefault()));
    assertEquals(expectedEndDates, endDateTimeCaptor.getAllValues());
  }

  @Test
  public void testCleanOldPushPullData_Uses2025_7_15AsCurrentDate() {
    LocalDate date = LocalDate.of(2025, 7, 15);
    ZonedDateTime currentDateTime = date.atStartOfDay(ZoneId.systemDefault());
    this.pushPullRunCleanupService =
        new PushPullRunCleanupServiceTestImpl(
            this.pushRunServiceMock, this.pullRunServiceMock, currentDateTime);

    this.pushPullRunCleanupService.cleanOldPushPullData(this.configurationProperties);

    verify(this.pushRunServiceMock)
        .deleteAllPushEntitiesOlderThan(
            eq(LocalDate.of(2025, 6, 16).atStartOfDay(ZoneId.systemDefault())), eq(1));
    verify(this.pullRunServiceMock)
        .deleteAllPullEntitiesOlderThan(
            eq(LocalDate.of(2025, 6, 16).atStartOfDay(ZoneId.systemDefault())), eq(1));
    verify(this.pushRunServiceMock, times(1))
        .deletePushRunsByAsset(
            this.startDateTimeCaptor.capture(),
            this.endDateTimeCaptor.capture(),
            anyInt(),
            anyInt());

    List<ZonedDateTime> expectedStartDates =
        List.of(LocalDate.of(2025, 6, 16).atStartOfDay(ZoneId.systemDefault()));
    assertEquals(expectedStartDates, startDateTimeCaptor.getAllValues());
    List<ZonedDateTime> expectedEndDates =
        List.of(LocalDate.of(2025, 6, 30).atStartOfDay(ZoneId.systemDefault()));
    assertEquals(expectedEndDates, endDateTimeCaptor.getAllValues());
  }

  @Test
  public void testCleanOldPushPullData_Retains3WeeksExtra() {
    LocalDate date = LocalDate.of(2025, 7, 22);
    ZonedDateTime currentDateTime = date.atStartOfDay(ZoneId.systemDefault());
    this.pushPullRunCleanupService =
        new PushPullRunCleanupServiceTestImpl(
            this.pushRunServiceMock, this.pullRunServiceMock, currentDateTime);
    this.configurationProperties.setExtraNumberOfWeeksToRetain(3);

    this.pushPullRunCleanupService.cleanOldPushPullData(this.configurationProperties);

    verify(this.pushRunServiceMock)
        .deleteAllPushEntitiesOlderThan(
            eq(LocalDate.of(2025, 6, 16).atStartOfDay(ZoneId.systemDefault())), eq(1));
    verify(this.pullRunServiceMock)
        .deleteAllPullEntitiesOlderThan(
            eq(LocalDate.of(2025, 6, 16).atStartOfDay(ZoneId.systemDefault())), eq(1));
    verify(this.pushRunServiceMock, times(1))
        .deletePushRunsByAsset(
            this.startDateTimeCaptor.capture(),
            this.endDateTimeCaptor.capture(),
            anyInt(),
            anyInt());

    List<ZonedDateTime> expectedStartDates =
        List.of(LocalDate.of(2025, 6, 16).atStartOfDay(ZoneId.systemDefault()));
    assertEquals(expectedStartDates, startDateTimeCaptor.getAllValues());
    List<ZonedDateTime> expectedEndDates =
        List.of(LocalDate.of(2025, 7, 7).atStartOfDay(ZoneId.systemDefault()));
    assertEquals(expectedEndDates, endDateTimeCaptor.getAllValues());
  }

  @Test
  public void testCleanOldPushPullData_Uses60DaysRetentionDuration() {
    LocalDate date = LocalDate.of(2025, 7, 18);
    ZonedDateTime currentDateTime = date.atStartOfDay(ZoneId.systemDefault());
    this.pushPullRunCleanupService =
        new PushPullRunCleanupServiceTestImpl(
            this.pushRunServiceMock, this.pullRunServiceMock, currentDateTime);
    this.configurationProperties.setRetentionDuration(Duration.ofDays(60));

    this.pushPullRunCleanupService.cleanOldPushPullData(this.configurationProperties);

    verify(this.pushRunServiceMock)
        .deleteAllPushEntitiesOlderThan(
            eq(LocalDate.of(2025, 5, 5).atStartOfDay(ZoneId.systemDefault())), eq(1));
    verify(this.pullRunServiceMock)
        .deleteAllPullEntitiesOlderThan(
            eq(LocalDate.of(2025, 5, 5).atStartOfDay(ZoneId.systemDefault())), eq(1));
    verify(this.pushRunServiceMock, times(1))
        .deletePushRunsByAsset(
            this.startDateTimeCaptor.capture(),
            this.endDateTimeCaptor.capture(),
            anyInt(),
            anyInt());

    List<ZonedDateTime> expectedStartDates =
        List.of(LocalDate.of(2025, 5, 5).atStartOfDay(ZoneId.systemDefault()));
    assertEquals(expectedStartDates, startDateTimeCaptor.getAllValues());
    List<ZonedDateTime> expectedEndDates =
        List.of(LocalDate.of(2025, 5, 19).atStartOfDay(ZoneId.systemDefault()));
    assertEquals(expectedEndDates, endDateTimeCaptor.getAllValues());
  }

  @Test
  public void testCleanOldPushPullData_DoesNotRunDeletePushRunsByAsset() {
    LocalDate date = LocalDate.of(2025, 7, 1);
    ZonedDateTime currentDateTime = date.atStartOfDay(ZoneId.systemDefault());
    this.pushPullRunCleanupService =
        new PushPullRunCleanupServiceTestImpl(
            this.pushRunServiceMock, this.pullRunServiceMock, currentDateTime);
    this.configurationProperties.setExtraNumberOfWeeksToRetain(0);

    this.pushPullRunCleanupService.cleanOldPushPullData(this.configurationProperties);

    verify(this.pushRunServiceMock)
        .deleteAllPushEntitiesOlderThan(
            eq(LocalDate.of(2025, 6, 16).atStartOfDay(ZoneId.systemDefault())), eq(1));
    verify(this.pullRunServiceMock)
        .deleteAllPullEntitiesOlderThan(
            eq(LocalDate.of(2025, 6, 16).atStartOfDay(ZoneId.systemDefault())), eq(1));
    verify(this.pushRunServiceMock, times(1))
        .deletePushRunsByAsset(
            this.startDateTimeCaptor.capture(),
            this.endDateTimeCaptor.capture(),
            anyInt(),
            anyInt());

    List<ZonedDateTime> expectedStartDates =
        List.of(LocalDate.of(2025, 6, 16).atStartOfDay(ZoneId.systemDefault()));
    assertEquals(expectedStartDates, startDateTimeCaptor.getAllValues());
    List<ZonedDateTime> expectedEndDates =
        List.of(LocalDate.of(2025, 6, 16).atStartOfDay(ZoneId.systemDefault()));
    assertEquals(expectedEndDates, endDateTimeCaptor.getAllValues());
  }

  @AfterEach
  void tearDown() throws Exception {
    this.mocks.close();
  }

  private static class PushPullRunCleanupServiceTestImpl extends PushPullRunCleanupService {
    private final ZonedDateTime currentDateTime;

    public PushPullRunCleanupServiceTestImpl(
        PushRunService pushRunService,
        PullRunService pullRunService,
        ZonedDateTime currentDateTime) {
      super(pushRunService, pullRunService);
      this.currentDateTime = currentDateTime;
    }

    @Override
    public ZonedDateTime getCurrentDateTime() {
      return currentDateTime;
    }
  }
}
