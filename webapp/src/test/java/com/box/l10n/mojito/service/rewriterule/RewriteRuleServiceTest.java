package com.box.l10n.mojito.service.rewriterule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.RewriteRule;
import com.box.l10n.mojito.rest.rewriterule.ActiveRewriteRuleWithSameRewriteFromException;
import com.box.l10n.mojito.rest.rewriterule.RepositoryLocaleForRepositoryAndLocaleNotFoundException;
import com.box.l10n.mojito.rest.rewriterule.RewriteRuleBody;
import com.box.l10n.mojito.rest.rewriterule.RewriteRuleScope;
import com.box.l10n.mojito.rest.rewriterule.RewriteRuleWithIdNotFoundException;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.locale.LocaleRepository;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.collect.Sets;
import java.text.MessageFormat;
import java.util.List;
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
    Repository repository = this.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));

    Locale localeNotInRepository = new Locale();
    localeNotInRepository.setBcp47Tag(testIdWatcher.getEntityName("es-locale"));
    localeNotInRepository = localeRepository.saveAndFlush(localeNotInRepository);

    try {
      rewriteRuleService.createRewriteRule(
          newRewriteRuleBody(
              repository.getId(), localeNotInRepository.getId(), "source", "target", true));
      fail("Expected missing RepositoryLocale exception");
    } catch (RepositoryLocaleForRepositoryAndLocaleNotFoundException ex) {
      assertTrue(
          ex.getMessage()
              .contains(MessageFormat.format("locale id: {0}", localeNotInRepository.getId())));
      assertTrue(
          ex.getMessage()
              .contains(MessageFormat.format("repository with id: {0}", repository.getId())));
    }
  }

  private Repository createRepository(String repositoryId)
      throws RepositoryNameAlreadyUsedException, RepositoryLocaleCreationException {
    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    return repositoryService.createRepository(
        repositoryId,
        "",
        this.localeService.getDefaultLocale(),
        false,
        Sets.newHashSet(),
        Sets.newHashSet(esRepositoryLocale));
  }

  @Test
  public void testCreateRewriteRule_failsWhenAnotherActiveHasSameRewriteFrom() throws Exception {
    Repository repository = this.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));
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
    Repository repository = this.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));

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
    Repository repository = this.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));

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
    Repository firstRepository = this.createRepository(repositoryName);
    Repository secondRepository = this.createRepository(repositoryName + "-2");

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
    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Locale frLocale = this.localeService.findByBcp47Tag("fr-FR");
    RepositoryLocale frRepositoryLocale = new RepositoryLocale();
    frRepositoryLocale.setLocale(frLocale);
    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("rewrite-rule-repo"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale, frRepositoryLocale));

    String rewriteFromText = "same-rewrite-from";
    RewriteRule firstRule =
        rewriteRuleService.createRewriteRule(
            newRewriteRuleBody(
                repository.getId(), esLocale.getId(), rewriteFromText, "target-a", true));

    RewriteRule secondRule =
        rewriteRuleService.createRewriteRule(
            newRewriteRuleBody(
                repository.getId(), frLocale.getId(), rewriteFromText, "target-b", true));

    assertNotNull(firstRule.getId());
    assertEquals(esLocale.getId(), firstRule.getLocale().getId());
    assertNotNull(secondRule.getId());
    assertEquals(frLocale.getId(), secondRule.getLocale().getId());
  }

  @Test
  public void
      testCreateRewriteRule_createsEnabledGlobalAndRepositoryRewriteRulesWithSameRewriteFrom()
          throws Exception {
    Repository repository = this.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));

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
    Repository repository = this.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));

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
    Repository repository = this.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));

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
    Repository repository = this.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(null, "global-enabled", "target-a", true));

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(repository.getId(), "repo-enabled", "target-b", true));

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(repository.getId(), "repo-disabled", "target-c", false));

    Page<RewriteRule> globalEnabled =
        rewriteRuleService.findRewriteRules(
            null, null, true, RewriteRuleScope.GLOBAL, null, PageRequest.of(0, 10));

    Page<RewriteRule> repositoryEnabled =
        rewriteRuleService.findRewriteRules(
            null, null, true, RewriteRuleScope.REPOSITORY, null, PageRequest.of(0, 10));

    Page<RewriteRule> repositoryDisabled =
        rewriteRuleService.findRewriteRules(
            null, null, false, RewriteRuleScope.REPOSITORY, null, PageRequest.of(0, 10));

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
    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    Repository firstRepository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("rewrite-rule-repo"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));

    Repository secondRepository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("rewrite-rule-repo-2"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(
            firstRepository.getId(), esLocale.getId(), "r1-secondary", "target-b", true));

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(
            secondRepository.getId(), esLocale.getId(), "r2-secondary", "target-c", true));

    Page<RewriteRule> filtered =
        rewriteRuleService.findRewriteRules(
            List.of(firstRepository.getId()),
            List.of(esLocale.getId()),
            true,
            null,
            null,
            PageRequest.of(0, 10));

    assertEquals(1, filtered.getTotalElements());
    assertEquals(firstRepository.getId(), filtered.getContent().getFirst().getRepository().getId());
    assertEquals(esLocale.getId(), filtered.getContent().getFirst().getLocale().getId());
  }

  @Test
  public void testFindRewriteRules_filtersByMultipleRepositoryIds() throws Exception {
    String repositoryName = testIdWatcher.getEntityName("rewrite-rule-repo");
    Repository firstRepository = this.createRepository(repositoryName);
    Repository secondRepository = this.createRepository(repositoryName + "-2");
    Repository thirdRepository = this.createRepository(repositoryName + "-3");

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(firstRepository.getId(), "first-repo-rule", "target-a", true));
    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(secondRepository.getId(), "second-repo-rule", "target-b", true));
    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(thirdRepository.getId(), "third-repo-rule", "target-c", true));

    Page<RewriteRule> filtered =
        rewriteRuleService.findRewriteRules(
            List.of(firstRepository.getId(), secondRepository.getId()),
            null,
            null,
            null,
            null,
            PageRequest.of(0, 10));

    assertEquals(2, filtered.getTotalElements());
    assertTrue(
        filtered.getContent().stream()
            .allMatch(
                rule ->
                    rule.getRepository().getId().equals(firstRepository.getId())
                        || rule.getRepository().getId().equals(secondRepository.getId())));
  }

  @Test
  public void testFindRewriteRules_filtersByMultipleLocaleIds() throws Exception {
    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");
    Locale frLocale = this.localeService.findByBcp47Tag("fr-FR");
    Locale jaLocale = this.localeService.findByBcp47Tag("ja-JP");

    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);
    RepositoryLocale frRepositoryLocale = new RepositoryLocale();
    frRepositoryLocale.setLocale(frLocale);
    RepositoryLocale jaRepositoryLocale = new RepositoryLocale();
    jaRepositoryLocale.setLocale(jaLocale);

    Repository repository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("rewrite-rule-repo"),
            "",
            this.localeService.getDefaultLocale(),
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale, frRepositoryLocale, jaRepositoryLocale));

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(repository.getId(), esLocale.getId(), "es-rule", "target-es", true));
    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(repository.getId(), frLocale.getId(), "fr-rule", "target-fr", true));
    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(repository.getId(), jaLocale.getId(), "ja-rule", "target-ja", true));

    Page<RewriteRule> filtered =
        rewriteRuleService.findRewriteRules(
            null,
            List.of(esLocale.getId(), frLocale.getId()),
            null,
            null,
            null,
            PageRequest.of(0, 10));

    assertEquals(2, filtered.getTotalElements());
    assertTrue(
        filtered.getContent().stream()
            .allMatch(
                rule ->
                    rule.getLocale().getId().equals(esLocale.getId())
                        || rule.getLocale().getId().equals(frLocale.getId())));
  }

  @Test
  public void testFindRewriteRules_filtersByRewriteFromPartialMatch() throws Exception {
    Repository repository = this.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(repository.getId(), "hello-world-greeting", "target-a", true));
    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(repository.getId(), "goodbye-world-farewell", "target-b", true));
    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(repository.getId(), "something-else", "target-c", true));

    Page<RewriteRule> filtered =
        rewriteRuleService.findRewriteRules(null, null, null, null, "world", PageRequest.of(0, 10));

    assertEquals(2, filtered.getTotalElements());
    assertTrue(
        filtered.getContent().stream().allMatch(rule -> rule.getRewriteFrom().contains("world")));
  }

  @Test
  public void testFindRewriteRules_rewriteFromFilterReturnsEmptyWhenNoMatch() throws Exception {
    Repository repository = this.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(repository.getId(), "hello-world", "target-a", true));
    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(repository.getId(), "goodbye-world", "target-b", true));

    Page<RewriteRule> filtered =
        rewriteRuleService.findRewriteRules(
            null, null, null, null, "nonexistent", PageRequest.of(0, 10));

    assertEquals(0, filtered.getTotalElements());
  }

  @Test
  public void testFindRewriteRules_combinesRewriteFromWithOtherFilters() throws Exception {
    Repository repository = this.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(repository.getId(), "common-prefix-enabled", "target-a", true));
    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(repository.getId(), "common-prefix-disabled", "target-b", false));
    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(null, "common-prefix-global", "target-c", true));

    Page<RewriteRule> filtered =
        rewriteRuleService.findRewriteRules(
            List.of(repository.getId()), null, true, null, "common-prefix", PageRequest.of(0, 10));

    assertEquals(1, filtered.getTotalElements());
    assertEquals("common-prefix-enabled", filtered.getContent().getFirst().getRewriteFrom());
    assertTrue(filtered.getContent().getFirst().isEnabled());
    assertEquals(repository.getId(), filtered.getContent().getFirst().getRepository().getId());
  }

  @Test
  public void testDeleteRewriteRule_deletesExistingRule() throws Exception {
    Repository repository = this.createRepository(testIdWatcher.getEntityName("rewrite-rule-repo"));

    RewriteRule rewriteRule =
        rewriteRuleService.createRewriteRule(
            newRewriteRuleBody(repository.getId(), "to-delete", "target", true));

    rewriteRuleService.deleteRewriteRule(rewriteRule.getId());

    try {
      rewriteRuleService.getRewriteRuleById(rewriteRule.getId());
      fail("Expected deleted rewrite rule to be missing");
    } catch (RewriteRuleWithIdNotFoundException expected) {
    }
  }

  @Test
  public void testFindActiveRewriteRules_filtersByScopeRepositoryAndLocale() throws Exception {
    Locale defaultLocale = localeService.getDefaultLocale();
    Locale esLocale = this.localeService.findByBcp47Tag("es-ES");

    RepositoryLocale esRepositoryLocale = new RepositoryLocale();
    esRepositoryLocale.setLocale(esLocale);

    Repository firstRepository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("rewrite-rule-repo"),
            "",
            defaultLocale,
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));

    Repository secondRepository =
        repositoryService.createRepository(
            testIdWatcher.getEntityName("rewrite-rule-repo-2"),
            "",
            defaultLocale,
            false,
            Sets.newHashSet(),
            Sets.newHashSet(esRepositoryLocale));

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(null, esLocale.getId(), "global-es", "global", true));

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(firstRepository.getId(), esLocale.getId(), "repo-es", "repo", true));

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(secondRepository.getId(), esLocale.getId(), "repo2-es", "repo2", true));

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(
            firstRepository.getId(), esLocale.getId(), "repo-disabled", "repo-disabled", false));

    rewriteRuleService.createRewriteRule(
        newRewriteRuleBody(null, esLocale.getId(), "global2-es", "global-es", true));

    List<RewriteRule> allRulesForRepositoryAndLocale =
        rewriteRuleService.findActiveRewriteRules(esLocale.getId(), firstRepository.getId());

    List<RewriteRule> globalRulesForLocale =
        allRulesForRepositoryAndLocale.stream()
            .filter(rewriteRule -> rewriteRule.getRepository() == null)
            .toList();

    List<RewriteRule> repositoryRulesForLocale =
        allRulesForRepositoryAndLocale.stream()
            .filter(rewriteRule -> rewriteRule.getRepository() != null)
            .toList();
    ;

    assertEquals(3, allRulesForRepositoryAndLocale.size());
    assertTrue(
        allRulesForRepositoryAndLocale.stream()
            .anyMatch(rule -> "global-es".equals(rule.getRewriteFrom())));
    assertTrue(
        allRulesForRepositoryAndLocale.stream()
            .anyMatch(
                rule ->
                    "repo-es".equals(rule.getRewriteFrom())
                        && rule.getRepository() != null
                        && firstRepository.getId().equals(rule.getRepository().getId())));

    assertEquals(2, globalRulesForLocale.size());
    assertNull(globalRulesForLocale.getFirst().getRepository());
    assertEquals("global-es", globalRulesForLocale.getFirst().getRewriteFrom());

    assertEquals(1, repositoryRulesForLocale.size());
    assertNotNull(repositoryRulesForLocale.getFirst().getRepository());
    assertEquals(
        firstRepository.getId(), repositoryRulesForLocale.getFirst().getRepository().getId());
    assertEquals("repo-es", repositoryRulesForLocale.getFirst().getRewriteFrom());
  }

  private RewriteRuleBody newRewriteRuleBody(
      Long repositoryId, String rewriteFrom, String rewriteTo, boolean enabled) {
    return newRewriteRuleBody(
        repositoryId,
        localeService.findByBcp47Tag("es-ES").getId(),
        rewriteFrom,
        rewriteTo,
        enabled);
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
