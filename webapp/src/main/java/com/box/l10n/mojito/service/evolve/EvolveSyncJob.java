package com.box.l10n.mojito.service.evolve;

import com.box.l10n.mojito.LocaleMappingHelper;
import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionByBranchRepository;
import com.box.l10n.mojito.service.assetcontent.AssetContentRepository;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchService;
import com.box.l10n.mojito.service.branch.BranchStatisticRepository;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.xliff.XliffUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("l10n.evolve.url")
public class EvolveSyncJob extends QuartzPollableJob<EvolveSyncJobInput, Void> {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(EvolveSyncJob.class);

  @Autowired private EvolveConfigurationProperties evolveConfigurationProperties;

  @Autowired private RepositoryRepository repositoryRepository;

  @Autowired private EvolveClient evolveClient;

  @Autowired private AssetService assetService;

  @Autowired private PollableTaskService pollableTaskService;

  @Autowired private XliffUtils xliffUtils;

  @Autowired private BranchStatisticRepository branchStatisticRepository;

  @Autowired private BranchRepository branchRepository;

  @Autowired private AssetExtractionByBranchRepository assetExtractionByBranchRepository;

  @Autowired private AssetContentRepository assetContentRepository;

  @Autowired private TMService tmService;

  @Autowired private BranchService branchService;

  @Autowired private SyncDateService syncDateService;

  @Autowired private LocaleMappingHelper localeMappingHelper;

  @Override
  public Void call(EvolveSyncJobInput input) {
    logger.debug("Run EvolveSyncJob");
    new EvolveService(
            input.getRepositoryId(),
            input.getLocaleMapping(),
            this.evolveConfigurationProperties,
            this.repositoryRepository,
            this.evolveClient,
            this.assetService,
            this.pollableTaskService,
            this.xliffUtils,
            this.branchRepository,
            this.branchStatisticRepository,
            this.assetContentRepository,
            this.tmService,
            this.branchService,
            this.assetExtractionByBranchRepository,
            this.syncDateService,
            this.localeMappingHelper)
        .sync();
    return null;
  }
}
