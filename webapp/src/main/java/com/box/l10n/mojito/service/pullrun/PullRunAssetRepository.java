package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PullRunAsset;
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
public interface PullRunAssetRepository extends JpaRepository<PullRunAsset, Long> {
  List<PullRunAsset> findByPullRun(PullRun pullRun);

  Optional<PullRunAsset> findByPullRunAndAsset(PullRun pullRun, Asset asset);

  @Transactional
  void deleteByPullRun(PullRun pullRun);

  @Transactional
  @Modifying
  @Query(
      nativeQuery = true,
      value =
          """
          delete pra
          from pull_run pr
          join pull_run_asset pra on pra.pull_run_id = pr.id
          where pr.created_date < :beforeDate
          """)
  void deleteAllByPullRunWithCreatedDateBefore(@Param("beforeDate") ZonedDateTime beforeDate);

  @Transactional
  @Modifying
  @Query(
      nativeQuery = true,
      value =
          """
          delete pra
            from pull_run pr
            join pull_run_asset pra on pra.pull_run_id = pr.id
           where pr.created_date between :startDate and :endDate
             and pr.id not in (select max_id
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
  void deleteAllByPullRunAndAsset(
      @Param("startDate") ZonedDateTime startDate, @Param("endDate") ZonedDateTime endDate);
}
