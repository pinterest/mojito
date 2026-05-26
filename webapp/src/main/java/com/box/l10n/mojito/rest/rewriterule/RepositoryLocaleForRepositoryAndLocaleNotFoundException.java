package com.box.l10n.mojito.rest.rewriterule;

import com.box.l10n.mojito.rest.EntityWithIdNotFoundException;

public class RepositoryLocaleForRepositoryAndLocaleNotFoundException
    extends EntityWithIdNotFoundException {

  public RepositoryLocaleForRepositoryAndLocaleNotFoundException(Long repositoryId, Long localeId) {
    super("RepositoryLocale for localeId: " + localeId + " in repository", repositoryId);
  }
}
