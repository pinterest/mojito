package com.box.l10n.mojito.service;

import com.box.l10n.mojito.evolve.EvolveClient;
import com.box.l10n.mojito.evolve.EvolveConfigurationProperties;
import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionByBranchRepository;
import com.box.l10n.mojito.service.assetcontent.AssetContentRepository;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchService;
import com.box.l10n.mojito.service.branch.BranchStatisticRepository;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.xliff.XliffUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("l10n.evolve.url")
public class EvolveSyncJob extends QuartzPollableJob<Void, Void> {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(EvolveSyncJob.class);

  private final EvolveConfigurationProperties evolveConfigurationProperties;

  private final RepositoryService repositoryService;

  private final EvolveClient evolveClient;

  private final AssetService assetService;

  private final PollableTaskService pollableTaskService;

  private final XliffUtils xliffUtils;

  private final BranchStatisticRepository branchStatisticRepository;

  private final BranchRepository branchRepository;

  private final AssetExtractionByBranchRepository assetExtractionByBranchRepository;

  private AssetContentRepository assetContentRepository;

  private TMService tmService;

  private BranchService branchService;

  private EvolveService2 evolveService2;

  @Autowired
  public EvolveSyncJob(
      EvolveConfigurationProperties evolveConfigurationProperties,
      RepositoryService repositoryService,
      EvolveClient evolveClient,
      AssetService assetService,
      PollableTaskService pollableTaskService,
      XliffUtils xliffUtils,
      BranchStatisticRepository branchStatisticRepository,
      BranchRepository branchRepository,
      AssetExtractionByBranchRepository assetExtractionByBranchRepository,
      AssetContentRepository assetContentRepository,
      TMService tmService,
      BranchService branchService,
      EvolveService2 evolveService2) {
    this.evolveConfigurationProperties = evolveConfigurationProperties;
    this.repositoryService = repositoryService;
    this.evolveClient = evolveClient;
    this.assetService = assetService;
    this.pollableTaskService = pollableTaskService;
    this.xliffUtils = xliffUtils;
    this.branchStatisticRepository = branchStatisticRepository;
    this.branchRepository = branchRepository;
    this.assetExtractionByBranchRepository = assetExtractionByBranchRepository;
    this.assetContentRepository = assetContentRepository;
    this.tmService = tmService;
    this.branchService = branchService;
    this.evolveService2 = evolveService2;
  }

  @Override
  public Void call(Void input) {
    logger.debug("Run EvolveSyncJob");
    new EvolveService(
            this.evolveConfigurationProperties,
            this.repositoryService,
            this.evolveClient,
            this.assetService,
            this.pollableTaskService,
            this.xliffUtils,
            this.branchRepository,
            this.branchStatisticRepository,
            this.assetContentRepository,
            this.tmService,
            this.branchService,
            this.evolveService2 /*,
            this.assetExtractionByBranchRepository,
            */)
        .sync();
    return null;
  }
}
