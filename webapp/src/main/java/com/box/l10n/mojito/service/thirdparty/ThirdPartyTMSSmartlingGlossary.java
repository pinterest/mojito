package com.box.l10n.mojito.service.thirdparty;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;
import static com.box.l10n.mojito.service.thirdparty.ThirdPartyTMSSmartling.getSmartlingLocale;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.VirtualAsset;
import com.box.l10n.mojito.service.asset.VirtualAssetBadRequestException;
import com.box.l10n.mojito.service.asset.VirtualAssetRequiredException;
import com.box.l10n.mojito.service.asset.VirtualAssetService;
import com.box.l10n.mojito.service.asset.VirtualAssetTextUnit;
import com.box.l10n.mojito.service.asset.VirutalAssetMissingTextUnitException;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingTBXReader;
import com.box.l10n.mojito.service.thirdparty.smartling.glossary.BuildGlossaryCacheJob;
import com.box.l10n.mojito.service.thirdparty.smartling.glossary.GlossaryCacheConfiguration;
import com.box.l10n.mojito.service.thirdparty.smartling.glossary.GlossaryCacheService;
import com.box.l10n.mojito.service.thirdparty.smartling.glossary.SmartlingGlossaryConfigParameter;
import com.box.l10n.mojito.smartling.SmartlingClient;
import com.box.l10n.mojito.smartling.SmartlingClientException;
import com.box.l10n.mojito.smartling.response.GlossarySourceTerm;
import com.box.l10n.mojito.smartling.response.GlossaryTargetTerm;
import com.box.l10n.mojito.smartling.response.GlossaryTermTranslation;
import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.lib.terminology.ConceptEntry;
import net.sf.okapi.lib.terminology.LangEntry;
import net.sf.okapi.lib.terminology.TermEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Syncs Smartling glossary with a Mojito repository.
 *
 * <p>{@link ThirdPartyTMSSmartling} will redirect request to this class based on an option
 */
@ConditionalOnProperty(value = "l10n.ThirdPartyTMS.impl", havingValue = "ThirdPartyTMSSmartling")
@Component
public class ThirdPartyTMSSmartlingGlossary {

  static Logger logger = LoggerFactory.getLogger(ThirdPartyTMSSmartlingGlossary.class);

  @Autowired VirtualAssetService virtualAssetService;

  @Autowired SmartlingClient smartlingClient;

  @Autowired AssetRepository assetRepository;

  @Autowired AssetTextUnitRepository assetTextUnitRepository;

  @Autowired(required = false)
  GlossaryCacheService glossaryCacheService;

  @Autowired(required = false)
  GlossaryCacheConfiguration glossaryCacheConfiguration;

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Value("${l10n.smartling.accountId:}")
  String accountId;

  public void pullSourceTextUnits(
      Repository repository, String glossaryUID, Map<String, String> localeMapping) {
    String glossaryName = getGlossaryName(glossaryUID);
    String sourceLocale = getSmartlingLocale(localeMapping, getRepositorySourceLocale(repository));
    VirtualAsset virtualAsset = getVirtualAsset(repository, glossaryName);
    List<VirtualAssetTextUnit> textUnits =
        getSourceVirtualAssetTextUnits(glossaryUID, sourceLocale);
    deleteExistingTextUnits(virtualAsset);
    importSourceTextUnits(virtualAsset, textUnits);
  }

  public void pull(Repository repository, String glossaryUID, Map<String, String> localeMapping) {
    String glossaryName = getGlossaryName(glossaryUID);
    VirtualAsset virtualAsset = getVirtualAsset(repository, glossaryName);
    String sourceLocale = getSmartlingLocale(localeMapping, getRepositorySourceLocale(repository));
    repository.getRepositoryLocales().stream()
        .filter(repositoryLocale -> repositoryLocale.getParentLocale() != null)
        .forEach(
            repositoryLocale -> {
              String smartlingLocale =
                  getSmartlingLocale(localeMapping, repositoryLocale.getLocale().getBcp47Tag());
              List<VirtualAssetTextUnit> translatedTextUnits =
                  getTranslatedTextUnits(glossaryUID, smartlingLocale, sourceLocale);
              importLocalizedTextUnits(virtualAsset, repositoryLocale, translatedTextUnits);
            });
    if (glossaryCacheConfiguration != null
        && glossaryCacheConfiguration.getEnabled()
        && glossaryCacheConfiguration.getRepositories().contains(repository.getName())) {
      logger.debug(
          "Glossary cache is enabled and repository {} is configured for glossary cache, updating the cache",
          repository.getName());
      QuartzJobInfo<Void, Void> quartzJobInfo =
          QuartzJobInfo.newBuilder(BuildGlossaryCacheJob.class)
              .withInlineInput(false)
              .withInput(null)
              .withScheduler(DEFAULT_SCHEDULER_NAME)
              .build();
      quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
    }
  }

  public List<ThirdPartyTextUnit> getThirdPartyTextUnits(String glossaryId) {
    String glossaryName = getGlossaryName(glossaryId);
    String glossaryFile = downloadGlossaryFile(glossaryId);
    SmartlingTBXReader tbxReader = new SmartlingTBXReader();
    tbxReader.open(new ByteArrayInputStream(glossaryFile.getBytes(StandardCharsets.UTF_8)));
    List<Optional<ThirdPartyTextUnit>> thirdPartyTextUnits = new ArrayList<>();
    while (tbxReader.hasNext()) {
      thirdPartyTextUnits.add(mapConceptEntryToThirdPartyTextUnit(glossaryName, tbxReader.next()));
    }

    return thirdPartyTextUnits.stream()
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  private void deleteExistingTextUnits(VirtualAsset virtualAsset) {
    Asset asset = assetRepository.findById(virtualAsset.getId()).orElse(null);
    if (asset != null) {
      logger.debug("Deleting text units found for glossary asset: {}", asset.getPath());
      List<AssetTextUnit> assetTextUnits =
          assetTextUnitRepository.findByAssetExtractionId(
              asset.getLastSuccessfulAssetExtraction().getId());

      for (AssetTextUnit assetTextUnit : assetTextUnits) {
        logger.debug(
            "Deleting glossary asset text unit with name: {}, content: {}, perform delete",
            assetTextUnit.getName(),
            assetTextUnit.getContent());
        virtualAssetService.deleteTextUnit(asset.getId(), assetTextUnit.getName());
      }
    }
  }

  private String downloadGlossaryFile(String glossaryId) {
    checkForAccountId();
    return Mono.fromCallable(() -> smartlingClient.downloadGlossaryFile(accountId, glossaryId))
        .retryWhen(
            smartlingClient
                .getRetryConfiguration()
                .doBeforeRetry(
                    e ->
                        logger.info(
                            String.format(
                                "Retrying after failure to download glossary file for glossary id: %s",
                                glossaryId),
                            e.failure())))
        .doOnError(
            e -> {
              String msg =
                  String.format(
                      "Error downloading glossary file from Smartling for glossary id: %s",
                      glossaryId);
              logger.error(msg, e);
              throw new SmartlingClientException(msg, e);
            })
        .blockOptional()
        .orElseThrow(
            () ->
                new SmartlingClientException(
                    String.format(
                        "Error downloading glossary file from Smartling for glossary id: %s, optional is empty",
                        glossaryId)));
  }

  private String getGlossaryName(String glossaryUID) {
    checkForAccountId();
    return Mono.fromCallable(
            () -> smartlingClient.getGlossaryDetails(accountId, glossaryUID).getName())
        .retryWhen(
            smartlingClient
                .getRetryConfiguration()
                .doBeforeRetry(
                    e ->
                        logger.info(
                            String.format(
                                "Retrying after failure to retrieve glossary details from Smartling for glossary id: %s",
                                glossaryUID),
                            e.failure())))
        .doOnError(
            e -> {
              String msg =
                  String.format(
                      "Error retrieving glossary details from Smartling for glossary id: %s",
                      glossaryUID);
              logger.info(msg, e);
              throw new SmartlingClientException(msg, e);
            })
        .blockOptional()
        .orElseThrow(
            () ->
                new SmartlingClientException(
                    String.format(
                        "Error retrieving glossary details from Smartling for glossary id: %s, optional is empty",
                        glossaryUID)));
  }

  private String getRepositorySourceLocale(Repository repository) {
    return repository.getRepositoryLocales().stream()
        .filter(repositoryLocale -> repositoryLocale.getParentLocale() == null)
        .map(repositoryLocale -> repositoryLocale.getLocale().getBcp47Tag())
        .findFirst()
        .orElseThrow(
            () ->
                new ThirdPartyTMSGlossarySyncException(
                    "Unable to find source locale for repository: " + repository.getName()));
  }

  private VirtualAsset getVirtualAsset(Repository repository, String glossaryName) {
    VirtualAsset virtualAsset = new VirtualAsset();
    virtualAsset.setRepositoryId(repository.getId());
    virtualAsset.setPath(glossaryName);
    virtualAsset.setDeleted(false);
    try {
      virtualAsset = virtualAssetService.createOrUpdateVirtualAsset(virtualAsset);
    } catch (VirtualAssetBadRequestException e) {
      throw new ThirdPartyTMSGlossarySyncException(e.getMessage(), e);
    }
    return virtualAsset;
  }

  private void importSourceTextUnits(
      VirtualAsset virtualAsset, List<VirtualAssetTextUnit> textUnits) {
    try {
      virtualAssetService.addTextUnits(virtualAsset.getId(), textUnits);
    } catch (VirtualAssetRequiredException e) {
      logger.error("Error importing source text units to virtual asset: " + e.getMessage(), e);
      throw new ThirdPartyTMSGlossarySyncException(
          "Error importing source text units to virtual asset: " + e.getMessage());
    }
  }

  private void importLocalizedTextUnits(
      VirtualAsset virtualAsset,
      RepositoryLocale repositoryLocale,
      List<VirtualAssetTextUnit> translatedTextUnits) {
    try {
      virtualAssetService.importLocalizedTextUnits(
          virtualAsset.getId(), repositoryLocale.getLocale().getId(), translatedTextUnits);
    } catch (VirtualAssetRequiredException | VirutalAssetMissingTextUnitException e) {
      logger.error("Error importing localized text units to virtual asset: " + e.getMessage(), e);
      throw new ThirdPartyTMSGlossarySyncException(
          "Error importing localized text units to virtual asset: " + e.getMessage(), e);
    }
  }

  private VirtualAssetTextUnit getTranslatedVirtualAssetTextUnit(
      GlossaryTargetTerm glossaryTermTranslation) {
    VirtualAssetTextUnit virtualAssetTextUnit =
        mapGlossaryTermToVirtualAssetTextUnit(glossaryTermTranslation);
    virtualAssetTextUnit.setContent(
        glossaryTermTranslation.getGlossaryTermTranslation().getTranslatedTerm());
    return virtualAssetTextUnit;
  }

  private List<VirtualAssetTextUnit> getSourceVirtualAssetTextUnits(
      String glossaryUID, String sourceLocale) {
    return getGlossarySourceTextUnits(glossaryUID, sourceLocale);
  }

  private List<VirtualAssetTextUnit> getGlossarySourceTextUnits(String glossaryUID, String locale) {
    String glossaryFile = downloadSourceGlossaryFile(glossaryUID, locale);
    SmartlingTBXReader tbxReader = new SmartlingTBXReader();
    tbxReader.open(new ByteArrayInputStream(glossaryFile.getBytes(StandardCharsets.UTF_8)));
    List<GlossarySourceTerm> glossarySourceTerms = new ArrayList<>();
    while (tbxReader.hasNext()) {
      glossarySourceTerms.add(mapConceptEntryToGlossarySourceTerm(locale, tbxReader.next()));
    }
    return glossarySourceTerms.stream()
        .filter(glossarySourceTerm -> !Strings.isNullOrEmpty(glossarySourceTerm.getTermText()))
        .filter(glossarySourceTerm -> !glossarySourceTerm.isArchived())
        .map(this::mapGlossaryTermToVirtualAssetTextUnit)
        .collect(Collectors.toList());
  }

  private String downloadSourceGlossaryFile(String glossaryUID, String locale) {
    checkForAccountId();
    return Mono.fromCallable(
            () -> smartlingClient.downloadSourceGlossaryFile(accountId, glossaryUID, locale))
        .retryWhen(
            smartlingClient
                .getRetryConfiguration()
                .doBeforeRetry(
                    e ->
                        logger.info(
                            String.format(
                                "Retrying after failure to download source file for glossary id: %s",
                                glossaryUID),
                            e.failure())))
        .doOnError(
            e -> {
              String msg =
                  String.format("Error downloading source file for glossary id: %s", glossaryUID);
              logger.error(msg, e);
              throw new SmartlingClientException(msg, e);
            })
        .blockOptional()
        .orElseThrow(
            () ->
                new SmartlingClientException(
                    String.format(
                        "Error downloading source file from Smartling for glossary id: %s, optional is not present",
                        glossaryUID)));
  }

  private void checkForAccountId() {
    if (Strings.isNullOrEmpty(accountId)) {
      throw new ThirdPartyTMSGlossarySyncException("Smartling account id cannot be empty.");
    }
  }

  private Optional<ThirdPartyTextUnit> mapConceptEntryToThirdPartyTextUnit(
      String glossaryName, ConceptEntry conceptEntry) {
    if (conceptEntry.getProperty("archived") != null
        && Boolean.parseBoolean(conceptEntry.getProperty("archived").getValue())) {
      return Optional.empty();
    }
    ThirdPartyTextUnit thirdPartyTextUnit = new ThirdPartyTextUnit();
    thirdPartyTextUnit.setId(conceptEntry.getId());
    thirdPartyTextUnit.setName(conceptEntry.getId());
    thirdPartyTextUnit.setAssetPath(glossaryName);
    return Optional.of(thirdPartyTextUnit);
  }

  private GlossarySourceTerm mapConceptEntryToGlossarySourceTerm(
      String locale, ConceptEntry conceptEntry) {
    GlossarySourceTerm glossarySourceTerm = new GlossarySourceTerm();
    glossarySourceTerm.setTermUid(conceptEntry.getId());
    glossarySourceTerm.setDefinition(
        conceptEntry.getProperty("definition") != null
            ? conceptEntry.getProperty("definition").getValue()
            : "");
    glossarySourceTerm.setPartOfSpeechCode(
        conceptEntry.getProperty("partOfSpeech") != null
            ? conceptEntry.getProperty("partOfSpeech").getValue()
            : "");
    glossarySourceTerm.setTermText(
        conceptEntry.getEntries(LocaleId.fromString(locale)) != null
            ? conceptEntry.getEntries(LocaleId.fromString(locale)).getTerm(0).getText()
            : null);
    glossarySourceTerm.setVariations(extractTermVariations(locale, conceptEntry));
    glossarySourceTerm.setCaseSensitive(
        conceptEntry.getProperty("caseSensitive") != null
            && Boolean.parseBoolean(conceptEntry.getProperty("caseSensitive").getValue()));
    glossarySourceTerm.setExactMatch(
        conceptEntry.getProperty("exactMatch") != null
            && Boolean.parseBoolean(conceptEntry.getProperty("exactMatch").getValue()));
    glossarySourceTerm.setDoNotTranslate(
        conceptEntry.getProperty("doNotTranslate") != null
            && Boolean.parseBoolean(conceptEntry.getProperty("doNotTranslate").getValue()));
    glossarySourceTerm.setArchived(
        conceptEntry.getProperty("archived") != null
            && Boolean.parseBoolean(conceptEntry.getProperty("archived").getValue()));
    return glossarySourceTerm;
  }

  private List<String> extractTermVariations(String locale, ConceptEntry conceptEntry) {
    List<String> termVariations = new ArrayList<>();
    if (conceptEntry.getEntries(LocaleId.fromString(locale)) != null) {
      // Okapi LangEntry class does not provide a way to get the count of terms so keep incrementing
      // until we run out of variations
      int i = 1;
      try {
        while (conceptEntry.getEntries(LocaleId.fromString(locale)).getTerm(i) != null) {
          termVariations.add(
              conceptEntry.getEntries(LocaleId.fromString(locale)).getTerm(i).getText());
          i++;
        }
      } catch (IndexOutOfBoundsException e) {
        logger.debug("No more term variations for glossary term id: {}", conceptEntry.getId());
      }
    }

    return termVariations;
  }

  private List<VirtualAssetTextUnit> getTranslatedTextUnits(
      String glossaryUID, String locale, String sourceLocale) {
    String glossaryFile = downloadTranslatedGlossaryFile(glossaryUID, locale, sourceLocale);
    SmartlingTBXReader tbxReader = new SmartlingTBXReader();
    tbxReader.open(new ByteArrayInputStream(glossaryFile.getBytes(StandardCharsets.UTF_8)));
    List<GlossaryTargetTerm> glossaryTargetTerms = new ArrayList<>();
    while (tbxReader.hasNext()) {
      glossaryTargetTerms.add(
          mapConceptEntryToGlossaryTargetTerm(locale, sourceLocale, tbxReader.next()));
    }
    return glossaryTargetTerms.stream()
        .filter(
            glossaryTargetTerm ->
                !Strings.isNullOrEmpty(glossaryTargetTerm.getTermText())
                    && glossaryTargetTerm.getGlossaryTermTranslation() != null)
        .filter(glossaryTargetTerm -> !glossaryTargetTerm.isArchived())
        .map(this::getTranslatedVirtualAssetTextUnit)
        .collect(Collectors.toList());
  }

  private String downloadTranslatedGlossaryFile(
      String glossaryUID, String locale, String sourceLocale) {
    checkForAccountId();
    return Mono.fromCallable(
            () ->
                smartlingClient.downloadGlossaryFileWithTranslations(
                    accountId, glossaryUID, locale, sourceLocale))
        .retryWhen(
            smartlingClient
                .getRetryConfiguration()
                .doBeforeRetry(
                    e ->
                        logger.info(
                            String.format(
                                "Retrying after failure to download translated file from Smartling for glossary id: %s",
                                glossaryUID),
                            e.failure())))
        .doOnError(
            e -> {
              String msg =
                  String.format(
                      "Error downloading translated glossary file from Smartling for glossary id: %s",
                      glossaryUID);
              logger.error(msg, e);
              throw new SmartlingClientException(msg, e);
            })
        .blockOptional()
        .orElseThrow(
            () ->
                new SmartlingClientException(
                    String.format(
                        "Error downloading translated glossary file from Smartling for glossary id: %s, optional is not present",
                        glossaryUID)));
  }

  private GlossaryTargetTerm mapConceptEntryToGlossaryTargetTerm(
      String locale, String sourceLocale, ConceptEntry conceptEntry) {
    GlossaryTargetTerm glossaryTargetTerm = new GlossaryTargetTerm();
    glossaryTargetTerm.setTermUid(conceptEntry.getId());
    glossaryTargetTerm.setDefinition(
        conceptEntry.getProperty("definition") != null
            ? conceptEntry.getProperty("definition").getValue()
            : "");
    glossaryTargetTerm.setPartOfSpeechCode(
        conceptEntry.getProperty("partOfSpeech") != null
            ? conceptEntry.getProperty("partOfSpeech").getValue()
            : "");
    LangEntry sourceEntry = conceptEntry.getEntries(LocaleId.fromString(sourceLocale));
    glossaryTargetTerm.setTermText(sourceEntry != null ? sourceEntry.getTerm(0).getText() : null);

    LangEntry targetEntry = conceptEntry.getEntries(LocaleId.fromString(locale));
    if (targetEntry != null && targetEntry.hasTerm()) {
      GlossaryTermTranslation glossaryTermTranslation = new GlossaryTermTranslation();
      TermEntry termEntry = targetEntry.getTerm(0);
      glossaryTermTranslation.setTranslatedTerm(termEntry.getText());
      glossaryTargetTerm.setGlossaryTermTranslation(glossaryTermTranslation);
      glossaryTargetTerm.setArchived(
          conceptEntry.getProperty("archived") != null
              && Boolean.parseBoolean(conceptEntry.getProperty("archived").getValue()));
    }

    return glossaryTargetTerm;
  }

  private VirtualAssetTextUnit mapGlossaryTermToVirtualAssetTextUnit(
      GlossarySourceTerm glossarySourceTerm) {
    VirtualAssetTextUnit virtualAssetTextUnit = new VirtualAssetTextUnit();
    virtualAssetTextUnit.setContent(glossarySourceTerm.getTermText());
    virtualAssetTextUnit.setName(getTextUnitName(glossarySourceTerm));
    virtualAssetTextUnit.setComment(getTextUnitComment(glossarySourceTerm));
    return virtualAssetTextUnit;
  }

  private String getTextUnitComment(GlossarySourceTerm glossarySourceTerm) {
    StringBuilder commentBuilder = new StringBuilder();
    commentBuilder.append(glossarySourceTerm.getDefinition());
    if (isPartOfSpeechAvailable(glossarySourceTerm)) {
      commentBuilder.append(" --- POS: ").append(glossarySourceTerm.getPartOfSpeechCode());
    }
    if (glossarySourceTerm.getVariations() != null
        && !glossarySourceTerm.getVariations().isEmpty()) {
      commentBuilder.append(
          String.format(" --- %s: ", SmartlingGlossaryConfigParameter.VARIATIONS));
      commentBuilder.append(String.join(", ", glossarySourceTerm.getVariations()));
    }

    commentBuilder
        .append(String.format(" --- %s: ", SmartlingGlossaryConfigParameter.CASE_SENSITIVE))
        .append(glossarySourceTerm.isCaseSensitive());

    commentBuilder
        .append(String.format(" --- %s: ", SmartlingGlossaryConfigParameter.EXACT_MATCH))
        .append(glossarySourceTerm.isExactMatch());

    commentBuilder
        .append(String.format(" --- %s: ", SmartlingGlossaryConfigParameter.DO_NOT_TRANSLATE))
        .append(glossarySourceTerm.isDoNotTranslate());

    return commentBuilder.toString();
  }

  private String getTextUnitName(GlossarySourceTerm glossarySourceTerm) {
    return glossarySourceTerm.getTermUid();
  }

  private boolean isPartOfSpeechAvailable(GlossarySourceTerm glossarySourceTerm) {
    return !Strings.isNullOrEmpty(glossarySourceTerm.getPartOfSpeechCode())
        && !glossarySourceTerm.getPartOfSpeechCode().equalsIgnoreCase("UNSPECIFIED");
  }
}
