package com.box.l10n.mojito.apiclient.resttemplate;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "l10n.resttemplate")
public class ResttemplateConfig {

  String host = "localhost";
  Integer port = 8080;
  String scheme = "http";
  String contextPath = "";
  boolean usesLoginAuthentication = true;

  ResttemplateConfig.Authentication authentication = new ResttemplateConfig.Authentication();

  public static class Authentication {

    String username = "admin";
    String password = "ChangeMe";

    ResttemplateConfig.Authentication.CredentialProvider credentialProvider =
        ResttemplateConfig.Authentication.CredentialProvider.CONFIG;

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

    public ResttemplateConfig.Authentication.CredentialProvider getCredentialProvider() {
      return credentialProvider;
    }

    public void setCredentialProvider(
        ResttemplateConfig.Authentication.CredentialProvider credentialProvider) {
      this.credentialProvider = credentialProvider;
    }

    public enum CredentialProvider {
      CONSOLE,
      CONFIG
    }
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getScheme() {
    return scheme;
  }

  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  public ResttemplateConfig.Authentication getAuthentication() {
    return authentication;
  }

  public void setAuthentication(ResttemplateConfig.Authentication authentication) {
    this.authentication = authentication;
  }

  public String getContextPath() {
    return contextPath;
  }

  public void setContextPath(String contextPath) {
    this.contextPath = contextPath;
  }

  public boolean usesLoginAuthentication() {
    return usesLoginAuthentication;
  }

  public void setUsesLoginAuthentication(boolean usesLoginAuthentication) {
    this.usesLoginAuthentication = usesLoginAuthentication;
  }
}
