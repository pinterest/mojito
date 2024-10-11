package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.entity.ScheduledJobTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ScheduledJobTypeRepository extends JpaRepository<ScheduledJobTypeEntity, Long> {
  @Query("SELECT sjt FROM ScheduledJobTypeEntity sjt " + "WHERE sjt.jobType = :jobType")
  ScheduledJobTypeEntity findByEnum(
      com.box.l10n.mojito.service.scheduledjob.ScheduledJobType jobType);
}
