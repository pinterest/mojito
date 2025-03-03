package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TmTextUnitPendingMT;
import com.box.l10n.mojito.service.ai.translation.TmTextUnitPendingMTRepository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.AssetMappingService;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pluralform.PluralFormService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.collect.ImmutableMap;
import jakarta.annotation.PostConstruct;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jaurambault
 */
@Configurable
public class TMTestData {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(TMTestData.class);

  @Autowired TMService tmService;

  @Autowired TMRepository tmRepository;

  @Autowired LocaleService localeService;

  @Autowired AssetExtractionRepository assetExtractionRepository;

  @Autowired AssetMappingService assetMappingService;

  @Autowired AssetExtractionService assetExtractionService;

  @Autowired AssetService assetService;

  @Autowired RepositoryService repositoryService;

  @Autowired RepositoryLocaleRepository repositoryLocaleRepository;

  @Autowired PluralFormService pluralFormService;

  @Autowired TmTextUnitPendingMTRepository tmTextUnitPendingMTRepository;

  public Repository repository;
  public TM tm;
  public TMTextUnit addTMTextUnit1;
  public TMTextUnit addTMTextUnit2;
  public TMTextUnit addTMTextUnit3;
  public AssetTextUnit createAssetTextUnit1;
  public AssetTextUnit createAssetTextUnit2;
  public Asset asset;
  public AssetExtraction assetExtraction;
  public TMTextUnitVariant addCurrentTMTextUnitVariant1KoKR;
  public TMTextUnitVariant addCurrentTMTextUnitVariant1FrFR;
  public TMTextUnitVariant addCurrentTMTextUnitVariant3FrFR;
  public TMTextUnitVariant addCurrentTMTextUnitVariant2FrCA;
  public TMTextUnitVariant addCurrentTMTextUnitVariant3FrCA;
  public Locale koKR;
  public Locale frFR;
  public Locale frCA;
  public Locale jaJP;
  public Locale en;
  public RepositoryLocale repoLocaleFrFR;
  public RepositoryLocale repoLocaleKoKR;

  TestIdWatcher testIdWatcher;

  private boolean addUsage;

  private boolean addPendingMTs;

  private ZonedDateTime pendingMTCreatedDate;

  private String repositoryName;

  public TMTestData(TestIdWatcher testIdWatcher) {
    this.testIdWatcher = testIdWatcher;
    this.repositoryName = testIdWatcher.getEntityName("repository");
  }

  public TMTestData(TestIdWatcher testIdWatcher, boolean addUsage) {
    this(testIdWatcher);
    this.addUsage = addUsage;
  }

  public TMTestData(TestIdWatcher testIdWatcher, boolean addUsage, String repositoryName) {
    this(testIdWatcher, addUsage);
    this.repositoryName = testIdWatcher.getEntityName(repositoryName);
  }

  public TMTestData(
      TestIdWatcher testIdWatcher, boolean addUsage, String repositoryName, boolean addPendingMTs) {
    this(
        testIdWatcher,
        addUsage,
        repositoryName,
        addPendingMTs,
        JSR310Migration.newDateTimeEmptyCtor());
  }

  public TMTestData(
      TestIdWatcher testIdWatcher,
      boolean addUsage,
      String repositoryName,
      boolean addPendingMTs,
      ZonedDateTime pendingMTCreatedDate) {
    this(testIdWatcher, addUsage, repositoryName);
    this.addPendingMTs = addPendingMTs;
    this.pendingMTCreatedDate = pendingMTCreatedDate;
  }

  @PostConstruct
  @Transactional
  private void createData() throws Exception {

    logger.debug("Create data set tot test searches");

    en = localeService.findByBcp47Tag("en");
    koKR = localeService.findByBcp47Tag("ko-KR");
    frFR = localeService.findByBcp47Tag("fr-FR");
    frCA = localeService.findByBcp47Tag("fr-CA");
    jaJP = localeService.findByBcp47Tag("ja-JP");

    // This is to make sure data from other TM don't have side effects
    Repository otherRepository =
        repositoryService.createRepository(testIdWatcher.getEntityName("other-repository"));

    TM otherTM = tmRepository.save(new TM());
    Asset otherAsset =
        assetService.createAssetWithContent(
            otherRepository.getId(), "fake_for_test", "fake for test other tm repo");
    TMTextUnit addTMTextUnitOther =
        tmService.addTMTextUnit(
            otherTM.getId(), otherAsset.getId(), "TEST1", "Content1", "Comment1");

    AssetExtraction assetExtractionOther = assetExtractionRepository.save(new AssetExtraction());
    AssetTextUnit createAssetTextUnitOther =
        assetExtractionService.createAssetTextUnit(
            assetExtractionOther, "TEST2", "Content2", "Comment2");

    // This is the actual data that should be proccessed
    repository = repositoryService.createRepository(repositoryName);

    repoLocaleKoKR = repositoryService.addRepositoryLocale(repository, koKR.getBcp47Tag());
    repoLocaleFrFR = repositoryService.addRepositoryLocale(repository, frFR.getBcp47Tag());
    repositoryService.addRepositoryLocale(repository, frCA.getBcp47Tag());
    repositoryService.addRepositoryLocale(repository, jaJP.getBcp47Tag());

    RepositoryLocale parentLocale =
        repositoryLocaleRepository.findByRepositoryAndLocale(repository, frFR);
    RepositoryLocale childLocale =
        repositoryLocaleRepository.findByRepositoryAndLocale(repository, frCA);
    childLocale.setParentLocale(parentLocale);
    childLocale.setToBeFullyTranslated(false);
    repositoryLocaleRepository.save(childLocale);

    tm = repository.getTm();

    asset =
        assetService.createAssetWithContent(repository.getId(), "fake_for_test", "fake for test");
    Long assetId = asset.getId();

    addTMTextUnit1 =
        tmService.addTMTextUnit(
            tm.getId(),
            assetId,
            "zuora_error_message_verify_state_province",
            "Please enter a valid state, region or province",
            "Comment1");
    addTMTextUnit2 = tmService.addTMTextUnit(tm.getId(), assetId, "TEST2", "Content2", "Comment2");
    addTMTextUnit3 = tmService.addTMTextUnit(tm.getId(), assetId, "TEST3", "Content3", "Comment3");

    assetExtraction = new AssetExtraction();
    assetExtraction.setAsset(asset);
    assetExtraction = assetExtractionRepository.save(assetExtraction);

    Branch branch =
        assetExtraction.getAssetContent() != null
            ? assetExtraction.getAssetContent().getBranch()
            : null;
    Set<String> usages = this.addUsage ? Set.of("usage_test") : null;
    createAssetTextUnit1 =
        assetExtractionService.createAssetTextUnit(
            assetExtraction.getId(),
            "zuora_error_message_verify_state_province",
            "Please enter a valid state, region or province",
            "Comment1",
            null,
            null,
            false,
            usages,
            branch);
    createAssetTextUnit2 =
        assetExtractionService.createAssetTextUnit(
            assetExtraction, "TEST2", "Content2", "Comment2");

    assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(
        assetExtraction.getId(), tm.getId(), assetId, null, PollableTask.INJECT_CURRENT_TASK);
    assetExtractionService.markAssetExtractionAsLastSuccessful(asset, assetExtraction);

    addCurrentTMTextUnitVariant1KoKR =
        tmService.addCurrentTMTextUnitVariant(
            addTMTextUnit1.getId(), koKR.getId(), "올바른 국가, 지역 또는 시/도를 입력하십시오.");
    addCurrentTMTextUnitVariant1FrFR =
        tmService.addCurrentTMTextUnitVariant(
            addTMTextUnit1.getId(),
            frFR.getId(),
            "Veuillez indiquer un état, une région ou une province valide.");
    addCurrentTMTextUnitVariant3FrFR =
        tmService.addCurrentTMTextUnitVariant(
            addTMTextUnit3.getId(), frFR.getId(), "Content3 fr-FR");
    addCurrentTMTextUnitVariant2FrCA =
        tmService.addCurrentTMTextUnitVariant(
            addTMTextUnit2.getId(), frCA.getId(), "Content2 fr-CA");
    addCurrentTMTextUnitVariant3FrCA =
        tmService.addCurrentTMTextUnitVariant(
            addTMTextUnit3.getId(), frCA.getId(), "Content3 fr-CA");

    if (addPendingMTs) {
      TmTextUnitPendingMT tmTextUnitPendingMT = new TmTextUnitPendingMT();
      tmTextUnitPendingMT.setTmTextUnitId(addTMTextUnit1.getId());
      tmTextUnitPendingMT.setCreatedDate(pendingMTCreatedDate);
      tmTextUnitPendingMTRepository.save(tmTextUnitPendingMT);
    }
  }

  public ImmutableMap<String, TMTextUnit> addPluralString(String basename) {
    ZonedDateTime date = JSR310Migration.newDateTimeEmptyCtor();

    ImmutableMap<String, TMTextUnit> tmTextUnitsByForm =
        Stream.of("other", "zero", "one", "two", "few", "many")
            .map(form -> addPluralForm(basename, form, date))
            .collect(
                ImmutableMap.toImmutableMap(t -> t.getPluralForm().getName(), Function.identity()));

    return tmTextUnitsByForm;
  }

  TMTextUnit addPluralForm(String basename, String pluralForm, ZonedDateTime date) {
    return tmService.addTMTextUnit(
        tm.getId(),
        asset.getId(),
        basename + "_" + pluralForm,
        basename + "_" + pluralForm + " content",
        null,
        null,
        date,
        pluralFormService.findByPluralFormString(pluralForm),
        basename + "_other",
        en.getId());
  }
}
