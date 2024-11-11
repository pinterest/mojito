package com.box.l10n.mojito.react;

public class IntroducedIn {

  String url;
  String label;

  // Regex for only capturing specific branches that match regex, leave blank for all
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
