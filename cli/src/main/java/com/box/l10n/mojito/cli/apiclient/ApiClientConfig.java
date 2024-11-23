package com.box.l10n.mojito.cli;


import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.resttemplate.CookieStoreRestTemplate;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import io.swagger.client.ApiClient;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;

@Configuration
public class ApiClientConfig {
  @Autowired
  private ConsoleWriter consoleWriter;

  @Bean
  public ApiClient apiClient() {
    CookieHandler cookieHandler = new CookieManager(
        new CookieStore() {
          @Override
          public void add(URI uri, HttpCookie cookie) {

          }

          @Override
          public List<HttpCookie> get(URI uri) {
            return List.of();
          }

          @Override
          public List<HttpCookie> getCookies() {
            return List.of();
          }

          @Override
          public List<URI> getURIs() {
            return List.of();
          }

          @Override
          public boolean remove(URI uri, HttpCookie cookie) {
            return false;
          }

          @Override
          public boolean removeAll() {
            return false;
          }
        }, CookiePolicy.ACCEPT_ALL);
    OkHttpClient httpClient = new OkHttpClient();
    httpClient.setCookieHandler(cookieHandler);
    httpClient.interceptors().add(chain -> {
      final Request original = chain.request();
      final Request authorized = original.newBuilder()
          .addHeader("Cookie", "SESSION=MzRhMjg0NWUtZTc0Yi00M2Q4LTkzNjEtNjJhNGY2NWY4ODZj")
          .addHeader("X-CSRF-TOKEN", "h7eJAM4XAMrnALBAlnrOb3OEGMeUXISwFI2EtJuNNreRMbgT5NKwZapxOPjKNNYm9Ff6CULiNf-sZeadJOy1gvm0BdXzU40h")
          .build();
      ((CookieManager) httpClient.getCookieHandler()).getCookieStore().getCookies().forEach(cookie -> {
        consoleWriter.newLine().a(cookie.getName()).println();
      });
      return chain.proceed(authorized);
    });
    ApiClient apiClient = new ApiClient();
    apiClient.setHttpClient(httpClient);
    return apiClient;
  }
}
