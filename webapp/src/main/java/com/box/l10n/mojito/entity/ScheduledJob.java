package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.ZonedDateTime;
import org.hibernate.envers.Audited;

@Audited
@Entity
@Table(name = "scheduled_job")
public class ScheduledJob {
  @Id private String id;

  @ManyToOne
  @JoinColumn(
      name = "repository_id",
      foreignKey = @ForeignKey(name = "FK__SCHEDULED_JOB__IMPORT_REPOSITORY__ID"))
  private Repository repository;

  @ManyToOne
  @JoinColumn(name = "job_type", foreignKey = @ForeignKey(name = "FK__JOB_TYPE__JOB_TYPE_ID"))
  private ScheduledJobTypeEntity jobType;

  @Column(name = "cron")
  private String cron;

  @Transient private ScheduledJobProperties properties;

  @Column(name = "properties")
  private String propertiesString;

  @ManyToOne
  @JoinColumn(name = "job_status", foreignKey = @ForeignKey(name = "FK__JOB_STATUS__JOB_STATUS_ID"))
  private ScheduledJobStatusEntity jobStatus;

  @Column(name = "start_date")
  private ZonedDateTime startDate;

  @Column(name = "end_date")
  private ZonedDateTime endDate;

  @Column(name = "enabled")
  private Boolean enabled = true;

  @PostLoad
  public void deserializeProperties() {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      this.properties =
          objectMapper.readValue(propertiesString, jobType.getEnum().getPropertiesClass());
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to deserialize properties '"
              + propertiesString
              + "' for class: "
              + jobType.getEnum().getPropertiesClass().getName(),
          e);
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Repository getRepository() {
    return repository;
  }

  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  public ScheduledJobTypeEntity getJobType() {
    return jobType;
  }

  public void setJobType(ScheduledJobTypeEntity jobType) {
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

  public ScheduledJobStatusEntity getJobStatus() {
    return jobStatus;
  }

  public void setJobStatus(ScheduledJobStatusEntity jobStatus) {
    this.jobStatus = jobStatus;
  }

  public ZonedDateTime getStartDate() {
    return startDate;
  }

  public void setStartDate(ZonedDateTime startDate) {
    this.startDate = startDate;
  }

  public ZonedDateTime getEndDate() {
    return endDate;
  }

  public void setEndDate(ZonedDateTime endDate) {
    this.endDate = endDate;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }
}
