package com.box.l10n.mojito.rest.rewriterule;

import java.text.MessageFormat;

public class RepositoryLocaleForRepositoryAndLocaleNotFoundException extends Exception {

  public RepositoryLocaleForRepositoryAndLocaleNotFoundException(Long repositoryId, Long localeId) {
    super(getMessage(repositoryId, localeId));
  }

  static String getMessage(Long repositoryId, Long localeId) {
    return MessageFormat.format(
        "Repository locale for locale id: {0} could not be found in repository with id: {1}",
        localeId, repositoryId);
  }
}
