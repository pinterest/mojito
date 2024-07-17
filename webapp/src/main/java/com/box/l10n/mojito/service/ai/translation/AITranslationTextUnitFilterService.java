package com.box.l10n.mojito.service.ai.translation;

import com.box.l10n.mojito.entity.TMTextUnit;
import jakarta.annotation.PostConstruct;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AITranslationTextUnitFilterService {

  private static Logger logger = LoggerFactory.getLogger(AITranslationTextUnitFilterService.class);
  private static final String HTML_TAG_REGEX = "<[^>]*>";
  private static final Pattern HTML_TAG_PATTERN = Pattern.compile(HTML_TAG_REGEX);

  private Pattern excludePlaceholdersPattern;

  @Value("${l10n.ai.translation.filter.excludePlurals:false}")
  private boolean excludePlurals;

  @Value("${l10n.ai.translation.filter.excludePlaceholders:false}")
  private boolean excludePlaceholders;

  @Value("${l10n.ai.translation.filter.excludeHtmlTags:false}")
  private boolean excludeHtmlTags;

  @Value("${l10n.ai.translation.filter.excludePlaceholders.regex:\\{[^\\}]*\\}}")
  private String excludePlaceholdersRegex;

  public boolean isTranslatable(TMTextUnit tmTextUnit) {
    boolean isTranslatable = true;

    if (excludePlurals) {
      isTranslatable = !isPlural(tmTextUnit);
    }

    if (excludePlaceholders) {
      isTranslatable = isTranslatable && !containsPlaceholder(tmTextUnit);
    }

    if (excludeHtmlTags) {
      isTranslatable = isTranslatable && !containsHtmlTag(tmTextUnit);
    }

    logger.debug(
        "Text unit with name: {} should be translated: {}", tmTextUnit.getName(), isTranslatable);
    return isTranslatable;
  }

  private boolean containsPlaceholder(TMTextUnit tmTextUnit) {
    Matcher matcher = excludePlaceholdersPattern.matcher(tmTextUnit.getContent());
    return matcher.find();
  }

  private boolean isPlural(TMTextUnit tmTextUnit) {
    return tmTextUnit.getPluralForm() != null;
  }

  private boolean containsHtmlTag(TMTextUnit tmTextUnit) {
    Matcher matcher = HTML_TAG_PATTERN.matcher(tmTextUnit.getContent());
    return matcher.find();
  }

  @PostConstruct
  public void init() {
    excludePlaceholdersPattern = Pattern.compile(excludePlaceholdersRegex);
  }
}
