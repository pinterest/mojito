package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.entity.ScheduledJobStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ScheduledJobStatusRepository
    extends JpaRepository<ScheduledJobStatusEntity, Long> {
  @Query("SELECT sjs FROM ScheduledJobStatusEntity sjs " + "WHERE sjs.jobStatus = :jobStatus")
  ScheduledJobStatusEntity findByEnum(ScheduledJobStatus jobStatus);
}
