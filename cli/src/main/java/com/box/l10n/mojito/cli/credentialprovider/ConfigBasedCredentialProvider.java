package com.box.l10n.mojito.cli.credentialprovider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConfigBasedCredentialProvider implements CredentialProvider {
  @Autowired AuthenticationConfig authenticationConfig;

  @Override
  public String getUsername() {
    return this.authenticationConfig.getUsername();
  }

  @Override
  public String getPassword() {
    return this.authenticationConfig.getPassword();
  }
}
