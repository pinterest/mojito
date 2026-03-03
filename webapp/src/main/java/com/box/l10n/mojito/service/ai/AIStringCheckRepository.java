package com.box.l10n.mojito.service.ai;

import com.box.l10n.mojito.entity.AIStringCheck;
import java.time.ZonedDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

@RepositoryRestResource(exported = false)
public interface AIStringCheckRepository extends JpaRepository<AIStringCheck, Long> {
  @Modifying
  @Transactional
  @Query(
      """
      delete from #{#entityName} aisc
      where aisc.createdDate <= :beforeDate
      """)
  void deleteAllOlderThan(@Param("beforeDate") ZonedDateTime beforeDate);
}
