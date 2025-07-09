package com.box.l10n.mojito.service.commit;

import com.box.l10n.mojito.entity.CommitToPullRun;
import java.time.ZonedDateTime;
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
      nativeQuery = true,
      value =
          """
          delete ctpr
            from pull_run pr
            join commit_to_pull_run ctpr on ctpr.pull_run_id = pr.id
           where pr.created_date between :startDate and :endDate
             and pr.id not in (select MAX(pr.id) as max_id
                                 from pull_run pr
                                 join pull_run_asset pra
                                   on pra.pull_run_id = pr.id
                                 join (select pra.asset_id as asset_id,
                                              MAX(pr.created_date) as max_created_date
                                         from pull_run pr
                                         join pull_run_asset pra
                                           on pra.pull_run_id = pr.id
                                        where pr.created_date between :startDate and :endDate
                                        group by pra.asset_id) latest_pr
                                   on pra.asset_id = latest_pr.asset_id
                                  and pr.created_date = latest_pr.max_created_date
                                group by pra.asset_id)
          """)
  void deleteAllByPullRunAndAsset(
      @Param("startDate") ZonedDateTime startDate, @Param("endDate") ZonedDateTime endDate);
}
