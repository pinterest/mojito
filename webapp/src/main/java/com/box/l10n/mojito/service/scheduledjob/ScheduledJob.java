package com.box.l10n.mojito.service.scheduledjob;

import java.util.Date;

public class ScheduledJob<T> {
  String cron;
  String repository;
  // The goal here will be to have factory deserialize the properties to its correct class, this way
  // we can safely cast based on the current job execution method we are in.
  T properties;
  ScheduledJobStatus status;
  Date startDate;
  Date endDate;

  public T getProperties() {
    return properties;
  }

  public void setProperties(T properties) {
    this.properties = properties;
  }

  public ScheduledJobStatus getStatus() {
    return status;
  }

  public void setStatus(ScheduledJobStatus status) {
    this.status = status;
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

  public void setCron(String cron) {
    this.cron = cron;
  }

  public String getCron() {
    return cron;
  }

  public void setRepository(String repository) {
    this.repository = repository;
  }

  public String getRepository() {
    return repository;
  }
}
