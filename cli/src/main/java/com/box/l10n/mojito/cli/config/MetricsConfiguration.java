package com.box.l10n.mojito.cli.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to ensure MeterRegistry is available in the CLI application.
 *
 * <p>This provides a MeterRegistry bean for metrics collection even when running as a non-web
 * application (WebApplicationType.NONE). Spring Boot actuator auto-configuration will automatically
 * detect and register any configured metric registries (like StatsD) based on properties.
 */
@Configuration
public class MetricsConfiguration {

  /**
   * Creates a composite MeterRegistry that can hold multiple registries. Spring Boot
   * auto-configuration will automatically add configured registries (like StatsD) to this composite
   * based on properties and available dependencies.
   */
  @Bean
  @ConditionalOnMissingBean
  public MeterRegistry meterRegistry() {
    return new CompositeMeterRegistry();
  }
}
