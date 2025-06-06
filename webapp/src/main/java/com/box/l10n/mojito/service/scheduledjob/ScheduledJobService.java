package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.entity.ScheduledJob;
import java.util.Optional;
import java.util.UUID;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

  public ScheduledJob createJob(ScheduledJob scheduledJob) {
    if (scheduledJob.getRepository() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Repository must be provided to create a job");
    }
    if (scheduledJob.getCron() == null || scheduledJob.getCron().isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cron expression must be provided to create a job");
    }
    try {
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
                        new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Job type not found with id: " + scheduledJob.getJobType().getId())));
      } else {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Job type must be provided to create a job");
      }

      scheduledJobRepository.save(scheduledJob);
      scheduledJobManager.scheduleJob(scheduledJob);

      logger.info(
          "Job '{}' for repository '{}' was created.",
          scheduledJob.getUuid(),
          scheduledJob.getRepository().getName());
      return scheduledJob;
    } catch (ResponseStatusException e) {
      logger.error("Error creating job", e);
      throw e;
    } catch (ClassNotFoundException e) {
      logger.error("Error creating job", e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Error creating job: Class Not Found Exception from scheduledJobManager.scheduledJob(scheduledJob)",
          e);
    } catch (SchedulerException e) {
      logger.error("Error scheduling job", e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Error scheduling job: Scheduler Exception from scheduledJobManager.scheduledJob(scheduledJob)",
          e);
    }
  }

  public ScheduledJob updateJob(String uuid, ScheduledJob scheduledJob) {
    Optional<ScheduledJob> optScheduledJob = scheduledJobRepository.findByUuid(uuid);

    if (optScheduledJob.isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found with id: " + uuid);

    ScheduledJob existingJob = optScheduledJob.get();

    try {
      if (scheduledJob.getRepository() != null) {
        existingJob.setRepository(scheduledJob.getRepository());
      }
      if (scheduledJob.getCron() != null) {
        existingJob.setCron(scheduledJob.getCron());
      }
      if (scheduledJob.getJobStatus() != null) {
        existingJob.setJobStatus(scheduledJob.getJobStatus());
      }
      if (scheduledJob.getPropertiesString() != null) {
        existingJob.setPropertiesString(scheduledJob.getPropertiesString());
      }
      if (scheduledJob.getJobType() != null && scheduledJob.getJobType().getId() != null) {
        existingJob.setJobType(
            scheduledJobTypeRepository
                .findById(scheduledJob.getJobType().getId())
                .orElseThrow(
                    () ->
                        new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Job type not found with id: " + scheduledJob.getJobType().getId())));
      }

      scheduledJobRepository.save(existingJob);

      logger.info("Job '{}' was updated.", uuid);
      return existingJob;
    } catch (Exception e) {
      logger.error("Error updating job", e);
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Error updating job: " + e.getMessage(), e);
    }
  }

  public void deleteJob(String uuid) {
    scheduledJobRepository.deleteByUuid(uuid);
    logger.debug("Deleted scheduled job with uuid: {}", uuid);
  }
}
