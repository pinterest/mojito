package com.box.l10n.mojito.cli.apiclient;

import com.squareup.okhttp.OkHttpClient;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedApiClient extends ApiClient {

  @PostConstruct
  public void init() {
    this.setHttpClient(this.getOkHttpClient());
  }

  private OkHttpClient getOkHttpClient() {
    OkHttpClient httpClient = new OkHttpClient();
    httpClient.interceptors().add(new AuthenticatedApiInterceptor(this.getBasePath()));
    return httpClient;
  }
}
