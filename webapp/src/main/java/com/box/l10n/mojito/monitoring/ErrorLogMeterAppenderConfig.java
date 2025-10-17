package com.box.l10n.mojito.monitoring;

import ch.qos.logback.classic.Logger;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ErrorLogMeterAppenderConfig {
  final MeterRegistry meterRegistry;

  public ErrorLogMeterAppenderConfig(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @PostConstruct
  public void setupErrorLogMeterAppender() {
    ErrorLogMeterAppender errorLogMeterAppender = new ErrorLogMeterAppender(meterRegistry);
    errorLogMeterAppender.start();

    Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.addAppender(errorLogMeterAppender);
  }
}
