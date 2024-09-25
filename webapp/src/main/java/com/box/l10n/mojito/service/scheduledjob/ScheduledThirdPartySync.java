package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobConfig;
import java.util.Date;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ScheduledThirdPartySync implements IScheduledJob {

  static Logger logger = LoggerFactory.getLogger(ScheduledThirdPartySync.class);

  private final ScheduledJobTypes JOB_TYPE = ScheduledJobTypes.THIRD_PARTY_SYNC;

  @Autowired ScheduledJobManager scheduledJobManager;
  @Autowired ScheduledJobsRepository scheduledJobsRepository;
  private ScheduledJob<ThirdPartySyncJobConfig> job;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    // This class is instantiated when triggered by quartz
    // The job will be fetched from the job manager as injection isn't done
    job =
        (ScheduledJob<ThirdPartySyncJobConfig>) scheduledJobsRepository.getJob(jobExecutionContext);
    ThirdPartySyncJobConfig properties = job.properties;

    setStartDate(new Date());
    setStatus(ScheduledJobStatus.IN_PROGRESS);
    logger.debug("Third party sync started for " + job.repository);

    try {
      System.out.println("This is from the sync job. " + properties.getThirdPartyProjectId());
      Thread.sleep(20000);
    } catch (Exception e) {
      setStatus(ScheduledJobStatus.FAILED);
      setEndDate(new Date());
      throw new JobExecutionException("Error occurred during third party sync job execution", e);
    }

    setStatus(ScheduledJobStatus.SUCCEEDED);
    setEndDate(new Date());
  }

  @Override
  public void setStatus(ScheduledJobStatus status) {
    job.status = status;
  }

  @Override
  public ScheduledJobStatus getStatus() {
    return job.status;
  }

  @Override
  public void setStartDate(Date startDate) {
    job.startDate = startDate;
  }

  @Override
  public Date getStartDate() {
    return job.startDate;
  }

  @Override
  public void setEndDate(Date endDate) {
    job.endDate = endDate;
  }

  @Override
  public Date getEndDate() {
    return job.endDate;
  }
}
