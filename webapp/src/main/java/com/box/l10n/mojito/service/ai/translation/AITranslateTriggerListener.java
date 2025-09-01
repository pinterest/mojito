package com.box.l10n.mojito.service.ai.translation;

import io.micrometer.core.instrument.MeterRegistry;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * A trigger listener for AI translation jobs that controls the maximum number of concurrent jobs by
 * vetoing job execution when the limit is exceeded.
 *
 * @author mattwilshire
 */
@Component
public class AITranslateTriggerListener extends TriggerListenerSupport {

  private static final Logger logger = LoggerFactory.getLogger(AITranslateTriggerListener.class);

  private static final String LISTENER_NAME = "AITranslateTriggerListener";
  private static final String EXECUTING_JOBS_QUERY =
      "SELECT COUNT(*) FROM QRTZ_FIRED_TRIGGERS WHERE TRIGGER_NAME = ? AND STATE = 'EXECUTING'";

  private final int maxConcurrentJobs;
  private final JdbcTemplate jdbcTemplate;
  private final MeterRegistry meterRegistry;

  public AITranslateTriggerListener(
      @Value("${l10n.ai.translation.job.maxConcurrentJobs:0}") int maxConcurrentJobs,
      JdbcTemplate jdbcTemplate,
      MeterRegistry meterRegistry) {
    this.maxConcurrentJobs = maxConcurrentJobs;
    this.jdbcTemplate = jdbcTemplate;
    this.meterRegistry = meterRegistry;
  }

  @Override
  public String getName() {
    return LISTENER_NAME;
  }

  @Override
  public void triggerFired(Trigger trigger, JobExecutionContext context) {
    super.triggerFired(trigger, context);
    logger.debug("AITranslateTriggerListener: Trigger fired: {}", trigger.getKey());
  }

  /**
   * Determines whether to veto job execution based on the maximum concurrent jobs limit.
   *
   * @param trigger the trigger that was fired
   * @param context the job execution context
   * @return true if job execution should be vetoed, false otherwise
   */
  @Override
  public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
    if (maxConcurrentJobs <= 0) return false;

    final String triggerName = trigger.getKey().getName();
    final int currentlyRunningJobs = getCurrentlyRunningJobsCount(triggerName);
    final boolean veto = currentlyRunningJobs >= maxConcurrentJobs;

    if (veto) {
      logger.warn(
          "Vetoing job execution for trigger '{}' as currently running jobs ({}) exceed max allowed ({})",
          triggerName,
          currentlyRunningJobs,
          maxConcurrentJobs);

      meterRegistry.counter("AITranslateTriggerListener.jobVetoed").increment();
    }

    return veto;
  }

  /**
   * Fetches the count of currently running jobs for the specified trigger.
   *
   * @param triggerName the name of the trigger
   * @return the number of currently executing jobs for this trigger
   */
  private int getCurrentlyRunningJobsCount(String triggerName) {
    Integer count = jdbcTemplate.queryForObject(EXECUTING_JOBS_QUERY, Integer.class, triggerName);
    return count != null ? count : 0;
  }
}
