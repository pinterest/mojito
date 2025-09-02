package com.box.l10n.mojito.service.ai.translation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.quartz.QuartzService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.Trigger;

public class AITranslateTriggerListenerTest {

  @Mock QuartzService quartzService;

  @Mock Trigger triggerMock;

  @Mock MeterRegistry meterRegistryMock;

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
    when(triggerMock.getKey())
        .thenReturn(org.quartz.TriggerKey.triggerKey("triggerAiTranslateCronJob"));

    when(meterRegistryMock.counter(anyString()))
        .thenReturn(mock(io.micrometer.core.instrument.Counter.class));
  }

  @Test
  public void testVetoWhenUnderLimit() {
    AITranslateTriggerListener listener =
        new AITranslateTriggerListener(3, quartzService, meterRegistryMock);

    when(quartzService.getCurrentlyExecutingJobsCountForTrigger(anyString())).thenReturn(1);

    boolean veto =
        listener.vetoJobExecution(triggerMock, mock(org.quartz.JobExecutionContext.class));

    assertFalse(veto);
    verify(meterRegistryMock, times(0)).counter("AITranslateTriggerListener.jobVetoed");
  }

  @Test
  public void testVetoWhenOverLimit() {
    AITranslateTriggerListener listener =
        new AITranslateTriggerListener(3, quartzService, meterRegistryMock);

    when(quartzService.getCurrentlyExecutingJobsCountForTrigger(anyString())).thenReturn(4);

    boolean veto =
        listener.vetoJobExecution(triggerMock, mock(org.quartz.JobExecutionContext.class));

    assertTrue(veto);
    verify(meterRegistryMock, times(1)).counter("AITranslateTriggerListener.jobVetoed");
  }

  @Test
  public void testVetoWhenZero() {
    AITranslateTriggerListener listener =
        new AITranslateTriggerListener(0, quartzService, meterRegistryMock);

    when(quartzService.getCurrentlyExecutingJobsCountForTrigger(anyString())).thenReturn(10);

    boolean veto =
        listener.vetoJobExecution(triggerMock, mock(org.quartz.JobExecutionContext.class));

    assertFalse(veto);
    verify(meterRegistryMock, times(0)).counter("AITranslateTriggerListener.jobVetoed");
  }
}
