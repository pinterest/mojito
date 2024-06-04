package com.box.l10n.mojito.cli.command.checks;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;
import static java.util.stream.Collectors.toList;

import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.rest.client.OpenAIServiceClient;
import com.box.l10n.mojito.rest.entity.OpenAICheckRequest;
import com.box.l10n.mojito.rest.entity.OpenAICheckResponse;
import com.box.l10n.mojito.rest.entity.OpenAICheckResult;
import com.google.common.base.Strings;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

@Configurable
public class OpenAIChecker extends AbstractCliChecker {

  private static final int RETRY_MAX_ATTEMPTS = 10;

  private static final int RETRY_MIN_DURATION_SECONDS = 1;

  private static final int RETRY_MAX_BACKOFF_DURATION_SECONDS = 60;

  static Logger logger = LoggerFactory.getLogger(OpenAIChecker.class);

  RetryBackoffSpec retryConfiguration =
      Retry.backoff(RETRY_MAX_ATTEMPTS, Duration.ofSeconds(RETRY_MIN_DURATION_SECONDS))
          .maxBackoff(Duration.ofSeconds(RETRY_MAX_BACKOFF_DURATION_SECONDS));

  @Autowired OpenAIServiceClient openAIServiceClient;

  @Override
  public CliCheckResult run(List<AssetExtractionDiff> assetExtractionDiffs) {

    logger.debug("Running OpenAI checks");

    List<AssetExtractorTextUnit> textUnits =
        assetExtractionDiffs.stream()
            .flatMap(diff -> diff.getAddedTextunits().stream())
            .collect(toList());

    String repositoryName = cliCheckerOptions.getRepositoryName();

    if (Strings.isNullOrEmpty(repositoryName)) {
      throw new CommandException(
          "Repository name must be provided in checker options when using OpenAI checks.");
    }

    OpenAICheckRequest openAICheckRequest = new OpenAICheckRequest(textUnits, repositoryName);

    return Mono.fromCallable(() -> executeChecks(openAICheckRequest))
        .retryWhen(retryConfiguration)
        .doOnError(ex -> logger.error("Failed to run OpenAI checks: {}", ex.getMessage(), ex))
        .onErrorReturn(getRetriesExhaustedResult())
        .block();
  }

  private CliCheckResult executeChecks(OpenAICheckRequest openAICheckRequest) {
    OpenAICheckResponse response = openAIServiceClient.executeAIChecks(openAICheckRequest);

    if (response.isError()) {
      throw new CommandException("Failed to run OpenAI checks: " + response.getErrorMessage());
    }

    Map<String, List<OpenAICheckResult>> failureMap = new HashMap<>();

    response
        .getResults()
        .forEach(
            (sourceString, openAICheckResults) -> {
              List<OpenAICheckResult> failures =
                  openAICheckResults.stream()
                      .filter(result -> !result.isSuccess())
                      .collect(toList());

              if (!failures.isEmpty()) {
                failureMap.put(sourceString, failures);
              }
            });

    CliCheckResult cliCheckResult = createCliCheckerResult();
    if (!failureMap.isEmpty()) {
      String notificationText = buildNotificationText(failureMap);
      cliCheckResult.setNotificationText(notificationText);
      cliCheckResult.setSuccessful(false);
    }
    return cliCheckResult;
  }

  private String buildNotificationText(Map<String, List<OpenAICheckResult>> failures) {
    StringBuilder notification = new StringBuilder();
    for (String sourceString : failures.keySet()) {
      List<OpenAICheckResult> openAICheckResults = failures.get(sourceString);
      notification.append(buildStringFailureText(sourceString, openAICheckResults));
    }
    return notification.toString();
  }

  private String buildStringFailureText(String sourceString, List<OpenAICheckResult> failures) {
    StringBuilder failureText = new StringBuilder();

    failureText
        .append("The string ")
        .append(QUOTE_MARKER)
        .append(sourceString)
        .append(QUOTE_MARKER)
        .append(" has the following issues:")
        .append(System.lineSeparator());

    for (OpenAICheckResult failure : failures) {
      failureText
          .append(BULLET_POINT)
          .append(failure.getSuggestedFix())
          .append(System.lineSeparator());
    }

    return failureText.toString();
  }

  private CliCheckResult getRetriesExhaustedResult() {
    CliCheckResult cliCheckResult = createCliCheckerResult();
    cliCheckResult.setSuccessful(false);
    if (!Strings.isNullOrEmpty(cliCheckerOptions.getOpenAIErrorMessage())) {
      cliCheckResult.setNotificationText(cliCheckerOptions.getOpenAIErrorMessage());
    } else {
      cliCheckResult.setNotificationText("Failed to run OpenAI checks.");
    }
    return cliCheckResult;
  }
}
