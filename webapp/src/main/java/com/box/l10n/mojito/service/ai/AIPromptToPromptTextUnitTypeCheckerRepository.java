package com.box.l10n.mojito.service.ai;

import com.box.l10n.mojito.entity.AIPromptToPromptTextUnitTypeChecker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface AIPromptToPromptTextUnitTypeCheckerRepository
    extends JpaRepository<AIPromptToPromptTextUnitTypeChecker, Long> {}
