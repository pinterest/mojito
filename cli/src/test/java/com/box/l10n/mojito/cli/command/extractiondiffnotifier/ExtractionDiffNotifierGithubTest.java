package com.box.l10n.mojito.cli.command.extractiondiffnotifier;

import static com.box.l10n.mojito.github.PRLabel.SKIP_TRANSLATIONS_REQUIRED;
import static com.box.l10n.mojito.github.PRLabel.TRANSLATIONS_READY;
import static com.box.l10n.mojito.github.PRLabel.TRANSLATIONS_REQUIRED;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffStatistics;
import com.box.l10n.mojito.github.GithubClient;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.github.GHIssueComment;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

public class ExtractionDiffNotifierGithubTest {
  private static final String MESSAGE_REGEX =
      ".*[\\d]+ string[s]{0,1} removed and [\\d]+ string[s]{0,1} added.*";

  GithubClient githubClientMock = Mockito.mock(GithubClient.class);

  @Before
  public void before() {
    Mono<GHIssueComment> mono = Mockito.mock(Mono.class);
    Mono<GHIssueComment> ghIssueCommentMono = Mockito.mock(Mono.class);
    when(mono.subscribeOn(any(Scheduler.class))).thenReturn(ghIssueCommentMono);
    when(this.githubClientMock.updateOrAddCommentToPR(
            anyString(), anyInt(), anyString(), anyString()))
        .thenReturn(mono);
  }

  @Test
  public void sendDiffStatistics() {
    String repository = "repository";
    int prNumber = 1;
    when(this.githubClientMock.isLabelAppliedToPR(repository, 1, TRANSLATIONS_REQUIRED.toString()))
        .thenReturn(false);
    ExtractionDiffNotifierGithub extractionDiffNotifierGithub =
        new ExtractionDiffNotifierGithub(
            new ExtractionDiffNotifierMessageBuilder("{baseMessage}"),
            this.githubClientMock,
            repository,
            prNumber,
            MESSAGE_REGEX);

    final String msg =
        extractionDiffNotifierGithub.sendDiffStatistics(ExtractionDiffStatistics.builder().build());

    assertThat(msg).isEqualTo("ℹ️ 0 strings removed and 0 strings added (from 0 to 0)");
    verify(this.githubClientMock)
        .updateOrAddCommentToPR(
            repository,
            prNumber,
            "ℹ️ 0 strings removed and 0 strings added (from 0 to 0)",
            MESSAGE_REGEX);
    verify(this.githubClientMock, times(0))
        .addLabelToPR(repository, prNumber, TRANSLATIONS_REQUIRED.toString());
  }

  @Test
  public void testLabelAppliedWhenStringsAdded() {
    String repository = "repository";
    int prNumber = 1;
    when(this.githubClientMock.isLabelAppliedToPR(repository, 1, TRANSLATIONS_REQUIRED.toString()))
        .thenReturn(false);
    ExtractionDiffNotifierGithub extractionDiffNotifierGithub =
        new ExtractionDiffNotifierGithub(
            new ExtractionDiffNotifierMessageBuilder("{baseMessage}"),
            this.githubClientMock,
            repository,
            prNumber,
            MESSAGE_REGEX);

    final String msg =
        extractionDiffNotifierGithub.sendDiffStatistics(
            ExtractionDiffStatistics.builder().added(1).build());

    verify(this.githubClientMock, times(1))
        .addLabelToPR(repository, prNumber, TRANSLATIONS_REQUIRED.toString());
  }

  @Test
  public void testLabelNotAppliedWhenTranslationReadyLabelPresent() {
    String repository = "repository";
    int prNumber = 1;
    when(this.githubClientMock.isLabelAppliedToPR(repository, 1, TRANSLATIONS_READY.toString()))
        .thenReturn(true);

    ExtractionDiffNotifierGithub extractionDiffNotifierGithub =
        new ExtractionDiffNotifierGithub(
            new ExtractionDiffNotifierMessageBuilder("{baseMessage}"),
            this.githubClientMock,
            repository,
            prNumber,
            MESSAGE_REGEX);

    final String msg =
        extractionDiffNotifierGithub.sendDiffStatistics(
            ExtractionDiffStatistics.builder().added(1).build());

    verify(this.githubClientMock, times(0))
        .addLabelToPR(repository, prNumber, TRANSLATIONS_REQUIRED.toString());
  }

  @Test
  public void sendNoChangesNotification() {
    String repository = "repository";
    int prNumber = 1;
    when(this.githubClientMock.isLabelAppliedToPR(repository, 1, TRANSLATIONS_REQUIRED.toString()))
        .thenReturn(true);
    ExtractionDiffNotifierGithub extractionDiffNotifierGithub =
        new ExtractionDiffNotifierGithub(
            new ExtractionDiffNotifierMessageBuilder("{baseMessage}"),
            this.githubClientMock,
            repository,
            prNumber,
            MESSAGE_REGEX);

    extractionDiffNotifierGithub.sendNoChangesNotification();

    verify(this.githubClientMock, times(1))
        .removeLabelFromPR(repository, prNumber, TRANSLATIONS_REQUIRED.toString());
  }

  @Test
  public void clearTranslationsRequiredLabelIfStringsOnlyRemoved() {
    String repository = "repository";
    int prNumber = 1;
    when(this.githubClientMock.isLabelAppliedToPR(repository, 1, TRANSLATIONS_REQUIRED.toString()))
        .thenReturn(true);
    ExtractionDiffNotifierGithub extractionDiffNotifierGithub =
        new ExtractionDiffNotifierGithub(
            new ExtractionDiffNotifierMessageBuilder("{baseMessage}"),
            this.githubClientMock,
            repository,
            prNumber,
            MESSAGE_REGEX);

    final String msg =
        extractionDiffNotifierGithub.sendDiffStatistics(
            ExtractionDiffStatistics.builder().removed(1).build());

    verify(this.githubClientMock, times(1))
        .removeLabelFromPR(repository, prNumber, TRANSLATIONS_REQUIRED.toString());
  }

  @Test
  public void testSendDiffStatisticsWhenSkipTranslationsRequiredWasApplied() {
    String repository = "repository";
    int prNumber = 1;
    when(this.githubClientMock.isLabelAppliedToPR(repository, 1, TRANSLATIONS_REQUIRED.toString()))
        .thenReturn(false);
    when(this.githubClientMock.isLabelAppliedToPR(
            repository, 1, SKIP_TRANSLATIONS_REQUIRED.toString()))
        .thenReturn(true);
    ExtractionDiffNotifierGithub extractionDiffNotifierGithub =
        new ExtractionDiffNotifierGithub(
            new ExtractionDiffNotifierMessageBuilder("{baseMessage}"),
            this.githubClientMock,
            repository,
            prNumber,
            MESSAGE_REGEX);

    extractionDiffNotifierGithub.sendDiffStatistics(
        ExtractionDiffStatistics.builder().added(1).build());

    verify(this.githubClientMock, times(0))
        .addLabelToPR(repository, prNumber, TRANSLATIONS_REQUIRED.toString());

    this.githubClientMock = mock(GithubClient.class);

    when(this.githubClientMock.isLabelAppliedToPR(repository, 1, TRANSLATIONS_READY.toString()))
        .thenReturn(true);
    when(this.githubClientMock.isLabelAppliedToPR(
            repository, 1, SKIP_TRANSLATIONS_REQUIRED.toString()))
        .thenReturn(true);

    extractionDiffNotifierGithub.sendDiffStatistics(
        ExtractionDiffStatistics.builder().added(1).build());

    verify(this.githubClientMock, times(0))
        .addLabelToPR(repository, prNumber, TRANSLATIONS_REQUIRED.toString());
  }
}
