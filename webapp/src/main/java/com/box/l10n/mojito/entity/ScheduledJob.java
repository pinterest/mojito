package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.rest.View;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobProperties;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobStatus;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobType;
import com.box.l10n.mojito.service.scheduledjob.jobs.ScheduledThirdPartySyncProperties;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Date;

@Entity
@Table(name = "scheduled_job")
public class ScheduledJob extends BaseEntity {
  @Id private Long id;

  @ManyToOne
  @JoinColumn(name = "repository_id")
  private Repository repository;

  @Basic(optional = false)
  @Column(name = "job_type")
  @Enumerated(EnumType.STRING)
  @JsonView(View.Repository.class)
  private ScheduledJobType jobType;

  @Column(name = "cron")
  private String cron;

  @Transient private ScheduledJobProperties properties;

  @Column(name = "properties")
  private String propertiesString;

  @Basic(optional = false)
  @Column(name = "job_status")
  @Enumerated(EnumType.STRING)
  @JsonView(View.Repository.class)
  private ScheduledJobStatus jobStatus;

  @Column(name = "start_date")
  private Date startDate;

  @Column(name = "end_date")
  private Date endDate;

  @PostLoad
  public void deserializeProperties() {
    ObjectMapper objectMapper = new ObjectMapper();

    try {
      if (jobType == ScheduledJobType.THIRD_PARTY_SYNC) {
        this.properties =
            objectMapper.readValue(propertiesString, ScheduledThirdPartySyncProperties.class);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize properties", e);
    }
  }

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public void setId(Long id) {
    this.id = id;
  }

  public Repository getRepository() {
    return repository;
  }

  public void setRepository(Repository repository) {
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

    ObjectMapper objectMapper = new ObjectMapper();
    try {
      this.propertiesString = objectMapper.writeValueAsString(this.properties);
      System.out.println(propertiesString);
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize properties", e);
    }
  }

  public String getPropertiesString() {
    return propertiesString;
  }

  public void setPropertiesString(String properties) {
    this.propertiesString = properties;
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
