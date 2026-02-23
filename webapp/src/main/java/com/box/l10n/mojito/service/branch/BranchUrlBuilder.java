package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.utils.ServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class BranchUrlBuilder {

  @Autowired ServerConfig serverConfig;

  public String getBranchDashboardUrl(String branchName) {
    return UriComponentsBuilder.fromHttpUrl(serverConfig.getUrl())
        .path("branches")
        .queryParam("searchText", branchName)
        .queryParam("deleted", false)
        .queryParam("onlyMyBranches", false)
        .build()
        .toUriString();
  }

  public String getV2BranchDashboardUrl(String branchName, String repoName) {
    return UriComponentsBuilder.fromHttpUrl(serverConfig.getUrl())
        .path("/v2/branch")
        .queryParam("branchName", branchName)
        .queryParam("repoName", repoName)
        .build()
        .toUriString();
  }
}
