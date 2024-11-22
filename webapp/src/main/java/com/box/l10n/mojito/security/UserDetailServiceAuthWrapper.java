package com.box.l10n.mojito.security;

import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/** This service handles requests from users and services differently */
public class UserDetailServiceAuthWrapper
    implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
  protected PrincipalDetailService userDetailsService = null;
  protected HeaderSecurityConfig headerSecurityConfig;

  public UserDetailServiceAuthWrapper(
      PrincipalDetailService userDetailsService, HeaderSecurityConfig headerSecurityConfig) {
    this.userDetailsService = userDetailsService;
    this.headerSecurityConfig = headerSecurityConfig;
  }

  @Override
  public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token)
      throws UsernameNotFoundException {
    String username = token.getName();
    boolean isService =
        headerSecurityConfig != null && username.contains(headerSecurityConfig.servicePrefix);
    if (isService) {
      return this.userDetailsService.loadServiceWithName(username);
    } else {
      return this.userDetailsService.loadUserByUsername(username);
    }
  }
}
