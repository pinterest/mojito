package com.box.l10n.mojito.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class MetricsAppenderTest {

  private MeterRegistry meterRegistry;
  private Logger logger;

  @Before
  public void setUp() {
    meterRegistry = new SimpleMeterRegistry();

    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    MetricsAppender metricsAppender = new MetricsAppender(meterRegistry);
    metricsAppender.setContext(loggerContext);
    metricsAppender.start();

    logger = loggerContext.getLogger(MetricsAppenderTest.class);
    logger.addAppender(metricsAppender);
    logger.setLevel(Level.DEBUG);
  }

  @Test
  public void testErrorLogsIncrementCounter() {
    double initialCount = meterRegistry.counter("logging.errors").count();

    logger.error("Test error 1");
    logger.error("Test error 2");
    logger.error("Test error 3");

    double finalCount = meterRegistry.counter("logging.errors").count();
    assertThat(finalCount).isEqualTo(initialCount + 3);
  }

  @Test
  public void testNonErrorLogsDoNotIncrementCounter() {
    double initialCount = meterRegistry.counter("logging.errors").count();

    logger.info("Test info message");
    logger.warn("Test warn message");
    logger.debug("Test debug message");

    double finalCount = meterRegistry.counter("logging.errors").count();
    assertThat(finalCount).isEqualTo(initialCount);
  }
}
