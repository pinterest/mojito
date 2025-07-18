package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.service.tm.TextUnitForBatchMatcher;

public class ThirdPartyTextUnit implements TextUnitForBatchMatcher {

  /** Id in the third party TMS */
  String id;

  /** The tmTextUnitId in Mojito (should be extracted from information from the third party TMS) */
  Long tmTextUnitId;

  /** The asset path in Mojito (should be extracted from information from the third party TMS) */
  String assetPath;

  /**
   * The text unit name in Mojito (should be extracted from information from the third party TMS)
   */
  String name;

  /**
   * The source in Mojito (should be extracted from information from the third party TMS if
   * possible)
   */
  String content;

  /**
   * If the name is a plural prefix (instead of the full text unit name) and so the entry map to a
   * plural string in Mojito
   */
  boolean namePluralPrefix;

  String uploadedFileUri;

  String comment;

  String source;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getAssetPath() {
    return assetPath;
  }

  public void setAssetPath(String assetName) {
    this.assetPath = assetName;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  @Override
  public Long getTmTextUnitId() {
    return tmTextUnitId;
  }

  public void setTmTextUnitId(Long tmTextUnitId) {
    this.tmTextUnitId = tmTextUnitId;
  }

  @Override
  public boolean isNamePluralPrefix() {
    return namePluralPrefix;
  }

  public void setNamePluralPrefix(boolean namePluralPrefix) {
    this.namePluralPrefix = namePluralPrefix;
  }

  public String getUploadedFileUri() {
    return uploadedFileUri;
  }

  public void setUploadedFileUri(String uploadedFileUri) {
    this.uploadedFileUri = uploadedFileUri;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  @Override
  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }
}
