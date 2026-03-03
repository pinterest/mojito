package com.box.l10n.mojito.service.ai;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.AIPrompt;
import com.box.l10n.mojito.entity.AIPromptType;
import com.box.l10n.mojito.entity.AIStringCheck;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.time.Period;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class AIStringCheckCleanupServiceTest extends ServiceTestBase {
  @Autowired private AIStringCheckRepository aiStringCheckRepository;

  @Autowired private RepositoryService repositoryService;

  @Autowired private AIPromptRepository aiPromptRepository;

  @Autowired private AIPromptTypeRepository aiPromptTypeRepository;

  private AIStringCheckCleanupService aiStringCheckCleanupService;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Before
  public void before() {
    this.aiStringCheckCleanupService =
        new AIStringCheckCleanupService(this.aiStringCheckRepository);
  }

  @Test
  public void testCleanup() throws RepositoryNameAlreadyUsedException {
    Repository repository =
        this.repositoryService.createRepository(this.testIdWatcher.getEntityName("repository"));
    AIPromptType aiPromptType = new AIPromptType();
    aiPromptType.setName("Test Prompt");
    aiPromptType = this.aiPromptTypeRepository.save(aiPromptType);
    AIPrompt aiPrompt = new AIPrompt();
    aiPrompt.setPromptType(aiPromptType);
    aiPrompt = this.aiPromptRepository.save(aiPrompt);
    AIStringCheck aiStringCheck = new AIStringCheck();
    aiStringCheck.setRepositoryId(repository.getId());
    aiStringCheck.setAiPromptId(aiPrompt.getId());
    aiStringCheck.setCreatedDate(JSR310Migration.dateTimeNow().minusMonths(5));
    this.aiStringCheckRepository.save(aiStringCheck);

    this.aiStringCheckCleanupService.cleanup(Period.ofMonths(6));

    List<AIStringCheck> aiStringChecks = this.aiStringCheckRepository.findAll();

    assertEquals(1, aiStringChecks.size());

    this.aiStringCheckCleanupService.cleanup(Period.ofMonths(4));

    aiStringChecks = this.aiStringCheckRepository.findAll();

    assertEquals(0, aiStringChecks.size());
  }
}
