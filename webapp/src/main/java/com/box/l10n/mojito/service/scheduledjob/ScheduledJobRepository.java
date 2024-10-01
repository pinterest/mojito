package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.entity.ScheduledJob;
import org.quartz.JobKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, Long> {

  @Query(
      "SELECT sj FROM ScheduledJob sj "
          + "WHERE sj.repository.id = :repositoryId AND sj.jobType = :jobType")
  ScheduledJob findByRepositoryIdAndJobType(Long repositoryId, ScheduledJobType jobType);

  default ScheduledJob findByJobKey(JobKey jobKey) {
    return findByRepositoryIdAndJobType(
        Long.parseLong(jobKey.getName()), ScheduledJobType.valueOf(jobKey.getGroup()));
  }
}
