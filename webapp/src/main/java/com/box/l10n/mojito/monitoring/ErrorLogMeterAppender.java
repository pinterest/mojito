package com.box.l10n.mojito.monitoring;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public class ErrorLogMeterAppender extends AppenderBase<ILoggingEvent> {
  private Counter errorCounter;
  private String metricName = "error_log_count";

  public void setMeterRegistry(MeterRegistry meterRegistry) {
    this.errorCounter =
        Counter.builder(metricName).description("Number of error logs").register(meterRegistry);
  }

  public void setMetricName(String metricName) {
    this.metricName = metricName;
  }

  @Override
  protected void append(ILoggingEvent eventObject) {
    if (eventObject.getLevel() == Level.ERROR && errorCounter != null) {
      errorCounter.increment();
    }
  }
}
