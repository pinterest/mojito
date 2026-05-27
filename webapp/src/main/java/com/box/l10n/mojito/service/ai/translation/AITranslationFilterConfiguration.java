package com.box.l10n.mojito.service.ai.translation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "l10n.ai.translation.filter")
public class AITranslationFilterConfiguration {

  private Map<String, RepositoryConfig> repositoryConfig;

  public Map<String, RepositoryConfig> getRepositoryConfig() {
    return repositoryConfig;
  }

  public void setRepositoryConfig(Map<String, RepositoryConfig> repositoryConfig) {
    this.repositoryConfig = repositoryConfig;
  }

  public static class RepositoryConfig {
    private boolean excludePlurals;
    private boolean excludePlaceholders;
    private boolean excludeHtmlTags;
    private String excludePlaceholdersRegex = "\\{[^\\}]*\\}";
    private boolean excludeStringNamePatterns;
    private List<String> excludeStringNamePatternsRegex = new ArrayList<>();

    public boolean shouldExcludePlurals() {
      return excludePlurals;
    }

    public void setExcludePlurals(boolean excludePlurals) {
      this.excludePlurals = excludePlurals;
    }

    public boolean shouldExcludePlaceholders() {
      return excludePlaceholders;
    }

    public void setExcludePlaceholders(boolean excludePlaceholders) {
      this.excludePlaceholders = excludePlaceholders;
    }

    public boolean shouldExcludeHtmlTags() {
      return excludeHtmlTags;
    }

    public void setExcludeHtmlTags(boolean excludeHtmlTags) {
      this.excludeHtmlTags = excludeHtmlTags;
    }

    public String getExcludePlaceholdersRegex() {
      return excludePlaceholdersRegex;
    }

    public void setExcludePlaceholdersRegex(String excludePlaceholdersRegex) {
      this.excludePlaceholdersRegex = excludePlaceholdersRegex;
    }

    public boolean shouldExcludeStringNamePatterns() {
      return excludeStringNamePatterns;
    }

    public void setExcludeStringNamePatterns(boolean excludeStringNamePatterns) {
      this.excludeStringNamePatterns = excludeStringNamePatterns;
    }

    public List<String> getExcludeStringNamePatternsRegex() {
      return excludeStringNamePatternsRegex;
    }

    public void setExcludeStringNamePatternsRegex(List<String> excludeStringNamePatternsRegex) {
      this.excludeStringNamePatternsRegex = excludeStringNamePatternsRegex;
    }
  }
}
