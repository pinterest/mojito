package com.box.l10n.mojito.quartz;

import static com.box.l10n.mojito.quartz.QuartzConfig.DYNAMIC_GROUP_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class QuartzService {

  /** logger */
  static Logger logger = getLogger(QuartzService.class);

  private final QuartzSchedulerManager schedulerManager;
  private final JdbcTemplate jdbcTemplate;

  public QuartzService(QuartzSchedulerManager schedulerManager, JdbcTemplate jdbcTemplate) {
    this.schedulerManager = schedulerManager;
    this.jdbcTemplate = jdbcTemplate;
  }

  // TODO(mallen): Handle for other schedulers other than 'default'???
  public List<String> getDynamicJobs() throws SchedulerException {
    Set<JobKey> jobKeys =
        schedulerManager
            .getScheduler(QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME)
            .getJobKeys(GroupMatcher.jobGroupEquals(DYNAMIC_GROUP_NAME));
    return jobKeys.stream().map(jobKey -> jobKey.getName()).collect(Collectors.toList());
  }

  public void deleteAllDynamicJobs() throws SchedulerException {
    Scheduler scheduler =
        schedulerManager.getScheduler(QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME);
    Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(DYNAMIC_GROUP_NAME));
    scheduler.deleteJobs(new ArrayList<>(jobKeys));
  }

  public int getCurrentlyExecutingJobsCountForTrigger(String triggerName) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM QRTZ_FIRED_TRIGGERS WHERE TRIGGER_NAME = ? AND STATE = 'EXECUTING'",
            Integer.class,
            triggerName);
    return count != null ? count : 0;
  }
}
