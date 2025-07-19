package com.box.l10n.mojito.service.pollableTask;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.drop.DropRepository;
import com.box.l10n.mojito.service.tm.TMXliffRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * @author aloison
 */
@Service
public class PollableTaskCleanupService {

  /** logger */
  static Logger logger = getLogger(PollableTaskCleanupService.class);

  @Autowired PollableTaskRepository pollableTaskRepository;

  @Autowired PollableTaskService pollableTaskService;

  @Autowired DropRepository dropRepository;

  @Autowired TMXliffRepository tmxliffRepository;

  @Autowired AssetExtractionRepository assetExtractionRepository;

  @Autowired MeterRegistry meterRegistry;

  /**
   * Marks zombie tasks as finished with error. A zombie task can be defined as a task that did not
   * complete before its given timeout period.
   */
  public void finishZombieTasksWithError() {

    List<PollableTask> zombiePollableTasks;

    do {
      logger.debug("Fetching 5 zombie pollable tasks to clean up");

      // Fetching 5 by 5 to avoid locking too many rows.
      // It is also useful to distribute the load across multiple instances.
      PageRequest pageable = PageRequest.of(0, 5);
      zombiePollableTasks =
          pollableTaskRepository.findZombiePollableTasks(ZonedDateTime.now(), pageable);

      for (PollableTask zombiePollableTask : zombiePollableTasks) {
        markAsFinishedWithError(zombiePollableTask);
      }
    } while (!zombiePollableTasks.isEmpty());
  }

  /**
   * Sets the pollable task's state to "Finished" and adds an error message.
   *
   * @param pollableTask
   */
  private void markAsFinishedWithError(PollableTask pollableTask) {

    logger.debug("Zombie task detected: {}, mark as finished with error", pollableTask.getId());
    ExceptionHolder exceptionHolder = new ExceptionHolder(pollableTask);
    exceptionHolder.setExpected(true);
    exceptionHolder.setException(
        new PollableTaskTimeoutException("Zombie task detected: Maximum execution time exceeded."));

    pollableTaskService.finishTask(pollableTask.getId(), null, exceptionHolder, null);
    meterRegistry
        .counter("PollableTaskCleanupService.markAsZombieTask", "name", pollableTask.getName())
        .increment();
  }

  public void cleanOldPollableTaskData(Period retentionPeriod, int batchSize) {
    ZonedDateTime beforeDate = ZonedDateTime.now().minus(retentionPeriod);
    int deleteCount = this.dropRepository.cleanOldExportPollableTaskIds(beforeDate);
    logger.info("Updated {} Drop rows (exported)", deleteCount);
    deleteCount = this.dropRepository.cleanOldImportPollableTaskIds(beforeDate);
    logger.info("Updated {} Drop rows (imported)", deleteCount);
    deleteCount = this.assetExtractionRepository.cleanOldPollableTaskIds(beforeDate);
    logger.info("Updated {} Asset Extraction rows", deleteCount);
    deleteCount = this.tmxliffRepository.cleanOldExportPollableTaskIds(beforeDate);
    logger.info("Updated {} TM Xliff rows", deleteCount);
    int batchNumber = 1;
    do {
      deleteCount =
          this.pollableTaskRepository.clearParentTasksWithFinishedDateBefore(beforeDate, batchSize);
      logger.info("Updated {} Pollable Task rows in batch: {}", deleteCount, batchNumber++);
    } while (deleteCount == batchSize);

    batchNumber = 1;
    do {
      deleteCount =
          this.pollableTaskRepository.deleteAllByFinishedDateBefore(beforeDate, batchSize);
      logger.info("Deleted {} Pollable Task rows in batch: {}", deleteCount, batchNumber++);
    } while (deleteCount == batchSize);
  }
}
