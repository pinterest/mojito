package com.box.l10n.mojito.service.evolve;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.evolve")
public class EvolveConfigurationProperties {
  private String apiUid;

  private String privateKey;

  private String url;

  private String apiPath;

  private Integer maxRetries;

  private Integer retryMinBackoffSecs;

  private Integer retryMaxBackoffSecs;

  private Long taskTimeoutInSeconds = 3600L;

  private String courseEvolveType;

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

  public Integer getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(Integer maxRetries) {
    this.maxRetries = maxRetries;
  }

  public Integer getRetryMinBackoffSecs() {
    return retryMinBackoffSecs;
  }

  public void setRetryMinBackoffSecs(Integer retryMinBackoffSecs) {
    this.retryMinBackoffSecs = retryMinBackoffSecs;
  }

  public Integer getRetryMaxBackoffSecs() {
    return retryMaxBackoffSecs;
  }

  public void setRetryMaxBackoffSecs(Integer retryMaxBackoffSecs) {
    this.retryMaxBackoffSecs = retryMaxBackoffSecs;
  }

  public Long getTaskTimeoutInSeconds() {
    return taskTimeoutInSeconds;
  }

  public void setTaskTimeoutInSeconds(Long taskTimeoutInSeconds) {
    this.taskTimeoutInSeconds = taskTimeoutInSeconds;
  }

  public String getCourseEvolveType() {
    return courseEvolveType;
  }

  public void setCourseEvolveType(String courseEvolveType) {
    this.courseEvolveType = courseEvolveType;
  }
}
