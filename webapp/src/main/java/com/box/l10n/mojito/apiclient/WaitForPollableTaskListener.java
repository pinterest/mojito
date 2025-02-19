package com.box.l10n.mojito.apiclient;

import com.box.l10n.mojito.apiclient.model.PollableTask;

public interface WaitForPollableTaskListener {
  void afterPoll(PollableTask pollableTask);
}
