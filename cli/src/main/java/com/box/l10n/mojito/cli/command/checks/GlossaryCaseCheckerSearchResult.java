package com.box.l10n.mojito.cli.command.checks;

import java.util.List;

class GlossaryCaseCheckerSearchResult {
  List<String> failures;
  boolean isSuccess;
  boolean isMajorFailure;
  final String source;
  String name;

  public GlossaryCaseCheckerSearchResult(String source, String name) {
    this.source = source;
    this.isSuccess = true;
    this.name = name;
  }

  public boolean isSuccess() {
    return isSuccess;
  }

  public boolean isMajorFailure() {
    return isMajorFailure;
  }

  public List<String> getFailures() {
    return failures;
  }

  public String getSource() {
    return source;
  }

  public String getName() {
    return name;
  }
}
