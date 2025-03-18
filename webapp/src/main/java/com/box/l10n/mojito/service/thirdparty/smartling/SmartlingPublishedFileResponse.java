package com.box.l10n.mojito.service.thirdparty.smartling;

import com.box.l10n.mojito.android.strings.AndroidStringDocument;

public class SmartlingPublishedFileResponse {

  private String fileContent;

  private AndroidStringDocument androidStringDocument;

  public SmartlingPublishedFileResponse(
      String fileContent, AndroidStringDocument androidStringDocument) {
    this.fileContent = fileContent;
    this.androidStringDocument = androidStringDocument;
  }

  public String getFileContent() {
    return fileContent;
  }

  public void setFileContent(String fileContent) {
    this.fileContent = fileContent;
  }

  public AndroidStringDocument getAndroidStringDocument() {
    return androidStringDocument;
  }

  public void setAndroidStringDocument(AndroidStringDocument androidStringDocument) {
    this.androidStringDocument = androidStringDocument;
  }
}
