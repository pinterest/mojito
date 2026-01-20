package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.Repository;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jeanaurambault
 */
@RepositoryRestResource(exported = false)
public interface BranchRepository
    extends JpaRepository<Branch, Long>, JpaSpecificationExecutor<Branch> {

  @Override
  @EntityGraph(value = "Branch.legacy", type = EntityGraphType.FETCH)
  Optional<Branch> findById(Long aLong);

  Branch findByNameAndRepository(String name, Repository repository);

  List<Branch> findByRepositoryIdAndDeletedFalseAndNameNotNullAndNameNot(
      Long repositoryId, String primaryBranch);

  List<Branch> findByDeletedFalseAndNameNotNullAndNameNot(String primaryBranch);

  @Query(
      value =
          """
      SELECT b FROM Branch b
      INNER JOIN BranchStatistic bs ON bs.branch = b
      INNER JOIN BranchMergeTarget bmt ON b = bmt.branch
      WHERE bs.translatedDate IS NOT NULL
      AND bs.translatedDate >= :cutoffDate
      AND bs.forTranslationCount = 0
      AND bs.totalCount > 0
      AND bmt.targetsMain = true
      AND b.repository = :repository
      AND b.deleted = false
      ORDER BY bs.translatedDate ASC
      """)
  List<Branch> findBranchesForAppending(
      @Param("repository") Repository repository, @Param("cutoffDate") ZonedDateTime cutoffDate);

  @Query(
      """
      SELECT new com.box.l10n.mojito.service.branch.BranchTextUnitStatusDataModel(
        l.bcp47Tag,
        tu.content,
        tu.comment,
        ttuv.content,
        ttuv.comment,
        ttuv.status,
        r.name,
        b.name,
        tu.id,
        ttuv.id,
        tucv.id,
        ttuv.createdDate
      )
      FROM Branch b
      INNER JOIN b.repository r
      INNER JOIN r.repositoryLocales rl
      INNER JOIN rl.locale l
      LEFT JOIN b.branchStatistic bs
      LEFT JOIN bs.branchTextUnitStatistics btus
      LEFT JOIN btus.tmTextUnit tu
      LEFT JOIN TMTextUnitCurrentVariant tucv ON tucv.tmTextUnit = tu AND tucv.locale = l
      LEFT JOIN tucv.tm tm
      LEFT JOIN tucv.tmTextUnitVariant ttuv
      WHERE b.id = :branchId
      ORDER BY tu.content ASC, l.bcp47Tag ASC
      """)
  List<BranchTextUnitStatusDataModel> findBranchWithTextUnitStatuses(
      @Param("branchId") Long branchId);
}
