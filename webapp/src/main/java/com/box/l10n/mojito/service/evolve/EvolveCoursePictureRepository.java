package com.box.l10n.mojito.service.evolve;

import com.box.l10n.mojito.entity.EvolveCoursePicture;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface EvolveCoursePictureRepository extends JpaRepository<EvolveCoursePicture, Long> {
  List<EvolveCoursePicture> findByCourseId(int courseId);

  Optional<EvolveCoursePicture> findByCourseIdAndLocaleBcp47Tag(
      int courseId, String localeBcp47Tag);

  void deleteByCourseId(int courseId);
}
