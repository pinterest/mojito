package com.box.l10n.mojito.service.security.user;

import com.box.l10n.mojito.entity.security.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author wyau
 */
@RepositoryRestResource(exported = false)
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

  @EntityGraph(value = "User.legacy", type = EntityGraphType.FETCH)
  User findByUsername(String username);

  @Override
  @EntityGraph(value = "User.legacy", type = EntityGraphType.FETCH)
  List<User> findAll(Specification<User> spec, Sort sort);

  @Override
  @EntityGraph(value = "User.legacy", type = EntityGraphType.FETCH)
  Optional<User> findById(Long aLong);
}
