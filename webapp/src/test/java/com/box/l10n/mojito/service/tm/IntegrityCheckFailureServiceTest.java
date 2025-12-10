package com.box.l10n.mojito.service.tm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import com.box.l10n.mojito.entity.TMTUVIntegrityCheckFailure;
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
    TMTextUnitVariant textUnitVariantFrFR = tmTestData.addCurrentTMTextUnitVariant1FrFR;
    textUnitVariantFrFR.setStatus(TMTextUnitVariant.Status.INTEGRITY_FAILURE);
    this.textUnitVariantRepository.save(textUnitVariantFrFR);
    TMTUVIntegrityCheckFailure integrityCheckFailureFrFR = new TMTUVIntegrityCheckFailure();
    integrityCheckFailureFrFR.setTmTextUnit(textUnitVariantFrFR.getTmTextUnit());
    integrityCheckFailureFrFR.setLocale(textUnitVariantFrFR.getLocale());
    integrityCheckFailureFrFR.setTmTextUnitVariant(textUnitVariantFrFR);
    integrityCheckFailureFrFR.setIntegrityFailureName("Integrity Check 1");
    this.integrityCheckFailureRepository.save(integrityCheckFailureFrFR);
    TMTextUnitVariant textUnitVariantKoKR = tmTestData.addCurrentTMTextUnitVariant1KoKR;
    textUnitVariantKoKR.setStatus(TMTextUnitVariant.Status.INTEGRITY_FAILURE);
    this.textUnitVariantRepository.save(textUnitVariantKoKR);
    TMTUVIntegrityCheckFailure integrityCheckFailureKoKR = new TMTUVIntegrityCheckFailure();
    integrityCheckFailureKoKR.setTmTextUnit(textUnitVariantKoKR.getTmTextUnit());
    integrityCheckFailureKoKR.setLocale(textUnitVariantKoKR.getLocale());
    integrityCheckFailureKoKR.setTmTextUnitVariant(textUnitVariantKoKR);
    integrityCheckFailureKoKR.setIntegrityFailureName("Integrity Check 2");
    this.integrityCheckFailureRepository.save(integrityCheckFailureKoKR);
    Long textUnitId = textUnitVariantFrFR.getTmTextUnit().getId();
    tmService.addTMTextUnitCurrentVariant(
        textUnitId,
        textUnitVariantFrFR.getLocale().getId(),
        textUnitVariantFrFR.getContent(),
        "this translation fails compilation",
        TMTextUnitVariant.Status.INTEGRITY_FAILURE,
        false);

    List<TMTUVIntegrityCheckFailure> integrityCheckFailures =
        this.integrityCheckFailureRepository.findAll();
    assertEquals(2, integrityCheckFailures.size());

    this.integrityCheckFailureService.cleanUpAndAlertIntegrityCheckFailures();

    integrityCheckFailures = this.integrityCheckFailureRepository.findAll();
    assertEquals(1, integrityCheckFailures.size());
    verify(this.alertService).sendAlert(this.integrityCheckFailureArgumentCaptor.capture());
    this.integrityCheckFailureArgumentCaptor
        .getValue()
        .forEach(
            currentIntegrityCheckFailure -> {
              assertEquals(
                  textUnitVariantKoKR.getTmTextUnit().getId(),
                  currentIntegrityCheckFailure.getTmTextUnit().getId());
              assertEquals(
                  textUnitVariantKoKR.getLocale().getId(),
                  currentIntegrityCheckFailure.getLocale().getId());
              assertEquals(
                  textUnitVariantKoKR.getId(),
                  currentIntegrityCheckFailure.getTmTextUnitVariant().getId());
              assertEquals(
                  "Integrity Check 2", currentIntegrityCheckFailure.getIntegrityFailureName());
            });
  }
}
