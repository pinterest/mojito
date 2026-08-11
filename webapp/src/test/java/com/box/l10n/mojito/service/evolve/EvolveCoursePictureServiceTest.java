package com.box.l10n.mojito.service.evolve;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.entity.EvolveCoursePicture;
import com.box.l10n.mojito.service.evolve.dto.CourseDTO;
import com.box.l10n.mojito.service.evolve.dto.PictureDTO;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {EvolveCoursePictureServiceTest.class})
public class EvolveCoursePictureServiceTest {

  @Mock EvolveCoursePictureRepository evolveCoursePictureRepository;

  EvolveCoursePictureService evolveCoursePictureService;

  @Captor ArgumentCaptor<List<EvolveCoursePicture>> savedPicturesCaptor;

  @Before
  public void setUp() {
    evolveCoursePictureService = new EvolveCoursePictureService(evolveCoursePictureRepository);
  }

  @Test
  public void testFindByCourseIdAndLocaleBcp47TagReturnsPicture() {
    EvolveCoursePicture picture = new EvolveCoursePicture();
    picture.setCourseId(1);
    picture.setLocaleBcp47Tag("fr");
    when(evolveCoursePictureRepository.findByCourseIdAndLocaleBcp47Tag(1, "fr"))
        .thenReturn(Optional.of(picture));

    Optional<EvolveCoursePicture> result =
        evolveCoursePictureService.findByCourseIdAndLocaleBcp47Tag(1, "fr");

    assertTrue(result.isPresent());
    assertEquals("fr", result.get().getLocaleBcp47Tag());
    assertEquals(1, result.get().getCourseId());
  }

  @Test
  public void testFindByCourseIdAndLocaleBcp47TagReturnsEmpty() {
    when(evolveCoursePictureRepository.findByCourseIdAndLocaleBcp47Tag(99, "de"))
        .thenReturn(Optional.empty());

    Optional<EvolveCoursePicture> result =
        evolveCoursePictureService.findByCourseIdAndLocaleBcp47Tag(99, "de");

    assertFalse(result.isPresent());
  }

  @Test
  public void testUpsertCreatesNewPictureWhenNoneExist() {
    when(evolveCoursePictureRepository.findByCourseId(10)).thenReturn(ImmutableList.of());

    CourseDTO dto = new CourseDTO();
    dto.setLocale("es");
    PictureDTO picture = new PictureDTO();
    picture.setTargetUrl("https://cdn.example.com/es.jpg");
    dto.setPicture(picture);

    evolveCoursePictureService.upsertLocalizedCoursePictureUrls(10, ImmutableList.of(dto));

    verify(evolveCoursePictureRepository).saveAll(savedPicturesCaptor.capture());
    List<EvolveCoursePicture> saved = savedPicturesCaptor.getValue();
    assertEquals(1, saved.size());
    assertEquals(10, saved.get(0).getCourseId());
    assertEquals("es", saved.get(0).getLocaleBcp47Tag());
    assertEquals("https://cdn.example.com/es.jpg", saved.get(0).getPictureUrl());
  }

  @Test
  public void testUpsertUpdatesExistingPicture() {
    EvolveCoursePicture existing = new EvolveCoursePicture();
    existing.setCourseId(10);
    existing.setLocaleBcp47Tag("es");
    existing.setPictureUrl("https://cdn.example.com/old.jpg");
    when(evolveCoursePictureRepository.findByCourseId(10)).thenReturn(ImmutableList.of(existing));

    CourseDTO dto = new CourseDTO();
    dto.setLocale("es");
    PictureDTO picture = new PictureDTO();
    picture.setTargetUrl("https://cdn.example.com/new.jpg");
    dto.setPicture(picture);

    evolveCoursePictureService.upsertLocalizedCoursePictureUrls(10, ImmutableList.of(dto));

    verify(evolveCoursePictureRepository).saveAll(savedPicturesCaptor.capture());
    List<EvolveCoursePicture> saved = savedPicturesCaptor.getValue();
    assertEquals(1, saved.size());
    assertEquals("https://cdn.example.com/new.jpg", saved.get(0).getPictureUrl());
  }

  @Test
  public void testUpsertSkipsCourseDTOsWithNoPictureUrls() {
    when(evolveCoursePictureRepository.findByCourseId(10)).thenReturn(ImmutableList.of());

    CourseDTO dto = new CourseDTO();
    dto.setLocale("es");

    evolveCoursePictureService.upsertLocalizedCoursePictureUrls(10, ImmutableList.of(dto));

    verify(evolveCoursePictureRepository).saveAll(savedPicturesCaptor.capture());
    assertTrue(savedPicturesCaptor.getValue().isEmpty());
  }

  @Test
  public void testUpsertIncludesCourseDTOWithHeroPictureUrl() {
    when(evolveCoursePictureRepository.findByCourseId(5)).thenReturn(ImmutableList.of());

    CourseDTO dto = new CourseDTO();
    dto.setLocale("fr");
    PictureDTO heroPicture = new PictureDTO();
    heroPicture.setTargetUrl("https://cdn.example.com/hero.jpg");
    dto.setHeroPicture(heroPicture);

    evolveCoursePictureService.upsertLocalizedCoursePictureUrls(5, ImmutableList.of(dto));

    verify(evolveCoursePictureRepository).saveAll(savedPicturesCaptor.capture());
    List<EvolveCoursePicture> saved = savedPicturesCaptor.getValue();
    assertEquals(1, saved.size());
    assertEquals("https://cdn.example.com/hero.jpg", saved.get(0).getHeroPictureUrl());
  }

  @Test
  public void testUpsertWithEmptyListSavesNothing() {
    when(evolveCoursePictureRepository.findByCourseId(7)).thenReturn(ImmutableList.of());

    evolveCoursePictureService.upsertLocalizedCoursePictureUrls(7, ImmutableList.of());

    verify(evolveCoursePictureRepository).saveAll(savedPicturesCaptor.capture());
    assertTrue(savedPicturesCaptor.getValue().isEmpty());
  }

  @Test
  public void testUpsertHandlesMultipleLocales() {
    when(evolveCoursePictureRepository.findByCourseId(3)).thenReturn(ImmutableList.of());

    CourseDTO dtoEs = new CourseDTO();
    dtoEs.setLocale("es");
    PictureDTO picEs = new PictureDTO();
    picEs.setTargetUrl("https://cdn.example.com/es.jpg");
    dtoEs.setPicture(picEs);

    CourseDTO dtoFr = new CourseDTO();
    dtoFr.setLocale("fr");
    PictureDTO picFr = new PictureDTO();
    picFr.setTargetUrl("https://cdn.example.com/fr.jpg");
    dtoFr.setPicture(picFr);

    evolveCoursePictureService.upsertLocalizedCoursePictureUrls(3, ImmutableList.of(dtoEs, dtoFr));

    verify(evolveCoursePictureRepository).saveAll(savedPicturesCaptor.capture());
    List<EvolveCoursePicture> saved = savedPicturesCaptor.getValue();
    assertEquals(2, saved.size());
  }

  @Test
  public void testDeleteByCourseId() {
    evolveCoursePictureService.deleteByCourseId(42);
    verify(evolveCoursePictureRepository).deleteByCourseId(42);
  }
}
