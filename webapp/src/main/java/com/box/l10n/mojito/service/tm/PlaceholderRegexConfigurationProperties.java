package com.box.l10n.mojito.service.tm;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n")
public class PlaceholderRegexConfigurationProperties {
  private Map<String, String> placeholderRegExps = new HashMap<>();

  public Map<String, String> getPlaceholderRegExps() {
    return placeholderRegExps;
  }

  public void setPlaceholderRegExps(Map<String, String> placeholderRegExps) {
    this.placeholderRegExps = placeholderRegExps;
  }
}
