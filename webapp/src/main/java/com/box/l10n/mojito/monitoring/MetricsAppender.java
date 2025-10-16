package com.box.l10n.mojito.monitoring;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.micrometer.core.instrument.MeterRegistry;

/** Logback appender that emits a metric when an error is logged. */
public class MetricsAppender extends AppenderBase<ILoggingEvent> {

  private final MeterRegistry meterRegistry;

  public MetricsAppender(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  protected void append(ILoggingEvent eventObject) {
    if (eventObject.getLevel() != Level.ERROR) return;
    meterRegistry.counter("logging.errors").increment();
  }
}
