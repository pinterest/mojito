package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.Repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
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
          "SELECT b.*"
              + " FROM asset_text_unit_to_tm_text_unit atutu"
              + " JOIN asset_text_unit a ON a.id = atutu.asset_text_unit_id"
              + " JOIN branch b ON b.id = a.branch_id"
              + " WHERE atutu.tm_text_unit_id = :textUnitId AND b.name REGEXP :regex"
              + " ORDER BY b.created_date ASC LIMIT 1",
      nativeQuery = true)
  Branch findIntroducedInByTextUnitId(Long textUnitId, String regex);
}
