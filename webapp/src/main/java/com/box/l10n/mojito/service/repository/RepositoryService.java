package com.box.l10n.mojito.service.repository;

import static com.box.l10n.mojito.rest.repository.RepositorySpecification.deletedEquals;
import static com.box.l10n.mojito.rest.repository.RepositorySpecification.nameEquals;
import static com.box.l10n.mojito.specification.Specifications.ifParamNotNull;
import static org.springframework.data.jpa.domain.Specification.where;

import com.box.l10n.mojito.entity.AssetIntegrityChecker;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.RepositoryStatistic;
import com.box.l10n.mojito.entity.ScreenshotRun;
import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.service.assetintegritychecker.AssetIntegrityCheckerRepository;
import com.box.l10n.mojito.service.drop.exporter.DropExporterConfig;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.statistics.RepositoryLocaleStatisticRepository;
import com.box.l10n.mojito.service.repository.statistics.RepositoryStatisticRepository;
import com.box.l10n.mojito.service.screenshot.ScreenshotRunRepository;
import com.box.l10n.mojito.service.tm.TMRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to manage {@link Repository}
 *
 * @author jaurambault
 */
@Service
public class RepositoryService {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepositoryService.class);

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired RepositoryLocaleRepository repositoryLocaleRepository;

  @Autowired AssetIntegrityCheckerRepository assetIntegrityCheckerRepository;

  @Autowired TMRepository tmRepository;

  @Autowired LocaleService localeService;

  @Autowired RepositoryStatisticRepository repositoryStatisticRepository;

  @Autowired RepositoryLocaleStatisticRepository repositoryLocaleStatisticRepository;

  @Autowired DropExporterConfig dropExporterConfiguration;

  @Autowired ScreenshotRunRepository screenshotRunRepository;

  /**
   * Default root locale.
   *
   * <p>TODO(P1) Make this customizable during repository creation. Need to be reviewed when working
   * on the WS/CLI/FE
   */
  private static final String DEFAULT_ROOT_LOCALE = LocaleService.DEFAULT_LOCALE_BCP47_TAG;

  /**
   * Gets all the repositories that are not deleted, ordered by name
   *
   * <p>Could do something similar with criteria and more complex entity graph. When using more
   * complex entity graph with spring it returns duplicates that can be removed using "distinct" but
   * that can't be provided with the basic spring repository. To do the equivalent with criteria:
   *
   * <p>Specifications<Repository> spec =
   * where(deletedEquals(false)).and(ifParamNotNull(nameEquals(repositoryName))); Sort sort = new
   * Sort(Sort.Direction.ASC, "name"); CriteriaBuilder builder = entityManager.getCriteriaBuilder();
   * CriteriaQuery<Repository> query = builder.createQuery(Repository.class); Root<Repository> root
   * = query.from(Repository.class); query.where(spec.toPredicate(root, query, builder));
   * query.distinct(true); query.select(root); query.orderBy(toOrders(sort, root, builder));
   * EntityGraph entityGraph = entityManager.getEntityGraph("repository-view-summary");
   * TypedQuery<Repository> typedQuery = entityManager.createQuery(query);
   * typedQuery.setHint("javax.persistence.loadgraph", entityGraph); List<Repository> repositories =
   * typedQuery.getResultList(); return repositories;
   *
   * @param repositoryName
   * @return
   */
  public List<Repository> findRepositoriesIsNotDeletedOrderByName(String repositoryName) {
    return repositoryRepository.findAll(
        where(deletedEquals(false)).and(ifParamNotNull(nameEquals(repositoryName))),
        Sort.by(Sort.Direction.ASC, "name"));
  }

  /**
   * Creates a {@link Repository}. A {@link TM} is created and linked to the {@link Repository}.
   *
   * @param name the repository name (must not exist already)
   * @param description for the repo to be created
   * @param sourceLocale the source locale of this repository. if null, {@link
   *     LocaleService#DEFAULT_LOCALE_BCP47_TAG} is used
   * @param checkSLA if SLA will be checked for this repository
   * @return the created {@link Repository}
   */
  @Transactional
  public Repository createRepository(
      String name, String description, Locale sourceLocale, Boolean checkSLA)
      throws RepositoryNameAlreadyUsedException {

    logger.debug("Check no repository with name: {} exists", name);

    Repository repository = repositoryRepository.findByName(name);

    if (repository != null) {
      throw new RepositoryNameAlreadyUsedException(name + " is used by other repository");
    }

    logger.debug("Create a Repository with name: {}", name);

    repository = new Repository();
    repository.setName(name);
    repository.setDescription(description);
    repository.setCheckSLA(checkSLA != null ? checkSLA : false);
    repository.setDropExporterType(dropExporterConfiguration.getType());

    if (sourceLocale == null) {
      logger.debug("No source locale provided, use the default locale");
      sourceLocale = localeService.getDefaultLocale();
    }

    repository.setSourceLocale(sourceLocale);

    logger.debug("Create the repository TM");
    TM tm = tmRepository.save(new TM());

    repository.setTm(tm);

    logger.debug("Create the repository stastics entity");
    RepositoryStatistic repositoryStatistic =
        repositoryStatisticRepository.save(new RepositoryStatistic());

    repository.setRepositoryStatistic(repositoryStatistic);

    repository = repositoryRepository.save(repository);

    logger.debug("Create repository id: {} (name: {})", repository.getId(), name);

    addRootLocale(repository, sourceLocale);

    repository = addManualScreenshotRun(repository);

    return repository;
  }

  /**
   * Adds the {@link com.box.l10n.mojito.entity.ScreenshotRun} that will be used to store
   * screenshots uploaded by developpers/manually.
   *
   * <p>This run stays with {@link ScreenshotRun#lastSuccessfulRun} set to {@code false}.
   *
   * @param repository
   * @return
   */
  public Repository addManualScreenshotRun(Repository repository) {
    logger.debug("Create source screenshot run");
    ScreenshotRun screenshotRun = new ScreenshotRun();
    screenshotRun.setRepository(repository);
    screenshotRun.setName("for_source_" + repository.getId() + "_" + UUID.randomUUID().toString());
    screenshotRun = screenshotRunRepository.save(screenshotRun);

    repository.setManualScreenshotRun(screenshotRun);
    repositoryRepository.save(repository);
    return repository;
  }

  /**
   * Wrapper around the {@link #createRepository(String, String, Locale, Boolean)}
   *
   * @param name
   * @return
   */
  public Repository createRepository(String name) throws RepositoryNameAlreadyUsedException {
    return createRepository(name, "", localeService.getDefaultLocale(), false);
  }

  /**
   * Create {@link Repository} and add {@link RepositoryLocale} list at the same time
   *
   * @param name
   * @param description
   * @param sourceLocale
   * @param assetIntegrityCheckers
   * @param repositoryLocales Set of {@link RepositoryLocale}. See {@link
   *     RepositoryService#updateRepositoryLocales} to see requirements
   * @return The created {@link Repository}
   */
  @Transactional(
      rollbackFor = {
        RepositoryLocaleCreationException.class,
        RepositoryNameAlreadyUsedException.class
      })
  public Repository createRepository(
      String name,
      String description,
      Locale sourceLocale,
      Boolean checkSLA,
      Set<AssetIntegrityChecker> assetIntegrityCheckers,
      Set<RepositoryLocale> repositoryLocales)
      throws RepositoryLocaleCreationException, RepositoryNameAlreadyUsedException {

    Repository createdRepo = createRepository(name, description, sourceLocale, checkSLA);

    updateRepositoryLocales(createdRepo, repositoryLocales);
    addIntegrityCheckersToRepository(createdRepo, assetIntegrityCheckers);

    return createdRepo;
  }

  @Transactional
  public void updateAssetIntegrityCheckers(
      Repository repository, Set<AssetIntegrityChecker> assetIntegrityCheckers) {

    Set<AssetIntegrityChecker> existingAssetIntegrityCheckers =
        assetIntegrityCheckerRepository.findByRepository(repository);
    Map<String, Map<String, AssetIntegrityChecker>> existingAssetIntegrityCheckersToDelete =
        getAssetIntegrityCheckerMap(existingAssetIntegrityCheckers);

    for (AssetIntegrityChecker assetIntegrityChecker : assetIntegrityCheckers) {
      logger.debug(
          "Setting repository for integrity checker: " + assetIntegrityChecker.getAssetExtension());
      assetIntegrityChecker.setRepository(repository);
      Map<String, AssetIntegrityChecker> existingIntegrityCheckersMap =
          existingAssetIntegrityCheckersToDelete.get(assetIntegrityChecker.getAssetExtension());
      if (existingIntegrityCheckersMap != null) {
        AssetIntegrityChecker existingIntegrityChecker =
            existingIntegrityCheckersMap.get(
                assetIntegrityChecker.getIntegrityCheckerType().name());
        if (existingIntegrityChecker != null) {
          logger.debug("Updating existing integrity checker: " + assetIntegrityChecker.getId());
          assetIntegrityChecker.setId(existingIntegrityChecker.getId());
          existingIntegrityCheckersMap.remove(
              assetIntegrityChecker.getIntegrityCheckerType().name());
          if (existingIntegrityCheckersMap.isEmpty()) {
            existingAssetIntegrityCheckersToDelete.remove(
                assetIntegrityChecker.getAssetExtension());
          }
        }
      }
    }

    logger.debug("Deleting all unused existing asset integrity checkers for repository");
    for (Map<String, AssetIntegrityChecker> assetIntegrityCheckerMap :
        existingAssetIntegrityCheckersToDelete.values()) {
      for (AssetIntegrityChecker assetIntegrityChecker : assetIntegrityCheckerMap.values()) {
        assetIntegrityCheckerRepository.delete(assetIntegrityChecker);
      }
    }

    if (assetIntegrityCheckers.size() > 0) {
      assetIntegrityCheckerRepository.saveAll(assetIntegrityCheckers);
      logger.debug("Added integrity checkers: " + assetIntegrityCheckers.size());
    }
  }

  /**
   * Returns map of asset extension and {@link AssetIntegrityChecker}
   *
   * @param assetIntegrityCheckers
   * @return
   */
  private Map<String, Map<String, AssetIntegrityChecker>> getAssetIntegrityCheckerMap(
      Set<AssetIntegrityChecker> assetIntegrityCheckers) {
    Map<String, Map<String, AssetIntegrityChecker>> assetIntegrityCheckerMap = new HashMap<>();
    for (AssetIntegrityChecker assetIntegrityChecker : assetIntegrityCheckers) {
      Map<String, AssetIntegrityChecker> checksForAssetExtension =
          assetIntegrityCheckerMap.computeIfAbsent(
              assetIntegrityChecker.getAssetExtension(),
              k -> new HashMap<String, AssetIntegrityChecker>());
      checksForAssetExtension.put(
          assetIntegrityChecker.getIntegrityCheckerType().name(), assetIntegrityChecker);
      assetIntegrityCheckerMap.put(
          assetIntegrityChecker.getAssetExtension(), checksForAssetExtension);
    }
    return assetIntegrityCheckerMap;
  }

  /**
   * Save the List of {@link AssetIntegrityChecker} to {@link Repository}
   *
   * @param repository
   * @param assetIntegrityCheckers
   */
  private void addIntegrityCheckersToRepository(
      Repository repository, Set<AssetIntegrityChecker> assetIntegrityCheckers) {
    for (AssetIntegrityChecker assetIntegrityChecker : assetIntegrityCheckers) {
      logger.debug(
          "Setting repository for integrity checker: " + assetIntegrityChecker.getAssetExtension());
      assetIntegrityChecker.setRepository(repository);
    }

    if (assetIntegrityCheckers.size() > 0) {
      assetIntegrityCheckerRepository.saveAll(assetIntegrityCheckers);
      logger.debug("Added integrity checkers: " + assetIntegrityCheckers.size());
    }
  }

  /**
   * Save the Set of {@link RepositoryLocale}.
   *
   * <p>{
   *
   * @param repositoryLocales
   * @throws com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException @NOTE The Set
   *     contains {@link RepositoryLocale} that don't already exists (ie. {@link
   *     RepositoryLocale#id}s are null).}
   *     <p>{ @NOTE All {@link RepositoryLocale} are expected to be in the same {@link Repository}}
   *     <p>{ @NOTE All {@link RepositoryLocale} in the {@link Repository} will be cleared before
   *     being updated}
   *     <p>{ @NOTE We only expect one level of the hierarchy to be resolved in the input set of
   *     {@link RepositoryLocale}}
   *     <p>{ @NOTE The root locale should not be specified in this set}
   *     <p>For the following example: en-AU ONLY need to specify up to en-CA
   *     <p>en / \ en-CA en-GB / en-AU
   *     <pre>{@code
   * RepositoryLocale enAURepoLocale = new RepositoryLocale();
   * enAURepoLocale.setParentLocale(enCARepoLocale);
   *
   * Set<RepositoryLocale> repositoryLocales = new HashSet<>();
   * set.add(enAURepoLocale);
   * set.add(enCARepoLocale);
   * set.add(enGBRepoLocale);
   *
   * }</pre>
   *     <p>TODO(P1) move RepositoryLocale related into RepositoryLocale service
   */
  @Transactional
  public void updateRepositoryLocales(Set<RepositoryLocale> repositoryLocales)
      throws RepositoryLocaleCreationException {
    updateRepositoryLocales(null, repositoryLocales);
  }

  @Transactional
  public void updateRepositoryLocales(
      Repository repository, Set<RepositoryLocale> repositoryLocales)
      throws RepositoryLocaleCreationException {
    if (repository != null) {
      logger.debug("Updating Repository id with the newly created repository");
      for (RepositoryLocale repositoryLocale : repositoryLocales) {
        repositoryLocale.setRepository(repository);
      }
    }

    logger.debug("Build a Map of RepositoryLocale keyed by bcp47tag");
    Map<String, RepositoryLocale> repositoryLocalesKeyedByBcp47Tag =
        getMapAndCheckConflictAndAssertSameRepository(repositoryLocales);

    Set<Map.Entry<String, RepositoryLocale>> entries = repositoryLocalesKeyedByBcp47Tag.entrySet();
    if (!entries.isEmpty()) {
      Map.Entry<String, RepositoryLocale> repositoryLocaleEntry = entries.iterator().next();
      clearAllRepositoryLocalesExceptForRootLocale(
          repositoryLocaleEntry.getValue().getRepository());
    }

    logger.debug("For each repositoryLocale save it, saving its parent first if needed");
    Set<String> addedBcp47Tags = new HashSet<>();
    for (Map.Entry<String, RepositoryLocale> entrySet : entries) {
      saveRepositoryLocaleWithCycleCheck(
          entrySet.getValue(),
          repositoryLocalesKeyedByBcp47Tag,
          new HashSet<String>(),
          addedBcp47Tags);
    }
  }

  /**
   * Clears all {@link RepositoryLocale} in {@link Repository} but leaves root locale in there.
   *
   * @param repository The Repository for which to clear the RepositoryLocale
   */
  private void clearAllRepositoryLocalesExceptForRootLocale(Repository repository) {
    logger.debug("Clear all RepositoryLocale for Repository: " + repository.getId());

    Long aLong = repositoryLocaleRepository.deleteByRepositoryAndParentLocaleIsNotNull(repository);
    repositoryLocaleStatisticRepository.deleteByRepositoryStatisticId(
        repository.getRepositoryStatistic().getId());

    logger.debug("Deleted RepositoryLocale: " + aLong);
  }

  /**
   * Recursion function used to traverse through the hierarchy chain of the repository locale and
   * check for cycle.
   *
   * @param repositoryLocale
   * @param repositoryLocalesMap
   * @param bcp47TagsInParents
   * @param addedBcp47Tags
   * @throws RepositoryLocaleCreationException
   */
  private void saveRepositoryLocaleWithCycleCheck(
      RepositoryLocale repositoryLocale,
      Map<String, RepositoryLocale> repositoryLocalesMap,
      Set<String> bcp47TagsInParents,
      Set<String> addedBcp47Tags)
      throws RepositoryLocaleCreationException {
    checkCycle(repositoryLocale, bcp47TagsInParents);

    if (repositoryLocale.getParentLocale() != null) {
      logger.debug("Repository locale has a parent, process it first");
      RepositoryLocale parentRepositoryLocaleFromMap =
          repositoryLocalesMap.get(repositoryLocale.getParentLocale().getLocale().getBcp47Tag());
      if (parentRepositoryLocaleFromMap == null) {
        throw new RepositoryLocaleCreationException(
            "The parent RepositoryLocale was not specified in the 1st level set");
      } else {
        saveRepositoryLocaleWithCycleCheck(
            parentRepositoryLocaleFromMap,
            repositoryLocalesMap,
            bcp47TagsInParents,
            addedBcp47Tags);
      }
    }

    String bcp47Tag = repositoryLocale.getLocale().getBcp47Tag();
    if (!addedBcp47Tags.contains(bcp47Tag)) {
      addRepositoryLocale(
          repositoryLocale.getRepository(),
          bcp47Tag,
          repositoryLocale.getParentLocale() != null
              ? repositoryLocale.getParentLocale().getLocale().getBcp47Tag()
              : null,
          repositoryLocale.isToBeFullyTranslated());

      addedBcp47Tags.add(bcp47Tag);
    } else {
      logger.debug(bcp47Tag + " has already been added.  Don't need to add again");
    }
  }

  /**
   * Check in the {@code bcp47TagsInParents} to see if code has encountered this BCP47 Tag before.
   * It not in {@code bcp47TagsInParents}, then add to it
   *
   * @param repositoryLocale
   * @param bcp47TagsInParents
   * @throws RepositoryLocaleCreationException
   */
  private void checkCycle(RepositoryLocale repositoryLocale, Set<String> bcp47TagsInParents)
      throws RepositoryLocaleCreationException {
    logger.debug("Check for cycle: " + repositoryLocale.getLocale().getBcp47Tag());

    String bcp47Tag = repositoryLocale.getLocale().getBcp47Tag();
    if (bcp47TagsInParents.contains(bcp47Tag)) {
      String msg = "Found a cycle in locales [" + bcp47TagsInParents.toString() + "]";
      logger.error(msg);
      throw new RepositoryLocaleCreationException(msg);
    } else {
      logger.trace("No cycle, update for checks when saving parents");
      bcp47TagsInParents.add(bcp47Tag);
    }
  }

  /**
   * Convert to a Map keyed by BCP47 Tag
   *
   * <p>While it is converting to a map, it does the following:
   *
   * <p>1. Checks for conflict {@link RepositoryLocale}. See {@link
   * RepositoryService#checkConflictInMap} 2. Checks for the same {@link Repository} between {@link
   * RepositoryLocale}
   *
   * @param repositoryLocales {@link Set} of {@link RepositoryLocale} to be converted to a {@link
   *     Map}
   * @return
   */
  public Map<String, RepositoryLocale> getMapAndCheckConflictAndAssertSameRepository(
      Set<RepositoryLocale> repositoryLocales) throws RepositoryLocaleCreationException {

    Map<String, RepositoryLocale> hashMap = new HashMap<>();

    Repository repository = null;
    for (RepositoryLocale repositoryLocale : repositoryLocales) {
      String bcp47Tag = repositoryLocale.getLocale().getBcp47Tag();

      if (repository == null) {
        repository = repositoryLocale.getRepository();
      } else if (!repositoryLocale.getRepository().getId().equals(repository.getId())) {
        String msg =
            "All the RepositoryLocale in the Set<RepositoryLocale> should be part of the same Repository";
        logger.error(msg);
        throw new RepositoryLocaleCreationException(msg);
      }

      checkConflictInMap(hashMap, repositoryLocale, bcp47Tag);

      hashMap.put(bcp47Tag, repositoryLocale);
    }

    return hashMap;
  }

  /**
   * Check {@code hashMap} to see if there is an existing and conflicting {@link RepositoryLocale}
   *
   * @param hashMap
   * @param repositoryLocale
   * @param bcp47Tag
   * @throws RepositoryLocaleCreationException
   */
  protected void checkConflictInMap(
      Map<String, RepositoryLocale> hashMap, RepositoryLocale repositoryLocale, String bcp47Tag)
      throws RepositoryLocaleCreationException {
    RepositoryLocale repositoryLocaleFromMap = hashMap.get(bcp47Tag);

    if (repositoryLocaleFromMap != null) {
      RepositoryLocale mapParentRepositoryLocale = repositoryLocaleFromMap.getParentLocale();
      RepositoryLocale parentRepositoryLocale = repositoryLocale.getParentLocale();

      if (mapParentRepositoryLocale != null
          && parentRepositoryLocale != null
          && !mapParentRepositoryLocale
              .getLocale()
              .getBcp47Tag()
              .equals(parentRepositoryLocale.getLocale().getBcp47Tag())) {
        String msg =
            "There is a conflict for "
                + bcp47Tag
                + " with parent locales ["
                + mapParentRepositoryLocale.getLocale().getBcp47Tag()
                + ", "
                + parentRepositoryLocale.getLocale().getBcp47Tag()
                + "]";
        logger.error(msg);
        throw new RepositoryLocaleCreationException(msg);
      } else if (mapParentRepositoryLocale != null && parentRepositoryLocale == null) {
        String msg =
            "There is a conflict for "
                + bcp47Tag
                + " with parent locales ["
                + mapParentRepositoryLocale.getLocale().getBcp47Tag()
                + ", null]";
        logger.error(msg);
        throw new RepositoryLocaleCreationException(msg);
      } else if (mapParentRepositoryLocale == null && parentRepositoryLocale != null) {
        String msg =
            "There is a conflict for "
                + bcp47Tag
                + " with parent locales [null, "
                + parentRepositoryLocale.getLocale().getBcp47Tag()
                + "]";
        logger.error(msg);
        throw new RepositoryLocaleCreationException(msg);
      }
    }
  }

  /**
   * Add {@link RepositoryLocale}. Locale inheritance and copy are not set. The locale is set to be
   * fully translated.
   *
   * @param repository the repository that owns the {@link RepositoryLocale}
   * @param bcp47Tag the locale to be added or updated
   * @return the added {@link RepositoryLocale}
   */
  public RepositoryLocale addRepositoryLocale(Repository repository, String bcp47Tag)
      throws RepositoryLocaleCreationException {
    return addRepositoryLocale(repository, bcp47Tag, null, true);
  }

  /**
   * Adds the root locale into a {@link Repository}.
   *
   * <p>{
   *
   * @param repository the repository that owns the root locale
   * @param bcp47Tag the bcp47 tag of the root locale
   * @return the created {@link RepositoryLocale} that holds the root locale @NOTE Must be called
   *     only once per repository.}
   */
  @Transactional
  protected RepositoryLocale addRootLocale(Repository repository, Locale sourceLocale) {

    logger.debug(
        "Adding the root locale [{}] into repo [{}]",
        sourceLocale.getBcp47Tag(),
        repository.getName());

    checkNoRootLocaleExists(repository);

    RepositoryLocale repositoryLocale = new RepositoryLocale();
    repositoryLocale.setLocale(sourceLocale);
    repositoryLocale.setRepository(repository);
    repositoryLocale.setToBeFullyTranslated(false);

    // TODO(P1) handle this because it will fail if there's a conflict because of unique constract
    // between repo id and locale id
    repositoryLocale = repositoryLocaleRepository.save(repositoryLocale);

    // TODO(P1) Jean: fix test for now. review relationship
    repository.getRepositoryLocales().add(repositoryLocale);

    return repositoryLocale;
  }

  /**
   * Checks that no root locale exists in the {@link Repository}.
   *
   * @param repository repository to be checked
   */
  private void checkNoRootLocaleExists(Repository repository) {
    RepositoryLocale findByRepositoryAndParentLocaleIsNull =
        repositoryLocaleRepository.findByRepositoryAndParentLocaleIsNull(repository);
    if (findByRepositoryAndParentLocaleIsNull != null) {
      throw new RuntimeException(
          "Root locale already exists in repository [" + repository.getId() + "]");
    }
  }

  /**
   * Adds a {@link RepositoryLocale}. This is for adding regular {@link RepositoryLocale} so will
   * use the {@code DEFAULT_ROOT_LOCALE} if no {@code parentLocaleBcp47Tag} is passed in.
   *
   * <p>{
   *
   * @param repository the repository that owns the {@link RepositoryLocale}
   * @param bcp47Tag the locale to be added or updated
   * @param parentLocaleBcp47Tag the locale the {@link RepositoryLocale} will inherit from. {@code
   *     null} means no inheritance will be applied for this locale. See {@link
   *     RepositoryLocale#parentLocale}.
   * @param toBeFullyTranslated {@code true} if this {@link RepositoryLocale} is meant to be fully
   *     translated.
   * @return the added {@link RepositoryLocale} @NOTE Adding a {@link RepositoryLocale} here doesn't
   *     check the hierarchy for cycles or conflict.}
   */
  @Transactional
  public RepositoryLocale addRepositoryLocale(
      Repository repository,
      String bcp47Tag,
      String parentLocaleBcp47Tag,
      boolean toBeFullyTranslated)
      throws RepositoryLocaleCreationException {

    logger.debug(
        "Adding repo locale [{}] with parent locale [{}] to repo [{}]",
        bcp47Tag,
        parentLocaleBcp47Tag,
        repository.getName());

    if (parentLocaleBcp47Tag == null) {
      parentLocaleBcp47Tag = repository.getSourceLocale().getBcp47Tag();

      if (parentLocaleBcp47Tag.equals(bcp47Tag)) {
        throw new RepositoryLocaleCreationException(
            "Locale [" + bcp47Tag + "] cannot be added because it is the root locale");
      }
    }

    Locale locale = localeService.findByBcp47Tag(bcp47Tag);

    RepositoryLocale repositoryLocale = new RepositoryLocale();
    repositoryLocale.setLocale(locale);
    repositoryLocale.setRepository(repository);
    repositoryLocale.setToBeFullyTranslated(toBeFullyTranslated);

    RepositoryLocale parentLocale =
        repositoryLocaleRepository.findByRepositoryAndLocale_Bcp47Tag(
            repository, parentLocaleBcp47Tag);

    if (parentLocale == null) {
      throw new IllegalArgumentException(
          "Parent locale [" + parentLocaleBcp47Tag + "] doesn't exist in repository");
    }

    repositoryLocale.setParentLocale(parentLocale);

    // TODO(P1) handle this because it will fail if there's a conflict because of unique constract
    // between repo id and locale id
    repositoryLocale = repositoryLocaleRepository.save(repositoryLocale);

    // TODO(P1) Jean: fix test for now. review relationship
    repository.getRepositoryLocales().add(repositoryLocale);

    return repositoryLocale;
  }

  /**
   * Get a {@link Set<RepositoryLocale>} without the root locale
   *
   * @param repository
   * @return
   */
  public Set<RepositoryLocale> getRepositoryLocalesWithoutRootLocale(Repository repository) {

    HashSet<RepositoryLocale> repositoryLocalesWithoutRootLocale = new HashSet<>();

    for (RepositoryLocale repositoryLocale : repository.getRepositoryLocales()) {
      if (!isRootRepositoryLocale(repositoryLocale)) {
        repositoryLocalesWithoutRootLocale.add(repositoryLocale);
      }
    }

    return repositoryLocalesWithoutRootLocale;
  }

  /**
   * Checks to see if {@link RepositoryLocale} is a root locale
   *
   * @param repositoryLocale
   * @return
   */
  public boolean isRootRepositoryLocale(RepositoryLocale repositoryLocale) {
    return (repositoryLocale.getParentLocale() == null);
  }

  /**
   * Deletes a {@link Repository} by the {@link Repository#id}. It performs logical delete.
   *
   * @param repository
   */
  @Transactional
  public void deleteRepository(Repository repository) {

    logger.debug("Delete a repository with name: {}", repository.getName());

    // rename the deleted repository so that the name can be reused to create new repository
    String name = "deleted__" + System.currentTimeMillis() + "__" + repository.getName();
    repository.setName(StringUtils.abbreviate(name, Repository.NAME_MAX_LENGTH));
    repository.setDeleted(true);
    repositoryRepository.save(repository);

    logger.debug("Deleted repository with name: {}", repository.getName());
  }

  /**
   * Updates a {@link Repository}.
   *
   * @param repository
   * @param newName
   * @param description
   * @param repositoryLocales
   * @param assetIntegrityCheckers
   * @throws RepositoryLocaleCreationException
   * @throws com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException
   */
  @Transactional
  public void updateRepository(
      Repository repository,
      String newName,
      String description,
      Boolean checkSLA,
      Set<RepositoryLocale> repositoryLocales,
      Set<AssetIntegrityChecker> assetIntegrityCheckers)
      throws RepositoryLocaleCreationException, RepositoryNameAlreadyUsedException {

    logger.debug("Update a repository with name: {}", repository.getName());

    if (newName != null) {
      // check duplicated name
      Repository existingRepository = repositoryRepository.findByName(newName);
      if (existingRepository != null && repository.getId().equals(existingRepository.getId())) {
        throw new RepositoryNameAlreadyUsedException(newName + " is used by other repository");
      }
      repository.setName(newName);
    }
    if (description != null) {
      repository.setDescription(description);
    }

    if (checkSLA != null) {
      repository.setCheckSLA(checkSLA);
    }

    if (newName != null || description != null || checkSLA != null) {
      repositoryRepository.save(repository);
    }

    if (!repositoryLocales.isEmpty()) {
      updateRepositoryLocales(repository, repositoryLocales);
    }

    updateAssetIntegrityCheckers(repository, assetIntegrityCheckers);

    logger.debug("Updated repository with name: {}", repository.getName());
  }
}
