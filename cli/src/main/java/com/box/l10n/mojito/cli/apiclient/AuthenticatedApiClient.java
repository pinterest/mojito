package com.box.l10n.mojito.cli.apiclient;

import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.cli.credentialprovider.CredentialProvider;
import com.squareup.okhttp.OkHttpClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedApiClient extends ApiClient {

  @Autowired CredentialProvider credentialProvider;

  @Autowired ConsoleWriter consoleWriter;

  @PostConstruct
  public void init() {
    this.setHttpClient(this.getOkHttpClient());
  }

  private OkHttpClient getOkHttpClient() {
    OkHttpClient httpClient = new OkHttpClient();
    httpClient
        .interceptors()
        .add(
            new AuthenticatedApiInterceptor(
                this.getBasePath(), this.credentialProvider, this.consoleWriter));
    return httpClient;
  }
}
