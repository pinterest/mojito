package com.box.l10n.mojito.cli.command.extractioncheck;

import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.cli.command.utils.GithubReviewCommentService;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.github.GithubClients;
import com.box.l10n.mojito.thirdpartynotification.github.GithubIcon;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHIssueComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import reactor.core.publisher.Mono;

@Configurable
public class ExtractionCheckNotificationSenderGithub extends ExtractionCheckNotificationSender {

  private static final Logger logger =
      LoggerFactory.getLogger(ExtractionCheckNotificationSenderGithub.class);

  @Autowired GithubClients githubClients;

  @Autowired GithubReviewCommentService githubReviewCommentService;

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

  /**
   * Adds inline PR review comments for check failures using the GithubReviewCommentService. This
   * method will only post comments if all required data is available.
   *
   * @param failures the check failures to create review comments for
   * @return the review comments that were generated (and posted), or an empty list if none were
   *     generated or required data was missing
   */
  public List<GithubClient.ReviewComment> addInlineReviewComments(
      List<CliCheckResult> failures,
      List<AssetExtractionDiff> assetExtractionDiffs,
      Map<String, Set<Integer>> githubModifiedLines,
      String prefixToRemoveFromFileUris,
      ConsoleWriter consoleWriter) {
    if (assetExtractionDiffs == null
        || assetExtractionDiffs.isEmpty()
        || githubModifiedLines == null
        || commitSha == null
        || commitSha.isEmpty()) {
      return List.of();
    }

    try {
      /*List<GithubClient.ReviewComment> reviewComments =
          githubReviewCommentService.generateReviewComments(
              failures,
              assetExtractionDiffs,
              githubModifiedLines,
              githubRepo,
              prefixToRemoveFromFileUris);*/

      List<GithubClient.ReviewComment> reviewComments = List.of(new GithubClient.ReviewComment("TEST 1", "locale/Messages.properties",  2));

      if (reviewComments.isEmpty()) {
        return reviewComments;
      }

      consoleWriter
          .newLine()
          .a("Adding ")
          .a(reviewComments.size())
          .a(" review comment(s) to ")
          .a(githubOwner + "/" + githubRepo)
          .a(" PR #")
          .a(prNumber)
          .a(" @ ")
          .a(commitSha)
          .println();
      reviewComments.forEach(
          comment ->
              consoleWriter
                  .a("  - ")
                  .a(comment.getPath() + ":" + comment.getLine())
                  .println());

      githubClients
          .getClient(githubOwner)
          .addReviewCommentsToPR(githubRepo, prNumber, reviewComments, commitSha);

      return reviewComments;
    } catch (Exception e) {
      // Log the error but don't fail the notification process: the summary comment has already been
      // posted, and inline review comments are a best-effort enhancement.
      logger.error("Failed to add inline review comments to PR {}", prNumber, e);
      return List.of();
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
        if (!Strings.isNullOrEmpty(hardFailureMessage)) {
          sb.append(getDoubleNewLines());
          sb.append(hardFailureMessage);
          sb.append(getDoubleNewLines());
        }

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

  @Override
  public void sendChecksSkippedNotification() {
    if (isSetCommitStatus) {
      githubClients
          .getClient(githubOwner)
          .addStatusToCommit(
              githubRepo,
              commitSha,
              GHCommitState.SUCCESS,
              "Checks disabled because a SKIP_I18N_CHECKS comment or label was found.",
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
