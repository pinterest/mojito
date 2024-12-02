package com.box.l10n.mojito.cli.apiclient;

import com.squareup.okhttp.OkHttpClient;
import jakarta.annotation.PostConstruct;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedApiClient extends ApiClient {

  private CookieManager cookieManager;

  @PostConstruct
  public void init() {
    this.cookieManager = this.getCookieManager();
    this.setHttpClient(this.getOkHttpClient());
  }

  private CookieManager getCookieManager() {
    CookieManager cookieManager = new CookieManager();
    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    CookieHandler.setDefault(cookieManager);
    return cookieManager;
  }

  private OkHttpClient getOkHttpClient() {
    OkHttpClient httpClient = new OkHttpClient();
    httpClient
        .interceptors()
        .add(new AuthenticatedApiInterceptor(this.cookieManager, this.getBasePath()));
    return httpClient;
  }
}
