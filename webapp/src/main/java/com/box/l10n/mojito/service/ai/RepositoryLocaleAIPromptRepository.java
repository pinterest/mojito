package com.box.l10n.mojito.service.ai;

import com.box.l10n.mojito.entity.RepositoryLocaleAIPrompt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositoryLocaleAIPromptRepository
    extends JpaRepository<RepositoryLocaleAIPrompt, Long> {

  @Query(
      "SELECT rlap.repositoryId FROM RepositoryLocaleAIPrompt rlap WHERE rlap.aiPromptId = :aiPromptId")
  List<Long> findRepositoryIdsByAiPromptId(@Param("aiPromptId") Long aiPromptId);
}
