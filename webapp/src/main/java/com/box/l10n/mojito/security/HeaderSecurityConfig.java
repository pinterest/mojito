package com.box.l10n.mojito.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration()
public class HeaderSecurityConfig {

  @Value("${l10n.spring.security.header.user.identifyingHeader:}")
  public String userIdentifyingHeader;

  @Value("${l10n.spring.security.header.service.identifyingHeader:}")
  public String serviceIdentifyingHeader;

  @Value("${l10n.spring.security.header.service.identifyingPrefix:}")
  public String servicePrefix;

  @Value("${l10n.spring.security.header.service.delimiter:/}")
  public String serviceDelimiter;
}
