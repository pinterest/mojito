package com.box.l10n.mojito.service.appender;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppendedAssetBlobStorage {

  private final StructuredBlobStorage structuredBlobStorage;
  private final ObjectMapper objectMapper;

  @Autowired
  public AppendedAssetBlobStorage(
      StructuredBlobStorage structuredBlobStorage, ObjectMapper objectMapper) {
    this.structuredBlobStorage = structuredBlobStorage;
    this.objectMapper = objectMapper;
  }

  public void saveAppendedBranches(String jobId, List<Long> branchIds) {
    structuredBlobStorage.put(
        StructuredBlobStorage.Prefix.APPENDED_ASSET,
        getBranchesName(jobId),
        objectMapper.writeValueAsStringUnchecked(branchIds),
        Retention.MIN_1_DAY);
  }

  public List<Long> getAppendedBranches(String jobId) {
    String branchesString =
        structuredBlobStorage
            .getString(StructuredBlobStorage.Prefix.APPENDED_ASSET, getBranchesName(jobId))
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "Can't get the appended asset branches json for: " + jobId));
    return objectMapper.readValueUnchecked(branchesString, new TypeReference<List<Long>>() {});
  }

  public void saveAppendedSource(String jobId, String content) {
    structuredBlobStorage.put(
        StructuredBlobStorage.Prefix.APPENDED_ASSET,
        getSourceArtifactName(jobId),
        content,
        Retention.PERMANENT);
  }

  private String getBranchesName(String jobId) {
    return String.format("%s/branches", jobId);
  }

  private String getSourceArtifactName(String jobId) {
    return String.format("%s/appended-source", jobId);
  }
}
