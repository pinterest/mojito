package com.box.l10n.mojito.service.rewriterule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RewriteRule;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RewriteRuleProcessorTest {

  @Mock RewriteRuleService rewriteRuleService;

  RewriteRuleProcessor rewriteRuleProcessor;

  @BeforeEach
  public void before() {
    rewriteRuleProcessor = new RewriteRuleProcessor(rewriteRuleService, new SimpleMeterRegistry());
  }

  @Test
  public void testProcessExactMatches_resolvesLocaleVariables() {
    Long localeId = 5L;
    when(rewriteRuleService.findActiveRewriteRules(eq(localeId), eq(10L)))
        .thenReturn(
            List.of(
                newRule(
                    1L,
                    10L,
                    localeId,
                    "https://example.com/home",
                    "https://example.com/$language/$country/$locale/home")));

    String rewritten =
        rewriteRuleProcessor.processExactMatches(
            "Go to https://example.com/home now", 10L, localeId, "fr-CA");

    assertThat(rewritten).isEqualTo("Go to https://example.com/fr/ca/fr-ca/home now");
  }

  @Test
  public void testProcessExactMatches_prefersLongestOverlappingMatch() {
    Long localeId = 5L;
    when(rewriteRuleService.findActiveRewriteRules(eq(localeId), eq(10L)))
        .thenReturn(
            List.of(
                newRule(1L, null, localeId, "https://example.com/help", "https://global/help"),
                newRule(
                    2L,
                    null,
                    localeId,
                    "https://example.com/help/article",
                    "https://global/help/article")));

    String rewritten =
        rewriteRuleProcessor.processExactMatches(
            "Visit https://example.com/help/article today", 10L, localeId, "fr-CA");

    assertThat(rewritten).isEqualTo("Visit https://global/help/article today");
  }

  @Test
  public void testProcessExactMatches_prefersRepositoryScopedRuleWhenRewriteFromMatches() {
    Long localeId = 5L;
    when(rewriteRuleService.findActiveRewriteRules(eq(localeId), eq(10L)))
        .thenReturn(
            List.of(
                newRule(1L, null, localeId, "https://example.com/help", "https://global/help"),
                newRule(2L, 10L, localeId, "https://example.com/help", "https://repo/help")));

    String rewritten =
        rewriteRuleProcessor.processExactMatches(
            "Visit https://example.com/help today", 10L, localeId, "fr-CA");

    assertThat(rewritten).isEqualTo("Visit https://repo/help today");
  }

  @Test
  public void testProcessExactMatches_returnsNullForNullContent() {
    String rewritten = rewriteRuleProcessor.processExactMatches(null, 10L, 5L, "fr-CA");

    assertThat(rewritten).isNull();
    verifyNoInteractions(rewriteRuleService);
  }

  @Test
  public void testProcessExactMatches_returnsEmptyForEmptyContent() {
    String rewritten = rewriteRuleProcessor.processExactMatches("", 10L, 5L, "fr-CA");

    assertThat(rewritten).isEmpty();
    verifyNoInteractions(rewriteRuleService);
  }

  @Test
  public void testProcessExactMatches_returnsOriginalWhenNoRules() {
    when(rewriteRuleService.findActiveRewriteRules(eq(5L), eq(10L))).thenReturn(List.of());

    String rewritten =
        rewriteRuleProcessor.processExactMatches(
            "Visit https://example.com/help", 10L, 5L, "fr-CA");

    assertThat(rewritten).isEqualTo("Visit https://example.com/help");
  }

  @Test
  public void testProcessExactMatches_ignoresRulesWithoutRewriteFrom() {
    when(rewriteRuleService.findActiveRewriteRules(eq(5L), eq(10L)))
        .thenReturn(
            List.of(
                newRule(1L, 10L, 5L, "", "https://repo/help"),
                newRule(2L, 10L, 5L, null, "https://repo/help")));

    String rewritten =
        rewriteRuleProcessor.processExactMatches(
            "Visit https://example.com/help", 10L, 5L, "fr-CA");

    assertThat(rewritten).isEqualTo("Visit https://example.com/help");
  }

  @Test
  public void testProcessExactMatches_returnsOriginalWhenNoKeywordMatchFound() {
    when(rewriteRuleService.findActiveRewriteRules(eq(5L), eq(10L)))
        .thenReturn(List.of(newRule(1L, 10L, 5L, "https://example.com/help", "https://repo/help")));

    String rewritten =
        rewriteRuleProcessor.processExactMatches(
            "Visit https://example.com/home", 10L, 5L, "fr-CA");

    assertThat(rewritten).isEqualTo("Visit https://example.com/home");
  }

  @Test
  public void testProcessExactMatches_rewritesMultipleNonOverlappingMatches() {
    when(rewriteRuleService.findActiveRewriteRules(eq(5L), eq(10L)))
        .thenReturn(
            List.of(
                newRule(1L, 10L, 5L, "https://example.com/help", "https://repo/help"),
                newRule(2L, 10L, 5L, "https://example.com/docs", "https://repo/docs")));

    String rewritten =
        rewriteRuleProcessor.processExactMatches(
            "Links: https://example.com/help and https://example.com/docs", 10L, 5L, "fr-CA");

    assertThat(rewritten).isEqualTo("Links: https://repo/help and https://repo/docs");
  }

  @Test
  public void testProcessExactMatches_escapesXmlSpecialCharactersInRewriteFrom() {
    when(rewriteRuleService.findActiveRewriteRules(eq(5L), eq(10L)))
        .thenReturn(
            List.of(
                newRule(
                    1L,
                    10L,
                    5L,
                    "https://example.com/help?lang=fr&country=ca&locale=fr-ca&token=<xml>",
                    "https://repo/help?lang=fr")));

    String rewritten =
        rewriteRuleProcessor.processExactMatches(
            "Visit https://example.com/help?lang=fr&amp;country=ca&amp;locale=fr-ca&amp;token=&lt;xml&gt;",
            10L,
            5L,
            "fr-CA");

    assertThat(rewritten).isEqualTo("Visit https://repo/help?lang=fr");
  }

  @Test
  public void testProcessExactMatches_escapesXmlSpecialCharactersInRewriteTo() {
    when(rewriteRuleService.findActiveRewriteRules(eq(5L), eq(10L)))
        .thenReturn(
            List.of(
                newRule(
                    1L,
                    10L,
                    5L,
                    "https://example.com/help",
                    "https://repo/help?lang=fr&country=ca&locale=fr-ca&token=<xml>")));

    String rewritten =
        rewriteRuleProcessor.processExactMatches(
            "Visit https://example.com/help", 10L, 5L, "fr-CA");

    assertThat(rewritten)
        .isEqualTo(
            "Visit https://repo/help?lang=fr&amp;country=ca&amp;locale=fr-ca&amp;token=&lt;xml&gt;");
  }

  @Test
  public void testProcessExactMatches_throwsWhenRepositoryIdIsNull() {
    assertThatThrownBy(
            () ->
                rewriteRuleProcessor.processExactMatches(
                    "Visit https://example.com/help", null, 5L, "fr-CA"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("repositoryId must not be null");
  }

  @Test
  public void testProcessExactMatches_throwsWhenLocaleIdIsNull() {
    assertThatThrownBy(
            () ->
                rewriteRuleProcessor.processExactMatches(
                    "Visit https://example.com/help", 10L, null, "fr-CA"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("localeId must not be null");
  }

  @Test
  public void testProcessExactMatches_throwsWhenLocaleTagIsNull() {
    assertThatThrownBy(
            () ->
                rewriteRuleProcessor.processExactMatches(
                    "Visit https://example.com/help", 10L, 5L, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("localeTag must not be null");
  }

  private RewriteRule newRule(
      Long id, Long repositoryId, Long localeId, String rewriteFrom, String rewriteTo) {
    RewriteRule rewriteRule = new RewriteRule();
    rewriteRule.setId(id);
    rewriteRule.setEnabled(true);
    rewriteRule.setRewriteFrom(rewriteFrom);
    rewriteRule.setRewriteTo(rewriteTo);

    Locale locale = new Locale();
    locale.setId(localeId);
    rewriteRule.setLocale(locale);

    if (repositoryId != null) {
      Repository repository = new Repository();
      repository.setId(repositoryId);
      rewriteRule.setRepository(repository);
    }

    return rewriteRule;
  }
}
