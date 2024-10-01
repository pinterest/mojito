package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.service.scheduledjob.jobs.ScheduledThirdPartySync;

public enum ScheduledJobType {
  THIRD_PARTY_SYNC(ScheduledThirdPartySync.class.getName());

  final String className;

  ScheduledJobType(String className) {
    this.className = className;
  }

  public String getClassName() {
    return className;
  }
}
