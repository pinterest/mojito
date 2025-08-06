package com.box.l10n.mojito.service.pollableTask;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.Drop;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMXliff;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.drop.DropRepository;
import com.box.l10n.mojito.service.drop.DropService;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMXliffRepository;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.time.Duration;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author aloison
 */
public class PollableTaskCleanupServiceTest extends ServiceTestBase {

  static Logger logger = getLogger(PollableTaskCleanupServiceTest.class);

  @Autowired PollableTaskService pollableTaskService;

  @Autowired PollableTaskCleanupService pollableTaskCleanupService;

  @Autowired PollableTaskRepository pollableTaskRepository;

  @Autowired AssetExtractionRepository assetExtractionRepository;

  @Autowired RepositoryService repositoryService;

  @Autowired DropService dropService;

  @Autowired DropRepository dropRepository;

  @Autowired TMService tmService;

  @Autowired AssetService assetService;

  @Autowired TMXliffRepository tmXliffRepository;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Before
  public void finishAllPollableTasks() {
    List<PollableTask> pollableTasks = pollableTaskRepository.findAll();
    for (PollableTask pollableTask : pollableTasks) {
      pollableTask.setFinishedDate(JSR310Migration.newDateTimeEmptyCtor());
    }
    pollableTaskRepository.saveAll(pollableTasks);
  }

  @Test
  public void testMarkZombieTasksAsFinishedWithErrorWithZombies() throws Exception {

    PollableTask pollableTask =
        pollableTaskService.createPollableTask(null, "test-pollable", null, 0, 1);
    PollableTask pollableTaskInPast = setPollableTaskCreatedDateInPast(pollableTask);
    assertFalse(isMarkedAsZombie(pollableTaskInPast));

    pollableTaskCleanupService.finishZombieTasksWithError();

    PollableTask pollableTaskAfterCleanup =
        pollableTaskService.getPollableTask(pollableTaskInPast.getId());
    try {
      assertTrue(isMarkedAsZombie(pollableTaskAfterCleanup));
    } catch (AssertionError ae) {
      logger.error("Make sure the server is configure in UTC");
      throw ae;
    }
  }

  @Transactional
  private PollableTask setPollableTaskCreatedDateInPast(PollableTask pollableTask) {
    ZonedDateTime pastCreatedDate = (pollableTask.getCreatedDate()).minusHours(2);
    pollableTask.setCreatedDate(pastCreatedDate);
    pollableTaskRepository.save(pollableTask);

    return pollableTaskRepository.findById(pollableTask.getId()).orElse(null);
  }

  private boolean isMarkedAsZombie(PollableTask pollableTask) {
    return pollableTask.getFinishedDate() != null
        && pollableTask.getErrorMessage() != null
        && pollableTask.getErrorStack().contains("Zombie task detected");
  }

  @Test
  public void testMarkZombieTasksAsFinishedWithErrorWithoutZombies() throws Exception {

    PollableTask pollableTask =
        pollableTaskService.createPollableTask(null, "test-pollable", null, 0);
    pollableTaskService.finishTask(pollableTask.getId(), null, null, null);
    assertFalse(isMarkedAsZombie(pollableTask));

    pollableTaskCleanupService.finishZombieTasksWithError();

    PollableTask pollableTaskAfterCleanup =
        pollableTaskService.getPollableTask(pollableTask.getId());
    assertFalse(isMarkedAsZombie(pollableTaskAfterCleanup));
  }

  @Transactional
  private PollableTask setPollableTaskFinishedInPast(PollableTask pollableTask, Duration duration) {
    ZonedDateTime pastCreatedDate = (pollableTask.getCreatedDate()).minus(duration);
    pollableTask.setFinishedDate(pastCreatedDate);
    return pollableTaskRepository.save(pollableTask);
  }

  @Test
  public void testCleanStalePollableTaskData_DeletesSuccessfully() {
    PollableTask pollableTask =
        this.pollableTaskService.createPollableTask(null, "test-pollable", null, 0);

    pollableTaskService.finishTask(pollableTask.getId(), null, null, null);
    PollableTask pollableTaskInPast =
        this.setPollableTaskFinishedInPast(pollableTask, Duration.ofDays(5));

    this.pollableTaskCleanupService.cleanStalePollableTaskData(Period.ofDays(4), 10, 1);

    assertThrows(
        NullPointerException.class,
        () -> this.pollableTaskService.getPollableTask(pollableTaskInPast.getId()));

    PollableTask pollableTask2 =
        this.pollableTaskService.createPollableTask(null, "test-pollable-2", null, 0);

    pollableTaskService.finishTask(pollableTask2.getId(), null, null, null);
    PollableTask pollableTaskInPast2 =
        this.setPollableTaskFinishedInPast(
            pollableTask2, Duration.ofDays(5).plus(Duration.ofSeconds(1)));

    this.pollableTaskCleanupService.cleanStalePollableTaskData(Period.ofDays(5), 10, 1);

    assertThrows(
        NullPointerException.class,
        () -> this.pollableTaskService.getPollableTask(pollableTaskInPast2.getId()));
  }

  @Test
  public void testCleanStalePollableTaskData_DeletesParentPollableTaskSuccessfully() {
    PollableTask pollableTask =
        this.pollableTaskService.createPollableTask(null, "test-pollable", null, 0);

    PollableTask childPollableTask =
        this.pollableTaskService.createPollableTask(
            pollableTask.getId(), "test-child-pollable", null, 0);

    pollableTaskService.finishTask(pollableTask.getId(), null, null, null);
    PollableTask pollableTaskInPast =
        this.setPollableTaskFinishedInPast(pollableTask, Duration.ofDays(5));

    this.pollableTaskCleanupService.cleanStalePollableTaskData(Period.ofDays(4), 10, 1);

    assertThrows(
        NullPointerException.class,
        () -> this.pollableTaskService.getPollableTask(pollableTaskInPast.getId()));

    PollableTask updatedChildPollableTask =
        this.pollableTaskService.getPollableTask(childPollableTask.getId());
    assertNull(updatedChildPollableTask.getParentTask());
  }

  @Transactional
  private Drop setExportPollableTask(Drop drop, PollableTask pollableTask) {
    drop.setExportPollableTask(pollableTask);
    return this.dropRepository.save(drop);
  }

  @Transactional
  private Drop setImportPollableTask(Drop drop, PollableTask pollableTask) {
    drop.setImportPollableTask(pollableTask);
    return this.dropRepository.save(drop);
  }

  @Test
  public void testCleanStalePollableTaskData_DeletesPollableTasksWithAssociatedDrop()
      throws RepositoryNameAlreadyUsedException {
    // exportPollableTask
    PollableTask pollableTask =
        this.pollableTaskService.createPollableTask(null, "test-pollable", null, 0);

    Repository repository =
        this.repositoryService.createRepository(this.testIdWatcher.getEntityName("repository"));
    Drop drop = this.dropService.createDrop(repository);
    Drop updatedDrop = this.setExportPollableTask(drop, pollableTask);

    this.pollableTaskService.finishTask(pollableTask.getId(), null, null, null);
    PollableTask pollableTaskInPast =
        this.setPollableTaskFinishedInPast(pollableTask, Duration.ofDays(5));

    assertNotNull(updatedDrop.getExportPollableTask());

    this.pollableTaskCleanupService.cleanStalePollableTaskData(Period.ofDays(4), 10, 1);

    assertThrows(
        NullPointerException.class,
        () -> this.pollableTaskService.getPollableTask(pollableTaskInPast.getId()));

    Optional<Drop> updatedDropOptional = this.dropRepository.findById(updatedDrop.getId());
    assertTrue(updatedDropOptional.isPresent());
    assertNull(updatedDropOptional.get().getExportPollableTask());

    // importPollableTask
    PollableTask pollableTask2 =
        this.pollableTaskService.createPollableTask(null, "test-pollable-2", null, 0);

    Drop drop2 = this.dropService.createDrop(repository);
    Drop updatedDrop2 = this.setImportPollableTask(drop2, pollableTask2);

    this.pollableTaskService.finishTask(pollableTask2.getId(), null, null, null);
    PollableTask pollableTaskInPast2 =
        this.setPollableTaskFinishedInPast(pollableTask2, Duration.ofDays(5));

    assertNotNull(updatedDrop2.getImportPollableTask());

    this.pollableTaskCleanupService.cleanStalePollableTaskData(Period.ofDays(4), 10, 1);

    assertThrows(
        NullPointerException.class,
        () -> this.pollableTaskService.getPollableTask(pollableTaskInPast2.getId()));

    Optional<Drop> updatedDropOptional2 = this.dropRepository.findById(updatedDrop2.getId());
    assertTrue(updatedDropOptional2.isPresent());
    assertNull(updatedDropOptional2.get().getImportPollableTask());
  }

  @Transactional
  private TMXliff setPollableTask(TMXliff tmXliff, PollableTask pollableTask) {
    tmXliff.setPollableTask(pollableTask);
    return this.tmXliffRepository.save(tmXliff);
  }

  @Test
  public void testCleanStalePollableTaskData_DeletesPollableTasksWithAssociatedTMXliff()
      throws RepositoryNameAlreadyUsedException {
    PollableTask pollableTask =
        this.pollableTaskService.createPollableTask(null, "test-pollable", null, 0);

    Repository repository =
        this.repositoryService.createRepository(this.testIdWatcher.getEntityName("repository"));
    Asset asset =
        this.assetService.createAssetWithContent(
            repository.getId(), "test-asset-path.xliff", "test asset content");
    TMXliff tmXliff = this.tmService.createTMXliff(asset.getId(), "en", null, null);
    TMXliff updatedTmXliff = this.setPollableTask(tmXliff, pollableTask);

    this.pollableTaskService.finishTask(pollableTask.getId(), null, null, null);
    PollableTask pollableTaskInPast =
        this.setPollableTaskFinishedInPast(pollableTask, Duration.ofDays(5));

    assertNotNull(updatedTmXliff.getPollableTask());

    this.pollableTaskCleanupService.cleanStalePollableTaskData(Period.ofDays(4), 10, 1);

    assertThrows(
        NullPointerException.class,
        () -> this.pollableTaskService.getPollableTask(pollableTaskInPast.getId()));

    Optional<TMXliff> updatedTmXliffOptional =
        this.tmXliffRepository.findById(updatedTmXliff.getId());
    assertTrue(updatedTmXliffOptional.isPresent());
    assertNull(updatedTmXliffOptional.get().getPollableTask());
  }

  @Test
  public void testCleanStalePollableTaskData_DeletesPollableTasksWithAssociatedAssetExtraction()
      throws RepositoryNameAlreadyUsedException {
    PollableTask pollableTask =
        this.pollableTaskService.createPollableTask(null, "test-pollable", null, 0);

    Repository repository =
        this.repositoryService.createRepository(this.testIdWatcher.getEntityName("repository"));
    Asset asset =
        this.assetService.createAssetWithContent(
            repository.getId(), "test-asset-path.xliff", "test asset content");
    AssetExtraction assetExtraction = new AssetExtraction();
    assetExtraction.setAsset(asset);
    assetExtraction.setPollableTask(pollableTask);
    assetExtraction = assetExtractionRepository.save(assetExtraction);

    this.pollableTaskService.finishTask(pollableTask.getId(), null, null, null);
    PollableTask pollableTaskInPast =
        this.setPollableTaskFinishedInPast(pollableTask, Duration.ofDays(5));

    assertNotNull(assetExtraction.getPollableTask());

    this.pollableTaskCleanupService.cleanStalePollableTaskData(Period.ofDays(4), 10, 1);

    assertThrows(
        NullPointerException.class,
        () -> this.pollableTaskService.getPollableTask(pollableTaskInPast.getId()));

    Optional<AssetExtraction> updatedAssetExtraction =
        this.assetExtractionRepository.findById(assetExtraction.getId());
    assertTrue(updatedAssetExtraction.isPresent());
    assertNull(updatedAssetExtraction.get().getPollableTask());
  }

  @Test
  public void testCleanStalePollableTaskData_CallsDeleteAndUpdateMethodThrice() {
    PollableTask pollableTask1 =
        this.pollableTaskService.createPollableTask(null, "test-pollable-1", null, 0);
    PollableTask pollableTask2 =
        this.pollableTaskService.createPollableTask(null, "test-pollable-2", null, 0);

    PollableTask childPollableTask1 =
        this.pollableTaskService.createPollableTask(
            pollableTask1.getId(), "test-child-pollable-1", null, 0);
    PollableTask childPollableTask2 =
        this.pollableTaskService.createPollableTask(
            pollableTask2.getId(), "test-child-pollable-2", null, 0);

    this.pollableTaskService.finishTask(pollableTask1.getId(), null, null, null);
    this.pollableTaskService.finishTask(pollableTask2.getId(), null, null, null);
    PollableTask pollableTask1InPast =
        this.setPollableTaskFinishedInPast(pollableTask1, Duration.ofDays(5));
    PollableTask pollableTask2InPast =
        this.setPollableTaskFinishedInPast(pollableTask2, Duration.ofDays(6));

    this.pollableTaskCleanupService.cleanStalePollableTaskData(Period.ofDays(4), 1, 2);

    assertThrows(
        NullPointerException.class,
        () -> this.pollableTaskService.getPollableTask(pollableTask1InPast.getId()));
    assertThrows(
        NullPointerException.class,
        () -> this.pollableTaskService.getPollableTask(pollableTask2InPast.getId()));

    PollableTask updatedChildPollableTask1 =
        this.pollableTaskService.getPollableTask(childPollableTask1.getId());
    assertNull(updatedChildPollableTask1.getParentTask());
    PollableTask updatedChildPollableTask2 =
        this.pollableTaskService.getPollableTask(childPollableTask2.getId());
    assertNull(updatedChildPollableTask2.getParentTask());
  }

  @Test
  public void testCleanStalePollableTaskData_DoesNotDeleteAny() {
    PollableTask pollableTask =
        this.pollableTaskService.createPollableTask(null, "test-pollable", null, 0);

    pollableTaskService.finishTask(pollableTask.getId(), null, null, null);
    PollableTask pollableTaskInPast =
        this.setPollableTaskFinishedInPast(pollableTask, Duration.ofDays(5));

    this.pollableTaskCleanupService.cleanStalePollableTaskData(Period.ofDays(6), 10, 1);

    PollableTask pollableTaskAfterCleanup =
        this.pollableTaskService.getPollableTask(pollableTaskInPast.getId());

    assertNotNull(pollableTaskAfterCleanup);
  }

  @Test
  public void testCleanStalePollableTaskData_DeletesOnlyOnePollableTask() {
    PollableTask pollableTask1 =
        this.pollableTaskService.createPollableTask(null, "test-pollable-1", null, 0);
    PollableTask pollableTask2 =
        this.pollableTaskService.createPollableTask(null, "test-pollable-2", null, 0);

    PollableTask childPollableTask1 =
        this.pollableTaskService.createPollableTask(
            pollableTask1.getId(), "test-child-pollable-1", null, 0);
    PollableTask childPollableTask2 =
        this.pollableTaskService.createPollableTask(
            pollableTask2.getId(), "test-child-pollable-2", null, 0);

    this.pollableTaskService.finishTask(pollableTask1.getId(), null, null, null);
    this.pollableTaskService.finishTask(pollableTask2.getId(), null, null, null);
    PollableTask pollableTask1InPast =
        this.setPollableTaskFinishedInPast(pollableTask1, Duration.ofDays(5));
    PollableTask pollableTask2InPast =
        this.setPollableTaskFinishedInPast(pollableTask2, Duration.ofDays(6));

    this.pollableTaskCleanupService.cleanStalePollableTaskData(Period.ofDays(4), 1, 1);

    long pollableTaskCount =
        this.pollableTaskRepository.findAll().stream()
            .filter(
                pollableTask ->
                    Objects.equals(pollableTask.getId(), pollableTask1InPast.getId())
                        || Objects.equals(pollableTask.getId(), pollableTask2InPast.getId()))
            .count();

    assertEquals(1, pollableTaskCount);

    long childPollableTaskCount =
        this.pollableTaskRepository.findAll().stream()
            .filter(
                pollableTask ->
                    Objects.equals(pollableTask.getId(), childPollableTask1.getId())
                        || Objects.equals(pollableTask.getId(), childPollableTask2.getId()))
            .filter(pollableTask -> pollableTask.getParentTask() == null)
            .count();

    assertEquals(1, childPollableTaskCount);
  }
}
