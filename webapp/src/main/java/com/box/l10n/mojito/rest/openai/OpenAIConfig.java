package com.box.l10n.mojito.rest.openai;

import com.box.l10n.mojito.openai.OpenAIClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "l10n.openai")
public class OpenAIConfig {

  boolean enabled;

  String apiKey;

  String host;

  Map<String, String> customHeaders = new HashMap<>();

  List<String> allowRestrictedHeaders;

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public void setCustomHeaders(Map<String, String> customHeaders) {
    this.customHeaders = customHeaders;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setAllowRestrictedHeaders(List<String> allowRestrictedHeaders) {
    this.allowRestrictedHeaders = allowRestrictedHeaders;
  }

  @Bean
  @ConditionalOnProperty(value = "l10n.openai.enabled", havingValue = "true")
  public OpenAIClient openAIClient() {
    if (allowRestrictedHeaders != null && !allowRestrictedHeaders.isEmpty()) {
      System.setProperty(
          "jdk.httpclient.allowRestrictedHeaders",
          allowRestrictedHeaders.stream().reduce((a, b) -> a + "," + b).get());
    }
    return OpenAIClient.builder().apiKey(apiKey).host(host).customHeaders(customHeaders).build();
  }
}
