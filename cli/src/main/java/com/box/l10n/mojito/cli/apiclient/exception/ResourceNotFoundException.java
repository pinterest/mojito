package com.box.l10n.mojito.cli.apiclient.exception;

public class ResourceNotFoundException extends RestClientException {
  public ResourceNotFoundException(String message) {
    super(message);
  }
}
