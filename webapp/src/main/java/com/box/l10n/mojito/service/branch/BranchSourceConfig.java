package com.box.l10n.mojito.service.branch;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n")
public class BranchSourceConfig {
  Map<String, BranchSource> branchSources = new HashMap<>();

  public Map<String, BranchSource> getBranchSources() {
    return branchSources;
  }

  public void setBranchSources(Map<String, BranchSource> branchSources) {
    this.branchSources = branchSources;
  }
}
