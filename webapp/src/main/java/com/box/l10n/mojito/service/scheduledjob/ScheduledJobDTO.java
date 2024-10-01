package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.entity.ScheduledJob;
import java.util.Date;

public class ScheduledJobDTO {
  private Long id;
  private String repository;
  private ScheduledJobType jobType;
  private String cron;
  private ScheduledJobProperties properties;
  private ScheduledJobStatus jobStatus;
  private Date startDate;
  private Date endDate;

  public ScheduledJobDTO(ScheduledJob scheduledJob) {
    this.id = scheduledJob.getId();
    this.repository = scheduledJob.getRepository().getName();
    this.jobType = scheduledJob.getJobType();
    this.cron = scheduledJob.getCron();
    this.properties = scheduledJob.getProperties();
    this.jobStatus = scheduledJob.getJobStatus();
    this.startDate = scheduledJob.getStartDate();
    this.endDate = scheduledJob.getEndDate();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getRepository() {
    return repository;
  }

  public void setRepositoryId(String repository) {
    this.repository = repository;
  }

  public ScheduledJobType getJobType() {
    return jobType;
  }

  public void setJobType(ScheduledJobType jobType) {
    this.jobType = jobType;
  }

  public String getCron() {
    return cron;
  }

  public void setCron(String cron) {
    this.cron = cron;
  }

  public ScheduledJobProperties getProperties() {
    return properties;
  }

  public void setProperties(ScheduledJobProperties properties) {
    this.properties = properties;
  }

  public ScheduledJobStatus getJobStatus() {
    return jobStatus;
  }

  public void setJobStatus(ScheduledJobStatus jobStatus) {
    this.jobStatus = jobStatus;
  }

  public Date getStartDate() {
    return startDate;
  }

  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  public Date getEndDate() {
    return endDate;
  }

  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }
}
