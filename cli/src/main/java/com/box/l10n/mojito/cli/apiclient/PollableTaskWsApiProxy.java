package com.box.l10n.mojito.cli.apiclient;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.cli.apiclient.exceptions.PollableTaskException;
import com.box.l10n.mojito.cli.apiclient.exceptions.PollableTaskExecutionException;
import com.box.l10n.mojito.cli.apiclient.exceptions.PollableTaskTimeoutException;
import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.model.PollableTask;
import com.box.l10n.mojito.cli.models.ErrorMessage;
import com.box.l10n.mojito.json.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PollableTaskWsApiProxy extends PollableTaskWsApi {
  /** logger */
  static Logger logger = getLogger(PollableTaskWsApiProxy.class);

  public static final Long NO_TIMEOUT = -1L;

  private final ObjectMapper objectMapper;

  @Autowired
  public PollableTaskWsApiProxy(ApiClient apiClient, ObjectMapper objectMapper) {
    super(apiClient);
    this.objectMapper = objectMapper;
  }

  /**
   * Waits for {@link PollableTask} to be all finished (see {@link PollableTask#isAllFinished() }).
   * Infinite timeout.
   *
   * @param pollableId the {@link PollableTask#getId()}
   * @throws PollableTaskException
   */
  public void waitForPollableTask(Long pollableId) throws PollableTaskException {
    waitForPollableTask(pollableId, NO_TIMEOUT);
  }

  public void waitForPollableTask(Long pollableTaskId, long timeout) throws PollableTaskException {
    waitForPollableTask(pollableTaskId, timeout, null);
  }

  /**
   * Waits for {@link PollableTask} to be all finished (see {@link PollableTask#isAllFinished() }).
   *
   * @param pollableId the {@link com.box.l10n.mojito.cli.model.PollableTask#getId()}
   * @param timeout timeout in milliseconds.
   * @param waitForPollableTaskListener listener to be called during polling
   * @throws PollableTaskException
   */
  public void waitForPollableTask(
      Long pollableId, long timeout, WaitForPollableTaskListener waitForPollableTaskListener)
      throws PollableTaskException {

    long timeoutTime = System.currentTimeMillis() + timeout;
    long waitTime = 0;

    PollableTask pollableTask = null;

    while (pollableTask == null || !pollableTask.isAllFinished()) {

      logger.debug("Waiting for PollableTask: {} to finish", pollableId);

      try {
        pollableTask = this.getPollableTaskById(pollableId);
      } catch (ApiException e) {
        throw new CommandException(e.getMessage(), e);
      }

      if (waitForPollableTaskListener != null) {
        waitForPollableTaskListener.afterPoll(pollableTask);
      }

      List<PollableTask> pollableTaskWithErrors = getAllPollableTasksWithError(pollableTask);

      if (!pollableTaskWithErrors.isEmpty()) {

        for (PollableTask pollableTaskWithError : pollableTaskWithErrors) {
          ErrorMessage errorMessage =
              this.objectMapper.convertValue(
                  pollableTaskWithError.getErrorMessage(), ErrorMessage.class);
          logger.debug(
              "Error happened in PollableTask {}: {}",
              pollableTaskWithError.getId(),
              errorMessage.getMessage());
        }

        // Last task is the root task if it has an error or any of the sub task
        // TODO(P1) we might want to show all errors
        PollableTask lastTaskInError =
            pollableTaskWithErrors.get(pollableTaskWithErrors.size() - 1);

        ErrorMessage errorMessage =
            this.objectMapper.convertValue(lastTaskInError.getErrorMessage(), ErrorMessage.class);
        throw new PollableTaskExecutionException(errorMessage.getMessage());
      }

      if (!pollableTask.isAllFinished()) {

        if (timeout != NO_TIMEOUT && System.currentTimeMillis() > timeoutTime) {
          logger.debug(
              "Timed out waiting for PollableTask: {} to finish. \n{}",
              pollableId,
              ReflectionToStringBuilder.toString(pollableTask));
          throw new PollableTaskTimeoutException(
              "Timed out waiting for PollableTask: " + pollableId);
        }

        try {
          Thread.sleep(waitTime);
          waitTime = getNextWaitTime(waitTime);
        } catch (InterruptedException ie) {
          throw new RuntimeException(ie);
        }
      } else {
        logger.debug("PollableTask: {} finished", pollableId);
      }
    }
  }

  long getNextWaitTime(long lastWaitTime) {
    int maxTime = 500;
    long nextWaitTime = lastWaitTime + 25;
    nextWaitTime = Math.max(maxTime, nextWaitTime);
    return nextWaitTime;
  }

  /**
   * Get all the PollableTasks with error (traverses all the PollableTask's subtasks)
   *
   * @param pollableTask
   * @return
   */
  public List<PollableTask> getAllPollableTasksWithError(PollableTask pollableTask) {
    List<PollableTask> result = new ArrayList<>();
    recursivelyGetAllPollableTaskWithError(pollableTask, result);
    return result;
  }

  /**
   * Recursively traverses all subtasks of {@code pollableTask} and return all the {@link
   * PollableTask} which had an error message
   *
   * @param pollableTask
   * @param pollableTasksWithError
   */
  private void recursivelyGetAllPollableTaskWithError(
      PollableTask pollableTask, List<PollableTask> pollableTasksWithError) {

    for (PollableTask subTask : pollableTask.getSubTasks()) {
      recursivelyGetAllPollableTaskWithError(subTask, pollableTasksWithError);
    }

    if (pollableTask.getErrorMessage() != null) {
      pollableTasksWithError.add(pollableTask);
    }
  }
}
