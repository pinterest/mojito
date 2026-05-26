package com.box.l10n.mojito.rest.rewriterule;

import com.box.l10n.mojito.entity.RewriteRule;
import com.box.l10n.mojito.rest.EntityWithIdNotFoundException;
import com.box.l10n.mojito.service.rewriterule.RewriteRuleService;
import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired RewriteRuleService rewriteRuleService;

  @Operation(summary = "Get paginated rewrite rules")
  @RequestMapping(value = "/api/rewrite-rules", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public Page<RewriteRuleDTO> getRewriteRules(
      @RequestParam(required = false) Long repositoryId,
      @RequestParam(required = false) Long localeId,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(required = false) RewriteRuleScope scope,
      @ParameterObject @PageableDefault(sort = "id", direction = Sort.Direction.ASC)
          Pageable pageable) {
    return rewriteRuleService
        .findRewriteRules(repositoryId, localeId, enabled, scope, pageable)
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
      throws EntityWithIdNotFoundException {
    RewriteRule rewriteRule = rewriteRuleService.createRewriteRule(body);
    return RewriteRuleDTO.fromEntity(rewriteRule);
  }

  @RequestMapping(value = "/api/rewrite-rules/{id}", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  public RewriteRuleDTO updateRewriteRule(@PathVariable Long id, @RequestBody RewriteRuleBody body)
      throws EntityWithIdNotFoundException {
    RewriteRule rewriteRule = rewriteRuleService.updateRewriteRule(id, body);
    return RewriteRuleDTO.fromEntity(rewriteRule);
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
      throws RewriteRuleWithIdNotFoundException, ActiveRewriteRuleWithSameRewriteFromException {
    RewriteRule rewriteRule = rewriteRuleService.setRewriteRuleEnabled(id, false);
    return RewriteRuleDTO.fromEntity(rewriteRule);
  }
}
