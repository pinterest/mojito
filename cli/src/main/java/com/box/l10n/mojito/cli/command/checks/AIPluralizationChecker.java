package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.apiclient.AIServiceClient;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.function.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIPluralizationChecker extends AbstractCliChecker {
  private static final String PLURALIZATION_PROMPT_NAME = "PLURALIZATION_CHECKER";

  static final String PLURALIZATION_SUGGESTED_FIX_PLACEHOLDER =
      "[mojito_pluralization_suggested_fix]";

  @Autowired private AIServiceClient aiServiceClient;

  private final AIChecker aiChecker;

  public AIPluralizationChecker() {
    this.aiChecker = new AIChecker();
    this.aiChecker.setAssetExtractorTextUnitFunction(
        AIPluralizationChecker::getAssetExtractorTextUnits);
    this.aiChecker.setPromptTypeName(PLURALIZATION_PROMPT_NAME);
  }

  @PostConstruct
  public void init() {
    this.aiChecker.setAiServiceClient(this.aiServiceClient);
  }

  private static boolean isPlural(AssetExtractorTextUnit assetExtractorTextUnit) {
    return !Strings.isNullOrEmpty(assetExtractorTextUnit.getPluralForm());
  }

  private static List<AssetExtractorTextUnit> getAssetExtractorTextUnits(
      List<AssetExtractionDiff> assetExtractionDiffs) {
    List<AssetExtractorTextUnit> textUnits =
        AIChecker.getAssetExtractorTextUnits(assetExtractionDiffs);
    return textUnits.stream().filter(Predicate.not(AIPluralizationChecker::isPlural)).toList();
  }

  @Override
  public CliCheckResult run(List<AssetExtractionDiff> assetExtractionDiffs) {
    CliCheckResult cliCheckResult = this.aiChecker.run(assetExtractionDiffs);
    String pluralizationSuggestedFixMessage =
        this.cliCheckerOptions.getPluralizationSuggestedFixMessage();
    if (pluralizationSuggestedFixMessage != null && !cliCheckResult.isSuccessful()) {
      cliCheckResult.setNotificationText(
          cliCheckResult
              .getNotificationText()
              .replace(PLURALIZATION_SUGGESTED_FIX_PLACEHOLDER, pluralizationSuggestedFixMessage));
    }
    return cliCheckResult;
  }

  @Override
  public void setCliCheckerOptions(CliCheckerOptions options) {
    this.aiChecker.setCliCheckerOptions(options);
    super.setCliCheckerOptions(options);
  }

  @VisibleForTesting
  AIChecker getAiChecker() {
    return this.aiChecker;
  }
}
