package com.box.l10n.mojito.cli.apiclient;

import com.squareup.okhttp.OkHttpClient;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedApiClient extends ApiClient {

  private final CookieManager cookieManager;

  public AuthenticatedApiClient() {
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
    String cookieName =
        this.getBasePath().equals("http://localhost:8080") ? "SESSION" : "JSESSIONID";
    OkHttpClient httpClient = new OkHttpClient();
    httpClient
        .interceptors()
        .add(new AuthenticatedApiInterceptor(this.cookieManager, cookieName, this.getBasePath()));
    return httpClient;
  }
}
