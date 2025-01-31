package com.box.l10n.mojito.cli.apiclient.exception;

public class RepositoryNotFoundException extends ResourceNotFoundException {
  public RepositoryNotFoundException(String message) {
    super(message);
  }
}
