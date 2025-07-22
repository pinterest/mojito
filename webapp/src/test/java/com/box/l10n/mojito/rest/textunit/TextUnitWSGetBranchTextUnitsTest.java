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
  void getBranchTranslationStatus_EmptyResult_ReturnsEmptyStatus() {
    // Given
    when(tmTextUnitRepository.getBranchTextUnits(TEST_BRANCH_NAME)).thenReturn(new ArrayList<>());

    // When
    BranchTranslationStatusDTO result = textUnitWS.getBranchTranslationStatus(TEST_BRANCH_NAME);

    // Then
    assertNotNull(result);
    assertEquals(TEST_BRANCH_NAME, result.getBranchName());
    assertNotNull(result.getStatistics());
    assertEquals(0, result.getStatistics().getTotalApprovedTranslations());
    assertEquals(0, result.getStatistics().getTotalInProgressTranslations());
    assertEquals(0, result.getStatistics().getTotalTextUnits());
    assertEquals(0, result.getStatistics().getTotalConfiguredLocales());
    assertEquals(0.0, result.getStatistics().getCompletionPercentage());

    assertNotNull(result.getTextUnits());
    assertTrue(result.getTextUnits().isEmpty());

    verify(tmTextUnitRepository).getBranchTextUnits(TEST_BRANCH_NAME);
    verify(logger)
        .debug("Getting comprehensive translation status for branch: {}", TEST_BRANCH_NAME);
  }

  @Test
  void getBranchTranslationStatus_SingleTextUnitWithApprovedVariant_ReturnsCorrectStructure() {
    // Given
    BranchTextUnitVariantDTO mockResult =
        createMockBranchTextUnitVariantDTO(
            TEXT_UNIT_ID_1,
            "welcome.message",
            "Welcome",
            "Main welcome message",
            301L,
            "Bienvenue",
            TMTextUnitVariant.Status.APPROVED,
            "fr-FR",
            false);

    when(tmTextUnitRepository.getBranchTextUnits(TEST_BRANCH_NAME)).thenReturn(List.of(mockResult));

    // When
    BranchTranslationStatusDTO result = textUnitWS.getBranchTranslationStatus(TEST_BRANCH_NAME);

    // Then
    assertNotNull(result);
    assertEquals(TEST_BRANCH_NAME, result.getBranchName());

    // Check statistics
    assertEquals(1, result.getStatistics().getTotalApprovedTranslations());
    assertEquals(0, result.getStatistics().getTotalInProgressTranslations());
    assertEquals(1, result.getStatistics().getTotalTextUnits());
    assertEquals(1, result.getStatistics().getTotalConfiguredLocales());
    assertEquals(100.0, result.getStatistics().getCompletionPercentage());

    // Check text units
    assertEquals(1, result.getTextUnits().size());
    BranchTranslationStatusDTO.TextUnitTranslationStatusDTO textUnit =
        result.getTextUnits().getFirst();
    assertEquals(TEXT_UNIT_ID_1, textUnit.getTmTextUnitId());
    assertEquals("welcome.message", textUnit.getName());
    assertEquals("Welcome", textUnit.getContent());
    assertEquals("Main welcome message", textUnit.getComment());

    assertEquals(1, textUnit.getApprovedVariants().size());
    assertEquals(0, textUnit.getRemainingVariants().size());
    assertEquals(0, textUnit.getMissingVariants().size());

    BranchTranslationStatusDTO.VariantDTO approvedVariant =
        textUnit.getApprovedVariants().getFirst();
    assertEquals(301L, approvedVariant.getVariantId());
    assertEquals("fr-FR", approvedVariant.getLocaleCode());
    assertEquals("Bienvenue", approvedVariant.getVariantContent());
    assertEquals(TMTextUnitVariant.Status.APPROVED, approvedVariant.getStatus());
  }

  @Test
  void getBranchTranslationStatus_SingleTextUnitWithUnapprovedVariant_ReturnsCorrectStructure() {
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
            "fr-FR",
            false);

    when(tmTextUnitRepository.getBranchTextUnits(TEST_BRANCH_NAME)).thenReturn(List.of(mockResult));

    // When
    BranchTranslationStatusDTO result = textUnitWS.getBranchTranslationStatus(TEST_BRANCH_NAME);

    // Then
    assertNotNull(result);
    assertEquals(TEST_BRANCH_NAME, result.getBranchName());

    // Check statistics
    assertEquals(0, result.getStatistics().getTotalApprovedTranslations());
    assertEquals(1, result.getStatistics().getTotalInProgressTranslations());
    assertEquals(1, result.getStatistics().getTotalTextUnits());
    assertEquals(1, result.getStatistics().getTotalConfiguredLocales());
    assertEquals(0.0, result.getStatistics().getCompletionPercentage());

    // Check text units
    assertEquals(1, result.getTextUnits().size());
    BranchTranslationStatusDTO.TextUnitTranslationStatusDTO textUnit =
        result.getTextUnits().getFirst();

    assertEquals(0, textUnit.getApprovedVariants().size());
    assertEquals(1, textUnit.getRemainingVariants().size());
    assertEquals(0, textUnit.getMissingVariants().size());

    BranchTranslationStatusDTO.VariantDTO unapprovedVariant =
        textUnit.getRemainingVariants().getFirst();
    assertEquals(301L, unapprovedVariant.getVariantId());
    assertEquals("fr-FR", unapprovedVariant.getLocaleCode());
    assertEquals("Bienvenue", unapprovedVariant.getVariantContent());
    assertEquals(TMTextUnitVariant.Status.REVIEW_NEEDED, unapprovedVariant.getStatus());
  }

  @Test
  void getBranchTranslationStatus_SingleTextUnitWithMissingVariant_ReturnsCorrectStructure() {
    // Given
    BranchTextUnitVariantDTO mockResult =
        createMockBranchTextUnitVariantDTO(
            TEXT_UNIT_ID_1,
            "welcome.message",
            "Welcome",
            "Main welcome message",
            null,
            null,
            null,
            "fr-FR",
            true);

    when(tmTextUnitRepository.getBranchTextUnits(TEST_BRANCH_NAME)).thenReturn(List.of(mockResult));

    // When
    BranchTranslationStatusDTO result = textUnitWS.getBranchTranslationStatus(TEST_BRANCH_NAME);

    // Then
    assertNotNull(result);
    assertEquals(TEST_BRANCH_NAME, result.getBranchName());

    // Check statistics
    assertEquals(0, result.getStatistics().getTotalApprovedTranslations());
    assertEquals(1, result.getStatistics().getTotalInProgressTranslations());
    assertEquals(1, result.getStatistics().getTotalTextUnits());
    assertEquals(1, result.getStatistics().getTotalConfiguredLocales());
    assertEquals(0.0, result.getStatistics().getCompletionPercentage());

    // Check text units
    assertEquals(1, result.getTextUnits().size());
    BranchTranslationStatusDTO.TextUnitTranslationStatusDTO textUnit =
        result.getTextUnits().getFirst();

    assertEquals(0, textUnit.getApprovedVariants().size());
    assertEquals(0, textUnit.getRemainingVariants().size());
    assertEquals(1, textUnit.getMissingVariants().size());

    BranchTranslationStatusDTO.MissingVariantDTO missingVariant =
        textUnit.getMissingVariants().getFirst();
    assertEquals("fr-FR", missingVariant.getLocaleCode());
  }

  @Test
  void getBranchTranslationStatus_MultipleTextUnitsAndVariants_ReturnsCorrectAggregation() {
    // Given
    List<BranchTextUnitVariantDTO> mockResults =
        List.of(
            // Text unit 1: approved French, unapproved Spanish
            createMockBranchTextUnitVariantDTO(
                TEXT_UNIT_ID_1,
                "welcome.message",
                "Welcome",
                "Main welcome message",
                301L,
                "Bienvenue",
                TMTextUnitVariant.Status.APPROVED,
                "fr-FR",
                false),
            createMockBranchTextUnitVariantDTO(
                TEXT_UNIT_ID_1,
                "welcome.message",
                "Welcome",
                "Main welcome message",
                302L,
                "Bienvenido",
                TMTextUnitVariant.Status.REVIEW_NEEDED,
                "es-ES",
                false),
            // Text unit 2: missing French, approved Spanish
            createMockBranchTextUnitVariantDTO(
                TEXT_UNIT_ID_2,
                "goodbye.message",
                "Goodbye",
                "Farewell message",
                null,
                null,
                null,
                "fr-FR",
                true),
            createMockBranchTextUnitVariantDTO(
                TEXT_UNIT_ID_2,
                "goodbye.message",
                "Goodbye",
                "Farewell message",
                401L,
                "Adiós",
                TMTextUnitVariant.Status.APPROVED,
                "es-ES",
                false));

    when(tmTextUnitRepository.getBranchTextUnits(TEST_BRANCH_NAME)).thenReturn(mockResults);

    // When
    BranchTranslationStatusDTO result = textUnitWS.getBranchTranslationStatus(TEST_BRANCH_NAME);

    // Then
    assertNotNull(result);
    assertEquals(TEST_BRANCH_NAME, result.getBranchName());

    // Check statistics: 2 approved, 2 in-progress (1 unapproved + 1 missing), 2 text units, 2
    // locales
    assertEquals(2, result.getStatistics().getTotalApprovedTranslations());
    assertEquals(2, result.getStatistics().getTotalInProgressTranslations());
    assertEquals(2, result.getStatistics().getTotalTextUnits());
    assertEquals(2, result.getStatistics().getTotalConfiguredLocales());
    assertEquals(4, result.getStatistics().getTotalExpectedTranslations());
    assertEquals(50.0, result.getStatistics().getCompletionPercentage());

    // Check text units
    assertEquals(2, result.getTextUnits().size());

    // Check first text unit (welcome.message)
    BranchTranslationStatusDTO.TextUnitTranslationStatusDTO textUnit1 =
        result.getTextUnits().get(0);
    assertEquals(TEXT_UNIT_ID_1, textUnit1.getTmTextUnitId());
    assertEquals(1, textUnit1.getApprovedVariants().size());
    assertEquals(1, textUnit1.getRemainingVariants().size());
    assertEquals(0, textUnit1.getMissingVariants().size());

    // Check second text unit (goodbye.message)
    BranchTranslationStatusDTO.TextUnitTranslationStatusDTO textUnit2 =
        result.getTextUnits().get(1);
    assertEquals(TEXT_UNIT_ID_2, textUnit2.getTmTextUnitId());
    assertEquals(1, textUnit2.getApprovedVariants().size());
    assertEquals(0, textUnit2.getRemainingVariants().size());
    assertEquals(1, textUnit2.getMissingVariants().size());
  }

  @Test
  void getBranchTranslationStatus_VerifyRepositoryCall() {
    // Given
    String customBranchName = "custom/branch-name";
    when(tmTextUnitRepository.getBranchTextUnits(customBranchName)).thenReturn(new ArrayList<>());

    // When
    textUnitWS.getBranchTranslationStatus(customBranchName);

    // Then
    verify(tmTextUnitRepository, times(1)).getBranchTextUnits(customBranchName);
    verify(logger)
        .debug("Getting comprehensive translation status for branch: {}", customBranchName);
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
      String localeCode,
      boolean isMissingVariant) {

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
    when(dto.isMissingVariant()).thenReturn(isMissingVariant);
    return dto;
  }
}
