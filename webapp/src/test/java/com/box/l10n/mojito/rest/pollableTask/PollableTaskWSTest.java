package com.box.l10n.mojito.rest.pollableTask;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import com.box.l10n.mojito.apiclient.PollableTaskClient;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.rest.WSTestBase;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jaurambault
 */
public class PollableTaskWSTest extends WSTestBase {

  @Autowired PollableTaskService pollableTaskService;

  @Autowired PollableTaskClient pollableTaskClient;

  @Test
  public void testGetPollableTask() throws Exception {

    String pollableTaskName = "testGetPollableTask";
    PollableTask parentTask =
        pollableTaskService.createPollableTask(null, pollableTaskName, null, 0);
    com.box.l10n.mojito.apiclient.model.PollableTask pollableTask =
        pollableTaskClient.getPollableTaskById(parentTask.getId());

    assertEquals(pollableTaskName, pollableTask.getName());
    assertNull(pollableTask.getFinishedDate());
    assertFalse(pollableTask.isAllFinished());
  }
}
