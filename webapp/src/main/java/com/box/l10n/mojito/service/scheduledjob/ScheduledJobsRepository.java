package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobConfig;
import java.util.HashMap;
import java.util.List;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

/*
 This is a class to store the Scheduled Jobs, we can't store the actual Jobs that Quartz is managing as we require
 more metadata per job and Quartz only instantiates the jobs when they are triggered.
*/
@Component
public class ScheduledJobsRepository {
  private final HashMap<String, ScheduledJob<?>> jobs = new HashMap<>();

  public void addJobFromConfig(
      ThirdPartySyncJobConfig jobConfig, ScheduledJobTypes scheduledJobType) {
    ScheduledJob<ThirdPartySyncJobConfig> jobDetails = new ScheduledJob<>();
    jobDetails.cron = jobConfig.getCron();
    jobDetails.repository = jobConfig.getRepository();
    jobDetails.status = ScheduledJobStatus.SCHEDULED;
    jobDetails.properties = jobConfig;
    jobs.put(jobConfig.getRepository() + scheduledJobType.toString(), jobDetails);
  }

  public ScheduledJob<?> getJob(JobExecutionContext jobExecutionContext) {
    String jobName = jobExecutionContext.getJobDetail().getKey().getName();
    String jobGroup = jobExecutionContext.getJobDetail().getKey().getGroup();
    return this.getJob(jobName, jobGroup);
  }

  public ScheduledJob<?> getJob(String jobName, String jobType) {
    return jobs.get(jobName + jobType);
  }

  public List<ScheduledJob<?>> getJobs() {
    return jobs.values().stream().toList();
  }
}
