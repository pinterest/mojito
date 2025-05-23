package com.box.l10n.mojito.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/** This service handles requests from users and services differently */
public class UserDetailServiceAuthWrapper
    implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

  Logger logger = LoggerFactory.getLogger(UserDetailServiceAuthWrapper.class);

  protected PrincipalDetailService userDetailsService = null;
  protected HeaderSecurityConfig headerSecurityConfig;
  protected ServiceIdentifierParser serviceIdentifierParser;

  public UserDetailServiceAuthWrapper(
      PrincipalDetailService userDetailsService,
      HeaderSecurityConfig headerSecurityConfig,
      ServiceIdentifierParser serviceIdentifierParser) {
    this.userDetailsService = userDetailsService;
    this.headerSecurityConfig = headerSecurityConfig;
    this.serviceIdentifierParser = serviceIdentifierParser;
  }

  @Override
  public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token)
      throws UsernameNotFoundException {
    String username = token.getName();
    boolean isService =
        headerSecurityConfig != null && username.contains(headerSecurityConfig.servicePrefix);
    logger.debug("User identifier: {}", username);
    logger.debug("Is using service authentication flow: {}", isService);
    if (isService) {
      String serviceName = serviceIdentifierParser.parseHeader(username);
      if (headerSecurityConfig.getForbiddenServicePattern() != null
          && headerSecurityConfig.getForbiddenServicePattern().matcher(serviceName).matches()) {
        logger.debug("Forbidden service identified: {}. Not allowing authentication", serviceName);
        // Cannot use a custom exception since Spring only expects a UsernameNotFoundException to
        // abort with a non 500 error code
        throw new UsernameNotFoundException("Forbidden service identified: " + serviceName);
      }

      return this.userDetailsService.loadServiceWithName(serviceName);
    } else {
      return this.userDetailsService.loadUserByUsername(username);
    }
  }
}
