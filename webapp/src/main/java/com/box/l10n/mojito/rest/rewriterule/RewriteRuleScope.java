package com.box.l10n.mojito.rest.rewriterule;

public enum RewriteRuleScope {
  REPOSITORY("repository"),
  GLOBAL("global");

  private final String value;

  RewriteRuleScope(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static RewriteRuleScope fromValue(String value) {
    if (value == null) {
      return null;
    }

    for (RewriteRuleScope scope : values()) {
      if (scope.value.equalsIgnoreCase(value.trim())) {
        return scope;
      }
    }

    throw new IllegalArgumentException("Invalid scope. Supported values are: repository, global");
  }
}
