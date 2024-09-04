package com.box.l10n.mojito.common.notification;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IntegrityCheckNotifierTest {

  private IntegrityCheckNotifier integrityCheckNotifier;
  private IntegrityCheckNotifierConfiguration integrityCheckNotifierConfiguration;
  private SlackClients slackClients;

  @BeforeEach
  public void setUp() {
    // Setup Slack clients
    slackClients = mock(SlackClients.class);
    when(slackClients.getById("test-slack-id")).thenReturn(new SlackClient("xxx_xxx_xxx"));

    integrityCheckNotifierConfiguration = new IntegrityCheckNotifierConfiguration();
    integrityCheckNotifierConfiguration.setSlackChannel("#test-channel");
    integrityCheckNotifierConfiguration.setSlackClientId("test-slack-id");

    integrityCheckNotifier =
        new IntegrityCheckNotifier(slackClients, null, integrityCheckNotifierConfiguration, null);
  }

  @Test
  public void testDefaults() {
    try {
      integrityCheckNotifier.init();
    } catch (Exception e) {
      fail("IntegrityCheckNotifier should not be catching exceptions with default setup.");
    }
  }

  @Test
  public void testNullSlackClientId() {
    integrityCheckNotifierConfiguration.setSlackClientId(null);
    try {
      integrityCheckNotifier.init();
      fail("IntegrityCheckNotifier should be thrown for null slack clientId.");
    } catch (Exception e) {
      assertEquals(e.getMessage(), "Slack client id not defined.");
    }
  }

  @Test
  public void testNullSlackChannel() {
    integrityCheckNotifierConfiguration.setSlackChannel(null);
    try {
      integrityCheckNotifier.init();
      fail("IntegrityCheckNotifier should be thrown for null slack channel.");
    } catch (Exception e) {
      assertEquals(e.getMessage(), "Slack channel not defined.");
    }
  }

  @Test
  public void testBadSlackChannelFormat() {
    integrityCheckNotifierConfiguration.setSlackChannel("testing-bad-channel-format");
    try {
      integrityCheckNotifier.init();
      fail("IntegrityCheckNotifier should be thrown for null slack channel.");
    } catch (Exception e) {
      assertEquals(e.getMessage(), "Slack channel must start with #.");
    }
  }

  @Test
  public void testSlackClientNotFound() {
    when(slackClients.getById("test-slack-id")).thenReturn(null);
    try {
      integrityCheckNotifier.init();
      fail("IntegrityCheckNotifier should be thrown for null slack client..");
    } catch (Exception e) {
      assertEquals(e.getMessage(), "Slack client id defined but doesn't exist.");
    }
  }
}
