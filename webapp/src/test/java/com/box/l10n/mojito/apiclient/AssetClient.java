package com.box.l10n.mojito.apiclient;

import com.box.l10n.mojito.cli.apiclient.AssetWsApi;
import com.box.l10n.mojito.cli.apiclient.model.AssetAssetSummary;
import com.box.l10n.mojito.cli.apiclient.model.LocalizedAssetBody;
import com.box.l10n.mojito.cli.apiclient.model.PollableTask;
import com.box.l10n.mojito.cli.apiclient.model.SourceAsset;
import com.box.l10n.mojito.cli.apiclient.model.XliffExportBody;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class AssetClient {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(AssetClient.class);

  @Autowired private AssetWsApi assetWsApi;

  public List<Long> getAssetIds(
      Long repositoryId, Boolean deleted, Boolean virtual, Long branchId) {
    Assert.notNull(repositoryId, "The repositoryId must not be null");
    return this.assetWsApi.getAssetIds(repositoryId, deleted, virtual, branchId);
  }

  public PollableTask deleteAssetsOfBranches(List<Long> body, Long branchId) {
    logger.debug("Deleting assets by asset ids = {} or branch id: {}", body.toString(), branchId);
    return this.assetWsApi.deleteAssetsOfBranches(body, branchId);
  }

  public List<AssetAssetSummary> getAssets(
      Long repositoryId, String path, Boolean deleted, Boolean virtual, Long branchId) {
    logger.debug("Get assets by path = {} repo id = {} deleted = {}", path, repositoryId, deleted);
    return this.assetWsApi.getAssets(repositoryId, path, deleted, virtual, branchId);
  }

  public XliffExportBody xliffExportAsync(XliffExportBody body, String bcp47tag, Long assetId) {
    logger.debug("Export asset id: {} for locale: {}", assetId, bcp47tag);
    return this.assetWsApi.xliffExportAsync(body, bcp47tag, assetId);
  }

  public XliffExportBody xliffExport(Long assetId, Long tmXliffId) {
    logger.debug("Get exported xliff for asset id: {} for tm xliff id: {}", assetId, tmXliffId);
    return this.assetWsApi.xliffExport(assetId, tmXliffId);
  }

  public SourceAsset importSourceAsset(SourceAsset body) {
    return this.assetWsApi.importSourceAsset(body);
  }

  public LocalizedAssetBody getPseudoLocalizedAssetForContent(
      LocalizedAssetBody body, Long assetId) {
    return this.assetWsApi.getPseudoLocalizedAssetForContent(body, assetId);
  }

  public void deleteAssetById(Long assetId) {
    this.assetWsApi.deleteAssetById(assetId);
  }
}
