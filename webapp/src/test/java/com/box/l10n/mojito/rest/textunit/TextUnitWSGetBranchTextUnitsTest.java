package com.box.l10n.mojito.rest.textunit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.service.tm.BranchTextUnitVariantDTO;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class TextUnitWSGetBranchTextUnitsTest {

  @Mock private TMTextUnitRepository tmTextUnitRepository;

  @Mock private Logger logger;

  @InjectMocks private TextUnitWS textUnitWS;

  private static final String TEST_BRANCH_NAME = "feature/test-branch";
  private static final Long TEXT_UNIT_ID_1 = 100L;
  private static final Long TEXT_UNIT_ID_2 = 200L;

  @BeforeEach
  void setUp() {
    // Mock the static logger field
    TextUnitWS.logger = logger;
  }

  @Test
  void getTextUnitsNotYetFullyTranslated_EmptyResult_ReturnsEmptyList() {
    // Given
    when(tmTextUnitRepository.getBranchTextUnits(TEST_BRANCH_NAME)).thenReturn(new ArrayList<>());

    // When
    List<BranchTextUnitVariantStatusDTO> result =
        textUnitWS.getTextUnitsNotYetFullyTranslated(TEST_BRANCH_NAME);

    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(tmTextUnitRepository).getBranchTextUnits(TEST_BRANCH_NAME);
    verify(logger)
        .debug("Getting text units with non-approved variants for branch: {}", TEST_BRANCH_NAME);
  }

  @Test
  void
      getTextUnitsWithNonApprovedVariantsByBranch_SingleTextUnitSingleVariant_ReturnsCorrectStructure() {
    // Given
    BranchTextUnitVariantDTO mockResult =
        createMockBranchTextUnitVariantDTO(
            TEXT_UNIT_ID_1,
            "welcome.message",
            "Welcome",
            "Main welcome message",
            301L,
            "Bienvenue",
            TMTextUnitVariant.Status.REVIEW_NEEDED,
            "fr-FR");

    when(tmTextUnitRepository.getBranchTextUnits(TEST_BRANCH_NAME)).thenReturn(List.of(mockResult));

    // When
    List<BranchTextUnitVariantStatusDTO> result =
        textUnitWS.getTextUnitsNotYetFullyTranslated(TEST_BRANCH_NAME);

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());

    BranchTextUnitVariantStatusDTO textUnit = result.getFirst();
    assertEquals(TEXT_UNIT_ID_1, textUnit.getTmTextUnitId());
    assertEquals("welcome.message", textUnit.getName());
    assertEquals("Welcome", textUnit.getContent());
    assertEquals("Main welcome message", textUnit.getComment());
    assertEquals(TEST_BRANCH_NAME, textUnit.getBranchName());

    assertEquals(1, textUnit.getRemainingVariants().size());
    BranchTextUnitVariantStatusDTO.RemainingVariantDTO variant =
        textUnit.getRemainingVariants().getFirst();
    assertEquals(301L, variant.getVariantId());
    assertEquals("fr-FR", variant.getLocaleCode());
    assertEquals("Bienvenue", variant.getVariantContent());
    assertEquals(TMTextUnitVariant.Status.REVIEW_NEEDED, variant.getStatus());
  }

  @Test
  void
      getTextUnitsWithNonApprovedVariantsByBranch_SingleTextUnitMultipleVariants_AggregatesCorrectly() {
    // Given
    BranchTextUnitVariantDTO frenchVariant =
        createMockBranchTextUnitVariantDTO(
            TEXT_UNIT_ID_1,
            "welcome.message",
            "Welcome",
            "Main welcome message",
            301L,
            "Bienvenue",
            TMTextUnitVariant.Status.REVIEW_NEEDED,
            "fr-FR");

    BranchTextUnitVariantDTO spanishVariant =
        createMockBranchTextUnitVariantDTO(
            TEXT_UNIT_ID_1,
            "welcome.message",
            "Welcome",
            "Main welcome message",
            302L,
            "Bienvenido",
            TMTextUnitVariant.Status.TRANSLATION_NEEDED,
            "es-ES");

    when(tmTextUnitRepository.getBranchTextUnits(TEST_BRANCH_NAME))
        .thenReturn(List.of(frenchVariant, spanishVariant));

    // When
    List<BranchTextUnitVariantStatusDTO> result =
        textUnitWS.getTextUnitsNotYetFullyTranslated(TEST_BRANCH_NAME);

    // Then
    assertNotNull(result);
    assertEquals(1, result.size()); // Should be aggregated into one text unit

    BranchTextUnitVariantStatusDTO textUnit = result.getFirst();
    assertEquals(TEXT_UNIT_ID_1, textUnit.getTmTextUnitId());
    assertEquals(2, textUnit.getRemainingVariants().size()); // Should have 2 variants

    // Verify French variant
    BranchTextUnitVariantStatusDTO.RemainingVariantDTO frVariant =
        textUnit.getRemainingVariants().getFirst();
    assertEquals(301L, frVariant.getVariantId());
    assertEquals("fr-FR", frVariant.getLocaleCode());
    assertEquals(TMTextUnitVariant.Status.REVIEW_NEEDED, frVariant.getStatus());

    // Verify Spanish variant
    BranchTextUnitVariantStatusDTO.RemainingVariantDTO esVariant =
        textUnit.getRemainingVariants().get(1);
    assertEquals(302L, esVariant.getVariantId());
    assertEquals("es-ES", esVariant.getLocaleCode());
    assertEquals(TMTextUnitVariant.Status.TRANSLATION_NEEDED, esVariant.getStatus());
  }

  @Test
  void getTextUnitsWithNonApprovedVariantsByBranch_MultipleTextUnits_ReturnsCorrectStructure() {
    // Given
    BranchTextUnitVariantDTO textUnit1Variant =
        createMockBranchTextUnitVariantDTO(
            TEXT_UNIT_ID_1,
            "welcome.message",
            "Welcome",
            "Main welcome message",
            301L,
            "Bienvenue",
            TMTextUnitVariant.Status.REVIEW_NEEDED,
            "fr-FR");

    BranchTextUnitVariantDTO textUnit2Variant =
        createMockBranchTextUnitVariantDTO(
            TEXT_UNIT_ID_2,
            "goodbye.message",
            "Goodbye",
            "Farewell message",
            401L,
            "Au revoir",
            TMTextUnitVariant.Status.MT_TRANSLATED,
            "fr-FR");

    when(tmTextUnitRepository.getBranchTextUnits(TEST_BRANCH_NAME))
        .thenReturn(List.of(textUnit1Variant, textUnit2Variant));

    // When
    List<BranchTextUnitVariantStatusDTO> result =
        textUnitWS.getTextUnitsNotYetFullyTranslated(TEST_BRANCH_NAME);

    // Then
    assertNotNull(result);
    assertEquals(2, result.size());

    // Verify first text unit
    BranchTextUnitVariantStatusDTO firstTextUnit = result.getFirst();
    assertEquals(TEXT_UNIT_ID_1, firstTextUnit.getTmTextUnitId());
    assertEquals("welcome.message", firstTextUnit.getName());
    assertEquals(1, firstTextUnit.getRemainingVariants().size());

    // Verify second text unit
    BranchTextUnitVariantStatusDTO secondTextUnit = result.get(1);
    assertEquals(TEXT_UNIT_ID_2, secondTextUnit.getTmTextUnitId());
    assertEquals("goodbye.message", secondTextUnit.getName());
    assertEquals(1, secondTextUnit.getRemainingVariants().size());
    assertEquals(401L, secondTextUnit.getRemainingVariants().getFirst().getVariantId());
    assertEquals("fr-FR", secondTextUnit.getRemainingVariants().getFirst().getLocaleCode());
    assertEquals(
        TMTextUnitVariant.Status.MT_TRANSLATED,
        secondTextUnit.getRemainingVariants().getFirst().getStatus());
  }

  @Test
  void getTextUnitsNotYetFullyTranslated_DifferentVariantStatuses_HandlesAllCorrectly() {
    // Given
    List<BranchTextUnitVariantDTO> mockResults =
        List.of(
            createMockBranchTextUnitVariantDTO(
                TEXT_UNIT_ID_1,
                "test.message",
                "Test",
                null,
                501L,
                "Test FR",
                TMTextUnitVariant.Status.TRANSLATION_NEEDED,
                "fr-FR"),
            createMockBranchTextUnitVariantDTO(
                TEXT_UNIT_ID_1,
                "test.message",
                "Test",
                null,
                502L,
                "Test ES",
                TMTextUnitVariant.Status.REVIEW_NEEDED,
                "es-ES"),
            createMockBranchTextUnitVariantDTO(
                TEXT_UNIT_ID_1,
                "test.message",
                "Test",
                null,
                503L,
                "Test DE",
                TMTextUnitVariant.Status.MT_TRANSLATED,
                "de-DE"),
            createMockBranchTextUnitVariantDTO(
                TEXT_UNIT_ID_1,
                "test.message",
                "Test",
                null,
                504L,
                "Test IT",
                TMTextUnitVariant.Status.MT_REVIEW_NEEDED,
                "it-IT"));

    when(tmTextUnitRepository.getBranchTextUnits(TEST_BRANCH_NAME)).thenReturn(mockResults);

    // When
    List<BranchTextUnitVariantStatusDTO> result =
        textUnitWS.getTextUnitsNotYetFullyTranslated(TEST_BRANCH_NAME);

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());

    BranchTextUnitVariantStatusDTO textUnit = result.getFirst();
    assertEquals(4, textUnit.getRemainingVariants().size());

    // Verify each variant has the correct status
    List<TMTextUnitVariant.Status> expectedStatuses =
        List.of(
            TMTextUnitVariant.Status.TRANSLATION_NEEDED,
            TMTextUnitVariant.Status.REVIEW_NEEDED,
            TMTextUnitVariant.Status.MT_TRANSLATED,
            TMTextUnitVariant.Status.MT_REVIEW_NEEDED);

    for (int i = 0; i < textUnit.getRemainingVariants().size(); i++) {
      assertEquals(expectedStatuses.get(i), textUnit.getRemainingVariants().get(i).getStatus());
    }
  }

  @Test
  void getTextUnitsNotYetFullyTranslated_NullComments_HandlesCorrectly() {
    // Given
    BranchTextUnitVariantDTO mockResult =
        createMockBranchTextUnitVariantDTO(
            TEXT_UNIT_ID_1,
            "test.message",
            "Test",
            null,
            301L,
            "Test Translation",
            TMTextUnitVariant.Status.REVIEW_NEEDED,
            "fr-FR");

    when(tmTextUnitRepository.getBranchTextUnits(TEST_BRANCH_NAME)).thenReturn(List.of(mockResult));

    // When
    List<BranchTextUnitVariantStatusDTO> result =
        textUnitWS.getTextUnitsNotYetFullyTranslated(TEST_BRANCH_NAME);

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());

    BranchTextUnitVariantStatusDTO textUnit = result.getFirst();
    assertNull(textUnit.getComment());
    assertEquals(1, textUnit.getRemainingVariants().size());
  }

  @Test
  void getTextUnitsNotYetFullyTranslated_VerifyRepositoryCall() {
    // Given
    String customBranchName = "custom/branch-name";
    when(tmTextUnitRepository.getBranchTextUnits(customBranchName)).thenReturn(new ArrayList<>());

    // When
    textUnitWS.getTextUnitsNotYetFullyTranslated(customBranchName);

    // Then
    verify(tmTextUnitRepository, times(1)).getBranchTextUnits(customBranchName);
    verify(logger)
        .debug("Getting text units with non-approved variants for branch: {}", customBranchName);
  }

  /** Helper method to create mock BranchTextUnitVariantDTO objects for testing. */
  private BranchTextUnitVariantDTO createMockBranchTextUnitVariantDTO(
      Long textUnitId,
      String textUnitName,
      String textUnitContent,
      String textUnitComment,
      Long variantId,
      String variantContent,
      TMTextUnitVariant.Status variantStatus,
      String localeCode) {

    BranchTextUnitVariantDTO dto = mock(BranchTextUnitVariantDTO.class, withSettings().lenient());
    when(dto.getTextUnitId()).thenReturn(textUnitId);
    when(dto.getTextUnitName()).thenReturn(textUnitName);
    when(dto.getTextUnitContent()).thenReturn(textUnitContent);
    when(dto.getTextUnitComment()).thenReturn(textUnitComment);
    when(dto.getBranchName()).thenReturn(TEST_BRANCH_NAME);
    when(dto.getVariantId()).thenReturn(variantId);
    when(dto.getVariantContent()).thenReturn(variantContent);
    when(dto.getVariantStatus()).thenReturn(variantStatus);
    when(dto.getLocaleCode()).thenReturn(localeCode);
    return dto;
  }
}
