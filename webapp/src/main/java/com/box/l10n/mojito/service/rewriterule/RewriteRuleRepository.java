package com.box.l10n.mojito.service.rewriterule;

import com.box.l10n.mojito.entity.RewriteRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface RewriteRuleRepository
    extends JpaRepository<RewriteRule, Long>, JpaSpecificationExecutor<RewriteRule> {}
