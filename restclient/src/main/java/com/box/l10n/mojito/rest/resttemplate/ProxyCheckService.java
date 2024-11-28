package com.box.l10n.mojito.rest.resttemplate;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ProxyCheckService implements InitializingBean {
  Logger logger = LoggerFactory.getLogger(ProxyCheckService.class);

  private boolean hasProxy;

  @Autowired ResttemplateConfig restTemplateConfig;

  @Override
  public void afterPropertiesSet() {
    hasProxy = isProxyConfigured();
  }

  public boolean hasProxy() {
    return hasProxy;
  }

  private boolean isProxyConfigured() {

    String testUrl =
        UriComponentsBuilder.newInstance()
            .scheme(restTemplateConfig.getProxyScheme())
            .host(restTemplateConfig.getProxyHost())
            .port(restTemplateConfig.getProxyPort())
            .path("login")
            .build()
            .toUriString();
    logger.debug("Checking if proxy is configured with URL '{}'", testUrl);
    HttpHeaders headers = new HttpHeaders();
    headers.set("Host", restTemplateConfig.getHost());
    logger.debug("With headers {}", headers);
    HttpEntity<String> httpEntity = new HttpEntity<>(null, headers);
    try {
      RestTemplate tempRestTemplate = buildRestTemplate();
      ResponseEntity<Void> response =
          tempRestTemplate.exchange(testUrl, HttpMethod.GET, httpEntity, Void.class);
      logger.debug("Proxy login request response code {}", response.getStatusCode());
      return response.getStatusCode().is2xxSuccessful();
    } catch (Exception e) {
      logger.warn(
          "Proxy does not allow access to specified host. Falling back to directly accessing it",
          e);
      return false;
    }
  }

  /***
   * This is needed because the default RestTemplate does not allow the caller
   * to set the Host header
   */
  private RestTemplate buildRestTemplate() {
    CloseableHttpClient httpClient =
        HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom().build()).build();
    return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
  }
}
