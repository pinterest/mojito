package com.box.l10n.mojito.service.tm;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTUVIntegrityCheckFailure;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.ThirdPartyTextUnit;
import com.box.l10n.mojito.pagerduty.PagerDutyException;
import com.box.l10n.mojito.pagerduty.PagerDutyIntegrationService;
import com.box.l10n.mojito.pagerduty.PagerDutyPayload;
import com.box.l10n.mojito.react.LinkConfig;
import com.box.l10n.mojito.react.RepositoryConfig;
import com.box.l10n.mojito.react.ThirdParty;
import com.box.l10n.mojito.service.thirdparty.ThirdPartyTextUnitRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IntegrityCheckFailureAlertService {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(IntegrityCheckFailureAlertService.class);

  private static final String REJECTED_TRANSLATION_LABEL_FORMAT = "Rejected Translation %d";

  private static final String CUSTOM_DETAILS_FORMAT =
      "TM Text Unit ID: %d\nLocale: %s\nURL: %s\nIntegrity Check Failure:\n%s";

  private final PagerDutyIntegrationService pagerDutyIntegrationService;

  private final LinkConfig linkConfig;

  private final ThirdPartyTextUnitRepository thirdPartyTextUnitRepository;

  private final IntegrityCheckFailureConfigurationProperties configurationProperties;

  public IntegrityCheckFailureAlertService(
      PagerDutyIntegrationService pagerDutyIntegrationService,
      LinkConfig linkConfig,
      ThirdPartyTextUnitRepository thirdPartyTextUnitRepository,
      IntegrityCheckFailureConfigurationProperties configurationProperties) {
    this.pagerDutyIntegrationService = pagerDutyIntegrationService;
    this.linkConfig = linkConfig;
    this.thirdPartyTextUnitRepository = thirdPartyTextUnitRepository;
    this.configurationProperties = configurationProperties;
  }

  private String getIntegrityCheckFailureKey(TMTUVIntegrityCheckFailure integrityCheckFailure) {
    return String.format(
        "%d_%d", integrityCheckFailure.getTmTextUnit_Id(), integrityCheckFailure.getLocale_Id());
  }

  private Map<String, List<TMTUVIntegrityCheckFailure>> getIntegrityCheckFailuresByKey(
      List<TMTUVIntegrityCheckFailure> integrityCheckFailures) {
    Map<String, List<TMTUVIntegrityCheckFailure>> integrityCheckFailureByKey = new HashMap<>();
    integrityCheckFailures.forEach(
        integrityCheckFailure -> {
          String key = this.getIntegrityCheckFailureKey(integrityCheckFailure);
          if (integrityCheckFailureByKey.containsKey(key)) {
            integrityCheckFailureByKey.get(key).add(integrityCheckFailure);
          } else {
            integrityCheckFailureByKey.put(key, new ArrayList<>(List.of(integrityCheckFailure)));
          }
        });
    return integrityCheckFailureByKey;
  }

  private Map<String, String> getThirdPartyUrlByKey(
      Map<String, List<TMTUVIntegrityCheckFailure>> integrityCheckFailuresByKey) {
    Map<String, String> thirdPartyUrlByKey = new HashMap<>();
    Map<String, RepositoryConfig> links = linkConfig.getLink();
    for (Map.Entry<String, List<TMTUVIntegrityCheckFailure>> integrityCheckFailuresEntry :
        integrityCheckFailuresByKey.entrySet()) {
      String key = integrityCheckFailuresEntry.getKey();
      TMTUVIntegrityCheckFailure integrityCheckFailure =
          integrityCheckFailuresEntry.getValue().getFirst();
      Optional<String> repositoryName =
          ofNullable(integrityCheckFailure.getTmTextUnit())
              .map(TMTextUnit::getAsset)
              .map(Asset::getRepository)
              .map(Repository::getName);
      String url = "";
      if (repositoryName.isPresent() && links.containsKey(repositoryName.get())) {
        Optional<String> thirdPartyUrl =
            of(links.get(repositoryName.get()))
                .map(RepositoryConfig::getThirdParty)
                .map(ThirdParty::getUrl);
        if (thirdPartyUrl.isPresent()) {
          ThirdPartyTextUnit thirdPartyTextUnit =
              this.thirdPartyTextUnitRepository.findByTmTextUnit(
                  integrityCheckFailure.getTmTextUnit());
          url =
              thirdPartyUrl
                  .get()
                  .replace(
                      this.configurationProperties.getThirdPartyTextUnitIdPlaceholder(),
                      thirdPartyTextUnit.getThirdPartyId());
        }
      }
      thirdPartyUrlByKey.put(key, url);
    }
    return thirdPartyUrlByKey;
  }

  private Map<String, String> getCustomDetails(
      Map<String, List<TMTUVIntegrityCheckFailure>> integrityCheckFailuresByKey,
      Map<String, String> thirdPartyUrlByKey) {
    Map<String, String> customDetails = new HashMap<>();
    int index = 1;
    for (Map.Entry<String, List<TMTUVIntegrityCheckFailure>> integrityCheckFailuresEntry :
        integrityCheckFailuresByKey.entrySet()) {
      for (TMTUVIntegrityCheckFailure integrityCheckFailure :
          integrityCheckFailuresEntry.getValue()) {
        if (customDetails.containsKey(String.format(REJECTED_TRANSLATION_LABEL_FORMAT, index))) {
          customDetails.compute(
              String.format(REJECTED_TRANSLATION_LABEL_FORMAT, index),
              (k, value) ->
                  String.format("%s\n%s", value, integrityCheckFailure.getIntegrityFailureName()));
        } else {
          String url = thirdPartyUrlByKey.getOrDefault(integrityCheckFailuresEntry.getKey(), "");
          customDetails.put(
              String.format(REJECTED_TRANSLATION_LABEL_FORMAT, index),
              String.format(
                  CUSTOM_DETAILS_FORMAT,
                  integrityCheckFailure.getTmTextUnit().getId(),
                  integrityCheckFailure.getLocale().getBcp47Tag(),
                  url,
                  integrityCheckFailure.getIntegrityFailureName()));
        }
      }
      index++;
    }
    return customDetails;
  }

  public void sendAlert(List<TMTUVIntegrityCheckFailure> integrityCheckFailures) {
    this.pagerDutyIntegrationService
        .getDefaultPagerDutyClient()
        .ifPresent(
            pagerDutyClient -> {
              try {
                if (integrityCheckFailures.isEmpty()) {
                  pagerDutyClient.resolveIncident(
                      this.configurationProperties.getPagerDutyDedupKey());
                } else {
                  Map<String, List<TMTUVIntegrityCheckFailure>> integrityCheckFailuresByKey =
                      this.getIntegrityCheckFailuresByKey(integrityCheckFailures);
                  Map<String, String> thirdPartyUrlByKey =
                      this.getThirdPartyUrlByKey(integrityCheckFailuresByKey);
                  Map<String, String> customDetails =
                      this.getCustomDetails(integrityCheckFailuresByKey, thirdPartyUrlByKey);
                  String source = String.join("\n", thirdPartyUrlByKey.values());
                  PagerDutyPayload payload =
                      new PagerDutyPayload(
                          this.configurationProperties.getPagerDutyIncidentSummary(),
                          source,
                          PagerDutyPayload.Severity.CRITICAL,
                          customDetails);
                  pagerDutyClient.triggerIncident(
                      this.configurationProperties.getPagerDutyDedupKey(), payload);
                }
              } catch (PagerDutyException e) {
                LOGGER.error(
                    "Couldn't send PagerDuty notification for Integrity Check Failure(s)", e);
              }
            });
  }
}
