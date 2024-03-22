package com.box.l10n.mojito.smartling;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;

public class SmartlingOAuth2TokenService {

  private static Logger logger =
      org.slf4j.LoggerFactory.getLogger(SmartlingOAuth2TokenService.class);

  private final String accessTokenUrl;

  private final String refreshTokenUrl;

  private final String clientId;

  private final String clientSecret;

  private final ObjectMapper objectMapper;

  protected HttpClient client = HttpClient.newHttpClient();

  private SmartlingOAuthAccessResponse.TokenData smartlingOAuthAccessToken;

  public SmartlingOAuth2TokenService(
      String clientId, String clientSecret, String accessTokenUrl, String refreshTokenUrl) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.accessTokenUrl = accessTokenUrl;
    this.refreshTokenUrl = refreshTokenUrl;
    this.objectMapper = new ObjectMapper();
  }

  public String getAccessToken() {
    if (isRefreshRequired()) {
      smartlingOAuthAccessToken = requestAccessToken();
    }
    return smartlingOAuthAccessToken.getAccessToken();
  }

  private boolean isTokenExpired() {
    return smartlingOAuthAccessToken == null
        || smartlingOAuthAccessToken.getTokenExpiryTime() <= System.currentTimeMillis() + 3000;
  }

  private boolean isRefreshTokenExpired() {
    return smartlingOAuthAccessToken == null
        || smartlingOAuthAccessToken.getRefreshExpiryTime() <= System.currentTimeMillis() + 3000;
  }

  private boolean isRefreshRequired() {
    // check if the access or refresh tokens are about to expire
    return isTokenExpired() || isRefreshTokenExpired();
  }

  private synchronized SmartlingOAuthAccessResponse.TokenData requestAccessToken() {
    if (isRefreshTokenExpired()) {
      requestNewAccessToken();
    } else if (isTokenExpired()) {
      refreshAccessToken();
    }

    return smartlingOAuthAccessToken;
  }

  private void requestNewAccessToken() {
    ClientAuthDetails authDetails = new ClientAuthDetails(clientId, clientSecret);
    try {
      String json = objectMapper.writeValueAsString(authDetails);
      retrieveToken(accessTokenUrl, json);
    } catch (URISyntaxException | IOException | InterruptedException e) {
      logger.error("Exception occurred retrieving OAuth token from Smartling: ", e);
      throw new SmartlingOAuthTokenException(
          "Exception occurred retrieving OAuth token from Smartling: ", e);
    }
  }

  private void refreshAccessToken() {
    RefreshRequest refreshRequest = new RefreshRequest(smartlingOAuthAccessToken.getRefreshToken());
    try {
      String json = objectMapper.writeValueAsString(refreshRequest);
      retrieveToken(refreshTokenUrl, json);
    } catch (URISyntaxException | IOException | InterruptedException e) {
      logger.error("Exception occurred refreshing OAuth token with Smartling: ", e);
      throw new SmartlingOAuthTokenException(
          "Exception occurred refreshing OAuth token with Smartling: ", e);
    }
  }

  private void retrieveToken(String authUrl, String json)
      throws URISyntaxException, IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(new URI(authUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    SmartlingOAuthAccessResponse smartlingOAuthAccessResponse =
        objectMapper.readValue(response.body(), SmartlingOAuthAccessResponse.class);
    smartlingOAuthAccessToken = smartlingOAuthAccessResponse.getResponse().getData();
    smartlingOAuthAccessToken.setTokenExpiryTime(
        System.currentTimeMillis() + (smartlingOAuthAccessToken.getExpiresIn() * 1000));
    smartlingOAuthAccessToken.setRefreshExpiryTime(
        System.currentTimeMillis() + (smartlingOAuthAccessToken.getRefreshExpiresIn() * 1000));
  }

  static class ClientAuthDetails {
    @JsonProperty("userIdentifier")
    String clientId;

    @JsonProperty("userSecret")
    String clientSecret;

    public ClientAuthDetails() {}

    public ClientAuthDetails(String clientId, String clientSecret) {
      this.clientId = clientId;
      this.clientSecret = clientSecret;
    }

    public String getClientId() {
      return clientId;
    }

    public void setClientId(String clientId) {
      this.clientId = clientId;
    }

    public String getClientSecret() {
      return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
      this.clientSecret = clientSecret;
    }
  }

  static class RefreshRequest {

    @JsonProperty("refreshToken")
    private String refreshToken;

    public RefreshRequest() {}

    public RefreshRequest(String refreshToken) {
      this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
      return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
      this.refreshToken = refreshToken;
    }
  }
}
