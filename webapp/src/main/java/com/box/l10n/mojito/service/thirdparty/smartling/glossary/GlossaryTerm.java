package com.box.l10n.mojito.service.thirdparty.smartling.glossary;

import java.util.HashMap;
import java.util.Map;

public class GlossaryTerm {

  private final Long tmTextUnitId;
  private final String term;
  private final Map<String, String> translations;
  private final boolean isExactMatch;
  private final boolean isCaseSensitive;
  private final boolean isDoNotTranslate;

  public GlossaryTerm(
      String term,
      boolean isExactMatch,
      boolean isCaseSensitive,
      boolean isDoNotTranslate,
      Long tmTextUnitId) {
    this.term = term;
    this.isExactMatch = isExactMatch;
    this.isCaseSensitive = isCaseSensitive;
    this.isDoNotTranslate = isDoNotTranslate;
    this.translations = new HashMap<>();
    this.tmTextUnitId = tmTextUnitId;
  }

  public Long getTmTextUnitId() {
    return tmTextUnitId;
  }

  public void addLocaleTranslation(String bcp47Tag, String translation) {
    translations.put(bcp47Tag, translation);
  }

  public void getLocaleTranslation(String bcp47Tag) {
    translations.get(bcp47Tag);
  }

  public String getTerm() {
    return term;
  }

  public Map<String, String> getTranslations() {
    return translations;
  }

  public boolean isExactMatch() {
    return isExactMatch;
  }

  public boolean isCaseSensitive() {
    return isCaseSensitive;
  }

  public boolean isDoNotTranslate() {
    return isDoNotTranslate;
  }
}
