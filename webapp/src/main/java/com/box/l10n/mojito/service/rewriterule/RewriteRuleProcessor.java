package com.box.l10n.mojito.service.rewriterule;

import com.box.l10n.mojito.entity.RewriteRule;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
public class RewriteRuleProcessor {

  private final RewriteRuleService rewriteRuleService;

  public RewriteRuleProcessor(RewriteRuleService rewriteRuleService) {
    this.rewriteRuleService = rewriteRuleService;
  }

  private String resolveVariables(String rewriteTo, String localeTag) {
    Assert.notNull(rewriteTo, "rewriteTo must not be null");
    Assert.notNull(localeTag, "localeTag must not be null");

    Locale locale = Locale.forLanguageTag(localeTag);
    String languageValue = locale.getLanguage() == null ? "" : locale.getLanguage();
    String countryValue = locale.getCountry() == null ? "" : locale.getCountry();

    return rewriteTo
        .replace("$language", languageValue)
        .replace("$country", countryValue)
        .replace("$locale", localeTag);
  }

  private boolean isRepositorySpecific(RewriteRule rewriteRule) {
    return rewriteRule.getRepository() != null;
  }

  private int getRulePriority(RewriteRule rewriteRule) {
    // Rewrite rules currently only model scope-based precedence.
    return this.isRepositorySpecific(rewriteRule) ? 1 : 0;
  }

  public String processExactMatches(
      String content, Long repositoryId, Long localeId, String localeTag) {
    Assert.notNull(repositoryId, "repositoryId must not be null");
    Assert.notNull(localeId, "localeId must not be null");
    Assert.notNull(localeTag, "localeTag must not be null");

    if (!StringUtils.hasLength(content)) {
      return content;
    }

    List<RewriteRule> rewriteRules =
        rewriteRuleService.findActiveRewriteRules(localeId, repositoryId).stream()
            .filter(rewriteRule -> StringUtils.hasLength(rewriteRule.getRewriteFrom()))
            .toList();

    if (rewriteRules.isEmpty()) {
      return content;
    }

    Map<String, List<RewriteRule>> rulesByRewriteFrom = new HashMap<>();
    Trie.TrieBuilder trieBuilder = Trie.builder().ignoreOverlaps();

    rewriteRules.forEach(
        rewriteRule -> {
          trieBuilder.addKeyword(rewriteRule.getRewriteFrom());
          rulesByRewriteFrom
              .computeIfAbsent(rewriteRule.getRewriteFrom(), key -> new ArrayList<>())
              .add(rewriteRule);
        });

    Trie trie = trieBuilder.build();
    Collection<Emit> emits = trie.parseText(content);

    if (emits.isEmpty()) {
      return content;
    }

    List<Match> matches = new ArrayList<>();
    for (Emit emit : emits) {
      List<RewriteRule> emitRules = rulesByRewriteFrom.get(emit.getKeyword());
      emitRules.sort(
          (leftRule, rightRule) ->
              Integer.compare(this.getRulePriority(rightRule), this.getRulePriority(leftRule)));
      RewriteRule emitRule = emitRules.getFirst();
      matches.add(new Match(emit.getStart(), emit.getEnd(), emitRule));
    }

    matches.sort(Comparator.comparingInt(Match::start));

    StringBuilder rewrittenContent = new StringBuilder();
    int currentIndex = 0;

    for (Match selectedCandidate : matches) {
      rewrittenContent.append(content, currentIndex, selectedCandidate.start());
      rewrittenContent.append(resolveVariables(selectedCandidate.rule().getRewriteTo(), localeTag));
      currentIndex = selectedCandidate.end() + 1;
    }

    rewrittenContent.append(content, currentIndex, content.length());
    return rewrittenContent.toString();
  }

  private record Match(int start, int end, RewriteRule rule) {}
}
