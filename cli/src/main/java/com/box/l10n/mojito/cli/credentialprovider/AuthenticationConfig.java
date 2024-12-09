package com.box.l10n.mojito.cli.credentialprovider;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "l10n.authentication")
public class AuthenticationConfig {
  String username = "admin";
  String password = "ChangeMe";

  CredentialProvider credentialProvider = CredentialProvider.CONFIG;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public CredentialProvider getCredentialProvider() {
    return credentialProvider;
  }

  public void setCredentialProvider(CredentialProvider credentialProvider) {
    this.credentialProvider = credentialProvider;
  }

  public enum CredentialProvider {
    CONSOLE,
    CONFIG
  }
}
