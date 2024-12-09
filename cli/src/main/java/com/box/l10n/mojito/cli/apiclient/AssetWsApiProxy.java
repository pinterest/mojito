package com.box.l10n.mojito.cli.apiclient;

import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.model.AssetAssetSummary;
import com.box.l10n.mojito.cli.model.ImportLocalizedAssetBody;
import com.box.l10n.mojito.cli.model.LocalizedAssetBody;
import com.box.l10n.mojito.cli.model.MultiLocalizedAssetBody;
import com.box.l10n.mojito.cli.model.PollableTask;
import com.box.l10n.mojito.cli.model.XliffExportBody;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

public class AssetWsApiProxy extends AssetWsApi {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(AssetWsApiProxy.class);

  public static final String OUTPUT_BCP47_TAG = "en-x-pseudo";

  public AssetWsApiProxy(ApiClient apiClient) {
    super(apiClient);
  }

  public AssetAssetSummary getAssetByPathAndRepositoryId(String path, Long repositoryId)
      throws CommandException, ApiException {
    Assert.notNull(path, "path must not be null");
    Assert.notNull(repositoryId, "repository must not be null");

    List<AssetAssetSummary> assets = this.getAssets(repositoryId, path, null, null, null);
    if (!assets.isEmpty()) {
      return assets.getFirst();
    } else {
      throw new CommandException(
          "Could not find asset with path = [" + path + "] at repo id [" + repositoryId + "]");
    }
  }

  @Override
  public LocalizedAssetBody getLocalizedAssetForContent(
      LocalizedAssetBody body, Long assetId, Long localeId) throws ApiException {
    logger.debug(
        "Getting localized asset with asset id = {}, locale id = {}, outputBcp47tag: {}",
        assetId,
        localeId,
        body.getOutputBcp47tag());
    return super.getLocalizedAssetForContent(body, assetId, localeId);
  }

  @Override
  public ImportLocalizedAssetBody importLocalizedAsset(
      ImportLocalizedAssetBody body, Long assetId, Long localeId) throws ApiException {
    logger.debug("Import localized asset with asset id = {}, locale id = {}", assetId, localeId);
    return super.importLocalizedAsset(body, assetId, localeId);
  }

  @Override
  public PollableTask getLocalizedAssetForContentParallel(
      MultiLocalizedAssetBody body, Long assetId) throws ApiException {
    logger.debug("Getting localized assets with asset id = {}", assetId);
    return super.getLocalizedAssetForContentParallel(body, assetId);
  }

  @Override
  public PollableTask getLocalizedAssetForContentAsync(LocalizedAssetBody body, Long assetId)
      throws ApiException {
    logger.debug(
        "Getting localized asset with asset id = {}, locale id = {}, outputBcp47tag: {}",
        assetId,
        body.getLocaleId(),
        body.getOutputBcp47tag());
    return super.getLocalizedAssetForContentAsync(body, assetId);
  }

  @Override
  public List<Long> getAssetIds(Long repositoryId, Boolean deleted, Boolean virtual, Long branchId)
      throws ApiException {
    Assert.notNull(repositoryId, "The repositoryId must not be null");
    return super.getAssetIds(repositoryId, deleted, virtual, branchId);
  }

  @Override
  public PollableTask deleteAssetsOfBranches(List<Long> body, Long branchId) throws ApiException {
    logger.debug("Deleting assets by asset ids = {} or branch id: {}", body.toString(), branchId);
    return super.deleteAssetsOfBranches(body, branchId);
  }

  @Override
  public List<AssetAssetSummary> getAssets(
      Long repositoryId, String path, Boolean deleted, Boolean virtual, Long branchId)
      throws ApiException {
    logger.debug("Get assets by path = {} repo id = {} deleted = {}", path, repositoryId, deleted);
    return super.getAssets(repositoryId, path, deleted, virtual, branchId);
  }

  @Override
  public XliffExportBody xliffExportAsync(XliffExportBody body, String bcp47tag, Long assetId)
      throws ApiException {
    logger.debug("Export asset id: {} for locale: {}", assetId, bcp47tag);
    return super.xliffExportAsync(body, bcp47tag, assetId);
  }

  @Override
  public XliffExportBody xliffExport(Long assetId, Long tmXliffId) throws ApiException {
    logger.debug("Get exported xliff for asset id: {} for tm xliff id: {}", assetId, tmXliffId);
    return super.xliffExport(assetId, tmXliffId);
  }
}
