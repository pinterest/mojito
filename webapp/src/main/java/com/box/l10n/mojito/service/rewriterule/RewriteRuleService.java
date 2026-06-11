package com.box.l10n.mojito.service.rewriterule;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.RewriteRule;
import com.box.l10n.mojito.rest.EntityWithIdNotFoundException;
import com.box.l10n.mojito.rest.repository.RepositoryWithIdNotFoundException;
import com.box.l10n.mojito.rest.rewriterule.ActiveRewriteRuleWithSameRewriteFromException;
import com.box.l10n.mojito.rest.rewriterule.RepositoryLocaleForRepositoryAndLocaleNotFoundException;
import com.box.l10n.mojito.rest.rewriterule.RewriteRuleBody;
import com.box.l10n.mojito.rest.rewriterule.RewriteRuleScope;
import com.box.l10n.mojito.rest.rewriterule.RewriteRuleWithIdNotFoundException;
import com.box.l10n.mojito.rest.rewriterule.RootRepositoryLocaleNotAllowedForRewriteRuleException;
import com.box.l10n.mojito.service.locale.LocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
public class RewriteRuleService {

  private static final String ACTIVE_REWRITE_FROM_UNIQUE_INDEX =
      "I__REWRITE_RULES__REPO_ID_SCOPE__LOCALE_ID__ACTIVE_REWRITE_FROM";

  private final RewriteRuleRepository rewriteRuleRepository;

  private final RepositoryRepository repositoryRepository;

  private final LocaleRepository localeRepository;

  private final RepositoryLocaleRepository repositoryLocaleRepository;

  public RewriteRuleService(
      RewriteRuleRepository rewriteRuleRepository,
      RepositoryRepository repositoryRepository,
      LocaleRepository localeRepository,
      RepositoryLocaleRepository repositoryLocaleRepository) {
    this.rewriteRuleRepository = rewriteRuleRepository;
    this.repositoryRepository = repositoryRepository;
    this.localeRepository = localeRepository;
    this.repositoryLocaleRepository = repositoryLocaleRepository;
  }

  public Page<RewriteRule> findRewriteRules(
      List<Long> repositoryIds,
      List<Long> localeIds,
      Boolean enabled,
      RewriteRuleScope scope,
      String rewriteFrom,
      Pageable pageable) {
    Specification<RewriteRule> spec =
        (root, query, builder) -> {
          var predicates = new ArrayList<Predicate>();

          if (repositoryIds != null && !repositoryIds.isEmpty()) {
            predicates.add(root.get("repository").get("id").in(repositoryIds));
          }

          if (localeIds != null && !localeIds.isEmpty()) {
            predicates.add(root.get("locale").get("id").in(localeIds));
          }

          if (enabled != null) {
            predicates.add(builder.equal(root.get("enabled"), enabled));
          }

          if (scope == RewriteRuleScope.REPOSITORY) {
            predicates.add(builder.isNotNull(root.get("repository")));
          } else if (scope == RewriteRuleScope.GLOBAL) {
            predicates.add(builder.isNull(root.get("repository")));
          }

          if (rewriteFrom != null && !rewriteFrom.isEmpty()) {
            predicates.add(builder.like(root.get("rewriteFrom"), "%" + rewriteFrom + "%"));
          }

          return builder.and(predicates.toArray(new Predicate[0]));
        };

    return rewriteRuleRepository.findAll(spec, pageable);
  }

  public List<RewriteRule> findActiveRewriteRules(Long localeId, Long repositoryId) {

    Assert.notNull(repositoryId, "repositoryId must not be null");
    Assert.notNull(localeId, "localeId must not be null");

    Specification<RewriteRule> spec =
        (root, query, builder) -> {
          var predicates = new ArrayList<Predicate>();

          predicates.add(builder.equal(root.get("enabled"), true));
          predicates.add(builder.equal(root.get("locale").get("id"), localeId));

          predicates.add(
              builder.or(
                  builder.isNull(root.get("repository")),
                  builder.equal(root.get("repository").get("id"), repositoryId)));

          return builder.and(predicates.toArray(new Predicate[0]));
        };

    return rewriteRuleRepository.findAll(spec);
  }

  public RewriteRule getRewriteRuleById(Long id) throws RewriteRuleWithIdNotFoundException {
    return rewriteRuleRepository
        .findById(id)
        .orElseThrow(() -> new RewriteRuleWithIdNotFoundException(id));
  }

  private void applyBody(RewriteRule rewriteRule, RewriteRuleBody body)
      throws EntityWithIdNotFoundException,
          RepositoryLocaleForRepositoryAndLocaleNotFoundException {

    Locale locale =
        localeRepository
            .findById(body.getLocaleId())
            .orElseThrow(() -> new EntityWithIdNotFoundException("Locale", body.getLocaleId()));

    Repository repository = null;

    if (body.getRepositoryId() != null) {
      repository =
          repositoryRepository
              .findById(body.getRepositoryId())
              .orElseThrow(() -> new RepositoryWithIdNotFoundException(body.getRepositoryId()));

      RepositoryLocale repositoryLocale =
          this.repositoryLocaleRepository.findByRepositoryAndLocale(repository, locale);

      if (repositoryLocale == null) {
        throw new RepositoryLocaleForRepositoryAndLocaleNotFoundException(
            body.getRepositoryId(), body.getLocaleId());
      } else if (repositoryLocale.getParentLocale() == null) {
        throw new RootRepositoryLocaleNotAllowedForRewriteRuleException(
            "Root Repository Locale is not valid for rewrite rules. Repository ID: "
                + body.getRepositoryId()
                + ", Locale ID: "
                + body.getLocaleId());
      }
    }

    rewriteRule.setRepository(repository);
    rewriteRule.setLocale(locale);
    rewriteRule.setRewriteFrom(body.getRewriteFrom());
    rewriteRule.setRewriteTo(body.getRewriteTo());
    rewriteRule.setEnabled(body.getEnabled());
  }

  private RewriteRule saveWithActiveConflictHandling(RewriteRule rewriteRule) {
    try {
      return rewriteRuleRepository.saveAndFlush(rewriteRule);
    } catch (DataIntegrityViolationException ex) {
      if (isActiveRewriteFromUniqueConstraintViolation(ex)) {
        throw new ActiveRewriteRuleWithSameRewriteFromException(rewriteRule.getRewriteFrom());
      }

      throw ex;
    }
  }

  @Transactional
  public RewriteRule createRewriteRule(RewriteRuleBody body)
      throws EntityWithIdNotFoundException,
          RepositoryLocaleForRepositoryAndLocaleNotFoundException {

    RewriteRule rewriteRule = new RewriteRule();
    applyBody(rewriteRule, body);
    assertNoActiveRewriteFromConflict(null, rewriteRule);
    return saveWithActiveConflictHandling(rewriteRule);
  }

  @Transactional
  public RewriteRule updateRewriteRule(Long id, RewriteRuleBody body)
      throws EntityWithIdNotFoundException,
          RepositoryLocaleForRepositoryAndLocaleNotFoundException {

    RewriteRule rewriteRule = getRewriteRuleById(id);

    // Build and validate a detached candidate first to avoid flushing managed state before save.
    RewriteRule candidateRewriteRule = new RewriteRule();
    applyBody(candidateRewriteRule, body);
    assertNoActiveRewriteFromConflict(rewriteRule.getId(), candidateRewriteRule);

    applyBody(rewriteRule, body);
    return saveWithActiveConflictHandling(rewriteRule);
  }

  @Transactional
  public RewriteRule setRewriteRuleEnabled(Long id, boolean enabled)
      throws RewriteRuleWithIdNotFoundException {
    RewriteRule rewriteRule = getRewriteRuleById(id);
    if (enabled) {
      RewriteRule candidateRewriteRule = new RewriteRule();
      candidateRewriteRule.setEnabled(true);
      candidateRewriteRule.setRewriteFrom(rewriteRule.getRewriteFrom());
      candidateRewriteRule.setRepository(rewriteRule.getRepository());
      candidateRewriteRule.setLocale(rewriteRule.getLocale());
      assertNoActiveRewriteFromConflict(rewriteRule.getId(), candidateRewriteRule);
    }
    rewriteRule.setEnabled(enabled);

    return saveWithActiveConflictHandling(rewriteRule);
  }

  @Transactional
  public void deleteRewriteRule(Long id) throws RewriteRuleWithIdNotFoundException {
    RewriteRule rewriteRule = getRewriteRuleById(id);
    rewriteRuleRepository.delete(rewriteRule);
  }

  private boolean isActiveRewriteFromUniqueConstraintViolation(Throwable throwable) {
    Throwable current = throwable;

    while (current != null) {
      String message = current.getMessage();

      if (message != null
          && message
              .toLowerCase(java.util.Locale.ROOT)
              .contains(ACTIVE_REWRITE_FROM_UNIQUE_INDEX.toLowerCase(java.util.Locale.ROOT))) {
        return true;
      }

      current = current.getCause();
    }

    return false;
  }

  private void assertNoActiveRewriteFromConflict(
      Long currentRewriteRuleId, RewriteRule rewriteRule) {

    if (!Boolean.TRUE.equals(rewriteRule.isEnabled())) {
      return;
    }

    Specification<RewriteRule> spec =
        (root, query, builder) -> {
          var predicates = new ArrayList<Predicate>();

          predicates.add(builder.equal(root.get("enabled"), true));
          predicates.add(
              builder.equal(root.get("locale").get("id"), rewriteRule.getLocale().getId()));
          predicates.add(builder.equal(root.get("rewriteFrom"), rewriteRule.getRewriteFrom()));

          if (rewriteRule.getRepository() == null) {
            predicates.add(builder.isNull(root.get("repository")));
          } else {
            predicates.add(
                builder.equal(
                    root.get("repository").get("id"), rewriteRule.getRepository().getId()));
          }

          if (currentRewriteRuleId != null) {
            predicates.add(builder.notEqual(root.get("id"), currentRewriteRuleId));
          }

          return builder.and(predicates.toArray(new Predicate[0]));
        };

    if (rewriteRuleRepository.count(spec) > 0) {
      throw new ActiveRewriteRuleWithSameRewriteFromException(rewriteRule.getRewriteFrom());
    }
  }
}
