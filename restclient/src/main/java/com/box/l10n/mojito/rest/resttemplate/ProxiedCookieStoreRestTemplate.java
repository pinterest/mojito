package com.box.l10n.mojito.rest.resttemplate;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * A Rest Template with {@link HttpComponentsClientHttpRequestFactory} that exposes the cookie
 * store.
 *
 * @author wyau
 */
@Component
public class ProxiedCookieStoreRestTemplate extends RestTemplate {

  CookieStore cookieStore;

  public ProxiedCookieStoreRestTemplate() {
    super();
    cookieStore = new BasicCookieStore();
    setCookieStoreAndUpdateRequestFactory(cookieStore);
  }

  public void setCookieStoreAndUpdateRequestFactory(CookieStore cookieStore) {
    this.cookieStore = cookieStore;
    HttpClient hc =
        HttpClientBuilder.create()
            .setDefaultCookieStore(cookieStore)
            // we have to turn off auto redirect in the rest template because
            // when session expires, it will return a 302 and restTemplate
            // will automatically redirect to /login even before returning
            // the ClientHttpResponse in the interceptor
            .disableRedirectHandling()
            .build();

    setRequestFactory(new HttpComponentsClientHttpRequestFactory(hc));
  }

  public void configureProxy(
      boolean useProxy, String proxyHost, int proxyPort, HttpRequestInterceptor proxyInterceptor) {
    logger.debug("Configuring proxy for Rest Template. Enabled = {}", useProxy);
    HttpClientBuilder clientBuilder =
        HttpClients.custom().setDefaultCookieStore(cookieStore).disableRedirectHandling();

    if (useProxy) {
      logger.debug("Configuring proxy for Rest Template. Proxy settings {}:{}", proxyHost, proxyPort);
      if (proxyInterceptor != null) {
        HttpHost proxy = new HttpHost(proxyHost, proxyPort);
        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
        RequestConfig config = RequestConfig.custom().build();
        clientBuilder.setDefaultRequestConfig(config);
        clientBuilder.addRequestInterceptorFirst(proxyInterceptor);
        clientBuilder.setRoutePlanner(routePlanner);
      }
    }

    // Rebuild the request factory with or without proxy settings
    setRequestFactory(new HttpComponentsClientHttpRequestFactory(clientBuilder.build()));
  }

  public CookieStore getCookieStore() {
    return cookieStore;
  }
}
