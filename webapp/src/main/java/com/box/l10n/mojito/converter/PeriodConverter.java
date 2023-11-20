package com.box.l10n.mojito.converter;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converts a String to a @{link Period}.
 *
 * @author jaurambault
 */
@Component
@ConfigurationPropertiesBinding
public class PeriodConverter implements Converter<String, Duration> {

  @Override
  public Duration convert(String source) {
    long sourceAsLong = Long.parseLong(source);
    return Duration.ofMillis(sourceAsLong);
  }
}
