package com.box.l10n.mojito.service.evolve;

import com.box.l10n.mojito.entity.EvolveCoursePicture;
import com.box.l10n.mojito.service.evolve.dto.CourseDTO;
import com.google.common.base.Strings;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvolveCoursePictureService {

  private final EvolveCoursePictureRepository evolveCoursePictureRepository;

  public EvolveCoursePictureService(EvolveCoursePictureRepository evolveCoursePictureRepository) {
    this.evolveCoursePictureRepository = evolveCoursePictureRepository;
  }

  public Optional<EvolveCoursePicture> findByCourseIdAndLocaleBcp47Tag(
      int courseId, String locale) {
    return evolveCoursePictureRepository.findByCourseIdAndLocaleBcp47Tag(courseId, locale);
  }

  /**
   * Creates or updates {@link EvolveCoursePicture} records for the given parent course, using the
   * picture URLs from each localized course DTO. DTOs with no picture URLs are skipped. Existing
   * records are matched by locale and updated in place; unmatched locales produce new records.
   *
   * @param parentCourseId the ID of the parent (source-language) course
   * @param localizedCourseDTOs the localized course DTOs containing picture URL data
   */
  public void upsertLocalizedCoursePictureUrls(
      int parentCourseId, List<CourseDTO> localizedCourseDTOs) {
    List<EvolveCoursePicture> existingCoursePictures =
        evolveCoursePictureRepository.findByCourseId(parentCourseId);
    List<EvolveCoursePicture> evolveCoursePictures =
        localizedCourseDTOs.stream()
            .filter(
                localizedCourseDTO ->
                    !Strings.isNullOrEmpty(localizedCourseDTO.getPictureUrl())
                        || !Strings.isNullOrEmpty(localizedCourseDTO.getHeroPictureUrl())
                        || !Strings.isNullOrEmpty(localizedCourseDTO.getHeroPictureMobileUrl()))
            .map(
                localizedCourseDTO -> {
                  EvolveCoursePicture evolveCoursePicture =
                      existingCoursePictures.stream()
                          .filter(
                              existingCoursePicture ->
                                  existingCoursePicture
                                      .getLocaleBcp47Tag()
                                      .equals(localizedCourseDTO.getLocale()))
                          .findFirst()
                          .orElseGet(EvolveCoursePicture::new);

                  evolveCoursePicture.setCourseId(parentCourseId);
                  evolveCoursePicture.setLocaleBcp47Tag(localizedCourseDTO.getLocale());
                  evolveCoursePicture.setPictureUrl(localizedCourseDTO.getPictureUrl());
                  evolveCoursePicture.setHeroPictureUrl(localizedCourseDTO.getHeroPictureUrl());
                  evolveCoursePicture.setHeroPictureMobileUrl(
                      localizedCourseDTO.getHeroPictureMobileUrl());
                  return evolveCoursePicture;
                })
            .collect(Collectors.toList());
    this.evolveCoursePictureRepository.saveAll(evolveCoursePictures);
  }

  @Transactional
  public void deleteByCourseId(int courseId) {
    evolveCoursePictureRepository.deleteByCourseId(courseId);
  }
}
