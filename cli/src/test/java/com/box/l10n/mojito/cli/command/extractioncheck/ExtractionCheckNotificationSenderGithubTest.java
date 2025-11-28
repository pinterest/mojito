package com.box.l10n.mojito.cli.command.extractioncheck;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.cli.command.checks.CheckerRuleId;
import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.github.GithubClients;
import com.box.l10n.mojito.thirdpartynotification.github.GithubIcon;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHIssueComment;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ExtractionCheckNotificationSenderGithubTest.class})
public class ExtractionCheckNotificationSenderGithubTest {
  static final String MESSAGE_REGEX = ".*\\*\\*i18n source string checks failed\\*\\*.*";

  @Mock GithubClients githubClientsMock;

  @Captor ArgumentCaptor<String> repoNameCaptor;

  @Captor ArgumentCaptor<Integer> prNumberCaptor;

  @Captor ArgumentCaptor<String> messageCaptor;

  @Captor ArgumentCaptor<String> keyMessageCaptor;

  GithubClient githubClientMock;

  ExtractionCheckNotificationSenderGithub extractionCheckNotificationSenderGithub;

  public void setup(Boolean usesSummaryNotification) {
    githubClientMock = Mockito.mock(GithubClient.class);
    when(githubClientsMock.getClient(isA(String.class))).thenReturn(githubClientMock);
    when(githubClientsMock.isClientAvailable(isA(String.class))).thenReturn(true);
    Mono<GHIssueComment> mono = Mockito.mock(Mono.class);
    Mono<GHIssueComment> ghIssueCommentMono = Mockito.mock(Mono.class);
    when(mono.subscribeOn(any(Scheduler.class))).thenReturn(ghIssueCommentMono);
    when(githubClientMock.addCommentToPR(anyString(), anyInt(), anyString())).thenReturn(mono);
    when(githubClientMock.updateOrAddCommentToPR(anyString(), anyInt(), anyString(), anyString()))
        .thenReturn(mono);
    extractionCheckNotificationSenderGithub =
        new ExtractionCheckNotificationSenderGithub(
            "{baseMessage}",
            MESSAGE_REGEX,
            "This is a hard failure message",
            "This is a checks skipped message",
            "testOwner",
            "testRepo",
            100,
            true,
            "123456789",
            "https://somewebaddress.com/",
            usesSummaryNotification);
    extractionCheckNotificationSenderGithub.githubClients = githubClientsMock;
  }

  @Test
  public void testSendFailureNotifications() {
    setup(true);
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(false, false, "Test Check");
    result.setNotificationText("Some notification text");
    result.appendToFailuresMap(
        Map.of(
            "file.py",
            new CliCheckResult.CheckFailure(
                CheckerRuleId.AGGREGATE_GLOSSARY_CASE_CHECKER_RESULTS, "Some notification text")));
    results.add(result);
    extractionCheckNotificationSenderGithub.sendFailureNotification(results, false);
    verify(githubClientMock, times(1))
        .updateOrAddCommentToPR(
            repoNameCaptor.capture(),
            prNumberCaptor.capture(),
            messageCaptor.capture(),
            keyMessageCaptor.capture());
    Assert.assertEquals("testRepo", repoNameCaptor.getValue());
    Assert.assertEquals(100, (int) prNumberCaptor.getValue());
    Assert.assertTrue(messageCaptor.getValue().contains(GithubIcon.WARNING.toString()));
    Assert.assertTrue(messageCaptor.getValue().contains("Warning check count: 1"));
    Assert.assertEquals(MESSAGE_REGEX, keyMessageCaptor.getValue());
    verify(githubClientMock, times(1))
        .addStatusToCommit(
            "testRepo",
            "123456789",
            GHCommitState.FAILURE,
            "Checks failed, please see 'Details' link for information on resolutions.",
            "I18N String Checks",
            "https://somewebaddress.com/");
  }

  @Test
  public void testCommitStateNotSetIfDisabled() {
    setup(true);
    extractionCheckNotificationSenderGithub =
        new ExtractionCheckNotificationSenderGithub(
            "{baseMessage}",
            MESSAGE_REGEX,
            "This is a hard failure message",
            "This is a checks skipped message",
            "testOwner",
            "testRepo",
            100,
            false,
            "123456789",
            "https://somewebaddress.com/",
            true);
    extractionCheckNotificationSenderGithub.githubClients = githubClientsMock;
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(false, false, "Test Check");
    result.appendToFailuresMap(
        Map.of(
            "file1.py",
            new CliCheckResult.CheckFailure(
                CheckerRuleId.AGGREGATE_GLOSSARY_CASE_CHECKER_RESULTS, "Some failure occurred")));
    results.add(result);
    extractionCheckNotificationSenderGithub.sendFailureNotification(results, false);
    verify(githubClientMock, times(1))
        .updateOrAddCommentToPR(
            repoNameCaptor.capture(),
            prNumberCaptor.capture(),
            messageCaptor.capture(),
            keyMessageCaptor.capture());
    Assert.assertEquals("testRepo", repoNameCaptor.getValue());
    Assert.assertEquals(100, (int) prNumberCaptor.getValue());
    Assert.assertEquals(MESSAGE_REGEX, keyMessageCaptor.getValue());
    verify(githubClientMock, times(0))
        .addStatusToCommit(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString());
  }

  @Test
  public void testSendFailureNotificationsMultipleFailures() {
    setup(true);
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(false, false, "Test Check");
    result.setNotificationText("Some notification text");
    result.appendToFailuresMap(
        Map.of(
            "fileA.py",
            new CliCheckResult.CheckFailure(
                CheckerRuleId.AGGREGATE_GLOSSARY_CASE_CHECKER_RESULTS, "Some notification text")));
    CliCheckResult result2 = new CliCheckResult(false, false, "Other Check");
    result2.setNotificationText("Some other notification text");
    result2.appendToFailuresMap(
        Map.of(
            "file1.py",
            new CliCheckResult.CheckFailure(
                CheckerRuleId.AGGREGATE_GLOSSARY_CASE_CHECKER_RESULTS,
                "Some other notification text")));
    results.add(result);
    results.add(result2);
    extractionCheckNotificationSenderGithub.sendFailureNotification(results, false);
    verify(githubClientMock, times(1))
        .updateOrAddCommentToPR(
            repoNameCaptor.capture(),
            prNumberCaptor.capture(),
            messageCaptor.capture(),
            keyMessageCaptor.capture());
    Assert.assertEquals("testRepo", repoNameCaptor.getValue());
    Assert.assertEquals(100, (int) prNumberCaptor.getValue());
    Assert.assertTrue(messageCaptor.getValue().contains(GithubIcon.WARNING.toString()));
    Assert.assertTrue(messageCaptor.getValue().contains("Warning check count: 2"));
    Assert.assertEquals(MESSAGE_REGEX, keyMessageCaptor.getValue());
    verify(githubClientMock, times(1))
        .addStatusToCommit(
            "testRepo",
            "123456789",
            GHCommitState.FAILURE,
            "Checks failed, please see 'Details' link for information on resolutions.",
            "I18N String Checks",
            "https://somewebaddress.com/");
  }

  @Test
  public void testSendFailureNotificationsHardFail() {
    setup(true);
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(false, true, "Test Check");
    result.setNotificationText("Some notification text");
    result.appendToFailuresMap(
        Map.of(
            "file.py",
            new CliCheckResult.CheckFailure(
                CheckerRuleId.EMPTY_COMMENT_STRING, "Some notification text")));
    results.add(result);
    extractionCheckNotificationSenderGithub.sendFailureNotification(results, true);
    verify(githubClientMock, times(1))
        .updateOrAddCommentToPR(
            repoNameCaptor.capture(),
            prNumberCaptor.capture(),
            messageCaptor.capture(),
            keyMessageCaptor.capture());
    Assert.assertEquals("testRepo", repoNameCaptor.getValue());
    Assert.assertEquals(100, (int) prNumberCaptor.getValue());
    Assert.assertTrue(
        messageCaptor
            .getValue()
            .contains(GithubIcon.WARNING + " **i18n source string checks failed**"));
    Assert.assertTrue(messageCaptor.getValue().contains("Hard check failure count: 1"));
    Assert.assertEquals(MESSAGE_REGEX, keyMessageCaptor.getValue());
    verify(githubClientMock, times(1))
        .addStatusToCommit(
            "testRepo",
            "123456789",
            GHCommitState.FAILURE,
            "Checks failed, please see 'Details' link for information on resolutions.",
            "I18N String Checks",
            "https://somewebaddress.com/");
  }

  @Test
  public void testSendChecksSkippedNotification() {
    setup(true);
    extractionCheckNotificationSenderGithub.sendChecksSkippedNotification();
    verify(githubClientMock, times(1))
        .addCommentToPR(
            repoNameCaptor.capture(), prNumberCaptor.capture(), messageCaptor.capture());
    Assert.assertTrue(repoNameCaptor.getValue().equals("testRepo"));
    Assert.assertTrue(prNumberCaptor.getValue().equals(100));
    Assert.assertTrue(
        messageCaptor.getValue().equals(GithubIcon.WARNING + " This is a checks skipped message"));
    verify(githubClientMock, times(1))
        .addStatusToCommit(
            "testRepo",
            "123456789",
            GHCommitState.SUCCESS,
            "Checks disabled as SKIP_I18N_CHECKS was applied in comments.",
            "I18N String Checks",
            "https://somewebaddress.com/");
  }

  @Test
  public void testNoNotificationsSentIfNoFailuresInResultList() {
    setup(true);
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(true, true, "Test Check");
    result.setNotificationText("Some notification text");
    results.add(result);
    extractionCheckNotificationSenderGithub.sendFailureNotification(results, true);
    verify(githubClientMock, times(0))
        .updateOrAddCommentToPR(
            isA(String.class), isA(Integer.class), isA(String.class), isA(String.class));
    verify(githubClientMock, times(0))
        .addStatusToCommit(
            isA(String.class),
            isA(String.class),
            isA(GHCommitState.class),
            isA(String.class),
            isA(String.class),
            isA(String.class));
  }

  @Test
  public void testNoNotificationsSentIfNullListSent() {
    setup(true);
    extractionCheckNotificationSenderGithub.sendFailureNotification(null, true);
    verify(githubClientMock, times(0))
        .updateOrAddCommentToPR(
            isA(String.class), isA(Integer.class), isA(String.class), isA(String.class));
    verify(githubClientMock, times(0))
        .addStatusToCommit(
            isA(String.class),
            isA(String.class),
            isA(GHCommitState.class),
            isA(String.class),
            isA(String.class),
            isA(String.class));
  }

  @Test(expected = ExtractionCheckNotificationSenderException.class)
  public void testExceptionThrownIfNoOwnerSpecified() {
    new ExtractionCheckNotificationSenderGithub(
        "", MESSAGE_REGEX, "some template", "", "", "testRepo", 100, true, "", "", true);
  }

  @Test(expected = ExtractionCheckNotificationSenderException.class)
  public void testExceptionThrownIfNoRepositorySpecified() {
    new ExtractionCheckNotificationSenderGithub(
        "", MESSAGE_REGEX, "some template", "", "testOwner", "", 100, true, "", "", true);
  }

  @Test(expected = ExtractionCheckNotificationSenderException.class)
  public void testExceptionThrownIfNoPRNumberProvided() {
    new ExtractionCheckNotificationSenderGithub(
        "", MESSAGE_REGEX, "some template", "", "testOwner", "testRepo", null, true, "", "", true);
  }

  @Test
  public void testQuoteMarkersAreReplaced() {
    setup(false);
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(false, true, "Test Check");
    result.setNotificationText(
        "Some notification text for " + QUOTE_MARKER + "some.text.id" + QUOTE_MARKER);
    results.add(result);
    extractionCheckNotificationSenderGithub.sendFailureNotification(results, true);
    verify(githubClientMock, times(1))
        .updateOrAddCommentToPR(
            repoNameCaptor.capture(),
            prNumberCaptor.capture(),
            messageCaptor.capture(),
            keyMessageCaptor.capture());
    Assert.assertTrue(repoNameCaptor.getValue().equals("testRepo"));
    Assert.assertTrue(prNumberCaptor.getValue().equals(100));
    Assert.assertTrue(
        messageCaptor
            .getValue()
            .contains(GithubIcon.WARNING + " **i18n source string checks failed**"));
    Assert.assertTrue(messageCaptor.getValue().contains("Test Check"));
    Assert.assertTrue(messageCaptor.getValue().contains("Some notification text"));
    Assert.assertTrue(messageCaptor.getValue().contains("This is a hard failure message"));
    Assert.assertTrue(messageCaptor.getValue().contains("`some.text.id`"));
    Assert.assertEquals(MESSAGE_REGEX, keyMessageCaptor.getValue());
  }

  @Test
  public void testSendFullFailureNotificationsMultipleFailures() {
    setup(false);
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(false, false, "Test Check");
    result.setNotificationText("Some notification text");
    CliCheckResult result2 = new CliCheckResult(false, false, "Other Check");
    result2.setNotificationText("Some other notification text");
    results.add(result);
    results.add(result2);
    extractionCheckNotificationSenderGithub.sendFailureNotification(results, false);
    verify(githubClientMock, times(1))
        .updateOrAddCommentToPR(
            repoNameCaptor.capture(),
            prNumberCaptor.capture(),
            messageCaptor.capture(),
            keyMessageCaptor.capture());
    Assert.assertTrue(messageCaptor.getValue().contains(GithubIcon.WARNING.toString()));
    Assert.assertTrue(messageCaptor.getValue().contains("Test Check"));
    Assert.assertTrue(messageCaptor.getValue().contains("Some notification text"));
    Assert.assertTrue(messageCaptor.getValue().contains("Other Check"));
    Assert.assertTrue(messageCaptor.getValue().contains("Some other notification text"));
    Assert.assertEquals(MESSAGE_REGEX, keyMessageCaptor.getValue());
  }

  @Test
  public void testFullCommitStateNotSetIfDisabled() {
    setup(true);
    extractionCheckNotificationSenderGithub =
        new ExtractionCheckNotificationSenderGithub(
            "{baseMessage}",
            MESSAGE_REGEX,
            "This is a hard failure message",
            "This is a checks skipped message",
            "testOwner",
            "testRepo",
            100,
            false,
            "123456789",
            "https://somewebaddress.com/",
            false);
    extractionCheckNotificationSenderGithub.githubClients = githubClientsMock;
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(false, false, "Test Check");
    result.setNotificationText("Some notification text");
    results.add(result);
    extractionCheckNotificationSenderGithub.sendFailureNotification(results, false);
    verify(githubClientMock, times(1))
        .updateOrAddCommentToPR(
            repoNameCaptor.capture(),
            prNumberCaptor.capture(),
            messageCaptor.capture(),
            keyMessageCaptor.capture());
    Assert.assertTrue(messageCaptor.getValue().contains(GithubIcon.WARNING.toString()));
    Assert.assertTrue(messageCaptor.getValue().contains("Test Check"));
    Assert.assertTrue(messageCaptor.getValue().contains("Some notification text"));
    Assert.assertEquals(MESSAGE_REGEX, keyMessageCaptor.getValue());
  }
}
