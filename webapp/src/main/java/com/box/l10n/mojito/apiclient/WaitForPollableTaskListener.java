package com.box.l10n.mojito.apiclient;

import com.box.l10n.mojito.model.PollableTask;

public interface WaitForPollableTaskListener {
  void afterPoll(PollableTask pollableTask);
}
