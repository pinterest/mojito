package com.box.l10n.mojito.cli.apiclient;

import io.swagger.client.ApiClient;
import io.swagger.client.api.ThirdPartyWsApi;
import io.swagger.client.api.UserWsApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WsApiConfig {
  @Autowired private ApiClient apiClient;

  @Bean
  public ThirdPartyWsApi thirdPartyWsApi() {
    return new ThirdPartyWsApi(this.apiClient);
  }

  @Bean
  public UserWsApi userWsApi() {
    return new UserWsApi(this.apiClient);
  }
}
