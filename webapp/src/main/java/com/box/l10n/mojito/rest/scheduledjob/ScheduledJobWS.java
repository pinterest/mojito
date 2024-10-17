package com.box.l10n.mojito.rest.scheduledjob;

import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobDTO;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobManager;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
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
  public ScheduledJobDTO getJob(@PathVariable Long id) {
    return scheduledJobRepository
        .findById(id)
        .map(ScheduledJobDTO::new)
        .orElseThrow(
            () ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found with id: " + id));
  }

  @RequestMapping(method = RequestMethod.GET, value = "/api/jobs/{id}/trigger")
  @ResponseStatus(HttpStatus.OK)
  public String triggerJob(@PathVariable Long id) {

    Optional<ScheduledJob> optionalScheduledJob = scheduledJobRepository.findById(id);
    if (optionalScheduledJob.isEmpty()) return "Job not found with id: " + id;
    ScheduledJob scheduledJob = optionalScheduledJob.get();

    try {
      return scheduledJobManager.triggerJob(scheduledJob);
    } catch (SchedulerException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Job with id: " + id + " could not be triggered");
    }
  }

  @RequestMapping(method = RequestMethod.GET, value = "/api/jobs/{id}/toggle")
  @ResponseStatus(HttpStatus.OK)
  public String toggleJob(@PathVariable Long id) {
    Optional<ScheduledJob> optionalScheduledJob = scheduledJobRepository.findById(id);
    if (optionalScheduledJob.isEmpty()) return "Job not found with id: " + id;
    ScheduledJob scheduledJob = optionalScheduledJob.get();

    try {
      return scheduledJobManager.toggleJob(scheduledJob);
    } catch (SchedulerException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Job with id: " + id + " could not be disabled");
    }
  }
}
