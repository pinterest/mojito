package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.base.Strings;
import java.util.function.Predicate;

public enum PromptTextUnitType {
  SINGULAR(Predicate.not(PromptTextUnitType::isPlural)),
  PLURAL(PromptTextUnitType::isPlural);
  final Predicate<AssetExtractorTextUnit> checker;

  PromptTextUnitType(Predicate<AssetExtractorTextUnit> predicate) {
    this.checker = predicate;
  }

  private static boolean isPlural(AssetExtractorTextUnit assetExtractorTextUnit) {
    return !Strings.isNullOrEmpty(assetExtractorTextUnit.getPluralForm());
  }

  public Predicate<AssetExtractorTextUnit> getChecker() {
    return this.checker;
  }
}
