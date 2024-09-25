package com.box.l10n.mojito.service.scheduledjob;

public enum ScheduledJobTypes {
  THIRD_PARTY_SYNC(ScheduledThirdPartySync.class.getName());

  final String className;

  ScheduledJobTypes(String className) {
    this.className = className;
  }

  public String getClassName() {
    return className;
  }
}
