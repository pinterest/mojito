package com.box.l10n.mojito.rest.rewriterule;

import java.text.MessageFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
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
