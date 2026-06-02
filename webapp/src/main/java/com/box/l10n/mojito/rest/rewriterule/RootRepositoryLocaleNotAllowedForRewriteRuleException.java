package com.box.l10n.mojito.rest.rewriterule;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RootRepositoryLocaleNotAllowedForRewriteRuleException extends RuntimeException {

  public RootRepositoryLocaleNotAllowedForRewriteRuleException(String message) {
    super(message);
  }
}
