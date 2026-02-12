package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.base.Strings;

public enum PromptTextUnitTypeChecker {
  SINGULAR {
    public boolean hasValidTextUnitType(AssetExtractorTextUnit assetExtractorTextUnit) {
      return !PromptTextUnitTypeChecker.isPlural(assetExtractorTextUnit);
    }
  },
  PLURAL {
    public boolean hasValidTextUnitType(AssetExtractorTextUnit assetExtractorTextUnit) {
      return PromptTextUnitTypeChecker.isPlural(assetExtractorTextUnit);
    }
  },
  ALL {
    public boolean hasValidTextUnitType(AssetExtractorTextUnit assetExtractorTextUnit) {
      return true;
    }
  };

  private static boolean isPlural(AssetExtractorTextUnit assetExtractorTextUnit) {
    return !Strings.isNullOrEmpty(assetExtractorTextUnit.getPluralForm());
  }

  public abstract boolean hasValidTextUnitType(AssetExtractorTextUnit assetExtractorTextUnit);
}
