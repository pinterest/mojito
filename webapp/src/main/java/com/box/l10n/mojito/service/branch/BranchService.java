package com.box.l10n.mojito.service.branch;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchMergeTarget;
import com.box.l10n.mojito.entity.BranchSource;
import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.tm.BranchSourceRepository;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service to manage {@link Branch}es.
 *
 * <p>When no branch name is specified, a branch with name: null will be used.
 *
 * @author jeanaurambault
 */
@Service
public class BranchService {

  /** logger */
  static Logger logger = getLogger(BranchService.class);

  @Autowired BranchRepository branchRepository;

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Autowired BranchSourceRepository branchSourceRepository;

  @Autowired BranchSourceConfig branchSourceConfig;

  @Autowired BranchMergeTargetRepository branchMergeTargetRepository;

  @Value("${l10n.branchService.quartz.schedulerName:" + DEFAULT_SCHEDULER_NAME + "}")
  String schedulerName;

  public Branch createBranch(
      Repository repository, String branchName, User createdByUser, Set<String> branchNotifierIds) {

    logger.debug("createBranch, name: {}, repository id: {}", branchName, repository.getId());

    Branch branch = new Branch();
    branch.setName(branchName);
    branch.setRepository(repository);
    branch.setCreatedByUser(createdByUser);
    branch.setNotifiers(branchNotifierIds);
    branch = branchRepository.save(branch);

    addBranchSource(branch);

    return branch;
  }

  public Branch getUndeletedOrCreateBranch(
      Repository repository,
      String branchName,
      User createdByUser,
      Set<String> branchNotifierIds,
      Boolean branchTargetsMain) {

    logger.debug(
        "getUndeletedOrCreateBranch, name: {}, repository id: {}", branchName, repository.getId());

    Branch branch = branchRepository.findByNameAndRepository(branchName, repository);

    if (branch == null) {
      branch = createBranch(repository, branchName, createdByUser, branchNotifierIds);
    } else if (branch.getDeleted()) {
      undeleteBranch(branch);
    }

    if (branch.getName() != null && branchTargetsMain != null) {
      logger.debug("Setting merge target of branch '{}' to '{}'.", branchName, branchTargetsMain);
      BranchMergeTarget mergeTarget =
          branchMergeTargetRepository.findByBranch(branch).orElse(new BranchMergeTarget());
      mergeTarget.setBranch(branch);
      mergeTarget.setTargetsMain(branchTargetsMain);
      branchMergeTargetRepository.save(mergeTarget);
    }

    return branch;
  }

  public void undeleteBranch(Branch branch) {
    branch.setDeleted(false);
    branchRepository.save(branch);
  }

  public PollableFuture<Void> asyncDeleteBranch(Long repositoryId, Long branchId) {
    DeleteBranchJobInput deleteBranchJobInput = new DeleteBranchJobInput();
    deleteBranchJobInput.setRepositoryId(repositoryId);
    deleteBranchJobInput.setBranchId(branchId);
    String pollableMessage =
        MessageFormat.format(" - Delete branch: {0} from repository: {1}", branchId, repositoryId);
    QuartzJobInfo<DeleteBranchJobInput, Void> quartzJobInfo =
        QuartzJobInfo.newBuilder(DeleteBranchJob.class)
            .withInput(deleteBranchJobInput)
            .withMessage(pollableMessage)
            .withScheduler(schedulerName)
            .build();
    return quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
  }

  public void addBranchSource(Branch branch) {
    // Mojito push links text unit extractions to empty branch, don't attempt to update the source
    if (branch.getName() == null) return;

    com.box.l10n.mojito.service.branch.BranchSource branchSource =
        branchSourceConfig.getRepoOverride().get(branch.getRepository().getName());

    String sourceUrl = (branchSource != null) ? branchSource.getUrl() : branchSourceConfig.getUrl();
    if (Strings.isNullOrEmpty(sourceUrl)) return;

    String url =
        StrSubstitutor.replace(
            sourceUrl, ImmutableMap.of("branchName", branch.getName()), "{", "}");

    BranchSource bSource = new BranchSource();
    bSource.setBranch(branch);
    bSource.setUrl(url);
    try {
      branchSourceRepository.save(bSource);
    } catch (Exception e) {
      logger.error(
          "Failed to save branch source for branch '{}' with url '{}'", branch.getName(), url, e);
    }
  }

  public BranchTextUnitStatusDTO getBranchTextUnitStatuses(String branchName, String repoName)
      throws BranchNotFoundException {
    List<BranchTextUnitStatusDataModel> branchWithTextUnitStatuses =
        branchRepository.findBranchWithTextUnitStatuses(branchName, repoName);
    if (branchWithTextUnitStatuses == null || branchWithTextUnitStatuses.isEmpty()) {
      throw new BranchNotFoundException("Branch could not be found with provided parameters");
    }

    return new BranchTextUnitStatusDTO(
        branchRepository.findBranchWithTextUnitStatuses(branchName, repoName));
  }

  public BranchStatusDTO getBranchStatus(String branchName) throws BranchNotFoundException {

    Branch branch = branchRepository.findFirstByNameAndDeletedFalse(branchName);
    if (branch == null) {
      throw new BranchNotFoundException("Branch not found: " + branchName);
    }

    Repository repository = branch.getRepository();

    BranchStatusDTO dto = new BranchStatusDTO();
    dto.setBranchId(branch.getId());
    dto.setBranchName(branch.getName());
    dto.setRepositoryName(repository.getName());
    dto.setCreatedDate(branch.getCreatedDate());

    BranchStatistic branchStatistic = branch.getBranchStatistic();
    if (branchStatistic != null) {
      dto.setForTranslationCount(branchStatistic.getForTranslationCount());
      dto.setTranslatedDate(branchStatistic.getTranslatedDate());
    }

    Optional<BranchMergeTarget> mergeTarget = branchMergeTargetRepository.findByBranch(branch);
    dto.setSafeI18nEnabled(mergeTarget.isPresent());
    mergeTarget.ifPresent(
        bmt -> {
          if (bmt.getCommit() != null) {
            dto.setMergeTargetCommitName(bmt.getCommit().getName());
          }
        });

    List<BranchTextUnitStatusDataModel> textUnitStatuses =
        branchRepository.findBranchWithTextUnitStatuses(branchName, repository.getName());

    dto.setTextUnits(buildTextUnitDTOs(textUnitStatuses, branch));
    return dto;
  }

  List<BranchStatusTextUnitDTO> buildTextUnitDTOs(
      List<BranchTextUnitStatusDataModel> dataModels, Branch branch) {

    if (dataModels == null || dataModels.isEmpty()) {
      return List.of();
    }

    Map<Long, ZonedDateTime> tmTextUnitCreatedDates =
        Optional.ofNullable(branch.getBranchStatistic())
            .map(
                bs ->
                    bs.getBranchTextUnitStatistics().stream()
                        .collect(
                            Collectors.toMap(
                                btus -> btus.getTmTextUnit().getId(),
                                btus -> btus.getTmTextUnit().getCreatedDate())))
            .orElse(Map.of());

    String srcLocale = dataModels.getFirst().getSrcLocaleBcpTag();

    record TextUnitKey(Long textUnitId, String name, String content, String comment) {}

    return dataModels.stream()
        .filter(dm -> dm.getTextUnitId() != null)
        .filter(dm -> dm.getBcp47Tag() != null)
        .filter(dm -> !dm.getBcp47Tag().equals(srcLocale))
        .filter(BranchTextUnitStatusDataModel::isToBeFullyTranslated)
        .collect(
            Collectors.groupingBy(
                dm ->
                    new TextUnitKey(
                        dm.getTextUnitId(), dm.getTuName(), dm.getTuContent(), dm.getTuComment()),
                Collectors.toList()))
        .entrySet()
        .stream()
        .map(
            entry -> {
              TextUnitKey key = entry.getKey();
              List<BranchTextUnitStatusDataModel> localeEntries = entry.getValue();

              List<String> missingLocales =
                  localeEntries.stream()
                      .filter(dm -> !TMTextUnitVariant.Status.APPROVED.equals(dm.getStatus()))
                      .map(BranchTextUnitStatusDataModel::getBcp47Tag)
                      .sorted()
                      .collect(Collectors.toList());

              BranchStatusTextUnitDTO textUnitDTO = new BranchStatusTextUnitDTO();
              textUnitDTO.setTmTextUnitId(key.textUnitId());
              textUnitDTO.setName(key.name());
              textUnitDTO.setContent(key.content());
              textUnitDTO.setComment(key.comment());
              textUnitDTO.setCreatedDate(tmTextUnitCreatedDates.get(key.textUnitId()));
              textUnitDTO.setForTranslationCount((long) missingLocales.size());
              textUnitDTO.setMissingLocales(missingLocales);
              return textUnitDTO;
            })
        .sorted(
            Comparator.comparing(
                BranchStatusTextUnitDTO::getCreatedDate,
                Comparator.nullsLast(Comparator.reverseOrder())))
        .collect(Collectors.toList());
  }
}
