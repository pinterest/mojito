package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.entity.ScheduledJob;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduledJobTriggerListener extends TriggerListenerSupport {

  static Logger logger = LoggerFactory.getLogger(ScheduledJobTriggerListener.class);

  private final ScheduledJobRepository scheduledJobRepository;

  ScheduledJobTriggerListener(ScheduledJobRepository scheduledJobRepository) {
    this.scheduledJobRepository = scheduledJobRepository;
  }

  @Override
  public String getName() {
    return "ScheduledJobTriggerListener";
  }

  @Override
  public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
    // Returning true vetoes the job execution, if its in progress & we don't allow overlapping
    // then skip execution
    ScheduledJob job = scheduledJobRepository.findByJobKey(context.getJobDetail().getKey());
    return job.getJobStatus().equals(ScheduledJobStatus.IN_PROGRESS)
        && !job.isAllowExecutionOverlap();
  }

  @Override
  public void triggerMisfired(Trigger trigger) {
    super.triggerMisfired(trigger);
    // Scheduled job misfired, most likely need to allocate more threads
    ScheduledJob job = scheduledJobRepository.findByJobKey(trigger.getJobKey());
    logger.warn("TRIGGER MISFIRE FOR {} | {}", job.getRepository().getName(), job.getJobType());
  }
}
