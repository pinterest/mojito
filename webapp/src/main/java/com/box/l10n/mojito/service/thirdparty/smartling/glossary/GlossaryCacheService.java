package com.box.l10n.mojito.service.thirdparty.smartling.glossary;

import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "l10n.glossary.cache.enabled", havingValue = "true")
public class GlossaryCacheService {

  @Autowired GlossaryCacheBlobStorage glossaryCacheBlobStorage;

  @Autowired GlossaryCacheBuilder glossaryCacheBuilder;

  @Autowired StemmerService stemmerService;

  private GlossaryCache glossaryCache;

  /**
   * Get the list of glossary terms matches the for provided text.
   *
   * @param text
   * @return
   */
  public List<GlossaryTerm> getGlossaryTermsInText(String text) {
    if (glossaryCache.size() == 0) {
      // If the cache is empty, load it from blob storage
      loadGlossaryCache();
    }

    // TODO (mallen): Need to handle multiple matches, case sensitive, exact match checking here in
    // a follow-up PR
    return glossaryCache.get(stemmerService.stem(text));
  }

  public void buildGlossaryCache() {
    glossaryCacheBuilder.buildCache();
    loadGlossaryCache();
  }

  public void loadGlossaryCache() {
    glossaryCache = glossaryCacheBlobStorage.getGlossaryCache().orElseGet(GlossaryCache::new);
  }

  @PostConstruct
  private void init() {
    loadGlossaryCache();
  }
}
