package com.box.l10n.mojito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "repository_locale_ai_prompt",
    indexes = {
      @Index(
          name = "I__REPOSITORY_LOCALE_AI_PROMPT__REPO_ID__AI_PROMPT_ID",
          columnList = "repository_id, ai_prompt_id")
    })
public class RepositoryLocaleAIPrompt extends BaseEntity {

  @Column(name = "repository_id")
  private Long repositoryId;

  @Column(name = "locale_id")
  private Long localeId;

  @Column(name = "ai_prompt_id")
  private Long aiPromptId;

  @Column(name = "disabled")
  private boolean disabled;

  public Long getAiPromptId() {
    return aiPromptId;
  }

  public void setAiPromptId(Long aiPromptId) {
    this.aiPromptId = aiPromptId;
  }

  public long getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(long repositoryId) {
    this.repositoryId = repositoryId;
  }

  public Long getLocaleId() {
    return localeId;
  }

  public void setLocaleId(Long localeId) {
    this.localeId = localeId;
  }

  public boolean isDisabled() {
    return disabled;
  }

  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }
}
