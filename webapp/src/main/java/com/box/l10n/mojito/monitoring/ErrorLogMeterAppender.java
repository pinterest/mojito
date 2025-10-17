package com.box.l10n.mojito.monitoring;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public class ErrorLogMeterAppender extends AppenderBase<ILoggingEvent> {
  private final Counter errorCounter;

  public ErrorLogMeterAppender(MeterRegistry meterRegistry) {
    this.errorCounter = meterRegistry.counter("error_log_count");
  }

  @Override
  protected void append(ILoggingEvent eventObject) {
    if (eventObject.getLevel() == Level.ERROR && errorCounter != null) {
      errorCounter.increment();
    }
  }
}
