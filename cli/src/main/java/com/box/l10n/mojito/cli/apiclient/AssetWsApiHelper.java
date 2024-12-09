package com.box.l10n.mojito.cli.apiclient;

import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.model.AssetAssetSummary;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class AssetWsApiHelper {
  @Autowired AssetWsApi assetClient;

  public AssetAssetSummary getAssetByPathAndRepositoryId(String path, Long repositoryId)
      throws CommandException {
    Assert.notNull(path, "path must not be null");
    Assert.notNull(repositoryId, "repository must not be null");

    List<AssetAssetSummary> assets;
    try {
      assets = this.assetClient.getAssets(repositoryId, path, null, null, null);
    } catch (ApiException e) {
      throw new RuntimeException(e);
    }
    if (!assets.isEmpty()) {
      return assets.getFirst();
    } else {
      throw new CommandException(
          "Could not find asset with path = [" + path + "] at repo id [" + repositoryId + "]");
    }
  }
}
