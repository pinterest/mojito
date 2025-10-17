package com.box.l10n.mojito.monitoring;

import ch.qos.logback.classic.Logger;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ErrorLogMeterAppenderConfig {
  final MeterRegistry meterRegistry;

  public ErrorLogMeterAppenderConfig(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Bean
  public ErrorLogMeterAppender errorLogMeterAppender() {
    ErrorLogMeterAppender appender = new ErrorLogMeterAppender();
    appender.setMeterRegistry(meterRegistry);
    appender.setName("Mojito.Logs.ErrorCount");
    appender.start();
    return appender;
  }

  // Attach the appender after the context is fully initialized to avoid circular dependencies
  @Bean
  public ApplicationListener<ApplicationReadyEvent> errorLogMeterAppenderListener(
      ErrorLogMeterAppender errorLogMeterAppender) {
    return event -> {
      Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
      rootLogger.addAppender(errorLogMeterAppender);
    };
  }
}
