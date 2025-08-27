package com.box.l10n.mojito.service.pollableTask;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.service.DBUtils;
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

  @Autowired private DBUtils dbUtils;

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

  private void cleanDropData(ZonedDateTime beforeDate) {
    if (this.dbUtils.isMysql()) {
      int deleteCount = this.dropRepository.cleanStaleExportPollableTaskIds(beforeDate);
      logger.debug("Updated {} Drop rows (exported)", deleteCount);
      deleteCount = this.dropRepository.cleanStaleImportPollableTaskIds(beforeDate);
      logger.debug("Updated {} Drop rows (imported)", deleteCount);
    } else {
      this.dropRepository.cleanStaleExportPollableTaskIdsForHsqldb(beforeDate);
      this.dropRepository.cleanStaleImportPollableTaskIdsForHsqldb(beforeDate);
    }
  }

  public void cleanStalePollableTaskData(
      Period retentionPeriod, int batchSize, int maxNumberOfBatches) {
    ZonedDateTime beforeDate = ZonedDateTime.now().minus(retentionPeriod);
    this.cleanDropData(beforeDate);
    int deleteCount = this.assetExtractionRepository.cleanStalePollableTaskIds(beforeDate);
    logger.debug("Updated {} Asset Extraction rows", deleteCount);
    deleteCount = this.tmxliffRepository.cleanStaleExportPollableTaskIds(beforeDate);
    logger.debug("Updated {} TM Xliff rows", deleteCount);
    int batchNumber = 1;
    do {
      deleteCount =
          this.pollableTaskRepository.cleanParentTasksWithFinishedDateBefore(beforeDate, batchSize);
      logger.debug("Updated {} Pollable Task rows in batch: {}", deleteCount, batchNumber++);
    } while (deleteCount == batchSize && batchNumber <= maxNumberOfBatches);

    batchNumber = 1;
    do {
      deleteCount =
          this.pollableTaskRepository.deleteAllByFinishedDateBefore(beforeDate, batchSize);
      logger.debug("Deleted {} Pollable Task rows in batch: {}", deleteCount, batchNumber++);
    } while (deleteCount == batchSize && batchNumber <= maxNumberOfBatches);
  }
}
