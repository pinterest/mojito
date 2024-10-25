package com.box.l10n.mojito.service.scheduledjob;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class NoOpScheduledJobTest implements IScheduledJob {
  @Override
  public void onSuccess(JobExecutionContext context) {}

  @Override
  public void onFailure(JobExecutionContext context, JobExecutionException jobException) {}

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {}
}
