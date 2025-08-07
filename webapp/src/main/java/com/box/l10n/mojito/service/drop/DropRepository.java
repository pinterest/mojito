package com.box.l10n.mojito.service.drop;

import com.box.l10n.mojito.entity.Drop;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface DropRepository extends JpaRepository<Drop, Long>, JpaSpecificationExecutor<Drop> {

  @Override
  @EntityGraph(value = "Drop.legacy", type = EntityGraphType.FETCH)
  Optional<Drop> findById(Long aLong);

  @Modifying
  @Transactional
  @Query(
      nativeQuery = true,
      value =
          """
        update `drop`
           set export_pollable_task_id = null
         where export_pollable_task_id in (select id
                                             from pollable_task
                                            where finished_date < :beforeDate)
        """)
  int cleanStaleExportPollableTaskIds(@Param("beforeDate") ZonedDateTime beforeDate);

  @Modifying
  @Transactional
  @Query(
      nativeQuery = true,
      value =
          """
        update `drop`
           set import_pollable_task_id = null
         where import_pollable_task_id in (select id
                                             from pollable_task
                                            where finished_date < :beforeDate)
        """)
  int cleanStaleImportPollableTaskIds(@Param("beforeDate") ZonedDateTime beforeDate);
}
