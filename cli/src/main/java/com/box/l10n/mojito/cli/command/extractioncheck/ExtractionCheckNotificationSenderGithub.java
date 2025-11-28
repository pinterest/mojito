package com.box.l10n.mojito.cli.command.extractioncheck;

import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.github.GithubClients;
import com.box.l10n.mojito.thirdpartynotification.github.GithubIcon;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHIssueComment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import reactor.core.publisher.Mono;

@Configurable
public class ExtractionCheckNotificationSenderGithub extends ExtractionCheckNotificationSender {

  @Autowired GithubClients githubClients;

  private final String githubRepo;

  private final String githubOwner;

  private final Integer prNumber;

  private final boolean isSetCommitStatus;

  private final String commitSha;

  private final String commitStatusTargetUrl;

  private final String messageRegex;

  private final boolean usesSummaryNotification;

  public ExtractionCheckNotificationSenderGithub(
      String messageTemplate,
      String messageRegex,
      String hardFailureMessage,
      String checksSkippedMessage,
      String githubOwner,
      String githubRepo,
      Integer prNumber,
      Boolean isSetCommitStatus,
      String commitSha,
      String commitStatusTargetUrl,
      Boolean usesSummaryNotification) {
    super(messageTemplate, hardFailureMessage, checksSkippedMessage);
    if (Strings.isNullOrEmpty(githubRepo)) {
      throw new ExtractionCheckNotificationSenderException(
          "Github repository owner must be provided if using Github notifications.");
    }
    this.githubOwner = githubOwner;
    if (Strings.isNullOrEmpty(githubOwner)) {
      throw new ExtractionCheckNotificationSenderException(
          "Github repository name must be provided if using Github notifications.");
    }
    this.githubRepo = githubRepo;
    if (prNumber == null) {
      throw new ExtractionCheckNotificationSenderException(
          "Github PR number must be provided if using Github notifications.");
    }

    this.prNumber = prNumber;
    this.isSetCommitStatus = isSetCommitStatus;
    this.commitSha = commitSha;
    this.commitStatusTargetUrl = commitStatusTargetUrl;
    this.messageRegex = Preconditions.checkNotNull(messageRegex);
    this.usesSummaryNotification = Objects.requireNonNullElse(usesSummaryNotification, false);
  }

  @Override
  public void sendFailureNotification(List<CliCheckResult> failures, boolean hardFail) {
    if (this.usesSummaryNotification) {
      sendSummaryNotification(failures, hardFail);
    } else {
      sendFullFailureNotification(failures, hardFail);
    }
  }

  protected void sendSummaryNotification(List<CliCheckResult> failures, boolean hardFail) {
    if (githubClients.isClientAvailable(githubOwner)
        && !isNullOrEmpty(failures)
        && failures.stream().anyMatch(result -> !result.isSuccessful())) {
      StringBuilder sb = new StringBuilder();

      long totalFailureCount =
          failures.stream().mapToLong(x -> x.getNameToFailuresMap().size()).sum();
      long hardCheckFailureCount =
          getCheckerHardFailures(failures).mapToLong(x -> x.getNameToFailuresMap().size()).sum();
      long remainingFailureCount = totalFailureCount - hardCheckFailureCount;

      sb.append("**i18n source string checks failed**").append(getDoubleNewLines());
      sb.append(getDoubleNewLines());
      if (hardFail && hardCheckFailureCount > 0) {
        if (!Strings.isNullOrEmpty(hardFailureMessage)) {
          sb.append(getDoubleNewLines());
          sb.append(hardFailureMessage);
          sb.append(getDoubleNewLines());
        }
        sb.append("Hard check failure count: ")
            .append(hardCheckFailureCount)
            .append(getDoubleNewLines());
      }

      if (remainingFailureCount > 0) {
        sb.append("Warning check count: ").append(remainingFailureCount);
      }

      if (totalFailureCount > 0) {
        sb.append(getDoubleNewLines())
            .append("**")
            .append("Please correct the above issues in a new commit.")
            .append("**");
      }

      String message =
          getFormattedNotificationMessage(
              messageTemplate, "baseMessage", replaceQuoteMarkers(sb.toString()));
      Mono<GHIssueComment> ghIssueCommentMono =
          githubClients
              .getClient(githubOwner)
              .updateOrAddCommentToPR(
                  githubRepo, prNumber, GithubIcon.WARNING + " " + message, this.messageRegex);
      ghIssueCommentMono.block();
      if (isSetCommitStatus) {
        githubClients
            .getClient(githubOwner)
            .addStatusToCommit(
                githubRepo,
                commitSha,
                GHCommitState.FAILURE,
                "Checks failed, please see 'Details' link for information on resolutions.",
                "I18N String Checks",
                commitStatusTargetUrl);
      }
    }
  }

  protected void sendFullFailureNotification(List<CliCheckResult> failures, boolean hardFail) {
    if (githubClients.isClientAvailable(githubOwner)
        && !isNullOrEmpty(failures)
        && failures.stream().anyMatch(result -> !result.isSuccessful())) {
      StringBuilder sb = new StringBuilder();
      sb.append("**i18n source string checks failed**").append(getDoubleNewLines());
      if (hardFail) {
        sb.append("The following checks had hard failures:")
            .append(System.lineSeparator())
            .append(
                getCheckerHardFailures(failures)
                    .map(failure -> "**" + failure.getCheckName() + "**")
                    .collect(Collectors.joining(System.lineSeparator())));
      }
      sb.append(getDoubleNewLines());
      sb.append("**Failed checks:**").append(getDoubleNewLines());
      sb.append(
          failures.stream()
              .map(
                  check -> {
                    GithubIcon icon = check.isHardFail() ? GithubIcon.STOP : GithubIcon.WARNING;
                    return icon
                        + " **"
                        + check.getCheckName()
                        + "**"
                        + getDoubleNewLines()
                        + check.getNotificationText();
                  })
              .collect(Collectors.joining(System.lineSeparator())));
      sb.append(getDoubleNewLines()).append("**Please correct the above issues in a new commit.**");
      String message =
          getFormattedNotificationMessage(
              messageTemplate,
              "baseMessage",
              replaceQuoteMarkers(appendHardFailureMessage(hardFail, sb)));
      Mono<GHIssueComment> ghIssueCommentMono =
          githubClients
              .getClient(githubOwner)
              .updateOrAddCommentToPR(
                  githubRepo, prNumber, GithubIcon.WARNING + " " + message, this.messageRegex);
      ghIssueCommentMono.block();
      if (isSetCommitStatus) {
        githubClients
            .getClient(githubOwner)
            .addStatusToCommit(
                githubRepo,
                commitSha,
                GHCommitState.FAILURE,
                "Checks failed, please see 'Details' link for information on resolutions.",
                "I18N String Checks",
                commitStatusTargetUrl);
      }
    }
  }

  @Override
  public void sendChecksSkippedNotification() {
    if (isSetCommitStatus) {
      githubClients
          .getClient(githubOwner)
          .addStatusToCommit(
              githubRepo,
              commitSha,
              GHCommitState.SUCCESS,
              "Checks disabled as SKIP_I18N_CHECKS was applied in comments.",
              "I18N String Checks",
              commitStatusTargetUrl);
    }
    if (!Strings.isNullOrEmpty(checksSkippedMessage)) {
      Mono<GHIssueComment> ghIssueCommentMono =
          githubClients
              .getClient(githubOwner)
              .addCommentToPR(
                  githubRepo, prNumber, GithubIcon.WARNING + " " + checksSkippedMessage);
      ghIssueCommentMono.block();
    }
  }

  @Override
  public String replaceQuoteMarkers(String message) {
    return message.replaceAll(QUOTE_MARKER, "`");
  }
}
