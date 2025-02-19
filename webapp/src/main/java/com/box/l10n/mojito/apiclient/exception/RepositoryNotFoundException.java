package com.box.l10n.mojito.apiclient.exception;

public class RepositoryNotFoundException extends ResourceNotFoundException {
  public RepositoryNotFoundException(String message) {
    super(message);
  }
}
