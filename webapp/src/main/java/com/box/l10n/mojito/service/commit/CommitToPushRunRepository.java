package com.box.l10n.mojito.service.commit;

import com.box.l10n.mojito.entity.CommitToPushRun;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author garion
 */
@RepositoryRestResource(exported = false)
public interface CommitToPushRunRepository extends JpaRepository<CommitToPushRun, Long> {
  Optional<CommitToPushRun> findByCommitId(Long commitId);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          delete from CommitToPushRun
          where pushRun in (select pr from PushRun pr where createdDate < :beforeDate)
          """)
  void deleteAllByPushRunWithCreatedDateBefore(@Param("beforeDate") ZonedDateTime beforeDate);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          delete from CommitToPushRun
           where pushRun.createdDate >= :startDate
             and pushRun.createdDate < :endDate
             and pushRun.id not in :latestPushRunIdsPerAsset
          """)
  void deleteByPushRunsNotLatestPerAsset(
      @Param("startDate") ZonedDateTime startDate,
      @Param("endDate") ZonedDateTime endDate,
      @Param("latestPushRunIdsPerAsset") List<Long> latestPushRunIdsPerAsset);
}
