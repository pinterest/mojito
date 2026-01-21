package com.box.l10n.mojito.service.branch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.entity.TMTextUnitVariant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BranchTextUnitStatusDTOTest {

  private final ZonedDateTime zonedDateTime = ZonedDateTime.now();

  @Test
  void constructorShouldThrowExceptionWhenInputListIsNull() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new BranchTextUnitStatusDTO(null));

    assertEquals("BranchTextUnitStatusDataModels is null or empty", exception.getMessage());
  }

  @Test
  void constructorShouldThrowExceptionWhenInputListIsEmpty() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new BranchTextUnitStatusDTO(Collections.emptyList()));

    assertEquals("BranchTextUnitStatusDataModels is null or empty", exception.getMessage());
  }

  @Test
  void constructorShouldExtractRepositoryAndBranchNameWhenValidInputProvided() {
    BranchTextUnitStatusDataModel mockDataModel = mock(BranchTextUnitStatusDataModel.class);
    when(mockDataModel.getRepositoryName()).thenReturn("test-repo");
    when(mockDataModel.getBranchName()).thenReturn("test-branch");

    BranchTextUnitStatusDTO dto = new BranchTextUnitStatusDTO(List.of(mockDataModel));

    assertEquals("test-repo", dto.getRepositoryName());
    assertEquals("test-branch", dto.getBranchName());
  }

  @Test
  void constructorShouldGroupByLocaleWhenSingleDataModelProvided() {
    BranchTextUnitStatusDataModel mockDataModel = mock(BranchTextUnitStatusDataModel.class);
    when(mockDataModel.getBcp47Tag()).thenReturn("en-US");
    when(mockDataModel.getStatus()).thenReturn(TMTextUnitVariant.Status.APPROVED);
    when(mockDataModel.getContent()).thenReturn("test content");
    when(mockDataModel.getComment()).thenReturn("test comment");

    BranchTextUnitStatusDTO dto = new BranchTextUnitStatusDTO(List.of(mockDataModel));

    Map<String, List<TextUnitStatusDTO>> localeTextUnitStatus = dto.getLocaleTextUnitStatus();
    assertNotNull(localeTextUnitStatus);
    assertEquals(1, localeTextUnitStatus.size());
    assertEquals(1, localeTextUnitStatus.get("en-US").size());
  }

  @Test
  void constructorShouldGroupMultipleTextUnitsWhenSameLocaleProvided() {
    BranchTextUnitStatusDataModel mockDataModel1 = mock(BranchTextUnitStatusDataModel.class);
    when(mockDataModel1.getStatus()).thenReturn(TMTextUnitVariant.Status.APPROVED);
    when(mockDataModel1.getBcp47Tag()).thenReturn("en-US");
    when(mockDataModel1.getContent()).thenReturn("test content 1");
    when(mockDataModel1.getComment()).thenReturn("test comment 1");

    BranchTextUnitStatusDataModel mockDataModel2 = mock(BranchTextUnitStatusDataModel.class);
    when(mockDataModel2.getBcp47Tag()).thenReturn("en-US");
    when(mockDataModel2.getStatus()).thenReturn(TMTextUnitVariant.Status.MT_REVIEW_NEEDED);
    when(mockDataModel2.getContent()).thenReturn("test content 2");
    when(mockDataModel2.getComment()).thenReturn("test comment 2");

    BranchTextUnitStatusDTO dto =
        new BranchTextUnitStatusDTO(Arrays.asList(mockDataModel1, mockDataModel2));

    Map<String, List<TextUnitStatusDTO>> localeTextUnitStatus = dto.getLocaleTextUnitStatus();
    assertNotNull(localeTextUnitStatus);
    assertEquals(1, localeTextUnitStatus.size());
    assertEquals(2, localeTextUnitStatus.get("en-US").size());
  }

  @Test
  void constructorShouldGroupByDifferentLocalesWhenMultipleLocalesProvided() {
    BranchTextUnitStatusDataModel mockDataModel1 = mock(BranchTextUnitStatusDataModel.class);
    when(mockDataModel1.getBcp47Tag()).thenReturn("en-US");
    when(mockDataModel1.getStatus()).thenReturn(TMTextUnitVariant.Status.APPROVED);
    when(mockDataModel1.getContent()).thenReturn("test content en");
    when(mockDataModel1.getComment()).thenReturn("test comment en");

    BranchTextUnitStatusDataModel mockDataModel2 = mock(BranchTextUnitStatusDataModel.class);
    when(mockDataModel2.getBcp47Tag()).thenReturn("fr-FR");
    when(mockDataModel2.getStatus()).thenReturn(TMTextUnitVariant.Status.MT_REVIEW_NEEDED);
    when(mockDataModel2.getContent()).thenReturn("test content fr");
    when(mockDataModel2.getComment()).thenReturn("test comment fr");

    BranchTextUnitStatusDTO dto =
        new BranchTextUnitStatusDTO(Arrays.asList(mockDataModel1, mockDataModel2));

    Map<String, List<TextUnitStatusDTO>> localeTextUnitStatus = dto.getLocaleTextUnitStatus();
    assertNotNull(localeTextUnitStatus);
    assertEquals(2, localeTextUnitStatus.size());
    assertEquals(1, localeTextUnitStatus.get("en-US").size());
    assertEquals(1, localeTextUnitStatus.get("fr-FR").size());
  }

  @Test
  void constructorShouldCreateTextUnitStatusDtoWithCorrectValuesWhenValidInputProvided() {
    BranchTextUnitStatusDataModel mockDataModel = mock(BranchTextUnitStatusDataModel.class);
    when(mockDataModel.getBcp47Tag()).thenReturn("en-US");
    when(mockDataModel.getTextUnitId()).thenReturn(1L);
    when(mockDataModel.getVariantId()).thenReturn(2L);
    when(mockDataModel.getCurrentVariantId()).thenReturn(3L);
    when(mockDataModel.getCreatedDate()).thenReturn(zonedDateTime);
    when(mockDataModel.getStatus()).thenReturn(TMTextUnitVariant.Status.APPROVED);
    when(mockDataModel.getContent()).thenReturn("test content");
    when(mockDataModel.getComment()).thenReturn("test comment");

    BranchTextUnitStatusDTO dto = new BranchTextUnitStatusDTO(List.of(mockDataModel));

    Map<String, List<TextUnitStatusDTO>> localeTextUnitStatus = dto.getLocaleTextUnitStatus();
    TextUnitStatusDTO textUnitStatus = localeTextUnitStatus.get("en-US").getFirst();

    assertEquals(1L, textUnitStatus.textUnitId());
    assertEquals(2L, textUnitStatus.variantId());
    assertEquals(3L, textUnitStatus.currentVariantId());
    assertEquals(zonedDateTime, textUnitStatus.modifiedDate());
    assertEquals(TMTextUnitVariant.Status.APPROVED, textUnitStatus.status());
    assertEquals("test content", textUnitStatus.content());
    assertEquals("test comment", textUnitStatus.comment());
  }
}
