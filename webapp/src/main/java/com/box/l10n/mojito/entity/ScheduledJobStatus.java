package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "scheduled_job_status_type")
public class ScheduledJobStatus extends BaseEntity {
  @Id private Long id;

  @Basic(optional = false)
  @Column(name = "name")
  @Enumerated(EnumType.STRING)
  @JsonView(View.Repository.class)
  private com.box.l10n.mojito.service.scheduledjob.ScheduledJobStatus jobStatus;

  public com.box.l10n.mojito.service.scheduledjob.ScheduledJobStatus getEnum() {
    return jobStatus;
  }

  public void setJobStatus(com.box.l10n.mojito.service.scheduledjob.ScheduledJobStatus jobStatus) {
    this.jobStatus = jobStatus;
  }
}
