package com.box.l10n.mojito.rest.scheduledjob;

import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobDTO;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(value = "l10.scheduledJobs.enabled", havingValue = "true")
public class ScheduledJobWS {

  static Logger logger = LoggerFactory.getLogger(ScheduledJobWS.class);

  @Autowired ScheduledJobRepository scheduledJobRepository;

  @RequestMapping(method = RequestMethod.GET, value = "/api/jobs")
  @ResponseStatus(HttpStatus.OK)
  public List<ScheduledJobDTO> getJobs() {
    List<ScheduledJob> scheduledJobs = scheduledJobRepository.findAll();
    return scheduledJobs.stream().map(ScheduledJobDTO::new).collect(Collectors.toList());
  }
}
