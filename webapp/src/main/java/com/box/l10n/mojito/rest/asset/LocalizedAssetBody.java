package com.box.l10n.mojito.rest.asset;

import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.okapi.InheritanceMode;
import com.box.l10n.mojito.okapi.Status;
import com.box.l10n.mojito.service.appender.AppendedAssetBlobStorage;
import java.util.List;

/**
 * @author wyau
 */
public class LocalizedAssetBody {

  /** Asset id */
  Long assetId;

  /** Locale id */
  Long localeId;

  /** bcp47 tag of the locale content */
  String bcp47Tag;

  /**
   * content to be localized (similar to the asset content stored in TMS) in the request and
   * localized asset in the response.
   */
  String content;

  /**
   * Optional, can be null. Allows to generate the file for a bcp47 tag that is different from the
   * repository locale (which is still used to fetch the translations). This can be used to generate
   * a file with tag "fr" even if the translations are stored with fr-FR repository locale.
   */
  String outputBcp47tag;

  /**
   * Optional, can be null. Allows to specify a specific Okapi filter to use to process the asset
   */
  FilterConfigIdOverride filterConfigIdOverride;

  /** Optional, can be null. To pass options to the Okapi filter */
  List<String> filterOptions;

  /**
   * Optional, can be null. If a name is provided, a pull run will be recored when the file is
   * generated
   */
  String pullRunName;

  InheritanceMode inheritanceMode = InheritanceMode.USE_PARENT;

  Status status = Status.ALL;

  /**
   * Unique identifier used to store the branches appended to the source content in blob storage.
   * The identifier is used in the commit step to fetch all the branches in the blob storage and
   * link them to the commit. See {@link AppendedAssetBlobStorage}
   */
  String appendBranchTextUnitsId;

  public LocalizedAssetBody() {}

  public LocalizedAssetBody(String bcp47Tag, String content) {
    this.bcp47Tag = bcp47Tag;
    this.content = content;
  }

  public Long getAssetId() {
    return assetId;
  }

  public void setAssetId(Long assetId) {
    this.assetId = assetId;
  }

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

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getOutputBcp47tag() {
    return outputBcp47tag;
  }

  public void setOutputBcp47tag(String outputBcp47tag) {
    this.outputBcp47tag = outputBcp47tag;
  }

  public FilterConfigIdOverride getFilterConfigIdOverride() {
    return filterConfigIdOverride;
  }

  public void setFilterConfigIdOverride(FilterConfigIdOverride filterConfigIdOverride) {
    this.filterConfigIdOverride = filterConfigIdOverride;
  }

  public InheritanceMode getInheritanceMode() {
    return inheritanceMode;
  }

  public void setInheritanceMode(InheritanceMode inheritanceMode) {
    this.inheritanceMode = inheritanceMode;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public List<String> getFilterOptions() {
    return filterOptions;
  }

  public void setFilterOptions(List<String> filterOptions) {
    this.filterOptions = filterOptions;
  }

  public String getPullRunName() {
    return pullRunName;
  }

  public void setPullRunName(String pullRunName) {
    this.pullRunName = pullRunName;
  }

  public String getAppendBranchTextUnitsId() {
    return appendBranchTextUnitsId;
  }

  public void setAppendBranchTextUnitsId(String appendBranchTextUnitsId) {
    this.appendBranchTextUnitsId = appendBranchTextUnitsId;
  }
}
