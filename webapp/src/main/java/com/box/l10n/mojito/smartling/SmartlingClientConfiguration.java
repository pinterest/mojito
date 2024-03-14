package com.box.l10n.mojito.smartling;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

/**
 * @author jaurambault
 */
@Configuration
@ConfigurationProperties("l10n.smartling")
public class SmartlingClientConfiguration {

  static Logger resttemplateLogger =
      LoggerFactory.getLogger(
          SmartlingClientConfiguration.class.getPackage().getName() + ".Resttemplate");

  String baseUri = "https://api.smartling.com/";
  String accessTokenUri = "https://api.smartling.com/auth-api/v2/authenticate";
  String refreshTokenUri = "https://api.smartling.com/auth-api/v2/authenticate/refresh";

  String clientID;
  String clientSecret;
  int retryMaxAttempts = 10;
  int retryMinDurationSeconds = 1;
  int retryMaxBackoffDurationSeconds = 60;

  @ConditionalOnProperty("l10n.smartling.clientID")
  @Bean
  public SmartlingClient getSmartlingClient() {
    RetryBackoffSpec retryConfiguration =
        Retry.backoff(getRetryMaxAttempts(), Duration.ofSeconds(getRetryMinDurationSeconds()))
            .maxBackoff(Duration.ofSeconds(getRetryMaxBackoffDurationSeconds()));

    return new SmartlingClient(webClient(), retryConfiguration);
  }

  @Bean
  public WebClient webClient() {

    ClientRegistration clientRegistration = smartlingClientCredentials();
    ClientRegistrationRepository clientRegistrationRepository =
        new InMemoryClientRegistrationRepository(clientRegistration);
    OAuth2AuthorizedClientProvider authorizedClientProvider =
        OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().refreshToken().build();
    OAuth2AuthorizedClientService authorizedClientService =
        new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
        new AuthorizedClientServiceOAuth2AuthorizedClientManager(
            clientRegistrationRepository, authorizedClientService);
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
    ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2client =
        new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
    WebClient webClient = WebClient.builder().baseUrl(baseUri).filter(oauth2client).build();

    return webClient;
  }

  @Bean
  public ClientRegistration smartlingClientCredentials() {
    return ClientRegistration.withRegistrationId("Smartling")
        .clientId(clientID)
        .clientSecret(clientSecret)
        .tokenUri(accessTokenUri)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .build();
  }

  public String getAccessTokenUri() {
    return accessTokenUri;
  }

  public void setAccessTokenUri(String accessTokenUri) {
    this.accessTokenUri = accessTokenUri;
  }

  public String getRefreshTokenUri() {
    return refreshTokenUri;
  }

  public void setRefreshTokenUri(String refreshTokenUri) {
    this.refreshTokenUri = refreshTokenUri;
  }

  public String getClientID() {
    return clientID;
  }

  public void setClientID(String clientID) {
    this.clientID = clientID;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public int getRetryMaxAttempts() {
    return retryMaxAttempts;
  }

  public void setRetryMaxAttempts(int retryMaxAttempts) {
    this.retryMaxAttempts = retryMaxAttempts;
  }

  public int getRetryMinDurationSeconds() {
    return retryMinDurationSeconds;
  }

  public void setRetryMinDurationSeconds(int retryMinDurationSeconds) {
    this.retryMinDurationSeconds = retryMinDurationSeconds;
  }

  public int getRetryMaxBackoffDurationSeconds() {
    return retryMaxBackoffDurationSeconds;
  }

  public void setRetryMaxBackoffDurationSeconds(int retryMaxBackoffDurationSeconds) {
    this.retryMaxBackoffDurationSeconds = retryMaxBackoffDurationSeconds;
  }
}
