package com.box.l10n.mojito.service.repository.statistics;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jaurambault
 */
public class RepositoryStatisticServiceITest extends ServiceTestBase {

  /**
   * logger
   */
  static Logger logger = getLogger(RepositoryStatisticServiceITest.class);

  @Autowired
  RepositoryStatisticService repositoryStatisticService;

  @Test
  public void updateStatistics() {
    System.out.println("update statistic");
    repositoryStatisticService.updateStatistics(2L);
  }

}
