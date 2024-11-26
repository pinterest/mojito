package com.box.l10n.mojito.cli.apiclient;

import com.box.l10n.mojito.cli.apiclient.ApiClient;
import com.box.l10n.mojito.cli.apiclient.ThirdPartyWsApi;
import com.box.l10n.mojito.cli.apiclient.UserWsApi;
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
