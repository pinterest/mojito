package com.box.l10n.mojito.service.thirdparty.smartling.glossary;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlossaryCache implements Serializable {

  private int maxNGramSize = 1;

  private Map<String, List<GlossaryTerm>> glossaryCache = new HashMap<>();

  public int getMaxNGramSize() {
    return maxNGramSize;
  }

  public void setMaxNGramSize(int maxNGramSize) {
    this.maxNGramSize = maxNGramSize;
  }

  public Map<String, List<GlossaryTerm>> getGlossaryCache() {
    return glossaryCache;
  }

  public void setGlossaryCache(Map<String, List<GlossaryTerm>> glossaryCache) {
    this.glossaryCache = glossaryCache;
  }

  public void addToGlossaryCache(String key, GlossaryTerm glossaryTerms) {
    if (glossaryCache.containsKey(key)) {
      glossaryCache.get(key).add(glossaryTerms);
    } else {
      glossaryCache.put(key, List.of(glossaryTerms));
    }
  }
}
