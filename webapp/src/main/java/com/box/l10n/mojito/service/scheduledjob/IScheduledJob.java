package com.box.l10n.mojito.service.scheduledjob;

import java.util.Date;
import org.quartz.Job;

public interface IScheduledJob extends Job {
  void setStatus(ScheduledJobStatus status);

  ScheduledJobStatus getStatus();

  void setStartDate(Date startDate);

  Date getStartDate();

  void setEndDate(Date endDate);

  Date getEndDate();
}
