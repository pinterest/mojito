package com.box.l10n.mojito.converter;

import java.time.Period;
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
// TODO(jean) JSR310 - Period exist in both Joda and JSR .. but it is not the same. need to check PeriodDuration
public class PeriodConverter implements Converter<String, Period> {

  @Override
  public Period convert(String source) {
    long sourceAsLong = Long.valueOf(source);
    // TODO(jean) JSR310 - update
    return new Period(sourceAsLong);
  }
}
