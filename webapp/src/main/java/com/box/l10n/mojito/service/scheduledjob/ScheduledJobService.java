package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.entity.ScheduledJob;
import java.util.Optional;
import java.util.UUID;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ScheduledJobService {

  static Logger logger = LoggerFactory.getLogger(ScheduledJobService.class);
  private final ScheduledJobRepository scheduledJobRepository;
  private final ScheduledJobStatusRepository scheduledJobStatusRepository;
  private final ScheduledJobTypeRepository scheduledJobTypeRepository;
  private final ScheduledJobManager scheduledJobManager;

  @Autowired
  public ScheduledJobService(
      ScheduledJobRepository scheduledJobRepository,
      ScheduledJobStatusRepository scheduledJobStatusRepository,
      ScheduledJobTypeRepository scheduledJobTypeRepository,
      ScheduledJobManager scheduledJobManager) {
    this.scheduledJobRepository = scheduledJobRepository;
    this.scheduledJobStatusRepository = scheduledJobStatusRepository;
    this.scheduledJobTypeRepository = scheduledJobTypeRepository;
    this.scheduledJobManager = scheduledJobManager;
  }

  public ScheduledJob createJob(ScheduledJob scheduledJob)
      throws ScheduledJobException, SchedulerException, ClassNotFoundException {
    if (scheduledJob.getRepository() == null) {
      throw new ScheduledJobException("Repository must be provided to create a job");
    }
    if (scheduledJob.getCron() == null || scheduledJob.getCron().isEmpty()) {
      throw new ScheduledJobException("Cron expression must be provided to create a job");
    }
    if (scheduledJob.getUuid() == null) {
      scheduledJob.setUuid(UUID.randomUUID().toString());
    }
    scheduledJob.setJobStatus(
        scheduledJobStatusRepository.findByEnum(ScheduledJobStatus.SCHEDULED));
    if (scheduledJob.getJobType() != null && scheduledJob.getJobType().getId() != null) {
      scheduledJob.setJobType(
          scheduledJobTypeRepository
              .findById(scheduledJob.getJobType().getId())
              .orElseThrow(
                  () ->
                      new ScheduledJobException(
                          "Job type not found with id: " + scheduledJob.getJobType().getId())));
    } else {
      throw new ScheduledJobException("Job type must be provided to create a job");
    }

    scheduledJobRepository.save(scheduledJob);
    scheduledJobManager.scheduleJob(scheduledJob);

    logger.info(
        "Job '{}' for repository '{}' was created.",
        scheduledJob.getUuid(),
        scheduledJob.getRepository().getName());
    return scheduledJob;
  }

  public ScheduledJob updateJob(String uuid, ScheduledJob scheduledJob)
      throws ScheduledJobException, SchedulerException, ClassNotFoundException {
    Optional<ScheduledJob> optScheduledJob = scheduledJobRepository.findByUuid(uuid);

    if (optScheduledJob.isEmpty())
      throw new ScheduledJobException("Job not found with id: " + uuid);

    ScheduledJob updatedJob = optScheduledJob.get();

    if (scheduledJob.getRepository() != null) {
      updatedJob.setRepository(scheduledJob.getRepository());
    }
    if (scheduledJob.getCron() != null) {
      updatedJob.setCron(scheduledJob.getCron());
    }
    if (scheduledJob.getJobStatus() != null) {
      updatedJob.setJobStatus(scheduledJob.getJobStatus());
    }
    if (scheduledJob.getPropertiesString() != null) {
      updatedJob.setPropertiesString(scheduledJob.getPropertiesString());
    }
    if (scheduledJob.getJobType() != null && scheduledJob.getJobType().getId() != null) {
      updatedJob.setJobType(
          scheduledJobTypeRepository
              .findById(scheduledJob.getJobType().getId())
              .orElseThrow(
                  () ->
                      new ScheduledJobException(
                          "Job type not found with id: " + scheduledJob.getJobType().getId())));
    }

    scheduledJobRepository.save(updatedJob);
    scheduledJobManager.scheduleJob(updatedJob);

    logger.info("Job '{}' was updated.", uuid);
    return updatedJob;
  }

  public void deleteJob(ScheduledJob scheduledJob) throws SchedulerException {
    scheduledJobRepository.deleteByUuid(scheduledJob.getUuid());
    scheduledJobManager.deleteJobFromQuartz(scheduledJob);
    logger.debug("Deleted scheduled job with uuid: {}", scheduledJob.getUuid());
  }
}
