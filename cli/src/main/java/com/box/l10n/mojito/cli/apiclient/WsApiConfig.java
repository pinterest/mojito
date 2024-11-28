package com.box.l10n.mojito.cli.apiclient;

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

  @Bean
  public QuartzWsApi quartzWsApi() {
    return new QuartzWsApi(this.apiClient);
  }

  @Bean
  public VirtualAssetWsApi virtualAssetWsApi() {
    return new VirtualAssetWsApi(this.apiClient);
  }

  @Bean
  public RepositoryWsApi repositoryWsApi() {
    return new RepositoryWsApi(this.apiClient);
  }

  @Bean
  public CommitWsApi commitWsApi() {
    return new CommitWsApi(this.apiClient);
  }

  @Bean
  public ImageWsApi imageWsApi() {
    return new ImageWsApi(this.apiClient);
  }

  @Bean
  public AiPromptWsApi aiPromptWsApi() {
    return new AiPromptWsApi(this.apiClient);
  }
}
