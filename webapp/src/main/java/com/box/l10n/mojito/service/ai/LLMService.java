package com.box.l10n.mojito.service.ai;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.AIPrompt;
import com.box.l10n.mojito.entity.AIStringCheck;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.rest.ai.AICheckRequest;
import com.box.l10n.mojito.rest.ai.AICheckResponse;
import com.box.l10n.mojito.rest.ai.AICheckResult;
import com.box.l10n.mojito.service.thirdparty.smartling.glossary.GlossaryTerm;
import java.util.List;

public interface LLMService {

  String SOURCE_STRING_PLACEHOLDER = "[mojito_source_string]";
  String COMMENT_STRING_PLACEHOLDER = "[mojito_comment_string]";
  String CONTEXT_STRING_PLACEHOLDER = "[mojito_context_string]";
  String SOURCE_LOCALE_PLACEHOLDER = "[mojito_source_locale]";
  String TARGET_LOCALE_PLACEHOLDER = "[mojito_target_locale]";
  String PLURAL_FORM_PLACEHOLDER = "[mojito_plural_form]";
  String GLOSSARY_TERM_MATCHES_PLACEHOLDER = "[mojito_glossary_term_matches]";

  /**
   * Executes AI checks on the provided text units.
   *
   * <p>Prompt must include a placeholder for the string to be checked.
   *
   * <p>The placeholder for the string is [mojito_source_string] and must be included in the prompt.
   *
   * <p>Example: "Is the following string correct? [mojito_source_string]"
   *
   * <p>A placeholder [mojito_comment_string] can also be used to include the comment in the prompt,
   * but it is not mandatory.
   *
   * <p>Your prompt must only return responses that align with the {@link AICheckResult} class,
   * otherwise JSON processing will fail and return an error to the caller.
   *
   * <p>e.g. '{"success": true, "suggestedFix": ""}' or '{"success": false, "suggestedFix": "It
   * looks like the word 'tpyo' is spelt incorrectly. It should be 'typo'."}'
   */
  AICheckResponse executeAIChecks(AICheckRequest AICheckRequest);

  default void persistCheckResult(
      AssetExtractorTextUnit textUnit,
      Repository repository,
      AIPrompt prompt,
      String promptOutput,
      AIStringCheckRepository aiStringCheckRepository) {
    AIStringCheck aiStringCheck = new AIStringCheck();
    aiStringCheck.setRepositoryId(repository.getId());
    aiStringCheck.setAiPromptId(prompt.getId());
    aiStringCheck.setContent(textUnit.getSource());
    aiStringCheck.setComment(textUnit.getComments());
    aiStringCheck.setPromptOutput(promptOutput);
    aiStringCheck.setCreatedDate(JSR310Migration.dateTimeNow());
    aiStringCheck.setStringName(textUnit.getName());
    aiStringCheckRepository.save(aiStringCheck);
  }

  String translate(
      TMTextUnit tmTextUnit,
      String sourceBcp47Tag,
      String targetBcp47Tag,
      AIPrompt prompt,
      List<GlossaryTerm> glossaryTerms);

  String translate(
      TMTextUnit tmTextUnit, String sourceBcp47Tag, String targetBcp47Tag, AIPrompt prompt);
}
