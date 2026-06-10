package com.box.l10n.mojito.rest.rewriterule;

import com.box.l10n.mojito.entity.RewriteRule;
import com.box.l10n.mojito.rest.EntityWithIdNotFoundException;
import com.box.l10n.mojito.service.rewriterule.RewriteRuleService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RewriteRuleWS {

  private final RewriteRuleService rewriteRuleService;

  public RewriteRuleWS(RewriteRuleService rewriteRuleService) {
    this.rewriteRuleService = rewriteRuleService;
  }

  @Operation(summary = "Get paginated rewrite rules")
  @RequestMapping(value = "/api/rewrite-rules", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public Page<RewriteRuleDTO> getRewriteRules(
      @RequestParam(required = false) List<Long> repositoryIds,
      @RequestParam(required = false) List<Long> localeIds,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(required = false) String scope,
      @RequestParam(required = false) String rewriteFrom,
      @ParameterObject @PageableDefault(sort = "id", direction = Sort.Direction.ASC)
          Pageable pageable) {
    RewriteRuleScope parsedScope = scope != null ? RewriteRuleScope.fromValue(scope) : null;

    return rewriteRuleService
        .findRewriteRules(repositoryIds, localeIds, enabled, parsedScope, rewriteFrom, pageable)
        .map(RewriteRuleDTO::fromEntity);
  }

  @RequestMapping(value = "/api/rewrite-rules/{id}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public RewriteRuleDTO getRewriteRuleById(@PathVariable Long id)
      throws RewriteRuleWithIdNotFoundException {
    return RewriteRuleDTO.fromEntity(rewriteRuleService.getRewriteRuleById(id));
  }

  @RequestMapping(value = "/api/rewrite-rules", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.CREATED)
  public RewriteRuleDTO createRewriteRule(@RequestBody RewriteRuleBody body)
      throws EntityWithIdNotFoundException,
          RepositoryLocaleForRepositoryAndLocaleNotFoundException {
    RewriteRule rewriteRule = rewriteRuleService.createRewriteRule(body);
    return RewriteRuleDTO.fromEntity(rewriteRule);
  }

  @RequestMapping(value = "/api/rewrite-rules/{id}", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  public RewriteRuleDTO updateRewriteRule(@PathVariable Long id, @RequestBody RewriteRuleBody body)
      throws EntityWithIdNotFoundException,
          RepositoryLocaleForRepositoryAndLocaleNotFoundException {
    RewriteRule rewriteRule = rewriteRuleService.updateRewriteRule(id, body);
    return RewriteRuleDTO.fromEntity(rewriteRule);
  }

  @RequestMapping(value = "/api/rewrite-rules/{id}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteRewriteRule(@PathVariable Long id) throws RewriteRuleWithIdNotFoundException {
    rewriteRuleService.deleteRewriteRule(id);
  }

  @RequestMapping(value = "/api/rewrite-rules/{id}/enable", method = RequestMethod.PATCH)
  @ResponseStatus(HttpStatus.OK)
  public RewriteRuleDTO enableRewriteRule(@PathVariable Long id)
      throws RewriteRuleWithIdNotFoundException {
    RewriteRule rewriteRule = rewriteRuleService.setRewriteRuleEnabled(id, true);
    return RewriteRuleDTO.fromEntity(rewriteRule);
  }

  @RequestMapping(value = "/api/rewrite-rules/{id}/disable", method = RequestMethod.PATCH)
  @ResponseStatus(HttpStatus.OK)
  public RewriteRuleDTO disableRewriteRule(@PathVariable Long id)
      throws RewriteRuleWithIdNotFoundException {
    RewriteRule rewriteRule = rewriteRuleService.setRewriteRuleEnabled(id, false);
    return RewriteRuleDTO.fromEntity(rewriteRule);
  }
}
