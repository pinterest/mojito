package com.box.l10n.mojito.monitoring;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.micrometer.core.instrument.MeterRegistry;
import javax.annotation.PostConstruct;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/** Attaches {@link MetricsAppender} to the root logger. */
@Configuration
public class MetricsAppenderConfiguration {

  private final MeterRegistry meterRegistry;

  @Autowired
  public MetricsAppenderConfiguration(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @PostConstruct
  public void setupMetricsAppender() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

    MetricsAppender metricsAppender = new MetricsAppender(meterRegistry);
    metricsAppender.setContext(loggerContext);
    metricsAppender.setName("METRICS");
    metricsAppender.start();

    Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.addAppender(metricsAppender);
  }
}
