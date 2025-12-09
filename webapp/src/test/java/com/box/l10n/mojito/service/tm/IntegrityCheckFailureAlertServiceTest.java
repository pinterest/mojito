package com.box.l10n.mojito.service.tm;

import static com.box.l10n.mojito.service.tm.IntegrityCheckFailureAlertService.REJECTED_TRANSLATION_LABEL_FORMAT;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTUVIntegrityCheckFailure;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.ThirdPartyTextUnit;
import com.box.l10n.mojito.pagerduty.PagerDutyClient;
import com.box.l10n.mojito.pagerduty.PagerDutyException;
import com.box.l10n.mojito.pagerduty.PagerDutyIntegrationService;
import com.box.l10n.mojito.pagerduty.PagerDutyPayload;
import com.box.l10n.mojito.react.LinkConfig;
import com.box.l10n.mojito.react.RepositoryConfig;
import com.box.l10n.mojito.react.ThirdParty;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.thirdparty.ThirdPartyTextUnitRepository;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public class IntegrityCheckFailureAlertServiceTest extends ServiceTestBase {
  static final String REPOSITORY_NAME = "repo";

  @Mock PagerDutyIntegrationService pagerDutyIntegrationServiceMock;

  @Mock ThirdPartyTextUnitRepository thirdPartyTextUnitRepositoryMock;

  IntegrityCheckFailureAlertService integrityCheckFailureAlertService;

  @Mock PagerDutyClient pagerDutyClient;

  IntegrityCheckFailureConfigurationProperties configurationProperties;

  @Captor ArgumentCaptor<PagerDutyPayload> pagerDutyPayloadArgumentCaptor;

  @Before
  public void setUp() {
    when(this.pagerDutyIntegrationServiceMock.getDefaultPagerDutyClient())
        .thenReturn(of(this.pagerDutyClient));
    ThirdPartyTextUnit thirdPartyTextUnit1 = new ThirdPartyTextUnit();
    thirdPartyTextUnit1.setThirdPartyId("third-party-id-1");
    ThirdPartyTextUnit thirdPartyTextUnit2 = new ThirdPartyTextUnit();
    thirdPartyTextUnit2.setThirdPartyId("third-party-id-2");
    ThirdPartyTextUnit thirdPartyTextUnit3 = new ThirdPartyTextUnit();
    thirdPartyTextUnit3.setThirdPartyId("third-party-id-3");
    when(this.thirdPartyTextUnitRepositoryMock.findByTmTextUnit(any(TMTextUnit.class)))
        .thenReturn(thirdPartyTextUnit1)
        .thenReturn(thirdPartyTextUnit1)
        .thenReturn(thirdPartyTextUnit2)
        .thenReturn(thirdPartyTextUnit3);
    this.configurationProperties = new IntegrityCheckFailureConfigurationProperties();
    this.integrityCheckFailureAlertService =
        new IntegrityCheckFailureAlertService(
            this.pagerDutyIntegrationServiceMock,
            this.getLinkConfig(),
            this.thirdPartyTextUnitRepositoryMock,
            configurationProperties);
  }

  private LinkConfig getLinkConfig() {
    LinkConfig linkConfig = new LinkConfig();
    RepositoryConfig repositoryConfig = new RepositoryConfig();
    ThirdParty thirdParty = new ThirdParty();
    thirdParty.setUrl(
        String.format(
            "http://localhost/%s",
            this.configurationProperties.getThirdPartyTextUnitIdPlaceholder()));
    repositoryConfig.setThirdParty(thirdParty);
    linkConfig.setLinks(ImmutableMap.of("repo", repositoryConfig));
    return linkConfig;
  }

  @Test
  public void testSendAlert_ResolvesIncident() throws PagerDutyException {
    this.integrityCheckFailureAlertService.sendAlert(ImmutableList.of());

    verify(this.pagerDutyClient)
        .resolveIncident(this.configurationProperties.getPagerDutyDedupKey());
  }

  private TMTUVIntegrityCheckFailure getIntegrityCheckFailure(
      long textUnitId,
      long localeId,
      Asset asset,
      String localeBcp47Tag,
      String integrityFailureName) {
    TMTUVIntegrityCheckFailure integrityCheckFailure = new TMTUVIntegrityCheckFailure();
    TMTextUnit textUnit = new TMTextUnit();
    textUnit.setId(textUnitId);
    textUnit.setAsset(asset);
    integrityCheckFailure.setTmTextUnit(textUnit);
    Locale locale = new Locale();
    locale.setId(localeId);
    locale.setBcp47Tag(localeBcp47Tag);
    integrityCheckFailure.setLocale(locale);
    integrityCheckFailure.setIntegrityFailureName(integrityFailureName);
    return integrityCheckFailure;
  }

  @Test
  public void testSendAlert_TriggersIncident() throws PagerDutyException {
    Asset asset = new Asset();
    Repository repository = new Repository();
    repository.setName(REPOSITORY_NAME);
    asset.setRepository(repository);
    TMTUVIntegrityCheckFailure integrityCheckFailure1 =
        this.getIntegrityCheckFailure(1L, 1L, asset, "ar-SA", "Integrity Check 1");
    TMTUVIntegrityCheckFailure integrityCheckFailure2 =
        this.getIntegrityCheckFailure(1L, 2L, asset, "en-GB", "Integrity Check 2");
    TMTUVIntegrityCheckFailure integrityCheckFailure3 =
        this.getIntegrityCheckFailure(2L, 1L, asset, "ar-SA", "Integrity Check 1");
    TMTUVIntegrityCheckFailure integrityCheckFailure4 =
        this.getIntegrityCheckFailure(3L, 1L, asset, "ar-SA", "Integrity Check 1");
    TMTUVIntegrityCheckFailure integrityCheckFailure5 =
        this.getIntegrityCheckFailure(3L, 1L, asset, "ar-SA", "Integrity Check 2");

    this.integrityCheckFailureAlertService.sendAlert(
        ImmutableList.of(
            integrityCheckFailure1,
            integrityCheckFailure2,
            integrityCheckFailure3,
            integrityCheckFailure4,
            integrityCheckFailure5));

    verify(this.pagerDutyClient)
        .triggerIncident(
            eq(this.configurationProperties.getPagerDutyDedupKey()),
            this.pagerDutyPayloadArgumentCaptor.capture());
    PagerDutyPayload pagerDutyPayload = this.pagerDutyPayloadArgumentCaptor.getValue();
    Map<String, String> customDetails = pagerDutyPayload.getCustomDetails();
    assertEquals(
        "http://localhost/third-party-id-1\nhttp://localhost/third-party-id-1\nhttp://localhost/third-party-id-2\nhttp://localhost/third-party-id-3",
        pagerDutyPayload.getSource());
    assertEquals(4, customDetails.size());
    for (int i = 1; i <= customDetails.size(); i++) {
      String key = String.format(REJECTED_TRANSLATION_LABEL_FORMAT, i);
      String customDetail = customDetails.get(key);
      switch (i) {
        case 1:
          assertTrue(customDetail.contains("TM Text Unit ID: 1"));
          assertTrue(customDetail.contains("ar-SA"));
          assertTrue(customDetail.contains("http://localhost/third-party-id-1"));
          assertTrue(customDetail.contains("Integrity Check 1"));
          break;
        case 2:
          assertTrue(customDetail.contains("TM Text Unit ID: 1"));
          assertTrue(customDetail.contains("en-GB"));
          assertTrue(customDetail.contains("http://localhost/third-party-id-1"));
          assertTrue(customDetail.contains("Integrity Check 2"));
          break;
        case 3:
          assertTrue(customDetail.contains("TM Text Unit ID: 2"));
          assertTrue(customDetail.contains("ar-SA"));
          assertTrue(customDetail.contains("http://localhost/third-party-id-2"));
          assertTrue(customDetail.contains("Integrity Check 1"));
          break;
        case 4:
          assertTrue(customDetail.contains("TM Text Unit ID: 3"));
          assertTrue(customDetail.contains("ar-SA"));
          assertTrue(customDetail.contains("http://localhost/third-party-id-3"));
          assertTrue(customDetail.contains("Integrity Check 1"));
          assertTrue(customDetail.contains("Integrity Check 2"));
      }
    }
  }
}
