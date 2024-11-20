package com.box.l10n.mojito.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;

// This must in sync with {@link
// com.box.l10n.mojito.security.SecurityConfig.AuthenticationType#HEADER}
@ConditionalOnExpression("'${l10n.security.authenticationType:}'.toUpperCase().contains('HEADER')")
@Configuration
class WebSecurityHeaderConfig {
  @Bean
  PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider() {
    PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider =
        new PreAuthenticatedAuthenticationProvider();
    UserDetailServiceAuthWrapper userDetailsByNameServiceWrapper =
        new UserDetailServiceAuthWrapper(getPrincipalDetailsServiceCreatePartial());
    preAuthenticatedAuthenticationProvider.setPreAuthenticatedUserDetailsService(
        userDetailsByNameServiceWrapper);
    return preAuthenticatedAuthenticationProvider;
  }

  @Bean
  protected PrincipalDetailService getPrincipalDetailsServiceCreatePartial() {
    return new UserDetailsServiceCreatePartialImpl();
  }
}
