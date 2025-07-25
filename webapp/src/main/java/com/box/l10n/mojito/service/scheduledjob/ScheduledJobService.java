package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.entity.ScheduledJobType;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
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
  private final RepositoryRepository repositoryRepository;

  @Autowired
  public ScheduledJobService(
      ScheduledJobRepository scheduledJobRepository,
      ScheduledJobStatusRepository scheduledJobStatusRepository,
      ScheduledJobTypeRepository scheduledJobTypeRepository,
      ScheduledJobManager scheduledJobManager,
      RepositoryRepository repositoryRepository) {
    this.scheduledJobRepository = scheduledJobRepository;
    this.scheduledJobStatusRepository = scheduledJobStatusRepository;
    this.scheduledJobTypeRepository = scheduledJobTypeRepository;
    this.scheduledJobManager = scheduledJobManager;
    this.repositoryRepository = repositoryRepository;
  }

  public ScheduledJob createJob(ScheduledJobDTO scheduledJobDTO)
      throws ScheduledJobException, SchedulerException, ClassNotFoundException {
    ScheduledJob scheduledJob = new ScheduledJob();

    if (scheduledJobDTO.getRepository() == null) {
      throw new ScheduledJobException("Valid repository must be provided to create a job");
    }
    if (scheduledJobRepository
        .findByRepositoryNameAndJobType(
            scheduledJobDTO.getRepository(), resolveJobTypeFromDTO(scheduledJobDTO).getEnum())
        .isPresent()) {
      throw new ScheduledJobException(
          "Scheduled job of type "
              + scheduledJobDTO.getType()
              + " with repository already exists: "
              + scheduledJobDTO.getRepository());
    }
    if (scheduledJobDTO.getCron() == null || scheduledJobDTO.getCron().isBlank()) {
      throw new ScheduledJobException("Cron expression must be provided to create a job");
    }
    if (scheduledJobDTO.getPropertiesString() == null
        || scheduledJobDTO.getPropertiesString().isBlank()) {
      throw new ScheduledJobException("Properties must be provided to create a job");
    }

    scheduledJob.setUuid(
        scheduledJobDTO.getId() != null ? scheduledJobDTO.getId() : UUID.randomUUID().toString());
    scheduledJob.setRepository(resolveRepositoryFromDTO(scheduledJobDTO));
    scheduledJob.setCron(scheduledJobDTO.getCron());
    scheduledJob.setJobType(resolveJobTypeFromDTO(scheduledJobDTO));
    scheduledJob.setPropertiesString(scheduledJobDTO.getPropertiesString());
    scheduledJob.deserializeProperties();
    scheduledJob.setJobStatus(
        scheduledJobStatusRepository.findByEnum(
            com.box.l10n.mojito.service.scheduledjob.ScheduledJobStatus.SCHEDULED));

    scheduledJobRepository.save(scheduledJob);
    scheduledJobManager.scheduleJob(scheduledJob);

    logger.info(
        "Job '{}' for repository '{}' was created.",
        scheduledJob.getUuid(),
        scheduledJob.getRepository().getName());
    return scheduledJob;
  }

  public ScheduledJob updateJob(String uuid, ScheduledJobDTO scheduledJobDTO)
      throws ScheduledJobException, SchedulerException, ClassNotFoundException {
    ScheduledJob updatedJob =
        scheduledJobRepository
            .findByUuid(uuid)
            .orElseThrow(() -> new ScheduledJobException("Job not found with id: " + uuid));

    if (scheduledJobDTO.getRepository() != null) {
      // Check if a job with the same type and repository already exists and updated repository is
      // different from the existing repository on the job
      if (scheduledJobRepository
              .findByRepositoryNameAndJobType(
                  scheduledJobDTO.getRepository(), updatedJob.getJobType().getEnum())
              .isPresent()
          && !updatedJob.getRepository().getName().equals(scheduledJobDTO.getRepository())) {
        throw new ScheduledJobException(
            "Scheduled job of type "
                + scheduledJobDTO.getType()
                + " with repository already exists: "
                + scheduledJobDTO.getRepository());
      }
      updatedJob.setRepository(resolveRepositoryFromDTO(scheduledJobDTO));
    }
    if (scheduledJobDTO.getType() != null) {
      updatedJob.setJobType(resolveJobTypeFromDTO(scheduledJobDTO));
    }
    if (scheduledJobDTO.getCron() != null) {
      updatedJob.setCron(scheduledJobDTO.getCron());
    }
    if (scheduledJobDTO.getPropertiesString() != null) {
      updatedJob.setPropertiesString(scheduledJobDTO.getPropertiesString());
      updatedJob.deserializeProperties();
    }

    scheduledJobRepository.save(updatedJob);
    scheduledJobManager.scheduleJob(updatedJob);

    logger.info("Job '{}' was updated.", uuid);
    return updatedJob;
  }

  public void deleteJob(ScheduledJob scheduledJob) throws SchedulerException {
    scheduledJob.setDeleted(true);
    scheduledJobRepository.save(scheduledJob);
    scheduledJobManager.deleteJobFromQuartz(scheduledJob);
    logger.info("Deleted scheduled job with uuid: {}", scheduledJob.getUuid());
  }

  public void restoreJob(ScheduledJob scheduledJob)
      throws SchedulerException, ClassNotFoundException {
    if (scheduledJobRepository
        .findByRepositoryNameAndJobType(
            scheduledJob.getRepository().getName(), scheduledJob.getJobType().getEnum())
        .isPresent()) {
      throw new ScheduledJobException(
          "Scheduled job of type "
              + scheduledJob.getJobType().getEnum()
              + " with repository already exists: "
              + scheduledJob.getRepository().getName());
    }
    scheduledJob.setDeleted(false);
    scheduledJobRepository.save(scheduledJob);
    scheduledJobManager.scheduleJob(scheduledJob);
    logger.info("Restored scheduled job with uuid: {}", scheduledJob.getUuid());
  }

  private Repository resolveRepositoryFromDTO(ScheduledJobDTO scheduledJobDTO) {
    if (scheduledJobDTO.getRepository() == null) {
      throw new ScheduledJobException("Repository must be provided");
    }
    Repository repository = repositoryRepository.findByName(scheduledJobDTO.getRepository());
    if (repository == null) {
      throw new ScheduledJobException("Repository not found: " + scheduledJobDTO.getRepository());
    }
    return repository;
  }

  private ScheduledJobType resolveJobTypeFromDTO(ScheduledJobDTO scheduledJobDTO) {
    if (scheduledJobDTO.getType() == null) {
      throw new ScheduledJobException("Job type must be provided");
    }
    ScheduledJobType jobType = scheduledJobTypeRepository.findByEnum(scheduledJobDTO.getType());
    if (jobType == null) {
      throw new ScheduledJobException("Job type not found: " + scheduledJobDTO.getType());
    }
    return jobType;
  }
}
