package com.box.l10n.mojito.service.ai.translation;

import static org.assertj.core.api.Assertions.assertThat;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantCommentRepository;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class AITranslationServiceIntegrationTest extends ServiceTestBase {

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired TMService tmService;
  @Autowired RepositoryService repositoryService;
  @Autowired AssetService assetService;
  @Autowired LocaleService localeService;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired TMTextUnitVariantCommentRepository tmTextUnitVariantCommentRepository;

  AITranslationService aiTranslationService;

  @Before
  public void setUp() {
    aiTranslationService = new AITranslationService();
    aiTranslationService.jdbcTemplate = jdbcTemplate;
    aiTranslationService.batchSize = 1000;
  }

  @Test
  public void testAddVariantCommentsResolvesCorrectVariantId() throws Exception {
    Repository repository =
        repositoryService.createRepository(testIdWatcher.getEntityName("variantCommentRepo"));
    Locale frCA = localeService.findByBcp47Tag("fr-CA");
    repositoryService.addRepositoryLocale(repository, frCA.getBcp47Tag());

    Asset asset =
        assetService.createAssetWithContent(repository.getId(), "test_asset", "test content");
    TMTextUnit setupTextUnit =
        tmService.addTMTextUnit(
            repository.getTm().getId(),
            asset.getId(),
            "setup_name",
            "setup content",
            "setup comment");

    // Advance the variant auto-increment ahead of current_variant by updating
    // the same translation multiple times (each update creates a new variant row
    // but reuses the same current_variant row).
    tmService.addTMTextUnitCurrentVariant(
        setupTextUnit.getId(), frCA.getId(), "v1", null, TMTextUnitVariant.Status.APPROVED);
    tmService.addTMTextUnitCurrentVariant(
        setupTextUnit.getId(), frCA.getId(), "v2", null, TMTextUnitVariant.Status.APPROVED);
    tmService.addTMTextUnitCurrentVariant(
        setupTextUnit.getId(), frCA.getId(), "v3", null, TMTextUnitVariant.Status.APPROVED);

    TMTextUnit textUnit =
        tmService.addTMTextUnit(
            repository.getTm().getId(), asset.getId(), "test_name", "test content", "test comment");

    TMTextUnitCurrentVariant currentVariant =
        tmService.addTMTextUnitCurrentVariant(
            textUnit.getId(),
            frCA.getId(),
            "traduction test",
            "comment",
            TMTextUnitVariant.Status.MT_TRANSLATED);

    Long currentVariantId = currentVariant.getId();
    Long actualVariantId = currentVariant.getTmTextUnitVariant().getId();
    assertThat(currentVariantId)
        .as("current_variant.id and variant.id should differ to make this test meaningful")
        .isNotEqualTo(actualVariantId);

    aiTranslationService.updateVariantStatusToMTReviewNeeded(List.of(currentVariantId));

    List<TMTextUnitVariantComment> comments =
        tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(actualVariantId);
    assertThat(comments).hasSize(1);
    assertThat(comments.get(0).getType()).isEqualTo(TMTextUnitVariantComment.Type.AI_TRANSLATION);
    assertThat(comments.get(0).getContent()).isEqualTo("AI translation sent for human review");
    assertThat(comments.get(0).getSeverity()).isEqualTo(TMTextUnitVariantComment.Severity.INFO);

    List<TMTextUnitVariantComment> wrongComments =
        tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(currentVariantId);
    assertThat(wrongComments)
        .as("No comment should be linked to the current_variant id (which is not a variant id)")
        .isEmpty();
  }
}
