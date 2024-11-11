package com.box.l10n.mojito.react;

public class IntroducedIn {

  String url;
  String label;
  // Regex to filter branch names in the introduced in query
  String branchMatch;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getBranchMatch() {
    return branchMatch;
  }

  public void setBranchMatch(String branchMatch) {
    this.branchMatch = branchMatch;
  }
}
