package com.box.l10n.mojito.sarif.model;

public class Driver {
  private String name;
  private String informationUri;
  private String version;

  public Driver(String name, String informationUri, String version) {
    this.name = name;
    this.informationUri = informationUri;
    this.version = version;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getInformationUri() {
    return informationUri;
  }

  public void setInformationUri(String informationUri) {
    this.informationUri = informationUri;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
