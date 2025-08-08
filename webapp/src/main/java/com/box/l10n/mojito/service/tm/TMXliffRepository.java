package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.TMXliff;
import java.time.ZonedDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jyi
 */
@RepositoryRestResource(exported = false)
public interface TMXliffRepository extends JpaRepository<TMXliff, Long> {

  TMXliff findByPollableTask(PollableTask pollableTask);

  @Modifying
  @Transactional
  @Query(
      nativeQuery = true,
      value =
          """
        update tm_xliff
           set export_pollable_task_id = null
         where export_pollable_task_id in (select id
                                             from pollable_task
                                            where finished_date < :beforeDate)
        """)
  int cleanStaleExportPollableTaskIds(@Param("beforeDate") ZonedDateTime beforeDate);
}
