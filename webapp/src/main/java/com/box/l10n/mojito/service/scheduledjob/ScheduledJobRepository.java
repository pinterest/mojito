package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.entity.ScheduledJob;
import org.quartz.JobKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, String> {

  @Query("SELECT sj FROM ScheduledJob sj WHERE sj.id = :id AND sj.jobType.jobType = :jobType")
  ScheduledJob findByIdAndJobType(String id, ScheduledJobType jobType);

  default ScheduledJob findByJobKey(JobKey jobKey) {
    return findByIdAndJobType(jobKey.getName(), ScheduledJobType.valueOf(jobKey.getGroup()));
  }
}
