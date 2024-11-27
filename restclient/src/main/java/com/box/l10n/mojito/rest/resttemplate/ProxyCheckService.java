package com.box.l10n.mojito.rest.resttemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ProxyCheckService implements InitializingBean {
  Logger logger = LoggerFactory.getLogger(ProxyCheckService.class);

  private boolean hasProxy;

  @Autowired ResttemplateConfig restTemplateConfig;

  @Autowired RestTemplate restTemplate;

  @Override
  public void afterPropertiesSet() {
    hasProxy = isProxyConfigured();
  }

  public boolean hasProxy() {
    return hasProxy;
  }

  protected boolean isProxyConfigured() {

    String testUrl =
        UriComponentsBuilder.newInstance()
            .scheme("http")
            .host(restTemplateConfig.getProxyHost())
            .port(restTemplateConfig.getProxyPort())
            .path("login")
            .build()
            .toUriString();
    logger.debug("Checking if proxy is configured with URL '{}'", testUrl);
    HttpHeaders headers = new HttpHeaders();
    headers.set("Host", restTemplateConfig.getHost());
    HttpEntity<String> httpEntity = new HttpEntity<>(null, headers);
    try {
      ResponseEntity<Void> response =
          restTemplate.exchange(testUrl, HttpMethod.GET, httpEntity, Void.class);
      logger.debug("Proxy login request response code {}", response.getStatusCode());
      return response.getStatusCode().is2xxSuccessful();
    } catch (Exception e) {
      logger.warn("Unable to reach service via proxy. Defaulting to direct access", e.getMessage());
      return false;
    }
  }
}
