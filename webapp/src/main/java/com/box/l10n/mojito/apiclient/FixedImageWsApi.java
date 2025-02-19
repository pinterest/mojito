package com.box.l10n.mojito.apiclient;

import com.box.l10n.mojito.apiclient.resttemplate.AuthenticatedRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
public class FixedImageWsApi {
  private final AuthenticatedRestTemplate authenticatedRestTemplate;

  @Autowired
  public FixedImageWsApi(AuthenticatedRestTemplate authenticatedRestTemplate) {
    this.authenticatedRestTemplate = authenticatedRestTemplate;
  }

  public void uploadImage(byte[] body, String imageName) throws RestClientException {
    this.authenticatedRestTemplate.put("/api/images/" + imageName, body);
  }
}
