package com.box.l10n.mojito.service.scheduledjob;

import java.util.List;
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

@RestController
public class ScheduledJobWS {

  static Logger logger = LoggerFactory.getLogger(ScheduledJobWS.class);

  @Autowired ScheduledJobManager scheduledJobManager;
  @Autowired ScheduledJobsRepository scheduledJobsRepository;

  @RequestMapping(method = RequestMethod.GET, value = "/api/jobs")
  @ResponseStatus(HttpStatus.OK)
  public List<ScheduledJob<?>> getJobs() {
    return scheduledJobsRepository.getJobs();
  }

  @RequestMapping(value = "/api/job/{id}", method = RequestMethod.GET)
  public ScheduledJob<?> runJob(@PathVariable("id") String id) {
    String[] idParts = id.split(":");
    String repo = idParts[0];
    String type = idParts[1];

    try {
      scheduledJobManager.triggerJob(repo, type);
    } catch (SchedulerException e) {
      throw new RuntimeException(e);
    }

    return scheduledJobsRepository.getJob(repo, type);
  }
}
