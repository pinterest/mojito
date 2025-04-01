package com.box.l10n.mojito.service.evolve;

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
import org.quartz.DisallowConcurrentExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("l10n.evolve.url")
@DisallowConcurrentExecution
public class EvolveSyncJob extends QuartzPollableJob<Void, Void> {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(EvolveSyncJob.class);

  @Autowired
  private EvolveConfigurationProperties evolveConfigurationProperties;

  @Autowired
  private RepositoryService repositoryService;

  @Autowired
  private EvolveClient evolveClient;

  @Autowired
  private AssetService assetService;

  @Autowired
  private PollableTaskService pollableTaskService;

  @Autowired
  private XliffUtils xliffUtils;

  @Autowired
  private BranchStatisticRepository branchStatisticRepository;

  @Autowired
  private BranchRepository branchRepository;

  @Autowired
  private AssetExtractionByBranchRepository assetExtractionByBranchRepository;

  @Autowired
  private AssetContentRepository assetContentRepository;

  @Autowired
  private TMService tmService;

  @Autowired
  private BranchService branchService;

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
            this.assetExtractionByBranchRepository)
        .sync();
    return null;
  }
}
