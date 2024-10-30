package com.box.l10n.mojito.rest.scheduledjob;

import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobDTO;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobManager;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobRepository;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Webservice for viewing, enabling/disabling and triggering scheduled jobs.
 *
 * @author mattwilshire
 */
@RestController
@ConditionalOnProperty(value = "l10n.scheduledJobs.enabled", havingValue = "true")
public class ScheduledJobWS {

  static Logger logger = LoggerFactory.getLogger(ScheduledJobWS.class);

  @Autowired ScheduledJobRepository scheduledJobRepository;

  @Autowired ScheduledJobManager scheduledJobManager;

  private final ResponseEntity<ScheduledJobResponse> notFoundResponse =
      createResponse(HttpStatus.NOT_FOUND, ScheduledJobResponse.Status.ERROR, "Job doesn't exist");

  @RequestMapping(method = RequestMethod.GET, value = "/api/jobs")
  @ResponseStatus(HttpStatus.OK)
  public List<ScheduledJobDTO> getAllJobs() {
    List<ScheduledJob> scheduledJobs = scheduledJobRepository.findAll();
    return scheduledJobs.stream().map(ScheduledJobDTO::new).collect(Collectors.toList());
  }

  @RequestMapping(method = RequestMethod.GET, value = "/api/jobs/{id}")
  @ResponseStatus(HttpStatus.OK)
  public ScheduledJobDTO getJob(@PathVariable UUID id) {
    return scheduledJobRepository
        .findById(id.toString())
        .map(ScheduledJobDTO::new)
        .orElseThrow(
            () ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found with id: " + id));
  }

  @RequestMapping(method = RequestMethod.POST, value = "/api/jobs/{id}/trigger")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<ScheduledJobResponse> triggerJob(@PathVariable UUID id) {

    Optional<ScheduledJob> optScheduledJob = scheduledJobRepository.findById(id.toString());

    if (optScheduledJob.isEmpty()) return notFoundResponse;

    ScheduledJob scheduledJob = optScheduledJob.get();
    JobKey jobKey = scheduledJobManager.getJobKey(scheduledJob);

    try {
      if (!scheduledJobManager.getScheduler().checkExists(jobKey)) return notFoundResponse;

      // Ignore the trigger request if job is currently running
      for (JobExecutionContext jobExecutionContext :
          scheduledJobManager.getScheduler().getCurrentlyExecutingJobs()) {
        if (jobExecutionContext.getJobDetail().getKey().equals(jobKey)) {
          return createResponse(
              HttpStatus.CONFLICT,
              ScheduledJobResponse.Status.ERROR,
              "Job is currently running, trigger request ignored");
        }
      }

      scheduledJobManager.getScheduler().triggerJob(jobKey);
      logger.info(
          "Job '{}' for repository '{}' was manually triggered.",
          scheduledJob.getJobType().getEnum(),
          scheduledJob.getRepository().getName());
      return ResponseEntity.status(HttpStatus.OK)
          .body(new ScheduledJobResponse(ScheduledJobResponse.Status.SUCCESS, "Job triggered"));
    } catch (SchedulerException e) {
      logger.error(
          "Error triggering job manually, job: {}", jobKey.getName() + ":" + jobKey.getGroup(), e);
      return createResponse(
          HttpStatus.INTERNAL_SERVER_ERROR, ScheduledJobResponse.Status.ERROR, e.getMessage());
    }
  }

  @RequestMapping(method = RequestMethod.POST, value = "/api/jobs/{id}/enable")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<ScheduledJobResponse> enableJob(@PathVariable UUID id) {
    return setJobActive(id, true);
  }

  @RequestMapping(method = RequestMethod.POST, value = "/api/jobs/{id}/disable")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<ScheduledJobResponse> disableJob(@PathVariable UUID id) {
    return setJobActive(id, false);
  }

  private ResponseEntity<ScheduledJobResponse> setJobActive(UUID id, boolean active) {
    Optional<ScheduledJob> optScheduledJob = scheduledJobRepository.findById(id.toString());

    if (optScheduledJob.isEmpty()) return notFoundResponse;

    ScheduledJob scheduledJob = optScheduledJob.get();
    JobKey jobKey = scheduledJobManager.getJobKey(scheduledJob);

    try {
      if (!scheduledJobManager.getScheduler().checkExists(jobKey)) return notFoundResponse;

      if (scheduledJob.getEnabled() == active)
        return createResponse(
            HttpStatus.ALREADY_REPORTED,
            ScheduledJobResponse.Status.SUCCESS,
            "The job is already " + (active ? "enabled" : "disabled"));

      scheduledJob.setEnabled(active);
      scheduledJobRepository.save(scheduledJob);

      return createResponse(
          HttpStatus.OK,
          ScheduledJobResponse.Status.SUCCESS,
          "Job is now " + (active ? "enabled" : "disabled"));
    } catch (SchedulerException e) {

      logger.error(
          "SchedulerException thrown from trying to change job '{}' : '{}' to active: {}",
          scheduledJob.getJobType().getEnum(),
          scheduledJob.getRepository().getName(),
          active,
          e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Job with id: " + id.toString() + " could not be disabled");
    }
  }

  private ResponseEntity<ScheduledJobResponse> createResponse(
      HttpStatus status, ScheduledJobResponse.Status jobResponseStatus, String message) {
    return ResponseEntity.status(status).body(new ScheduledJobResponse(jobResponseStatus, message));
  }
}
