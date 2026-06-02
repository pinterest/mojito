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

  private boolean overlaps(boolean[] occupiedIndexes, int start, int end) {
    for (int index = start; index <= end; index++) {
      if (occupiedIndexes[index]) {
        return true;
      }
    }

    return false;
  }

  private void markAsOccupied(boolean[] occupiedIndexes, int start, int end) {
    for (int index = start; index <= end; index++) {
      occupiedIndexes[index] = true;
    }
  }

  private boolean isRepositorySpecific(RewriteRule rewriteRule) {
    return rewriteRule.getRepository() != null;
  }

  private int getRulePriority(RewriteRule rewriteRule) {
    // Rewrite rules currently only model scope-based precedence.
    return this.isRepositorySpecific(rewriteRule) ? 1 : 0;
  }

  private int compare(MatchCandidate left, MatchCandidate right) {
    int lengthCompare = Integer.compare(right.length(), left.length());
    if (lengthCompare != 0) {
      return lengthCompare;
    }

    RewriteRule rightRule = right.rule();
    RewriteRule leftRule = left.rule();
    int priorityCompare =
        Integer.compare(this.getRulePriority(rightRule), this.getRulePriority(leftRule));
    if (priorityCompare != 0) {
      return priorityCompare;
    }

    int startCompare = Integer.compare(left.start(), right.start());
    if (startCompare != 0) {
      return startCompare;
    }

    return Integer.compare(left.end(), right.end());
  }

  private List<MatchCandidate> selectCandidates(
      List<MatchCandidate> candidates, int contentLength) {
    List<MatchCandidate> selectedCandidates = new ArrayList<>();
    candidates.sort(this::compare);

    boolean[] occupiedIndexes = new boolean[contentLength];
    for (MatchCandidate candidate : candidates) {
      if (!this.overlaps(occupiedIndexes, candidate.start(), candidate.end())) {
        this.markAsOccupied(occupiedIndexes, candidate.start(), candidate.end());
        selectedCandidates.add(candidate);
      }
    }

    return selectedCandidates;
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
    Trie.TrieBuilder trieBuilder = Trie.builder();

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

    List<MatchCandidate> matchCandidates = new ArrayList<>();
    for (Emit emit : emits) {
      List<RewriteRule> emitRules = rulesByRewriteFrom.get(emit.getKeyword());

      for (RewriteRule emitRule : emitRules) {
        matchCandidates.add(new MatchCandidate(emit.getStart(), emit.getEnd(), emitRule));
      }
    }

    List<MatchCandidate> selectedCandidates =
        this.selectCandidates(matchCandidates, content.length());

    selectedCandidates.sort(Comparator.comparingInt(MatchCandidate::start));

    StringBuilder rewrittenContent = new StringBuilder();
    int currentIndex = 0;

    for (MatchCandidate selectedCandidate : selectedCandidates) {
      rewrittenContent.append(content, currentIndex, selectedCandidate.start());
      rewrittenContent.append(resolveVariables(selectedCandidate.rule().getRewriteTo(), localeTag));
      currentIndex = selectedCandidate.end() + 1;
    }

    rewrittenContent.append(content, currentIndex, content.length());
    return rewrittenContent.toString();
  }

  private record MatchCandidate(int start, int end, RewriteRule rule) {
    int length() {
      return end - start + 1;
    }
  }
}
