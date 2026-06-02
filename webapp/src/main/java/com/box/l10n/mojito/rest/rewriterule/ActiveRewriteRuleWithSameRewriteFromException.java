package com.box.l10n.mojito.rest.rewriterule;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ActiveRewriteRuleWithSameRewriteFromException extends RuntimeException {

  public ActiveRewriteRuleWithSameRewriteFromException(String rewriteFrom) {
    super(
        String.format(
            "Cannot activate rewrite rule. Another active rule already exists with rewriteFrom: %s",
            rewriteFrom));
  }
}
