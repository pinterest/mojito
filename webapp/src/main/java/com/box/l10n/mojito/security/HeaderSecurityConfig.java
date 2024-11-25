package com.box.l10n.mojito.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration()
public class HeaderSecurityConfig {

  @Value("${l10n.spring.security.header.user.identifyingHeader:}")
  protected String userIdentifyingHeader;

  @Value("${l10n.spring.security.header.service.identifyingHeader:}")
  protected String serviceIdentifyingHeader;

  @Value("${l10n.spring.security.header.service.identifyingPrefix:}")
  protected String servicePrefix;

  @Value("${l10n.spring.security.header.service.delimiter:/}")
  protected String serviceDelimiter;

  public String getUserIdentifyingHeader() {
    return userIdentifyingHeader;
  }

  public void setUserIdentifyingHeader(String userIdentifyingHeader) {
    this.userIdentifyingHeader = userIdentifyingHeader;
  }

  public String getServiceIdentifyingHeader() {
    return serviceIdentifyingHeader;
  }

  public void setServiceIdentifyingHeader(String serviceIdentifyingHeader) {
    this.serviceIdentifyingHeader = serviceIdentifyingHeader;
  }

  public String getServicePrefix() {
    return servicePrefix;
  }

  public void setServicePrefix(String servicePrefix) {
    this.servicePrefix = servicePrefix;
  }

  public String getServiceDelimiter() {
    return serviceDelimiter;
  }

  public void setServiceDelimiter(String serviceDelimiter) {
    this.serviceDelimiter = serviceDelimiter;
  }
}
