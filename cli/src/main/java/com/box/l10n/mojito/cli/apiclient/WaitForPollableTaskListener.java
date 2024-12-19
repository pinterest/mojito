package com.box.l10n.mojito.cli.apiclient;

import com.box.l10n.mojito.cli.model.PollableTask;

public interface WaitForPollableTaskListener {
  void afterPoll(PollableTask pollableTask);
}
