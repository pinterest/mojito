package com.box.l10n.mojito.okapi.extractor;

import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.okapi.RawDocument;
import com.box.l10n.mojito.okapi.asset.AssetPathToFilterConfigMapper;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.okapi.filters.FilterOptions;
import com.box.l10n.mojito.okapi.steps.CheckForDoNotTranslateStep;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AssetExtractor {

  static Logger logger = LoggerFactory.getLogger(AssetExtractor.class);

  /** Matches the name attribute of a string declaration, for example {@code name="merge_board"} */
  static final Pattern NAME_ATTRIBUTE_PATTERN = Pattern.compile("name\\s*=\\s*[\"']([^\"']+)[\"']");

  @Autowired AssetPathToFilterConfigMapper assetPathToFilterConfigMapper;

  @Autowired IFilterConfigurationMapper filterConfigurationMapper;

  public List<AssetExtractorTextUnit> getAssetExtractorTextUnitsForAsset(
      String assetPath,
      String assetContent,
      FilterConfigIdOverride filterConfigIdOverride,
      List<String> filterOptions)
      throws UnsupportedAssetFilterTypeException {
    return getAssetExtractorTextUnitsForAsset(
        assetPath, assetContent, filterConfigIdOverride, filterOptions, false);
  }

  /**
   * Gets the name to look for in the asset, which for a plural form is the name of the plural group
   * since all the forms are declared in that single block.
   *
   * @param assetExtractorTextUnit the text unit to get the declared name of
   * @return the name or <code>null</code>
   */
  String getNameWithoutPluralForm(AssetExtractorTextUnit assetExtractorTextUnit) {

    String name = assetExtractorTextUnit.getName();
    String pluralForm = assetExtractorTextUnit.getPluralForm();

    if (name != null && pluralForm != null) {
      String pluralFormSuffix = "_" + pluralForm;
      if (name.endsWith(pluralFormSuffix)) {
        name = name.substring(0, name.length() - pluralFormSuffix.length());
      }
    }

    return name;
  }

  /**
   * Adds the line that declares the text unit name in the asset as a usage, for the text units that
   * have no usage provided by their filter.
   *
   * <p>Formats like PO or Mac strings reference the source code they are used in, but formats like
   * Android strings don't, which leaves the checks with no location to report. The declaration line
   * is the location that matters for a pull request anyway: it is the line that was just added or
   * modified.
   *
   * <p>The name declarations are indexed in a single pass over the asset content and the first
   * declaration of a name wins. All the forms of a plural group share the line of the block that
   * declares them.
   *
   * @param assetExtractorTextUnits the text units extracted from the asset
   * @param assetContent the content of the asset the text units were extracted from
   * @param assetPath the path of the asset, used as the file of the usage
   */
  void addUsagesFromNameDeclarationLine(
      List<AssetExtractorTextUnit> assetExtractorTextUnits, String assetContent, String assetPath) {

    Map<String, List<AssetExtractorTextUnit>> textUnitsByDeclaredName = new HashMap<>();

    for (AssetExtractorTextUnit assetExtractorTextUnit : assetExtractorTextUnits) {

      if (assetExtractorTextUnit.getUsages() != null
          && !assetExtractorTextUnit.getUsages().isEmpty()) {
        continue;
      }

      String name = getNameWithoutPluralForm(assetExtractorTextUnit);

      if (name == null || name.isEmpty()) {
        continue;
      }

      textUnitsByDeclaredName
          .computeIfAbsent(name, declaredName -> new ArrayList<>())
          .add(assetExtractorTextUnit);
    }

    Matcher matcher = NAME_ATTRIBUTE_PATTERN.matcher(assetContent);
    int lineNumber = 1;
    int lineScanIndex = 0;

    while (!textUnitsByDeclaredName.isEmpty() && matcher.find()) {

      while (lineScanIndex < matcher.start()) {
        if (assetContent.charAt(lineScanIndex) == '\n') {
          lineNumber++;
        }
        lineScanIndex++;
      }

      List<AssetExtractorTextUnit> textUnitsForName =
          textUnitsByDeclaredName.remove(matcher.group(1));

      if (textUnitsForName != null) {
        Set<String> usages = Set.of(assetPath + ":" + lineNumber);
        for (AssetExtractorTextUnit assetExtractorTextUnit : textUnitsForName) {
          assetExtractorTextUnit.setUsages(usages);
        }
      }
    }
  }

  /**
   * @param usagesFromDeclarationLine to add the declaration line of the text units as a usage when
   *     their filter provides no usage, see {@link #addUsagesFromNameDeclarationLine(List, String,
   *     String)}
   */
  public List<AssetExtractorTextUnit> getAssetExtractorTextUnitsForAsset(
      String assetPath,
      String assetContent,
      FilterConfigIdOverride filterConfigIdOverride,
      List<String> filterOptions,
      boolean usagesFromDeclarationLine)
      throws UnsupportedAssetFilterTypeException {

    logger.debug("Configuring pipeline");
    IPipelineDriver driver = new PipelineDriver();

    driver.addStep(new RawDocumentToFilterEventsStep());
    driver.addStep(new CheckForDoNotTranslateStep());
    AssetExtractionStep assetExtractionStep = new AssetExtractionStep();
    driver.addStep(assetExtractionStep);

    logger.debug("Adding all supported filters to the pipeline driver");
    driver.setFilterConfigurationMapper(filterConfigurationMapper);

    RawDocument rawDocument = new RawDocument(assetContent, LocaleId.ENGLISH);

    String filterConfigId = null;

    if (filterConfigIdOverride != null) {
      filterConfigId = filterConfigIdOverride.getOkapiFilterId();
    } else {
      filterConfigId = assetPathToFilterConfigMapper.getFilterConfigIdFromPath(assetPath);
    }

    rawDocument.setFilterConfigId(filterConfigId);
    logger.debug("Set filter config {} for asset {}", filterConfigId, assetPath);

    logger.debug("Filter options: {}", filterOptions);
    rawDocument.setAnnotation(new FilterOptions(filterOptions));

    driver.addBatchItem(rawDocument);

    logger.debug("Start processing batch");
    driver.processBatch();

    List<AssetExtractorTextUnit> assetExtractorTextUnits =
        assetExtractionStep.getAssetExtractorTextUnits();

    if (usagesFromDeclarationLine) {
      addUsagesFromNameDeclarationLine(assetExtractorTextUnits, assetContent, assetPath);
    }

    return assetExtractorTextUnits;
  }
}
