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
 * Webservice for enabling/disabling and triggering scheduled jobs.
 *
 * @author mattwilshire
 */
@RestController
@ConditionalOnProperty(value = "l10n.scheduledJobs.enabled", havingValue = "true")
public class ScheduledJobWS {

  static Logger logger = LoggerFactory.getLogger(ScheduledJobWS.class);

  @Autowired ScheduledJobRepository scheduledJobRepository;
  @Autowired ScheduledJobManager scheduledJobManager;

  @RequestMapping(method = RequestMethod.GET, value = "/api/jobs")
  @ResponseStatus(HttpStatus.OK)
  public List<ScheduledJobDTO> getJobs() {
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

  @RequestMapping(method = RequestMethod.POST, value = "/api/jobs/{id}")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<ScheduledJobResponse> triggerJob(@PathVariable UUID id) {

    Optional<ScheduledJob> optScheduledJob = scheduledJobRepository.findById(id.toString());
    if (optScheduledJob.isEmpty())
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new ScheduledJobResponse(ScheduledJobResponse.Status.ERROR, "Job not found"));

    ScheduledJob scheduledJob = optScheduledJob.get();
    JobKey jobKey = scheduledJobManager.getJobKey(scheduledJob);

    try {
      if (!scheduledJobManager.getScheduler().checkExists(jobKey))
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ScheduledJobResponse(ScheduledJobResponse.Status.ERROR, "Job doesn't exist"));

      // Is the job currently running ?
      // Ignore the trigger request
      for (JobExecutionContext jobExecutionContext :
          scheduledJobManager.getScheduler().getCurrentlyExecutingJobs()) {
        if (jobExecutionContext.getJobDetail().getKey().equals(jobKey)) {
          return ResponseEntity.status(HttpStatus.CONFLICT)
              .body(
                  new ScheduledJobResponse(
                      ScheduledJobResponse.Status.ERROR,
                      "Job is currently running, trigger request ignored"));
        }
      }

      scheduledJobManager.getScheduler().triggerJob(jobKey);
      return ResponseEntity.status(HttpStatus.OK)
          .body(new ScheduledJobResponse(ScheduledJobResponse.Status.SUCCESS, "Job triggered"));
    } catch (SchedulerException e) {
      logger.error(
          "Error triggering job manually, job: {}", jobKey.getName() + ":" + jobKey.getGroup(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new ScheduledJobResponse(ScheduledJobResponse.Status.ERROR, e.getMessage()));
    }
  }

  @RequestMapping(method = RequestMethod.POST, value = "/api/jobs/{id}/toggle")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<ScheduledJobResponse> toggleJob(@PathVariable UUID id) {
    Optional<ScheduledJob> optScheduledJob = scheduledJobRepository.findById(id.toString());
    if (optScheduledJob.isEmpty())
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new ScheduledJobResponse(ScheduledJobResponse.Status.ERROR, "Job not found"));

    ScheduledJob scheduledJob = optScheduledJob.get();
    JobKey jobKey = scheduledJobManager.getJobKey(scheduledJob);

    try {
      if (!scheduledJobManager.getScheduler().checkExists(jobKey))
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ScheduledJobResponse(ScheduledJobResponse.Status.ERROR, "Job doesn't exist"));

      scheduledJob.setEnabled(!scheduledJob.getEnabled());
      scheduledJobRepository.save(scheduledJob);

      return ResponseEntity.status(HttpStatus.OK)
          .body(
              new ScheduledJobResponse(
                  ScheduledJobResponse.Status.SUCCESS,
                  "Job " + (scheduledJob.getEnabled() ? "enabled" : "disabled")));
    } catch (SchedulerException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Job with id: " + id.toString() + " could not be disabled");
    }
  }
}
