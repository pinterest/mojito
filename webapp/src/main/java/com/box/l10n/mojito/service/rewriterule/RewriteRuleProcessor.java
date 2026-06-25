package com.box.l10n.mojito.service.rewriterule;

import com.box.l10n.mojito.entity.RewriteRule;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
public class RewriteRuleProcessor {

  private static final Logger logger = LoggerFactory.getLogger(RewriteRuleProcessor.class);

  private final RewriteRuleService rewriteRuleService;
  private final MeterRegistry meterRegistry;

  public RewriteRuleProcessor(RewriteRuleService rewriteRuleService, MeterRegistry meterRegistry) {
    this.rewriteRuleService = rewriteRuleService;
    this.meterRegistry = meterRegistry;
  }

  private boolean isRepositorySpecific(RewriteRule rewriteRule) {
    return rewriteRule.getRepository() != null;
  }

  private int getRulePriority(RewriteRule rewriteRule) {
    // Rewrite rules currently only model scope-based precedence.
    return this.isRepositorySpecific(rewriteRule) ? 1 : 0;
  }

  private String escapeXml(String xml) {
    return StringEscapeUtils.escapeXml10(StringEscapeUtils.unescapeXml(xml));
  }

  private void applyMatches(
      StringBuilder rewrittenContent, String content, List<Match> matches, String localeTag) {
    Locale locale = Locale.forLanguageTag(localeTag);
    String localeValue = localeTag.toLowerCase(Locale.ROOT);
    String languageValue = locale.getLanguage() == null ? "" : locale.getLanguage();
    String countryValue =
        locale.getCountry() == null ? "" : locale.getCountry().toLowerCase(Locale.ROOT);
    int currentIndex = 0;
    for (Match selectedCandidate : matches) {
      rewrittenContent.append(content, currentIndex, selectedCandidate.start());
      String rewriteTo =
          selectedCandidate
              .rule()
              .getRewriteTo()
              .replace("$language", languageValue)
              .replace("$country", countryValue)
              .replace("$locale", localeValue);
      rewrittenContent.append(this.escapeXml(rewriteTo));
      currentIndex = selectedCandidate.end() + 1;
    }
    rewrittenContent.append(content, currentIndex, content.length());
  }

  public String processExactMatches(
      String content, Long repositoryId, Long localeId, String localeTag) {
    Assert.notNull(repositoryId, "repositoryId must not be null");
    Assert.notNull(localeId, "localeId must not be null");
    Assert.notNull(localeTag, "localeTag must not be null");

    if (!StringUtils.hasLength(content)) {
      return content;
    }

    Timer.Sample timerSample = Timer.start(meterRegistry);

    logger.debug(
        "Processing exact matches for repositoryId={}, localeId={}, localeTag={}, contentLength={}",
        repositoryId,
        localeId,
        localeTag,
        content.length());

    List<RewriteRule> rewriteRules =
        rewriteRuleService.findActiveRewriteRules(localeId, repositoryId).stream()
            .filter(rewriteRule -> StringUtils.hasLength(rewriteRule.getRewriteFrom()))
            .toList();

    if (rewriteRules.isEmpty()) {
      logger.debug(
          "No active rewrite rules found for repositoryId={}, localeId={}", repositoryId, localeId);
      timerSample.stop(
          Timer.builder("RewriteRuleProcessor.processExactMatches")
              .tag("repositoryId", repositoryId.toString())
              .tag("localeTag", localeTag)
              .tag("matched", "false")
              .register(meterRegistry));
      return content;
    }

    logger.debug(
        "Found {} active rewrite rules for repositoryId={}, localeId={}",
        rewriteRules.size(),
        repositoryId,
        localeId);

    Map<String, List<RewriteRule>> rulesByRewriteFrom = new HashMap<>();
    Trie.TrieBuilder trieBuilder = Trie.builder().ignoreOverlaps();

    rewriteRules.forEach(
        rewriteRule -> {
          String rewriteFrom = this.escapeXml(rewriteRule.getRewriteFrom());
          trieBuilder.addKeyword(rewriteFrom);
          rulesByRewriteFrom
              .computeIfAbsent(rewriteFrom, key -> new ArrayList<>())
              .add(rewriteRule);
        });

    Trie trie = trieBuilder.build();
    Collection<Emit> emits = trie.parseText(content);

    if (emits.isEmpty()) {
      logger.debug(
          "No matches found in content for repositoryId={}, localeId={}", repositoryId, localeId);
      timerSample.stop(
          Timer.builder("RewriteRuleProcessor.processExactMatches")
              .tag("repositoryId", repositoryId.toString())
              .tag("localeTag", localeTag)
              .tag("matched", "false")
              .register(meterRegistry));
      return content;
    }

    List<Match> matches = new ArrayList<>();
    for (Emit emit : emits) {
      List<RewriteRule> emitRules = rulesByRewriteFrom.get(emit.getKeyword());
      RewriteRule emitRule =
          emitRules.stream().max(Comparator.comparingInt(this::getRulePriority)).orElseThrow();
      matches.add(new Match(emit.getStart(), emit.getEnd(), emitRule));
    }

    matches.sort(Comparator.comparingInt(Match::start));

    StringBuilder rewrittenContent = new StringBuilder();
    this.applyMatches(rewrittenContent, content, matches, localeTag);

    timerSample.stop(
        Timer.builder("RewriteRuleProcessor.processExactMatches")
            .tag("repositoryId", repositoryId.toString())
            .tag("localeTag", localeTag)
            .tag("matched", "true")
            .register(meterRegistry));

    meterRegistry
        .counter(
            "RewriteRuleProcessor.replacements",
            "repositoryId",
            repositoryId.toString(),
            "localeTag",
            localeTag)
        .increment(matches.size());

    logger.debug(
        "Completed rewrite processing for repositoryId={}, localeId={}, localeTag={}: "
            + "rulesEvaluated={}, matches={}",
        repositoryId,
        localeId,
        localeTag,
        rewriteRules.size(),
        matches.size());

    return rewrittenContent.toString();
  }

  private record Match(int start, int end, RewriteRule rule) {}
}
