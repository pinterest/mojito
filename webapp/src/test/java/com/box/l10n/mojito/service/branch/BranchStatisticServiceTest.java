package com.box.l10n.mojito.service.branch;

import static com.box.l10n.mojito.okapi.ImportTranslationsFromLocalizedAssetStep.StatusForEqualTarget.APPROVED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.entity.BranchTextUnitStatistic;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.assetcontent.AssetContentService;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class BranchStatisticServiceTest extends ServiceTestBase {

  /** logger */
  static Logger logger = getLogger(BranchStatisticServiceTest.class);

  @Autowired BranchStatisticService branchStatisticService;

  @Autowired BranchService branchService;

  @Autowired RepositoryService repositoryService;

  @Autowired AssetService assetService;

  @Autowired AssetExtractionService assetExtractionService;

  @Autowired AssetContentService assetContentService;

  @Autowired BranchStatisticRepository branchStatisticRepository;

  @Autowired TextUnitSearcher textUnitSearcher;

  @Autowired TMService tmService;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Test
  public void getBranchesToCheck() throws Exception {
    BranchTestData branchTestData = new BranchTestData(testIdWatcher);

    List<Branch> branchesToCheck =
        branchStatisticService.getBranchesToProcess(branchTestData.getRepository().getId());
    assertEquals("branch1", branchesToCheck.get(0).getName());
    assertEquals("branch2", branchesToCheck.get(1).getName());
    assertEquals(2, branchesToCheck.size());
  }

  @Test
  public void getBranchTextUnits() throws Exception {
    BranchTestData branchTestData = new BranchTestData(testIdWatcher);

    logger.debug("In master branch");
    List<TextUnitDTO> textUnitDTOsForBranchMaster =
        branchStatisticService.getTextUnitDTOsForBranch(branchTestData.getMaster());
    assertEquals("string1", textUnitDTOsForBranchMaster.get(0).getName());
    assertEquals("string2", textUnitDTOsForBranchMaster.get(1).getName());
    assertEquals(2, textUnitDTOsForBranchMaster.size());

    logger.debug("In branch1");
    List<TextUnitDTO> textUnitDTOsForBranch1 =
        branchStatisticService.getTextUnitDTOsForBranch(branchTestData.getBranch1());
    assertEquals("string1", textUnitDTOsForBranch1.get(0).getName());
    assertEquals("string3", textUnitDTOsForBranch1.get(1).getName());
    assertEquals(2, textUnitDTOsForBranch1.size());

    logger.debug("In branch2");
    List<TextUnitDTO> textUnitDTOsForBranch2 =
        branchStatisticService.getTextUnitDTOsForBranch(branchTestData.getBranch2());
    assertEquals("string1", textUnitDTOsForBranch2.get(0).getName());
    assertEquals("string2", textUnitDTOsForBranch2.get(1).getName());
    assertEquals("string4", textUnitDTOsForBranch2.get(2).getName());
    assertEquals("string5", textUnitDTOsForBranch2.get(3).getName());
    assertEquals(4, textUnitDTOsForBranch2.size());
  }

  @Test
  public void computeAndSaveBranchStatistic() throws Exception {
    BranchTestData branchTestData = new BranchTestData(testIdWatcher);
    Branch branch1 = branchTestData.getBranch1();
    Branch branch2 = branchTestData.getBranch2();

    branchStatisticService.computeAndSaveBranchStatistics(branch1);
    branchStatisticService.computeAndSaveBranchStatistics(branch2);

    BranchStatistic branchStatisticForBranch1 = getByBranchWithBranchTextUnitStatistics(branch1);
    assertEquals("branch1", branchStatisticForBranch1.getBranch().getName());

    Iterator<BranchTextUnitStatistic> itBranch1 =
        branchStatisticForBranch1.getBranchTextUnitStatistics().iterator();

    BranchTextUnitStatistic branchTextUnitStatisticForBranch1 = itBranch1.next();
    assertEquals("string1", branchTextUnitStatisticForBranch1.getTmTextUnit().getName());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch1.getTotalCount());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch1.getForTranslationCount());

    branchTextUnitStatisticForBranch1 = itBranch1.next();
    assertEquals("string3", branchTextUnitStatisticForBranch1.getTmTextUnit().getName());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch1.getTotalCount());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch1.getForTranslationCount());
    assertFalse(itBranch1.hasNext());

    BranchStatistic branchStatisticForBranch2 = getByBranchWithBranchTextUnitStatistics(branch2);
    assertEquals("branch2", branchStatisticForBranch2.getBranch().getName());

    Set<BranchTextUnitStatistic> branchTextUnitStatisticsForBranch2 =
        branchStatisticForBranch2.getBranchTextUnitStatistics();

    Iterator<BranchTextUnitStatistic> itBranch2 = branchTextUnitStatisticsForBranch2.iterator();

    BranchTextUnitStatistic branchTextUnitStatisticForBranch2 = itBranch2.next();
    assertEquals("string1", branchTextUnitStatisticForBranch2.getTmTextUnit().getName());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch2.getTotalCount());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch2.getForTranslationCount());

    branchTextUnitStatisticForBranch2 = itBranch2.next();
    assertEquals("string2", branchTextUnitStatisticForBranch2.getTmTextUnit().getName());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch2.getTotalCount());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch2.getForTranslationCount());

    branchTextUnitStatisticForBranch2 = itBranch2.next();
    assertEquals("string4", branchTextUnitStatisticForBranch2.getTmTextUnit().getName());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch2.getTotalCount());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch2.getForTranslationCount());

    branchTextUnitStatisticForBranch2 = itBranch2.next();
    assertEquals("string5", branchTextUnitStatisticForBranch2.getTmTextUnit().getName());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch2.getTotalCount());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch2.getForTranslationCount());

    assertFalse(itBranch2.hasNext());
  }

  @Test
  public void computeAndSaveBranchStatisticUpdates() throws Exception {
    BranchTestData branchTestData = new BranchTestData(testIdWatcher);
    Branch branch1 = branchTestData.getBranch1();
    Branch branch2 = branchTestData.getBranch2();
    RepositoryLocale repositoryLocaleFrFr = branchTestData.getRepositoryLocaleFrFr();
    RepositoryLocale repositoryLocaleJaJp = branchTestData.getRepositoryLocaleJaJp();
    Asset asset = branchTestData.getAsset();

    branchStatisticService.computeAndSaveBranchStatistics(branch1);
    branchStatisticService.computeAndSaveBranchStatistics(branch2);

    logger.debug("Add translation for french, expect ready count to change");
    String localizedAssetContent = "string3=content3_fr-FR";
    tmService
        .importLocalizedAssetAsync(
            asset.getId(),
            localizedAssetContent,
            repositoryLocaleFrFr.getLocale().getId(),
            APPROVED,
            null,
            null)
        .get();
    branchStatisticService.computeAndSaveBranchStatistics(branch1);

    BranchStatistic branchStatisticForBranch1 = getByBranchWithBranchTextUnitStatistics(branch1);
    assertEquals("branch1", branchStatisticForBranch1.getBranch().getName());
    assertEquals(4L, branchStatisticForBranch1.getTotalCount());
    assertEquals(3L, branchStatisticForBranch1.getForTranslationCount());

    Iterator<BranchTextUnitStatistic> itBranch1 =
        branchStatisticForBranch1.getBranchTextUnitStatistics().iterator();

    BranchTextUnitStatistic branchTextUnitStatisticForBranch1 = itBranch1.next();
    assertEquals("string1", branchTextUnitStatisticForBranch1.getTmTextUnit().getName());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch1.getTotalCount());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch1.getForTranslationCount());

    branchTextUnitStatisticForBranch1 = itBranch1.next();
    assertEquals("string3", branchTextUnitStatisticForBranch1.getTmTextUnit().getName());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch1.getTotalCount());
    assertEquals(1L, (long) branchTextUnitStatisticForBranch1.getForTranslationCount());

    logger.debug("Just make sure we have no side effect on other branches");
    BranchStatistic branchStatisticForBranch2 = getByBranchWithBranchTextUnitStatistics(branch2);
    assertEquals("branch2", branchStatisticForBranch2.getBranch().getName());
    assertEquals(8L, branchStatisticForBranch2.getTotalCount());
    assertEquals(8L, branchStatisticForBranch2.getForTranslationCount());

    Set<BranchTextUnitStatistic> branchTextUnitStatisticsForBranch2 =
        branchStatisticForBranch2.getBranchTextUnitStatistics();

    Iterator<BranchTextUnitStatistic> itBranch2 = branchTextUnitStatisticsForBranch2.iterator();

    BranchTextUnitStatistic branchTextUnitStatisticForBranch2 = itBranch2.next();
    assertEquals("string1", branchTextUnitStatisticForBranch2.getTmTextUnit().getName());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch2.getTotalCount());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch2.getForTranslationCount());

    branchTextUnitStatisticForBranch2 = itBranch2.next();
    assertEquals("string2", branchTextUnitStatisticForBranch2.getTmTextUnit().getName());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch2.getTotalCount());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch2.getForTranslationCount());

    branchTextUnitStatisticForBranch2 = itBranch2.next();
    assertEquals("string4", branchTextUnitStatisticForBranch2.getTmTextUnit().getName());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch2.getTotalCount());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch2.getForTranslationCount());

    branchTextUnitStatisticForBranch2 = itBranch2.next();
    assertEquals("string5", branchTextUnitStatisticForBranch2.getTmTextUnit().getName());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch2.getTotalCount());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch2.getForTranslationCount());

    assertFalse(itBranch2.hasNext());

    logger.debug(
        "Import string on branch2, expect japense to be fully translated, french half translated");
    tmService
        .importLocalizedAssetAsync(
            asset.getId(),
            "string1=content1_fr-FR\nstring2=content4_fr-FR",
            repositoryLocaleFrFr.getLocale().getId(),
            APPROVED,
            null,
            null)
        .get();
    tmService
        .importLocalizedAssetAsync(
            asset.getId(),
            "string1=content1_ja-JP\nstring2=content2_ja-J\nstring4=content4_ja-JP\nstring5=content5_ja-JP",
            repositoryLocaleJaJp.getLocale().getId(),
            APPROVED,
            null,
            null)
        .get();
    branchStatisticService.computeAndSaveBranchStatistics(branch2);

    branchStatisticForBranch2 = getByBranchWithBranchTextUnitStatistics(branch2);
    branchTextUnitStatisticsForBranch2 = branchStatisticForBranch2.getBranchTextUnitStatistics();
    assertEquals(8L, branchStatisticForBranch2.getTotalCount());
    assertEquals(2L, branchStatisticForBranch2.getForTranslationCount());

    itBranch2 = branchTextUnitStatisticsForBranch2.iterator();

    branchTextUnitStatisticForBranch2 = itBranch2.next();
    assertEquals("string1", branchTextUnitStatisticForBranch2.getTmTextUnit().getName());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch2.getTotalCount());
    assertEquals(0L, (long) branchTextUnitStatisticForBranch2.getForTranslationCount());

    branchTextUnitStatisticForBranch2 = itBranch2.next();
    assertEquals("string2", branchTextUnitStatisticForBranch2.getTmTextUnit().getName());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch2.getTotalCount());
    assertEquals(0L, (long) branchTextUnitStatisticForBranch2.getForTranslationCount());

    branchTextUnitStatisticForBranch2 = itBranch2.next();
    assertEquals("string4", branchTextUnitStatisticForBranch2.getTmTextUnit().getName());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch2.getTotalCount());
    assertEquals(1L, (long) branchTextUnitStatisticForBranch2.getForTranslationCount());

    branchTextUnitStatisticForBranch2 = itBranch2.next();
    assertEquals("string5", branchTextUnitStatisticForBranch2.getTmTextUnit().getName());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch2.getTotalCount());
    assertEquals(1L, (long) branchTextUnitStatisticForBranch2.getForTranslationCount());

    assertFalse(itBranch2.hasNext());
  }

  @Test
  public void computeAndSaveBranchStatisticDeletedTextUnit() throws Exception {
    BranchTestData branchTestData = new BranchTestData(testIdWatcher);
    Branch branch1 = branchTestData.getBranch1();

    branchStatisticService.computeAndSaveBranchStatistics(branch1);

    BranchStatistic branchStatisticForBranch1 = getByBranchWithBranchTextUnitStatistics(branch1);
    assertEquals("branch1", branchStatisticForBranch1.getBranch().getName());

    Iterator<BranchTextUnitStatistic> itBranch1 =
        branchStatisticForBranch1.getBranchTextUnitStatistics().iterator();

    BranchTextUnitStatistic branchTextUnitStatisticForBranch1 = itBranch1.next();
    assertEquals("string1", branchTextUnitStatisticForBranch1.getTmTextUnit().getName());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch1.getTotalCount());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch1.getForTranslationCount());

    branchTextUnitStatisticForBranch1 = itBranch1.next();
    assertEquals("string3", branchTextUnitStatisticForBranch1.getTmTextUnit().getName());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch1.getTotalCount());
    assertEquals(2L, (long) branchTextUnitStatisticForBranch1.getForTranslationCount());
    assertFalse(itBranch1.hasNext());

    AssetContent assetContentBranch1 =
        assetContentService.createAssetContent(branchTestData.getAsset(), "", false, branch1);
    assetExtractionService
        .processAssetAsync(assetContentBranch1.getId(), null, null, null, null)
        .get();
    branchStatisticService.computeAndSaveBranchStatistics(branch1);

    BranchStatistic branchStatisticForBranch1AfterDelete =
        getByBranchWithBranchTextUnitStatistics(branch1);
    assertEquals("branch1", branchStatisticForBranch1AfterDelete.getBranch().getName());
    assertTrue(branchStatisticForBranch1AfterDelete.getBranchTextUnitStatistics().isEmpty());
  }

  /**
   * Get the branch statistic and fetch the branch text unit stastics in transaction
   *
   * @param branch
   * @return
   */
  @Transactional
  BranchStatistic getByBranchWithBranchTextUnitStatistics(Branch branch) {
    BranchStatistic byBranch = branchStatisticRepository.findByBranch(branch);
    // TODO(ja-lib) L2: old comment "interesting ..." is this still needed?
    byBranch.getBranchTextUnitStatistics().forEach(t -> logger.trace("force initialize"));
    return byBranch;
  }
}
