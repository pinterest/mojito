package com.box.l10n.mojito.service.commit;

import com.box.l10n.mojito.entity.CommitToPullRun;
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
public interface CommitToPullRunRepository extends JpaRepository<CommitToPullRun, Long> {
  Optional<CommitToPullRun> findByCommitId(Long id);

  @Transactional
  @Modifying
  @Query(
      nativeQuery = true,
      value =
          """
          delete ctpr
          from pull_run pr
          join commit_to_pull_run ctpr on ctpr.pull_run_id = pr.id
          where pr.created_date < :beforeDate
          """)
  void deleteAllByPullRunWithCreatedDateBefore(@Param("beforeDate") ZonedDateTime beforeDate);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          delete from CommitToPullRun
           where pullRun.createdDate between :startDate and :endDate
             and pullRun.id not in :latestPullRunIdsPerAsset
          """)
  void deleteByPullRunsNotLatestPerAsset(
      @Param("startDate") ZonedDateTime startDate,
      @Param("endDate") ZonedDateTime endDate,
      @Param("latestPullRunIdsPerAsset") List<Long> latestPullRunIdsPerAsset);
}
