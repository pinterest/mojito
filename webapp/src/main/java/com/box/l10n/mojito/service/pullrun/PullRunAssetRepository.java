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
  Optional<PullRunAsset> findByPullRunAndAsset(PullRun pullRun, Asset asset);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          delete from PullRunAsset
           where pullRun in (select pr from PullRun pr where createdDate < :beforeDate)
          """)
  void deleteAllByPullRunWithCreatedDateBefore(@Param("beforeDate") ZonedDateTime beforeDate);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          delete from PullRunAsset
           where pullRun.createdDate >= :startDate
             and pullRun.createdDate < :endDate
             and pullRunTextUnitVariants is empty
             and pullRun.id not in :latestPullRunIdsPerAsset
          """)
  void deleteByPullRunsNotLatestPerAsset(
      @Param("startDate") ZonedDateTime startDate,
      @Param("endDate") ZonedDateTime endDate,
      @Param("latestPullRunIdsPerAsset") List<Long> latestPullRunIdsPerAsset);
}
