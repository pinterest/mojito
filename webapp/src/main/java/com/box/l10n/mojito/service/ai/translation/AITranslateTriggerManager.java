package com.box.l10n.mojito.service.ai.translation;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;

import com.box.l10n.mojito.quartz.QuartzSchedulerManager;
import jakarta.annotation.PostConstruct;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.KeyMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Attaches the {@link AITranslateTriggerListener} to the AITranslateCronJob trigger if the max
 * concurrent jobs is set and greater than 0. This allows the listener to veto job execution if the
 * number of currently running AI translation jobs exceed the limit.
 *
 * @author mattwilshire
 */
@Component
@ConditionalOnExpression("${l10n.ai.translation.job.maxConcurrentJobs:0} > 0")
public class AITranslateTriggerManager {

  @Autowired QuartzSchedulerManager schedulerManager;

  @Autowired AITranslateTriggerListener aiTranslateTriggerListener;

  @PostConstruct
  void init() throws SchedulerException {
    schedulerManager
        .getScheduler(DEFAULT_SCHEDULER_NAME)
        .getListenerManager()
        .addTriggerListener(
            aiTranslateTriggerListener,
            KeyMatcher.keyEquals(TriggerKey.triggerKey("triggerAiTranslateCronJob")));
  }
}
