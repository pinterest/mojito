package com.box.l10n.mojito.rest.asset;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Set;

/**
 * @author aloison
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SourceAsset {

  private Long repositoryId;
  private String path;
  private String content;
  private String branch;
  private String branchCreatedByUsername;
  private Boolean branchTargetsMain;

  private Set<String> branchNotifiers;
  private Long addedAssetId;
  private String pushRunName;
  private PollableTask pollableTask;
  private FilterConfigIdOverride filterConfigIdOverride;
  private List<String> filterOptions;
  private boolean extractedContent;

  private String commitHash;

  public Long getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(Long repositoryId) {
    this.repositoryId = repositoryId;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Long getAddedAssetId() {
    return addedAssetId;
  }

  public void setAddedAssetId(Long addedAssetId) {
    this.addedAssetId = addedAssetId;
  }

  public FilterConfigIdOverride getFilterConfigIdOverride() {
    return filterConfigIdOverride;
  }

  public void setFilterConfigIdOverride(FilterConfigIdOverride filterConfigIdOverride) {
    this.filterConfigIdOverride = filterConfigIdOverride;
  }

  public String getBranch() {
    return branch;
  }

  public void setBranch(String branch) {
    this.branch = branch;
  }

  public String getBranchCreatedByUsername() {
    return branchCreatedByUsername;
  }

  public void setBranchCreatedByUsername(String branchCreatedByUsername) {
    this.branchCreatedByUsername = branchCreatedByUsername;
  }

  public Boolean getBranchTargetsMain() {
    return branchTargetsMain;
  }

  public void setBranchTargetsMain(Boolean branchTargetsMain) {
    this.branchTargetsMain = branchTargetsMain;
  }

  public Set<String> getBranchNotifiers() {
    return branchNotifiers;
  }

  public void setBranchNotifiers(Set<String> branchNotifiers) {
    this.branchNotifiers = branchNotifiers;
  }

  public List<String> getFilterOptions() {
    return filterOptions;
  }

  public boolean isExtractedContent() {
    return extractedContent;
  }

  public void setExtractedContent(boolean extractedContent) {
    this.extractedContent = extractedContent;
  }

  public void setFilterOptions(List<String> filterOptions) {
    this.filterOptions = filterOptions;
  }

  public String getCommitHash() {
    return commitHash;
  }

  public void setCommitHash(String commitHash) {
    this.commitHash = commitHash;
  }

  public String getPushRunName() {
    return pushRunName;
  }

  public void setPushRunName(String pushRunName) {
    this.pushRunName = pushRunName;
  }

  @JsonProperty
  public PollableTask getPollableTask() {
    return pollableTask;
  }

  /**
   * @param pollableTask @JsonIgnore because this pollableTask is read only data generated by the
   *     server side, it is not aimed to by external process via WS
   */
  @JsonIgnore
  public void setPollableTask(PollableTask pollableTask) {
    this.pollableTask = pollableTask;
  }
}
