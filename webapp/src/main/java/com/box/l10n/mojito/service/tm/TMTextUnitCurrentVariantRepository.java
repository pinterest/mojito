package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface TMTextUnitCurrentVariantRepository
    extends JpaRepository<TMTextUnitCurrentVariant, Long> {

  @EntityGraph(value = "TMTextUnitCurrentVariant.legacy", type = EntityGraphType.FETCH)
  TMTextUnitCurrentVariant findByLocale_IdAndTmTextUnit_Id(Long localeId, Long tmTextUnitId);

  List<TMTextUnitCurrentVariant> findByTmTextUnit_Id(Long tmTextUnitId);

  List<TMTextUnitCurrentVariant> findByTmTextUnit_Tm_IdAndLocale_Id(Long tmId, Long localeId);

  @Query(
      """
      select new com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantDTO(ttucv.tmTextUnit.id, ttucv.tmTextUnitVariant.id)
      from #{#entityName} ttucv
      where ttucv.asset.id = ?1 and ttucv.locale.id = ?2
      """)
  List<TMTextUnitCurrentVariantDTO> findByAsset_idAndLocale_Id(Long assetId, Long localeId);

  @Query(
      """
      select l from TMTextUnitCurrentVariant ttucv
      join ttucv.locale l
      where ttucv.tmTextUnit.id = :tmTextUnitId
      """)
  Set<Locale> findLocalesWithVariantByTmTextUnit_Id(@Param("tmTextUnitId") Long tmTextUnitId);
}
