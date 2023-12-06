package com.box.l10n.mojito.converter;

import java.time.LocalTime;

import com.box.l10n.mojito.JSR310Migration;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converts a String to a {@link LocalTime}.
 *
 * @author jaurambault
 */
@Component
@ConfigurationPropertiesBinding
// TODO(jean) 2-JSR310 - replaced by since it standard types converter might not be required
public class LocalTimeConverter implements Converter<String, LocalTime> {

  @Override
  public LocalTime convert(String source) {
    return JSR310Migration.newLocalTimeWithString(source);
  }
}
