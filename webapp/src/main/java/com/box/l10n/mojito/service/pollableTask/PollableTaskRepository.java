package com.box.l10n.mojito.service.pollableTask;

import com.box.l10n.mojito.entity.PollableTask;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface PollableTaskRepository extends JpaRepository<PollableTask, Long> {

  @Override
  @EntityGraph(value = "PollableTask.legacy", type = EntityGraphType.FETCH)
  Optional<PollableTask> findById(Long aLong);

  /**
   * Retrieves pollable tasks that have not finished yet and have exceeded the maximum execution
   * time.
   *
   * <p>Must pass "now" as parameter due to HSQL persisting ZonedDateTime without TZ info. Comparing
   * ZonedDateTime against unix_timestamp() then fails because of the TZ difference.
   *
   * <p>This does not show if test are running in UTC like on CI
   */
  @Query(
      """
      select pt from #{#entityName} pt
      where pt.finishedDate is null
      and (cast(unix_timestamp(pt.createdDate) as biginteger) + cast(pt.timeout as biginteger)) < cast(unix_timestamp(:now) as biginteger)
      """)
  List<PollableTask> findZombiePollableTasks(@Param("now") ZonedDateTime now, Pageable pageable);

  @Modifying
  @Transactional
  @Query(
      nativeQuery = true,
      value =
          """
        update pollable_task pt_to_update
           set parent_task_id = null
         where id in (select id
                        from (select pt.id
                                from pollable_task pt
                                join pollable_task parent_pt
                                  on pt.parent_task_id = parent_pt.id
                               where parent_pt.finished_date < :beforeDate
                               limit :batchSize) pt
                     )
        """)
  int cleanParentTasksWithFinishedDateBefore(
      @Param("beforeDate") ZonedDateTime beforeDate, @Param("batchSize") int batchSize);

  @Modifying
  @Transactional
  @Query(
      nativeQuery = true,
      value =
          """
        delete from pollable_task
         where id in (select id
                        from (select pt.id
                                from pollable_task pt
                                left join pollable_task child_pt
                                  on child_pt.parent_task_id = pt.id
                               where pt.finished_date < :beforeDate
                                 and child_pt.id is null
                               limit :batchSize) pt
                     )
        """)
  int deleteAllByFinishedDateBefore(
      @Param("beforeDate") ZonedDateTime beforeDate, @Param("batchSize") int batchSize);
}
