package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author aloison
 */
@RepositoryRestResource(exported = false)
public interface AssetExtractionRepository extends JpaRepository<AssetExtraction, Long> {

  @EntityGraph(value = "AssetExtraction.legacy", type = EntityGraph.EntityGraphType.FETCH)
  @Override
  Optional<AssetExtraction> findById(Long id);

  List<AssetExtraction> findByAsset(Asset asset);

  @Query(
      """
      select ae.id from #{#entityName} ae
      inner join ae.asset a
      inner join ae.pollableTask pt
      left outer join ae.assetExtractionByBranches aea
      where aea.id is null
        and ae != a.lastSuccessfulAssetExtraction
        and pt.finishedDate is not null
        """)
  List<Long> findFinishedAndOldAssetExtractions(Pageable pageable);

  @Modifying
  @Transactional
  @Query(
      nativeQuery = true,
      value =
          """
        update asset_extraction ae
          join pollable_task pt
            on pt.id = ae.pollable_task_id
           set ae.pollable_task_id = null
         where pt.finished_date < :beforeDate
        """)
  int cleanStalePollableTaskIds(@Param("beforeDate") ZonedDateTime beforeDate);

  @Modifying
  @Transactional
  @Query(
      nativeQuery = true,
      value =
          """
        update asset_extraction todelete
           set todelete.asset_content_id = null
         where todelete.id in (select id
                                 from (select ae.id
                                         from asset_extraction ae
                                         join asset_content ac
                                           on ac.id = ae.asset_content_id
                                        where ac.last_modified_date < :beforeDate
                                        limit :batchSize) as ae
                              )
        """)
  int cleanStaleAssetContentIds(
      @Param("beforeDate") ZonedDateTime beforeDate, @Param("batchSize") int batchSize);
}
