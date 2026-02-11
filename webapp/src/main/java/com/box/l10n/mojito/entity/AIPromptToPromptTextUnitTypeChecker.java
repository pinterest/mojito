package com.box.l10n.mojito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_prompt_to_prompt_text_unit_type_checker")
public class AIPromptToPromptTextUnitTypeChecker {
  @Id
  @Column(name = "ai_prompt_id")
  private Long aiPromptId;

  @Enumerated(EnumType.STRING)
  @Column(name = "prompt_text_unit_type_checker")
  private PromptTextUnitTypeChecker promptTextUnitTypeChecker;

  public AIPromptToPromptTextUnitTypeChecker() {}

  public AIPromptToPromptTextUnitTypeChecker(
      Long aiPromptId, PromptTextUnitTypeChecker promptTextUnitTypeChecker) {
    this.aiPromptId = aiPromptId;
    this.promptTextUnitTypeChecker = promptTextUnitTypeChecker;
  }

  public Long getAiPromptId() {
    return aiPromptId;
  }

  public void setAiPromptId(Long aiPromptId) {
    this.aiPromptId = aiPromptId;
  }

  public PromptTextUnitTypeChecker getPromptTextUnitTypeChecker() {
    return promptTextUnitTypeChecker;
  }

  public void setPromptTextUnitTypeChecker(PromptTextUnitTypeChecker promptTextUnitTypeChecker) {
    this.promptTextUnitTypeChecker = promptTextUnitTypeChecker;
  }
}
