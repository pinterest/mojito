package com.box.l10n.mojito.service.evolve.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PictureDTO {
  @JsonProperty("mobile_target_url")
  private String mobileTargetUrl;

  @JsonProperty("target_url")
  private String targetUrl;

  public String getMobileTargetUrl() {
    return mobileTargetUrl;
  }

  public void setMobileTargetUrl(String mobileTargetUrl) {
    this.mobileTargetUrl = mobileTargetUrl;
  }

  public String getTargetUrl() {
    return targetUrl;
  }

  public void setTargetUrl(String targetUrl) {
    this.targetUrl = targetUrl;
  }
}
