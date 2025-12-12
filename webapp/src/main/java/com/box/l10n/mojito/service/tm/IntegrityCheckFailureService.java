package com.box.l10n.mojito.service.tm;

import static java.util.Optional.ofNullable;

import com.box.l10n.mojito.entity.TMTUVIntegrityCheckFailure;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntegrityCheckFailureService {
  private final TMTextUnitVariantRepository textUnitVariantRepository;

  private final TMTUVIntegrityCheckFailureRepository integrityCheckFailureRepository;

  private final TMTextUnitCurrentVariantRepository textUnitCurrentVariantRepository;

  private final IntegrityCheckFailureAlertService alertService;

  public IntegrityCheckFailureService(
      TMTextUnitVariantRepository textUnitVariantRepository,
      TMTUVIntegrityCheckFailureRepository integrityCheckFailureRepository,
      TMTextUnitCurrentVariantRepository textUnitCurrentVariantRepository,
      IntegrityCheckFailureAlertService alertService) {
    this.textUnitVariantRepository = textUnitVariantRepository;
    this.integrityCheckFailureRepository = integrityCheckFailureRepository;
    this.textUnitCurrentVariantRepository = textUnitCurrentVariantRepository;
    this.alertService = alertService;
  }

  @Transactional
  private void removeIntegrityCheckFailures(
      List<TMTUVIntegrityCheckFailure> existingIntegrityCheckFailures,
      List<TMTUVIntegrityCheckFailure> newIntegrityCheckFailures) {
    List<TMTUVIntegrityCheckFailure> integrityCheckFailuresToDelete = new ArrayList<>();
    existingIntegrityCheckFailures.forEach(
        existingIntegrityCheckFailure -> {
          if (!newIntegrityCheckFailures.contains(existingIntegrityCheckFailure)) {
            integrityCheckFailuresToDelete.add(existingIntegrityCheckFailure);
          }
        });
    this.integrityCheckFailureRepository.deleteAll(integrityCheckFailuresToDelete);
  }

  @Transactional
  private List<TMTUVIntegrityCheckFailure> updateIntegrityCheckFailures(
      List<TMTUVIntegrityCheckFailure> existingIntegrityCheckFailures,
      List<TMTUVIntegrityCheckFailure> newIntegrityCheckFailures) {
    List<TMTUVIntegrityCheckFailure> integrityCheckFailuresToUpdate = new ArrayList<>();
    newIntegrityCheckFailures.forEach(
        newIntegrityCheckFailure -> {
          int index = existingIntegrityCheckFailures.indexOf(newIntegrityCheckFailure);
          if (index != -1) {
            TMTUVIntegrityCheckFailure existingIntegrityCheckFailure =
                existingIntegrityCheckFailures.get(index);
            existingIntegrityCheckFailure.setIntegrityFailureName(
                newIntegrityCheckFailure.getIntegrityFailureName());
            existingIntegrityCheckFailure.setTmTextUnitVariant(
                this.textUnitVariantRepository.getReferenceById(
                    newIntegrityCheckFailure.getTmTextUnitVariant_Id()));
            integrityCheckFailuresToUpdate.add(existingIntegrityCheckFailure);
          }
        });
    this.integrityCheckFailureRepository.saveAll(integrityCheckFailuresToUpdate);
    return integrityCheckFailuresToUpdate;
  }

  @Transactional
  private void addIntegrityCheckFailures(
      List<TMTUVIntegrityCheckFailure> updatedIntegrityCheckFailures,
      List<TMTUVIntegrityCheckFailure> newIntegrityCheckFailures) {
    List<TMTUVIntegrityCheckFailure> integrityCheckFailuresToAdd = new ArrayList<>();
    newIntegrityCheckFailures.forEach(
        newIntegrityCheckFailure -> {
          if (!updatedIntegrityCheckFailures.contains(newIntegrityCheckFailure)) {
            integrityCheckFailuresToAdd.add(newIntegrityCheckFailure);
          }
        });
    this.integrityCheckFailureRepository.saveAll(integrityCheckFailuresToAdd);
  }

  @Transactional
  public void updateIntegrityCheckFailures(
      Long tmTextUnitId,
      Long localeId,
      List<TMTUVIntegrityCheckFailure> newIntegrityCheckFailures,
      Long tmTextUnitVariantId) {
    List<TMTUVIntegrityCheckFailure> existingIntegrityCheckFailures =
        this.integrityCheckFailureRepository.findByTmTextUnitIdAndLocaleId(tmTextUnitId, localeId);
    this.removeIntegrityCheckFailures(existingIntegrityCheckFailures, newIntegrityCheckFailures);
    TMTextUnitVariant textUnitVariant =
        this.textUnitVariantRepository.findById(tmTextUnitVariantId).orElse(null);
    newIntegrityCheckFailures.forEach(
        newIntegrityCheckFailure -> newIntegrityCheckFailure.setTmTextUnitVariant(textUnitVariant));
    List<TMTUVIntegrityCheckFailure> updatedIntegrityCheckFailures =
        this.updateIntegrityCheckFailures(
            existingIntegrityCheckFailures, newIntegrityCheckFailures);
    this.addIntegrityCheckFailures(updatedIntegrityCheckFailures, newIntegrityCheckFailures);
  }

  private boolean shouldIntegrityCheckFailureBeDeleted(
      TMTUVIntegrityCheckFailure integrityCheckFailure,
      Optional<TMTextUnitVariant> textUnitVariantOptional) {
    if (textUnitVariantOptional.isPresent()) {
      TMTextUnitVariant textUnitVariant = textUnitVariantOptional.get();
      return !textUnitVariant.getId().equals(integrityCheckFailure.getTmTextUnitVariant_Id())
          || textUnitVariant.getStatus() != TMTextUnitVariant.Status.INTEGRITY_FAILURE;
    }
    return true;
  }

  @Transactional
  public void cleanUpAndAlertIntegrityCheckFailures() {
    List<TMTUVIntegrityCheckFailure> allIntegrityCheckFailures =
        this.integrityCheckFailureRepository.findAll();
    List<TMTUVIntegrityCheckFailure> integrityCheckFailuresToDelete = new ArrayList<>();
    List<TMTUVIntegrityCheckFailure> integrityCheckFailuresToAlert = new ArrayList<>();
    allIntegrityCheckFailures.forEach(
        integrityCheckFailure -> {
          TMTextUnitCurrentVariant textUnitCurrentVariant =
              this.textUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(
                  integrityCheckFailure.getLocale_Id(), integrityCheckFailure.getTmTextUnit_Id());
          Optional<TMTextUnitVariant> textUnitVariant =
              ofNullable(textUnitCurrentVariant)
                  .map(TMTextUnitCurrentVariant::getTmTextUnitVariant);
          if (this.shouldIntegrityCheckFailureBeDeleted(integrityCheckFailure, textUnitVariant)) {
            integrityCheckFailuresToDelete.add(integrityCheckFailure);
          } else if (integrityCheckFailure.getTextUnitVariantStatus()
              == TMTextUnitVariant.Status.INTEGRITY_FAILURE) {
            integrityCheckFailuresToAlert.add(integrityCheckFailure);
          }
        });
    this.integrityCheckFailureRepository.deleteAll(integrityCheckFailuresToDelete);
    this.alertService.sendAlert(integrityCheckFailuresToAlert);
  }
}
