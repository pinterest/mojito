package com.box.l10n.mojito.smartling;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SmartlingOAuthAccessResponse {

  @JsonProperty("response")
  private Response response;

  public Response getResponse() {
    return response;
  }

  public void setResponse(Response response) {
    this.response = response;
  }

  static class Response {
    @JsonProperty("code")
    private String code;

    @JsonProperty("data")
    private TokenData data;

    @JsonProperty("errors")
    private ErrorDetails error;

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }

    public TokenData getData() {
      return data;
    }

    public void setData(TokenData data) {
      this.data = data;
    }

    public ErrorDetails getError() {
      return error;
    }

    public void setError(ErrorDetails error) {
      this.error = error;
    }
  }

  static class TokenData {
    @JsonProperty("accessToken")
    private String accessToken;

    @JsonProperty("refreshToken")
    private String refreshToken;

    @JsonProperty("expiresIn")
    private int expiresIn;

    @JsonProperty("refreshExpiresIn")
    private int refreshExpiresIn;

    @JsonProperty("tokenType")
    private String tokenType;

    @JsonIgnore private long refreshExpiryTime;

    @JsonIgnore private long tokenExpiryTime;

    public String getAccessToken() {
      return accessToken;
    }

    public void setAccessToken(String accessToken) {
      this.accessToken = accessToken;
    }

    public String getRefreshToken() {
      return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
      this.refreshToken = refreshToken;
    }

    public int getExpiresIn() {
      return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
      this.expiresIn = expiresIn;
    }

    public int getRefreshExpiresIn() {
      return refreshExpiresIn;
    }

    public void setRefreshExpiresIn(int refreshExpiresIn) {
      this.refreshExpiresIn = refreshExpiresIn;
    }

    public String getTokenType() {
      return tokenType;
    }

    public void setTokenType(String tokenType) {
      this.tokenType = tokenType;
    }

    public long getRefreshExpiryTime() {
      return refreshExpiryTime;
    }

    public void setRefreshExpiryTime(long refreshExpiryTime) {
      this.refreshExpiryTime = refreshExpiryTime;
    }

    public long getTokenExpiryTime() {
      return tokenExpiryTime;
    }

    public void setTokenExpiryTime(long tokenExpiryTime) {
      this.tokenExpiryTime = tokenExpiryTime;
    }
  }

  static class ErrorDetails {
    @JsonProperty("details")
    private Object details;

    @JsonProperty("key")
    private String key;

    @JsonProperty("message")
    private String message;

    public Object getDetails() {
      return details;
    }

    public void setDetails(Object details) {
      this.details = details;
    }

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }
  }
}