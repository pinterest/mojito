package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.entity.PullRun;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
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
public interface PullRunRepository extends JpaRepository<PullRun, Long> {
  Optional<PullRun> findByName(String name);

  @Query(
      value =
          """
          select p from PullRun p
          left join CommitToPullRun cpr on cpr.pullRun = p
          left join Commit c on c = cpr.commit
          where c.name in :commitNames and c.repository.id = :repositoryId
          and p.repository.id = :repositoryId order by p.createdDate desc
          """)
  List<PullRun> findLatestByCommitNames(
      @Param("commitNames") List<String> commitNames,
      @Param("repositoryId") Long repositoryId,
      Pageable pageable);

  @Transactional
  @Modifying
  @Query(
      nativeQuery = true,
      value =
          """
          delete pr
          from pull_run pr
          where pr.created_date < :beforeDate
          """)
  void deleteAllByCreatedDateBefore(@Param("beforeDate") ZonedDateTime beforeDate);

  @Transactional
  @Modifying
  @Query(
      nativeQuery = true,
      value =
          """
          delete from pull_run
           where created_date between :startDate and :endDate
             and id not in (select max_id
                              from (select MAX(pr.id) as max_id
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
                                     group by pra.asset_id) as latest_pr)
          """)
  void cleanPullRunPerAsset(
      @Param("startDate") ZonedDateTime startDate, @Param("endDate") ZonedDateTime endDate);
}
