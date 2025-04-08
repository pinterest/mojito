package com.box.l10n.mojito.service.evolve;

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

  private Integer maxRetries;

  private Integer retryMinBackoffSecs;

  private Integer retryMaxBackoffSecs;

  private Long taskTimeout = 3600L;

  private String jobFailureNotificationTitle = "MOJITO | Evolve sync failed";

  private String jobUuid;

  private String jobCron;

  private String localeMapping;

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

  public Long getTaskTimeout() {
    return taskTimeout;
  }

  public void setTaskTimeout(Long taskTimeout) {
    this.taskTimeout = taskTimeout;
  }

  public String getJobFailureNotificationTitle() {
    return jobFailureNotificationTitle;
  }

  public void setJobFailureNotificationTitle(String jobFailureNotificationTitle) {
    this.jobFailureNotificationTitle = jobFailureNotificationTitle;
  }

  public String getJobUuid() {
    return jobUuid;
  }

  public void setJobUuid(String jobUuid) {
    this.jobUuid = jobUuid;
  }

  public String getJobCron() {
    return jobCron;
  }

  public void setJobCron(String jobCron) {
    this.jobCron = jobCron;
  }

  public String getLocaleMapping() {
    return localeMapping;
  }

  public void setLocaleMapping(String localeMapping) {
    this.localeMapping = localeMapping;
  }
}
