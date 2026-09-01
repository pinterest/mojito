package com.box.l10n.mojito.github;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.apache.commons.codec.binary.Base64;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHPullRequestReviewComment;
import org.kohsuke.github.GHPullRequestReviewEvent;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public class GithubClient {

  /**
   * Maximum allowed Github JWT is 10 minutes
   *
   * @see <a
   *     https://docs.github.com/en/developers/apps/building-github-apps/authenticating-with-github-apps#authenticating-as-a-github-app<a/>
   */
  private static final long MAX_GITHUB_JWT_TTL = TimeUnit.MINUTES.toMillis(10);

  private static final long EXPIRY_REFRESH_THRESHOLD_MS = TimeUnit.SECONDS.toMillis(30);

  private static Logger logger = LoggerFactory.getLogger(GithubClient.class);

  private final String appId;
  private final String owner;
  private final long tokenTTL;
  private final String key;
  private GithubJWT githubJWT;
  private PrivateKey signingKey;
  private final String endpoint;
  protected MeterRegistry meterRegistry;

  protected GHAppInstallationToken githubAppInstallationToken;
  protected GitHub gitHubClient;
  protected int maxRetries;
  protected Duration retryMinBackoff;
  protected Duration retryMaxBackoff;

  public GithubClient(
      String appId,
      String key,
      String owner,
      long tokenTTL,
      String endpoint,
      int maxRetries,
      Duration retryMinBackoff,
      Duration retryMaxBackoff,
      MeterRegistry meterRegistry) {
    this.appId = appId;
    this.key = key;
    if (owner == null || owner.isEmpty()) {
      throw new GithubException(
          "Github integration requires that the 'owner' property is configured for each client.");
    }
    this.owner = owner;
    this.tokenTTL = tokenTTL;
    this.endpoint =
        endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
    this.maxRetries = maxRetries;
    this.retryMinBackoff = retryMinBackoff;
    this.retryMaxBackoff = retryMaxBackoff;
    this.meterRegistry = meterRegistry;
  }

  public GithubClient(String appId, String key, String owner, MeterRegistry meterRegistry) {
    this(
        appId,
        key,
        owner,
        60000L,
        "https://api.github.com",
        3,
        Duration.ofSeconds(5),
        Duration.ofSeconds(60),
        meterRegistry);
  }

  private PrivateKey createPrivateKey(String key)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] encodedKey = Base64.decodeBase64(key);
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(encodedKey);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    return keyFactory.generatePrivate(spec);
  }

  public Mono<GHIssueComment> addCommentToPR(String repository, int prNumber, String comment) {
    String repoFullPath = getRepositoryPath(repository);

    return Mono.fromCallable(
            () ->
                getGithubClient(repository)
                    .getRepository(getRepositoryPath(repository))
                    .getPullRequest(prNumber)
                    .comment(comment))
        .retryWhen(
            Retry.backoff(maxRetries, (retryMinBackoff))
                .maxBackoff(retryMaxBackoff)
                .filter(e -> e instanceof IOException || e instanceof GithubException))
        .doOnError(
            e -> {
              sendRetryExceededMetric(repository, "addCommentToPR");

              logger.error(
                  String.format(
                      "Error adding comment to PR %d in repository '%s': %s",
                      prNumber, repoFullPath, e.getMessage()),
                  e);
            });
  }

  public Mono<GHIssueComment> updateOrAddCommentToPR(
      String repository, int prNumber, String comment, String commentRegex) {
    Pattern commentPattern = Pattern.compile(commentRegex, Pattern.DOTALL);

    return Mono.fromCallable(
            () ->
                this.getGithubClient(repository)
                    .getRepository(this.getRepositoryPath(repository))
                    .getPullRequest(prNumber)
                    .getComments())
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
        .flatMap(
            comments -> {
              Optional<GHIssueComment> existing =
                  comments.stream()
                      .filter(c -> commentPattern.matcher(c.getBody()).matches())
                      .findFirst();
              return existing
                  .map(
                      ghIssueComment ->
                          Mono.fromCallable(
                                  () -> {
                                    ghIssueComment.update(comment);
                                    return ghIssueComment;
                                  })
                              .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()))
                  .orElseGet(
                      () ->
                          this.addCommentToPR(repository, prNumber, comment)
                              .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()));
            })
        .retryWhen(
            Retry.backoff(maxRetries, retryMinBackoff)
                .maxBackoff(retryMaxBackoff)
                .filter(e -> e instanceof IOException || e instanceof GithubException))
        .doOnError(
            e -> {
              sendRetryExceededMetric(repository, "updateOrAddCommentToPR");
              logger.error(
                  String.format(
                      "Error updating/adding a comment to PR %d in repository '%s': %s",
                      prNumber, this.getRepositoryPath(repository), e.getMessage()),
                  e);
            });
  }

  public void addStatusToCommit(
      String repository,
      String commitSha,
      GHCommitState statusState,
      String statusDescription,
      String statusContext,
      String targetUrl) {
    String repoFullPath = getRepositoryPath(repository);

    Mono.fromRunnable(
            () -> {
              try {
                getGithubClient(repository)
                    .getRepository(repoFullPath)
                    .createCommitStatus(
                        commitSha, statusState, targetUrl, statusDescription, statusContext);
              } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                String message =
                    String.format(
                        "Error adding status to commit %s in repository '%s': %s",
                        commitSha, repoFullPath, e.getMessage());
                logger.error(message);
                throw new GithubException(message, e);
              }
            })
        .retryWhen(
            Retry.backoff(maxRetries, (retryMinBackoff))
                .maxBackoff(retryMaxBackoff)
                .filter(e -> e instanceof IOException || e instanceof GithubException))
        .doOnError(
            e -> {
              sendRetryExceededMetric(repository, "addStatusToCommit");
              logger.error(
                  String.format(
                      "Error adding status to commit %s in repository '%s': %s",
                      commitSha, repoFullPath, e.getMessage()),
                  e);
            })
        .block();
  }

  public void addCommentToCommit(String repository, String commitSha1, String comment) {
    String repoFullPath = getRepositoryPath(repository);

    Mono.fromRunnable(
            () -> {
              try {
                getGithubClient(repository)
                    .getRepository(getRepositoryPath(repository))
                    .getCommit(commitSha1)
                    .createComment(comment);
              } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                String message =
                    String.format(
                        "Error adding comment to commit %s in repository '%s': %s",
                        commitSha1, repoFullPath, e.getMessage());
                logger.error(message, e);
                throw new GithubException(message, e);
              }
            })
        .retryWhen(
            Retry.backoff(maxRetries, (retryMinBackoff))
                .maxBackoff(retryMaxBackoff)
                .filter(e -> e instanceof IOException || e instanceof GithubException))
        .doOnError(
            e -> {
              sendRetryExceededMetric(repository, "addCommentToCommit");

              logger.error(
                  String.format(
                      "Error adding comment to commit %s in repository '%s': %s",
                      commitSha1, repoFullPath, e.getMessage()),
                  e);
            })
        .block();
  }

  public String getPRBaseCommit(String repository, int prNumber) {
    String repoFullPath = getRepositoryPath(repository);

    return Mono.fromCallable(
            () ->
                getGithubClient(repository)
                    .getRepository(repoFullPath)
                    .getPullRequest(prNumber)
                    .getBase()
                    .getSha())
        .retryWhen(
            Retry.backoff(maxRetries, (retryMinBackoff))
                .maxBackoff(retryMaxBackoff)
                .filter(e -> e instanceof IOException || e instanceof GithubException))
        .doOnError(
            e -> {
              sendRetryExceededMetric(repository, "getPRBaseCommit");
              logger.error(
                  String.format(
                      "Error retrieving base commit for PR %d in repository '%s': %s",
                      prNumber, repoFullPath, e.getMessage()),
                  e);
            })
        .onErrorReturn("")
        .block();
  }

  public Map<String, String> getPrFilePatches(String repository, int prNumber) {
    String repoFullPath = getRepositoryPath(repository);

    Mono<Map<String, String>> stringMono =
        Mono.fromCallable(
            () -> {
              GHPullRequest pr =
                  getGithubClient(repository).getRepository(repoFullPath).getPullRequest(prNumber);

              Map<String, String> patches = new HashMap<>();
              for (GHPullRequestFileDetail changedFile : pr.listFiles()) {
                logger.debug("Processing file diff: {}", changedFile.getFilename());
                patches.put(changedFile.getFilename(), changedFile.getPatch());
              }
              return patches;
            });
    stringMono.retryWhen(
        Retry.backoff(maxRetries, (retryMinBackoff))
            .maxBackoff(retryMaxBackoff)
            .filter(e -> e instanceof IOException || e instanceof GithubException));
    stringMono.doOnError(
        e ->
            logger.error(
                "Error retrieving PR files patches for PR {} in repository '{}': {}",
                prNumber,
                repoFullPath,
                e.getMessage(),
                e));
    stringMono.onErrorReturn(new HashMap<>());
    return stringMono.block();
  }

  public String getPRAuthorEmail(String repository, int prNumber) {
    String repoFullPath = getRepositoryPath(repository);

    return Mono.fromCallable(
            () ->
                getGithubClient(repository)
                    .getRepository(repoFullPath)
                    .getPullRequest(prNumber)
                    .getUser()
                    .getEmail())
        .retryWhen(
            Retry.backoff(maxRetries, (retryMinBackoff))
                .maxBackoff(retryMaxBackoff)
                .filter(e -> e instanceof IOException || e instanceof GithubException))
        .doOnError(
            e -> {
              sendRetryExceededMetric(repository, "updateOrAddCommentToPR");
              logger.error(
                  String.format(
                      "Error getting author email for PR %d in repository '%s': %s",
                      prNumber, repoFullPath, e.getMessage()),
                  e);
            })
        .onErrorReturn("")
        .block();
  }

  public void addLabelToPR(String repository, int prNumber, String labelName) {
    String repoFullPath = getRepositoryPath(repository);

    Mono.fromRunnable(
            () -> {
              try {
                getGithubClient(repository)
                    .getRepository(repoFullPath)
                    .getPullRequest(prNumber)
                    .addLabels(labelName);
              } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                String message =
                    String.format(
                        "Error adding label '%s' to PR %d in repository '%s': %s",
                        labelName, prNumber, repoFullPath, e.getMessage());
                logger.error(message, e);
                throw new GithubException(message, e);
              }
            })
        .retryWhen(
            Retry.backoff(maxRetries, (retryMinBackoff))
                .maxBackoff(retryMaxBackoff)
                .filter(e -> e instanceof IOException || e instanceof GithubException))
        .doOnError(
            e -> {
              sendRetryExceededMetric(repository, "addLabelToPR");
              logger.error(
                  String.format(
                      "Error adding label '%s' to PR %d in repository '%s': %s",
                      labelName, prNumber, repoFullPath, e.getMessage()),
                  e);
            })
        .block();
  }

  public void removeLabelFromPR(String repository, int prNumber, String labelName) {
    String repoFullPath = getRepositoryPath(repository);

    Mono.fromRunnable(
            () -> {
              try {
                getGithubClient(repository)
                    .getRepository(repoFullPath)
                    .getPullRequest(prNumber)
                    .removeLabel(labelName);
              } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                String message =
                    String.format(
                        "Error removing label '%s' from PR %d in repository '%s': %s",
                        labelName, prNumber, repoFullPath, e.getMessage());
                logger.error(message, e);
                throw new GithubException(message, e);
              }
            })
        .retryWhen(
            Retry.backoff(maxRetries, (retryMinBackoff))
                .maxBackoff(retryMaxBackoff)
                .filter(e -> e instanceof IOException || e instanceof GithubException))
        .doOnError(
            e -> {
              sendRetryExceededMetric(repository, "removeLabelFromPR");
              logger.error(
                  String.format(
                      "Error removing label '%s' from PR %d in repository '%s': %s",
                      labelName, prNumber, repoFullPath, e.getMessage()),
                  e);
            })
        .block();
  }

  public boolean isLabelAppliedToPR(String repository, int prNumber, String labelName) {
    String repoFullPath = getRepositoryPath(repository);

    return Mono.fromCallable(
            () ->
                getGithubClient(repository)
                    .getRepository(repoFullPath)
                    .getPullRequest(prNumber)
                    .getLabels()
                    .stream()
                    .anyMatch(ghLabel -> ghLabel.getName().equals(labelName)))
        .retryWhen(
            Retry.backoff(maxRetries, (retryMinBackoff))
                .maxBackoff(retryMaxBackoff)
                .filter(e -> e instanceof IOException || e instanceof GithubException))
        .doOnError(
            e -> {
              sendRetryExceededMetric(repository, "isLabelAppliedToPR");
              logger.error(
                  String.format(
                      "Error reading labels for PR %d in repository '%s' : '%s'",
                      prNumber, repoFullPath, e.getMessage()),
                  e);
            })
        .onErrorReturn(false)
        .block();
  }

  public List<GHIssueComment> getPRComments(String repository, int prNumber) {
    String repoFullPath = getRepositoryPath(repository);

    return Mono.fromCallable(
            () ->
                getGithubClient(repository)
                    .getRepository(repoFullPath)
                    .getPullRequest(prNumber)
                    .getComments())
        .retryWhen(
            Retry.backoff(maxRetries, (retryMinBackoff))
                .maxBackoff(retryMaxBackoff)
                .filter(
                    e ->
                        e instanceof IOException
                            || e instanceof GithubException
                            || e instanceof HttpException))
        .doOnError(
            e -> {
              sendRetryExceededMetric(repository, "getPRComments");
              logger.error(
                  String.format(
                      "Error retrieving comments for PR %d in repository '%s': %s",
                      prNumber, repoFullPath, e.getMessage()),
                  e);
            })
        .onErrorReturn(List.of())
        .block();
  }

  public String getOwner() {
    return owner;
  }

  public String getAppId() {
    return appId;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public boolean shouldRefreshToken(long tokenExpires, long now) {
    long refreshDeadlineMs = tokenExpires - EXPIRY_REFRESH_THRESHOLD_MS;
    return now >= refreshDeadlineMs;
  }

  public GHAppInstallationToken getGithubAppInstallationToken(String repository)
      throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    if (githubAppInstallationToken == null
        || shouldRefreshToken(
            githubAppInstallationToken.getExpiresAt().getTime(), System.currentTimeMillis())) {
      GitHub gitHub =
          new GitHubBuilder()
              .withEndpoint(getEndpoint())
              .withJwtToken(getGithubJWT(tokenTTL).getToken())
              .build();
      githubAppInstallationToken =
          gitHub.getApp().getInstallationByRepository(owner, repository).createToken().create();
    }
    return githubAppInstallationToken;
  }

  protected GitHub createGithubClient(String repository)
      throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    logger.debug("Creating GithubClient for repository: {}", repository);
    GitHubBuilder builder =
        new GitHubBuilder()
            .withEndpoint(getEndpoint())
            .withAppInstallationToken(getGithubAppInstallationToken(repository).getToken());

    if (meterRegistry != null) {
      logger.debug("Using MeterRegistry for GithubClient: {}", meterRegistry);
      builder = builder.withRateLimitChecker(new GithubRateLimitChecker(meterRegistry));
    }

    logger.debug("GithubClient Configured");
    return builder.build();
  }

  private String getRepositoryPath(String repository) {
    return owner != null && !owner.isEmpty() ? owner + "/" + repository : repository;
  }

  private GithubJWT getGithubJWT(long ttlMillis)
      throws NoSuchAlgorithmException, InvalidKeySpecException {

    Date now = new Date(System.currentTimeMillis());

    if (githubJWT != null
        && !shouldRefreshToken(githubJWT.getExpiryTime().getTime(), now.getTime())) {
      return githubJWT;
    } else {
      githubJWT = createGithubJWT(ttlMillis, now);
    }
    return githubJWT;
  }

  private GithubJWT createGithubJWT(long ttlMillis, Date now)
      throws NoSuchAlgorithmException, InvalidKeySpecException {

    JwtBuilder builder =
        Jwts.builder()
            .setIssuedAt(now)
            .setIssuer(appId)
            .signWith(getSigningKey(), SignatureAlgorithm.RS256);

    Date expiry = new Date(now.getTime() + ttlMillis);
    if (ttlMillis > MAX_GITHUB_JWT_TTL) {
      long expMillis = now.getTime() + MAX_GITHUB_JWT_TTL;
      expiry = new Date(expMillis);
    }
    builder.setExpiration(expiry);

    return new GithubJWT(builder.compact(), expiry);
  }

  protected PrivateKey getSigningKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
    if (signingKey == null) {
      signingKey = createPrivateKey(key);
    }
    return signingKey;
  }

  private GitHub getGithubClient(String repository)
      throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    if (gitHubClient == null || !gitHubClient.isCredentialValid()) {
      gitHubClient = createGithubClient(repository);
    }
    return gitHubClient;
  }

  private void sendRetryExceededMetric(String repository, String operationName) {
    meterRegistry
        .counter(
            "Mojito.GitHubClient.RetriesExhausted",
            "repository",
            repository,
            "operation",
            operationName)
        .increment();
  }

  /**
   * Filters out the review comments that would duplicate a comment already posted on the pull
   * request, ie. an existing review comment with the same body on the same file and line, as well
   * as duplicates within the given list.
   *
   * <p>Both the current and the original line of the existing comments are considered: when a
   * comment becomes outdated GitHub stops reporting a current line, in which case the original line
   * is the only reference available to detect the duplicate.
   *
   * @param pullRequest the pull request the comments are about to be posted to
   * @param reviewComments the comments to post
   * @param repository the repository name, used for metrics
   * @return the comments that are not already present on the pull request, in the original order
   */
  private List<ReviewComment> removeAlreadyPostedComments(
      GHPullRequest pullRequest, List<ReviewComment> reviewComments, String repository)
      throws IOException {

    Set<ReviewCommentKey> existingCommentKeys = new HashSet<>();
    for (GHPullRequestReviewComment existingComment : pullRequest.listReviewComments().toList()) {
      String body = existingComment.getBody();
      String path = existingComment.getPath();
      if (existingComment.getLine() > 0) {
        existingCommentKeys.add(new ReviewCommentKey(path, existingComment.getLine(), body));
      }
      if (existingComment.getOriginalLine() > 0) {
        existingCommentKeys.add(
            new ReviewCommentKey(path, existingComment.getOriginalLine(), body));
      }
    }

    List<ReviewComment> commentsToPost = new ArrayList<>();
    Set<ReviewCommentKey> seenCommentKeys = new HashSet<>();
    for (ReviewComment comment : reviewComments) {
      ReviewCommentKey key =
          new ReviewCommentKey(comment.getPath(), comment.getLine(), comment.getBody());
      if (existingCommentKeys.contains(key) || !seenCommentKeys.add(key)) {
        logger.debug(
            "Skipping review comment for {}:{} in repository '{}', the same comment already exists",
            comment.getPath(),
            comment.getLine(),
            repository);
        continue;
      }
      commentsToPost.add(comment);
    }

    int skippedCommentCount = reviewComments.size() - commentsToPost.size();
    if (skippedCommentCount > 0) {
      meterRegistry
          .counter("Mojito.GitHubClient.DuplicatedReviewCommentsSkipped", "repository", repository)
          .increment(skippedCommentCount);
      logger.info(
          "Skipping {} of {} review comments for PR {} in repository '{}', the same comments"
              + " already exist on the same lines",
          skippedCommentCount,
          reviewComments.size(),
          pullRequest.getNumber(),
          repository);
    }

    return commentsToPost;
  }

  /**
   * Posts review comments to a pull request. These are inline comments on specific lines of code.
   *
   * <p>Comments that are already present on the pull request, ie. an existing review comment with
   * the same body on the same file and line, are skipped so that re-running the checks on a pull
   * request does not create duplicated comments. Duplicates within the provided list are skipped
   * too.
   *
   * @param repository The repository name
   * @param prNumber The pull request number
   * @param reviewComments List of review comments to post
   * @param commitSha The commit SHA to attach the review to
   */
  public void addReviewCommentsToPR(
      String repository, int prNumber, List<ReviewComment> reviewComments, String commitSha) {
    String repoFullPath = getRepositoryPath(repository);

    if (reviewComments == null || reviewComments.isEmpty()) {
      logger.debug(
          "No review comments to post for PR {} in repository '{}'", prNumber, repoFullPath);
      return;
    }

    Mono.fromRunnable(
            () -> {
              try {
                GHPullRequest pullRequest =
                    getGithubClient(repository)
                        .getRepository(repoFullPath)
                        .getPullRequest(prNumber);

                List<ReviewComment> commentsToPost =
                    removeAlreadyPostedComments(pullRequest, reviewComments, repository);

                if (commentsToPost.isEmpty()) {
                  logger.info(
                      "All {} review comments are already present on PR {} in repository '{}',"
                          + " nothing to post",
                      reviewComments.size(),
                      prNumber,
                      repoFullPath);
                  return;
                }

                // Create a review with comments. The event must be set: leaving it blank creates
                // the review in the PENDING state, ie. a draft that is only visible to the
                // identity that created it and that still needs to be submitted, so the comments
                // would never be published on the pull request.
                var reviewBuilder =
                    pullRequest
                        .createReview()
                        .commitId(commitSha)
                        .event(GHPullRequestReviewEvent.COMMENT)
                        .body("I18N source string validation findings:");

                for (ReviewComment comment : commentsToPost) {
                  reviewBuilder.singleLineComment(
                      comment.getBody(), comment.getPath(), comment.getLine());
                }

                reviewBuilder.create();

                logger.info(
                    "Successfully posted {} review comments to PR {} in repository '{}'",
                    commentsToPost.size(),
                    prNumber,
                    repoFullPath);

              } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                String message =
                    String.format(
                        "Error adding review comments to PR %d in repository '%s': %s",
                        prNumber, repoFullPath, e.getMessage());
                logger.error(message, e);
                throw new GithubException(message, e);
              }
            })
        .retryWhen(
            Retry.backoff(maxRetries, (retryMinBackoff))
                .maxBackoff(retryMaxBackoff)
                .filter(e -> e instanceof IOException || e instanceof GithubException))
        .doOnError(
            e -> {
              sendRetryExceededMetric(repository, "addReviewCommentsToPR");
              logger.error(
                  String.format(
                      "Error adding review comments to PR %d in repository '%s': %s",
                      prNumber, repoFullPath, e.getMessage()),
                  e);
            })
        .block();
  }

  /** Identifies a review comment by its location and content, used to detect duplicates */
  private record ReviewCommentKey(String path, int line, String body) {
    ReviewCommentKey(String path, int line, String body) {
      this.path = path;
      this.line = line;
      // GitHub can return the body with normalized line endings, ignore them when comparing
      this.body = body == null ? null : body.replace("\r\n", "\n").strip();
    }
  }

  /** Data class representing a pull request review comment */
  public static class ReviewComment {
    private final String body;
    private final String path;
    private final int line;

    public ReviewComment(String body, String path, int line) {
      this.body = body;
      this.path = path;
      this.line = line;
    }

    public String getBody() {
      return body;
    }

    public String getPath() {
      return path;
    }

    public int getLine() {
      return line;
    }
  }
}
