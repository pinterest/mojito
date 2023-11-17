package com.box.l10n.mojito.rest.asset;

public class LocaleInfo {

  Long localeId;

  String bcp47Tag;

  String outputBcp47tag;

  String localizedContent;

  public Long getLocaleId() {
    return localeId;
  }

  public void setLocaleId(Long localeId) {
    this.localeId = localeId;
  }

  public String getBcp47Tag() {
    return bcp47Tag;
  }

  public void setBcp47Tag(String bcp47Tag) {
    this.bcp47Tag = bcp47Tag;
  }

  public String getOutputBcp47tag() {
    return outputBcp47tag;
  }

  public void setOutputBcp47tag(String outputBcp47tag) {
    this.outputBcp47tag = outputBcp47tag;
  }

  public String getLocalizedContent() {
    return localizedContent;
  }

  public void setLocalizedContent(String localizedContent) {
    this.localizedContent = localizedContent;
  }
}
