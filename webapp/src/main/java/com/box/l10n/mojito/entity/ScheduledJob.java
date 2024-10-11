package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Date;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "scheduled_job")
@Audited
public class ScheduledJob extends BaseEntity {
  @Id private Long id;

  @ManyToOne
  @JoinColumn(name = "repository_id")
  private Repository repository;

  @ManyToOne
  @JoinColumn(name = "job_type")
  private ScheduledJobTypeEntity jobType;

  @Column(name = "cron")
  private String cron;

  @Transient private ScheduledJobProperties properties;

  @Column(name = "properties")
  private String propertiesString;

  @ManyToOne
  @JoinColumn(name = "job_status")
  private ScheduledJobStatusEntity jobStatus;

  @Column(name = "start_date")
  private Date startDate;

  @Column(name = "end_date")
  private Date endDate;

  @Column(name = "enabled", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
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

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }
}
