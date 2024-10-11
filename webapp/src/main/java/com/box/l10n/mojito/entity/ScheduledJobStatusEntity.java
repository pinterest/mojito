package com.box.l10n.mojito.entity;

import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;

import com.box.l10n.mojito.rest.View;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobStatus;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "scheduled_job_status")
@Audited(targetAuditMode = NOT_AUDITED)
public class ScheduledJobStatusEntity extends BaseEntity {
  @Id private Long id;

  @Basic(optional = false)
  @Column(name = "name")
  @Enumerated(EnumType.STRING)
  @JsonView(View.Repository.class)
  private ScheduledJobStatus jobStatus;
}
