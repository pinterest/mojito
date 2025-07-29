package com.box.l10n.mojito.service.assetcontent;

import com.box.l10n.mojito.entity.AssetContent;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jeanaurambault
 */
@RepositoryRestResource(exported = false)
public interface AssetContentRepository
    extends JpaRepository<AssetContent, Long>, JpaSpecificationExecutor<AssetContent> {

  @EntityGraph(value = "AssetContent.legacy", type = EntityGraph.EntityGraphType.FETCH)
  @Override
  Optional<AssetContent> findById(Long id);

  List<AssetContent> findByAssetRepositoryIdAndBranchName(Long repositoryId, String branchName);

  @Transactional
  int deleteByAssetExtractionsIdIsNull();

  @Query(
      """
      select ac.id from AssetContent ac
        left join ac.assetExtractions ae
        left join ae.asset a
        left join ae.assetExtractionByBranches aebb
        left join aebb.branch b
       where ac.lastModifiedDate < :beforeDate
         and (b is null or b.deleted)
         and (a is null or a.lastSuccessfulAssetExtraction <> ae)
      """)
  List<Long> findStaleAssetContent(
      @Param("beforeDate") ZonedDateTime beforeDate, Pageable pageable);
}
