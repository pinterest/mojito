package com.box.l10n.mojito.service.tm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import com.box.l10n.mojito.entity.TMTUVIntegrityCheckFailure;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class IntegrityCheckFailureServiceTest extends ServiceTestBase {
  @Autowired TMTextUnitVariantRepository textUnitVariantRepository;

  @Autowired TMTUVIntegrityCheckFailureRepository integrityCheckFailureRepository;

  @Autowired TMTextUnitCurrentVariantRepository textUnitCurrentVariantRepository;

  @Mock IntegrityCheckFailureAlertService alertService;

  IntegrityCheckFailureService integrityCheckFailureService;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired TMService tmService;

  @Captor ArgumentCaptor<List<TMTUVIntegrityCheckFailure>> integrityCheckFailureArgumentCaptor;

  @Before
  public void before() {
    this.integrityCheckFailureService =
        new IntegrityCheckFailureService(
            this.textUnitVariantRepository,
            this.integrityCheckFailureRepository,
            this.textUnitCurrentVariantRepository,
            this.alertService);
  }

  @Transactional
  @Test
  public void testCleanUpAndAlertIntegrityCheckFailures() {
    TMTestData tmTestData = new TMTestData(this.testIdWatcher);
    Long textUnitId = tmTestData.addCurrentTMTextUnitVariant1FrFR.getTmTextUnit().getId();
    TMTextUnitCurrentVariant textUnitCurrentVariantFrFR =
        tmService.addTMTextUnitCurrentVariant(
            textUnitId,
            tmTestData.addCurrentTMTextUnitVariant1FrFR.getLocale().getId(),
            tmTestData.addCurrentTMTextUnitVariant1FrFR.getContent(),
            "this translation fails compilation",
            TMTextUnitVariant.Status.INTEGRITY_FAILURE,
            false);
    TMTextUnitVariant textUnitVariantFrFR = textUnitCurrentVariantFrFR.getTmTextUnitVariant();
    TMTUVIntegrityCheckFailure integrityCheckFailureFrFR = new TMTUVIntegrityCheckFailure();
    integrityCheckFailureFrFR.setTmTextUnit(textUnitVariantFrFR.getTmTextUnit());
    integrityCheckFailureFrFR.setLocale(textUnitVariantFrFR.getLocale());
    integrityCheckFailureFrFR.setTmTextUnitVariant(textUnitVariantFrFR);
    integrityCheckFailureFrFR.setIntegrityFailureName("Integrity Check 1");
    this.integrityCheckFailureRepository.save(integrityCheckFailureFrFR);
    textUnitId = tmTestData.addCurrentTMTextUnitVariant1KoKR.getTmTextUnit().getId();
    TMTextUnitCurrentVariant textUnitCurrentVariantKoKR =
        tmService.addTMTextUnitCurrentVariant(
            textUnitId,
            tmTestData.addCurrentTMTextUnitVariant1KoKR.getLocale().getId(),
            tmTestData.addCurrentTMTextUnitVariant1KoKR.getContent(),
            "this translation fails compilation",
            TMTextUnitVariant.Status.INTEGRITY_FAILURE,
            false);
    TMTextUnitVariant textUnitVariantKoKR = textUnitCurrentVariantKoKR.getTmTextUnitVariant();
    textUnitVariantKoKR.setStatus(TMTextUnitVariant.Status.INTEGRITY_FAILURE);
    this.textUnitVariantRepository.save(textUnitVariantKoKR);
    TMTUVIntegrityCheckFailure integrityCheckFailureKoKR = new TMTUVIntegrityCheckFailure();
    integrityCheckFailureKoKR.setTmTextUnit(textUnitVariantKoKR.getTmTextUnit());
    integrityCheckFailureKoKR.setLocale(textUnitVariantKoKR.getLocale());
    integrityCheckFailureKoKR.setTmTextUnitVariant(textUnitVariantKoKR);
    integrityCheckFailureKoKR.setIntegrityFailureName("Integrity Check 2");
    this.integrityCheckFailureRepository.save(integrityCheckFailureKoKR);

    List<TMTUVIntegrityCheckFailure> integrityCheckFailures =
        this.integrityCheckFailureRepository.findAll();
    assertEquals(2, integrityCheckFailures.size());

    tmService.addTMTextUnitCurrentVariant(
        textUnitId,
        textUnitVariantKoKR.getLocale().getId(),
        textUnitVariantKoKR.getContent(),
        "this translation fails compilation",
        TMTextUnitVariant.Status.APPROVED,
        true);

    this.integrityCheckFailureService.cleanUpAndAlertIntegrityCheckFailures();

    integrityCheckFailures = this.integrityCheckFailureRepository.findAll();
    assertEquals(1, integrityCheckFailures.size());
    verify(this.alertService).sendAlert(this.integrityCheckFailureArgumentCaptor.capture());
    this.integrityCheckFailureArgumentCaptor
        .getValue()
        .forEach(
            currentIntegrityCheckFailure -> {
              assertEquals(
                  textUnitVariantFrFR.getTmTextUnit().getId(),
                  currentIntegrityCheckFailure.getTmTextUnit().getId());
              assertEquals(
                  textUnitVariantFrFR.getLocale().getId(),
                  currentIntegrityCheckFailure.getLocale().getId());
              assertEquals(
                  textUnitVariantFrFR.getId(),
                  currentIntegrityCheckFailure.getTmTextUnitVariant().getId());
              assertEquals(
                  "Integrity Check 1", currentIntegrityCheckFailure.getIntegrityFailureName());
            });
  }
}
