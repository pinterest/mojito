package com.box.l10n.mojito.evolve;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.evolve")
public class EvolveConfigurationProperties {
  private String repositoryName;

  private String apiUid;

  private String privateKey;

  private String url;

  private String apiPath;

  private int maxRetries;

  private int retryMinBackoffSecs;

  private int retryMaxBackoffSecs;

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public String getApiUid() {
    return apiUid;
  }

  public void setApiUid(String apiUid) {
    this.apiUid = apiUid;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public void setPrivateKey(String privateKey) {
    this.privateKey = privateKey;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getApiPath() {
    return apiPath;
  }

  public void setApiPath(String apiPath) {
    this.apiPath = apiPath;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  public int getRetryMinBackoffSecs() {
    return retryMinBackoffSecs;
  }

  public void setRetryMinBackoffSecs(int retryMinBackoffSecs) {
    this.retryMinBackoffSecs = retryMinBackoffSecs;
  }

  public int getRetryMaxBackoffSecs() {
    return retryMaxBackoffSecs;
  }

  public void setRetryMaxBackoffSecs(int retryMaxBackoffSecs) {
    this.retryMaxBackoffSecs = retryMaxBackoffSecs;
  }
}
