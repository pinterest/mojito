package com.box.l10n.mojito.cli.credentialprovider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class CredentialProviderConfig {
  @Autowired AuthenticationConfig authenticationConfig;

  @Autowired SystemPromptCredentialProvider systemPromptCredentialProvider;

  @Autowired ConfigBasedCredentialProvider configBasedCredentialProvider;

  @Bean
  @Primary
  CredentialProvider getCredentialProvider() {
    if (AuthenticationConfig.CredentialProvider.CONSOLE.equals(
        authenticationConfig.getCredentialProvider())) {
      return this.systemPromptCredentialProvider;
    }
    return this.configBasedCredentialProvider;
  }
}
