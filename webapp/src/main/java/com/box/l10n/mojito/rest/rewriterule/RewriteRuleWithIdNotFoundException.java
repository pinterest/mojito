package com.box.l10n.mojito.rest.rewriterule;

import com.box.l10n.mojito.rest.EntityWithIdNotFoundException;

public class RewriteRuleWithIdNotFoundException extends EntityWithIdNotFoundException {

  public RewriteRuleWithIdNotFoundException(Long id) {
    super("RewriteRule", id);
  }
}
