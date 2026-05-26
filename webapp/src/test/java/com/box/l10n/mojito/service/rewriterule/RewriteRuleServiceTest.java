package com.box.l10n.mojito.service.rewriterule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RewriteRule;
import com.box.l10n.mojito.rest.rewriterule.ActiveRewriteRuleWithSameRewriteFromException;
import com.box.l10n.mojito.rest.rewriterule.RepositoryLocaleForRepositoryAndLocaleNotFoundException;
import com.box.l10n.mojito.rest.rewriterule.RewriteRuleBody;
import com.box.l10n.mojito.rest.rewriterule.RewriteRuleScope;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.locale.LocaleRepository;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

public class RewriteRuleServiceTest extends ServiceTestBase {

  @Autowired RewriteRuleService rewriteRuleService;

  @Autowired RepositoryService repositoryService;

  @Autowired LocaleService localeService;

  @Autowired LocaleRepository localeRepository;

  @Autowired RewriteRuleRepository rewriteRuleRepository;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Before
  public void before() {
    this.rewriteRuleRepository.deleteAll();
  }

  @Test
  public void testCreateRewriteRule_failsWhenRepositoryLocaleIsMissing() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));

    Locale localeNotInRepository = new Locale();
    localeNotInRepository.setBcp47Tag(testIdWatcher.getEntityName("es-locale"));
    localeNotInRepository = localeRepository.saveAndFlush(localeNotInRepository);

    try {
      rewriteRuleService.createRewriteRule(
          newRewriteRuleBody(
              repository.getId(), localeNotInRepository.getId(), "source", "target", true));
      fail("Expected missing RepositoryLocale exception");
    } catch (RepositoryLocaleForRepositoryAndLocaleNotFoundException ex) {
      assertTrue(ex.getMessage().contains("localeId: " + localeNotInRepository.getId()));
      assertTrue(ex.getMessage().contains("id: " + repository.getId()));
    }
  }

  @Test
  public void testCreateRewriteRule_failsWhenAnotherActiveHasSameRewriteFrom() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));
    String rewriteFromText = "same-rewrite-from";
    RewriteRuleBody firstRuleBody =
        newRewriteRuleBody(repository.getId(), rewriteFromText, "target-a", true);
    rewriteRuleService.createRewriteRule(firstRuleBody);

    RewriteRuleBody conflictingRuleBody =
        newRewriteRuleBody(repository.getId(), rewriteFromText, "target-b", true);

    try {
      rewriteRuleService.createRewriteRule(conflictingRuleBody);
      fail("Expected a conflict when creating an enabled rewrite rule with duplicate rewriteFrom");
    } catch (ActiveRewriteRuleWithSameRewriteFromException ex) {
      assertEquals(
          String.format(
              "Cannot activate rewrite rule. Another active rule already exists with rewriteFrom: %s",
              rewriteFromText),
          ex.getMessage());
    }
  }

  @Test
  public void testCreateRewriteRule_failsWhenAnotherActiveGlobalHasSameRewriteFrom()
      throws Exception {
    String rewriteFromText = "same-rewrite-from";
    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(null, rewriteFromText, "target-a", true));

    try {
      rewriteRuleService.createRewriteRule(
          newRewriteRuleBody(null, rewriteFromText, "target-b", true));
      fail(
          "Expected a conflict when creating an enabled global rewrite rule with duplicate rewriteFrom");
    } catch (ActiveRewriteRuleWithSameRewriteFromException ex) {
      assertEquals(
          String.format(
              "Cannot activate rewrite rule. Another active rule already exists with rewriteFrom: %s",
              rewriteFromText),
          ex.getMessage());
    }
  }

  @Test
  public void testSetRewriteRuleEnabled_failsWhenAnotherActiveHasSameRewriteFrom()
      throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));

    String rewriteFromText = "same-rewrite-from";
    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(repository.getId(), rewriteFromText, "target-a", true));

    RewriteRule disabledRule =
        rewriteRuleService.createRewriteRule(
            newRewriteRuleBody(repository.getId(), rewriteFromText, "target-b", false));

    try {
      rewriteRuleService.setRewriteRuleEnabled(disabledRule.getId(), true);
      fail("Expected a conflict when enabling a rewrite rule with duplicate rewriteFrom");
    } catch (ActiveRewriteRuleWithSameRewriteFromException ex) {
      assertEquals(
          String.format(
              "Cannot activate rewrite rule. Another active rule already exists with rewriteFrom: %s",
              rewriteFromText),
          ex.getMessage());
    }
  }

  @Test
  public void testSetRewriteRuleEnabled_failsWhenAnotherActiveGlobalHasSameRewriteFrom()
      throws Exception {

    String rewriteFromText = "same-rewrite-from";
    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(null, rewriteFromText, "target-a", true));

    RewriteRule disabledRule =
        rewriteRuleService.createRewriteRule(
            newRewriteRuleBody(null, rewriteFromText, "target-b", false));

    try {
      rewriteRuleService.setRewriteRuleEnabled(disabledRule.getId(), true);
      fail("Expected a conflict when enabling a global rewrite rule with duplicate rewriteFrom");
    } catch (ActiveRewriteRuleWithSameRewriteFromException ex) {
      assertEquals(
          String.format(
              "Cannot activate rewrite rule. Another active rule already exists with rewriteFrom: %s",
              rewriteFromText),
          ex.getMessage());
    }
  }

  @Test
  public void testSetRewriteRuleEnabled_allowsDisablingRule() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));

    RewriteRule enabledRule =
        rewriteRuleService.createRewriteRule(
            newRewriteRuleBody(repository.getId(), "to-disable", "target-a", true));

    RewriteRule updatedRule = rewriteRuleService.setRewriteRuleEnabled(enabledRule.getId(), false);

    assertEquals(enabledRule.getId(), updatedRule.getId());
    assertEquals(false, updatedRule.isEnabled());
  }

  @Test
  public void testCreateRewriteRule_allowsSameRewriteFromAcrossDifferentRepositories()
      throws Exception {
    String repositoryName = testIdWatcher.getEntityName("rewrite-rule-repo");
    Repository firstRepository = repositoryService.createRepository(repositoryName);
    Repository secondRepository = repositoryService.createRepository(repositoryName + "-2");

    String rewriteFromText = "same-rewrite-from";
    RewriteRule firstRule =
        rewriteRuleService.createRewriteRule(
            newRewriteRuleBody(firstRepository.getId(), rewriteFromText, "target-a", true));
    RewriteRule secondRule =
        rewriteRuleService.createRewriteRule(
            newRewriteRuleBody(secondRepository.getId(), rewriteFromText, "target-b", true));

    assertNotNull(firstRule.getId());
    assertNotNull(secondRule.getId());
  }

  @Test
  public void testCreateRewriteRule_allowsSameRewriteFromAcrossDifferentLocales() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));

    Locale secondaryLocale = new Locale();
    secondaryLocale.setBcp47Tag(testIdWatcher.getEntityName("fr-locale"));
    secondaryLocale = localeRepository.saveAndFlush(secondaryLocale);

    String rewriteFromText = "same-rewrite-from";
    RewriteRule firstRule =
        rewriteRuleService.createRewriteRule(
            newRewriteRuleBody(
                repository.getId(),
                localeService.getDefaultLocale().getId(),
                rewriteFromText,
                "target-a",
                true));

    RewriteRule secondRule =
        rewriteRuleService.createRewriteRule(
            newRewriteRuleBody(
                repository.getId(), secondaryLocale.getId(), rewriteFromText, "target-b", true));

    assertNotNull(firstRule.getId());
    assertNotNull(secondRule.getId());
    assertEquals(secondaryLocale.getId(), secondRule.getLocale().getId());
  }

  @Test
  public void
      testCreateRewriteRule_createsEnabledGlobalAndRepositoryRewriteRulesWithSameRewriteFrom()
          throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));

    String rewriteFromText = "same-rewrite-from";
    RewriteRule globalRule =
        rewriteRuleService.createRewriteRule(
            newRewriteRuleBody(null, rewriteFromText, "global-target", true));
    RewriteRule repositoryRule =
        rewriteRuleService.createRewriteRule(
            newRewriteRuleBody(repository.getId(), rewriteFromText, "repo-target", true));

    assertNotNull(globalRule.getId());
    assertNull(globalRule.getRepository());
    assertNotNull(repositoryRule.getId());
    assertNotNull(repositoryRule.getRepository());
    assertEquals(repository.getId(), repositoryRule.getRepository().getId());
  }

  @Test
  public void testUpdateRewriteRule_FailsWhenAnotherActiveHasSameRewriteFrom() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));

    String rewriteFromText = "same-rewrite-from";

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(repository.getId(), rewriteFromText, "target-a", true));

    RewriteRule ruleToUpdate =
        rewriteRuleService.createRewriteRule(
            newRewriteRuleBody(repository.getId(), "other-rewrite-from", "target-b", true));

    try {
      rewriteRuleService.updateRewriteRule(
          ruleToUpdate.getId(),
          newRewriteRuleBody(repository.getId(), rewriteFromText, "target-c", true));
      fail("Expected a conflict when updating a rewrite rule to a duplicate active rewriteFrom");
    } catch (ActiveRewriteRuleWithSameRewriteFromException ex) {
      assertEquals(
          "Cannot activate rewrite rule. Another active rule already exists with rewriteFrom: "
              + rewriteFromText,
          ex.getMessage());
    }

    RewriteRule persistedRule = rewriteRuleService.getRewriteRuleById(ruleToUpdate.getId());
    assertEquals("other-rewrite-from", persistedRule.getRewriteFrom());
    assertEquals("target-b", persistedRule.getRewriteTo());
  }

  @Test
  public void testUpdateRewriteRule_failsWhenAnotherActiveGlobalHasSameRewriteFrom()
      throws Exception {
    String rewriteFromText = "same-rewrite-from";
    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(null, rewriteFromText, "target-a", true));

    RewriteRule ruleToUpdate =
        rewriteRuleService.createRewriteRule(
            newRewriteRuleBody(null, "other-rewrite-from", "target-b", true));

    try {
      rewriteRuleService.updateRewriteRule(
          ruleToUpdate.getId(), newRewriteRuleBody(null, rewriteFromText, "target-c", true));
      fail(
          "Expected a conflict when updating a global rewrite rule to a duplicate active rewriteFrom");
    } catch (ActiveRewriteRuleWithSameRewriteFromException ex) {
      assertEquals(
          "Cannot activate rewrite rule. Another active rule already exists with rewriteFrom: "
              + rewriteFromText,
          ex.getMessage());
    }

    RewriteRule persistedRule = rewriteRuleService.getRewriteRuleById(ruleToUpdate.getId());
    assertEquals("other-rewrite-from", persistedRule.getRewriteFrom());
    assertEquals("target-b", persistedRule.getRewriteTo());
  }

  @Test
  public void testUpdateRewriteRule_allowsKeepingSameRewriteFromOnSameEntity() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));

    String rewriteFromText = "same-rewrite-from";

    RewriteRule rewriteRule =
        rewriteRuleService.createRewriteRule(
            newRewriteRuleBody(repository.getId(), rewriteFromText, "target-a", true));

    RewriteRule updatedRule =
        rewriteRuleService.updateRewriteRule(
            rewriteRule.getId(),
            newRewriteRuleBody(repository.getId(), rewriteFromText, "updated-target", true));

    assertEquals(rewriteRule.getId(), updatedRule.getId());
    assertEquals(rewriteFromText, updatedRule.getRewriteFrom());
    assertEquals("updated-target", updatedRule.getRewriteTo());
  }

  @Test
  public void testUpdateRewriteRule_allowsKeepingSameRewriteFromOnSameGlobalEntity()
      throws Exception {
    String rewriteFromText = "same-rewrite-from";
    RewriteRule rewriteRule =
        rewriteRuleService.createRewriteRule(
            newRewriteRuleBody(null, rewriteFromText, "target-a", true));

    RewriteRule updatedRule =
        rewriteRuleService.updateRewriteRule(
            rewriteRule.getId(), newRewriteRuleBody(null, rewriteFromText, "updated-target", true));

    assertEquals(rewriteRule.getId(), updatedRule.getId());
    assertEquals(rewriteFromText, updatedRule.getRewriteFrom());
    assertEquals("updated-target", updatedRule.getRewriteTo());
    assertNull(updatedRule.getRepository());
  }

  @Test
  public void testFindRewriteRules_filtersByScopeAndEnabled() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(null, "global-enabled", "target-a", true));

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(repository.getId(), "repo-enabled", "target-b", true));

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(repository.getId(), "repo-disabled", "target-c", false));

    Page<RewriteRule> globalEnabled =
        rewriteRuleService.findRewriteRules(
            null, null, true, RewriteRuleScope.GLOBAL, PageRequest.of(0, 10));

    Page<RewriteRule> repositoryEnabled =
        rewriteRuleService.findRewriteRules(
            null, null, true, RewriteRuleScope.REPOSITORY, PageRequest.of(0, 10));

    Page<RewriteRule> repositoryDisabled =
        rewriteRuleService.findRewriteRules(
            null, null, false, RewriteRuleScope.REPOSITORY, PageRequest.of(0, 10));

    assertEquals(1, globalEnabled.getTotalElements());
    assertNull(globalEnabled.getContent().get(0).getRepository());

    assertEquals(1, repositoryEnabled.getTotalElements());
    assertNotNull(repositoryEnabled.getContent().get(0).getRepository());
    assertTrue(repositoryEnabled.getContent().get(0).isEnabled());

    assertEquals(1, repositoryDisabled.getTotalElements());
    assertNotNull(repositoryDisabled.getContent().get(0).getRepository());
    assertEquals(false, repositoryDisabled.getContent().get(0).isEnabled());
  }

  @Test
  public void testFindRewriteRules_filtersByRepositoryAndLocale() throws Exception {
    Repository firstRepository =
        repositoryService.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));
    Repository secondRepository =
        repositoryService.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo-2"));

    Locale secondaryLocale = new Locale();
    secondaryLocale.setBcp47Tag(testIdWatcher.getEntityName("es-locale"));
    secondaryLocale = localeRepository.saveAndFlush(secondaryLocale);

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(firstRepository.getId(), "r1-default", "target-a", true));

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(
            firstRepository.getId(), secondaryLocale.getId(), "r1-secondary", "target-b", true));

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(
            secondRepository.getId(), secondaryLocale.getId(), "r2-secondary", "target-c", true));

    Page<RewriteRule> filtered =
        rewriteRuleService.findRewriteRules(
            firstRepository.getId(), secondaryLocale.getId(), true, null, PageRequest.of(0, 10));

    assertEquals(1, filtered.getTotalElements());
    assertEquals(firstRepository.getId(), filtered.getContent().get(0).getRepository().getId());
    assertEquals(secondaryLocale.getId(), filtered.getContent().get(0).getLocale().getId());
  }

  private RewriteRuleBody newRewriteRuleBody(
      Long repositoryId, String rewriteFrom, String rewriteTo, boolean enabled) {
    return newRewriteRuleBody(
        repositoryId, localeService.getDefaultLocale().getId(), rewriteFrom, rewriteTo, enabled);
  }

  private RewriteRuleBody newRewriteRuleBody(
      Long repositoryId, Long localeId, String rewriteFrom, String rewriteTo, boolean enabled) {
    RewriteRuleBody body = new RewriteRuleBody();
    body.setRepositoryId(repositoryId);
    body.setLocaleId(localeId);
    body.setRewriteFrom(rewriteFrom);
    body.setRewriteTo(rewriteTo);
    body.setEnabled(enabled);
    return body;
  }
}
