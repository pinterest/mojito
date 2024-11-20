package com.box.l10n.mojito.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;

@Configuration()
@ConditionalOnExpression("'${l10n.security.authenticationType:}'.toUpperCase().contains('HEADER')")
public class HeaderSecurityConfig {

  @Value("${l10n.spring.security.header.user.identifyingHeader:}")
  public String userIdentifyingHeader;

  @Value("${l10n.spring.security.header.service.identifyingHeader:}")
  public String serviceIdentifyingHeader;
}
