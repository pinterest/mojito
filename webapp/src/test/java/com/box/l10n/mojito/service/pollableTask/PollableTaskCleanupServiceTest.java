package com.box.l10n.mojito.service.pollableTask;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.service.DBUtils;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import java.time.Duration;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.Assume;
import org.junit.Before;
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

  @Autowired DBUtils dbUtils;

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
    pollableTaskRepository.save(pollableTask);

    return pollableTaskRepository.findById(pollableTask.getId()).orElse(null);
  }

  @Test
  public void testCleanOldPollableTaskData_DeletesSuccessfully() {
    Assume.assumeTrue(this.dbUtils.isMysql());
    PollableTask pollableTask =
        this.pollableTaskService.createPollableTask(null, "test-pollable", null, 0);

    pollableTaskService.finishTask(pollableTask.getId(), null, null, null);
    PollableTask pollableTaskInPast =
        this.setPollableTaskFinishedInPast(pollableTask, Duration.ofDays(5));

    this.pollableTaskCleanupService.cleanOldPollableTaskData(Period.ofDays(4), 10);

    assertThrows(
        NullPointerException.class,
        () -> this.pollableTaskService.getPollableTask(pollableTaskInPast.getId()));

    PollableTask pollableTask2 =
        this.pollableTaskService.createPollableTask(null, "test-pollable-2", null, 0);

    pollableTaskService.finishTask(pollableTask2.getId(), null, null, null);
    PollableTask pollableTaskInPast2 =
        this.setPollableTaskFinishedInPast(
            pollableTask2, Duration.ofDays(5).plus(Duration.ofSeconds(1)));

    this.pollableTaskCleanupService.cleanOldPollableTaskData(Period.ofDays(5), 10);

    assertThrows(
        NullPointerException.class,
        () -> this.pollableTaskService.getPollableTask(pollableTaskInPast2.getId()));
  }

  @Test
  public void testCleanOldPollableTaskData_DeletesParentPollableTaskSuccessfully() {
    Assume.assumeTrue(this.dbUtils.isMysql());
    PollableTask pollableTask =
        this.pollableTaskService.createPollableTask(null, "test-pollable", null, 0);

    PollableTask childPollableTask =
        this.pollableTaskService.createPollableTask(
            pollableTask.getId(), "test-child-pollable", null, 0);

    pollableTaskService.finishTask(pollableTask.getId(), null, null, null);
    PollableTask pollableTaskInPast =
        this.setPollableTaskFinishedInPast(pollableTask, Duration.ofDays(5));

    this.pollableTaskCleanupService.cleanOldPollableTaskData(Period.ofDays(4), 10);

    assertThrows(
        NullPointerException.class,
        () -> this.pollableTaskService.getPollableTask(pollableTaskInPast.getId()));

    PollableTask updatedChildPollableTask =
        this.pollableTaskService.getPollableTask(childPollableTask.getId());
    assertNull(updatedChildPollableTask.getParentTask());
  }

  @Test
  public void testCleanOldPollableTaskData_CallsDeleteAndUpdateMethodThrice() {
    Assume.assumeTrue(this.dbUtils.isMysql());
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

    this.pollableTaskCleanupService.cleanOldPollableTaskData(Period.ofDays(4), 1);

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
  public void testCleanOldPollableTaskData_DoesNotDeleteAny() {
    Assume.assumeTrue(this.dbUtils.isMysql());
    PollableTask pollableTask =
        this.pollableTaskService.createPollableTask(null, "test-pollable", null, 0);

    pollableTaskService.finishTask(pollableTask.getId(), null, null, null);
    PollableTask pollableTaskInPast =
        this.setPollableTaskFinishedInPast(pollableTask, Duration.ofDays(5));

    this.pollableTaskCleanupService.cleanOldPollableTaskData(Period.ofDays(6), 10);

    PollableTask pollableTaskAfterCleanup =
        this.pollableTaskService.getPollableTask(pollableTaskInPast.getId());

    assertNotNull(pollableTaskAfterCleanup);
  }
}
