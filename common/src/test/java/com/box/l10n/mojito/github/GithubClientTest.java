package com.box.l10n.mojito.github;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHPullRequestReviewBuilder;
import org.kohsuke.github.GHPullRequestReviewComment;
import org.kohsuke.github.GHPullRequestReviewEvent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.PagedIterator;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {GithubClientTest.class, GithubClientTest.TestConfig.class})
@EnableConfigurationProperties
public class GithubClientTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired(required = false)
  GithubClient githubClient;

  @Autowired TestConfig testConfig;

  @Mock GitHub gitHubMock;

  @Mock GHAppInstallationToken ghAppInstallationTokenMock;

  @Mock GHRepository ghRepoMock;

  @Mock GHPullRequest ghPullRequestMock;

  @Mock GHPullRequestReviewBuilder ghPullRequestReviewBuilderMock;

  @Mock GHCommit ghCommitMock;

  @Mock GHCommitPointer ghCommitPointerMock;

  @Mock GHUser ghUserMock;

  @Mock GHIssueComment ghCommentMock1;

  @Mock GHIssueComment ghCommentMock2;

  @Mock MeterRegistry meterRegistryMock;

  @Mock Counter counterMock;

  @Mock RestTemplate restTemplateMock;

  @Before
  public void setup() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    Assume.assumeNotNull(githubClient);
    githubClient.gitHubClient = gitHubMock;
    githubClient.githubAppInstallationToken = ghAppInstallationTokenMock;
    githubClient.restTemplate = restTemplateMock;
    githubClient.maxRetries = 3;
    githubClient.retryMinBackoff = Duration.ofMillis(1);
    githubClient.retryMaxBackoff = Duration.ofMillis(10);
    when(this.meterRegistryMock.counter(
            anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(this.counterMock);
    when(this.meterRegistryMock.counter(anyString(), anyString(), anyString()))
        .thenReturn(this.counterMock);
    githubClient.meterRegistry = meterRegistryMock;
    Mockito.reset(
        githubClient,
        gitHubMock,
        ghRepoMock,
        ghPullRequestMock,
        ghCommitMock,
        ghCommitPointerMock,
        ghUserMock);
    // Keep the installation token valid so that it is never refreshed, which would reach out to
    // Github
    when(ghAppInstallationTokenMock.getExpiresAt())
        .thenReturn(new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)));
    when(ghAppInstallationTokenMock.getToken()).thenReturn("installationToken");
    when(gitHubMock.isCredentialValid()).thenReturn(true);
    when(gitHubMock.getRepository(isA(String.class))).thenReturn(ghRepoMock);
    when(ghRepoMock.getPullRequest(isA(Integer.class))).thenReturn(ghPullRequestMock);
    when(ghRepoMock.getCommit(isA(String.class))).thenReturn(ghCommitMock);
    when(ghPullRequestMock.getBase()).thenReturn(ghCommitPointerMock);
    when(ghCommitPointerMock.getSha()).thenReturn("mockSha");
    when(ghPullRequestMock.getUser()).thenReturn(ghUserMock);
    when(ghUserMock.getEmail()).thenReturn("some@email.com");
    when(this.ghCommentMock1.getBody()).thenReturn("Test comment 1");
    when(this.ghCommentMock2.getBody()).thenReturn("Test 2");
    when(this.ghPullRequestMock.getComments())
        .thenReturn(Arrays.asList(this.ghCommentMock1, this.ghCommentMock2));

    GHPullRequestFileDetail file1 = Mockito.mock(GHPullRequestFileDetail.class);
    GHPullRequestFileDetail file2 = Mockito.mock(GHPullRequestFileDetail.class);
    List<GHPullRequestFileDetail> mockFiles = Arrays.asList(file1, file2);
    PagedIterable<GHPullRequestFileDetail> pagedIterableMock = Mockito.mock(PagedIterable.class);
    when(pagedIterableMock.asList()).thenReturn(mockFiles);
    PagedIterator<GHPullRequestFileDetail> pagedIteratorMock = Mockito.mock(PagedIterator.class);
    when(pagedIteratorMock.hasNext()).thenReturn(true, true, false);
    when(pagedIteratorMock.next()).thenReturn(file1, file2);
    when(pagedIterableMock.iterator()).thenReturn(pagedIteratorMock);
    when(ghPullRequestMock.listFiles()).thenReturn(pagedIterableMock);

    stubExistingReviewComments(List.of());
  }

  @Test
  public void testGetPRBaseCommit() throws IOException {
    assertEquals("mockSha", githubClient.getPRBaseCommit("testRepo", 1));
    verify(gitHubMock, times(1)).getRepository("testOwner/testRepo");
    verify(ghRepoMock, times(1)).getPullRequest(1);
  }

  @Test
  public void testAddCommentToPR() throws IOException {
    Mono<GHIssueComment> ghIssueCommentMono =
        githubClient.addCommentToPR("testRepo", 1, "Test comment");
    ghIssueCommentMono.block();
    verify(gitHubMock, times(1)).getRepository("testOwner/testRepo");
    verify(ghRepoMock, times(1)).getPullRequest(1);
    verify(ghPullRequestMock, times(1)).comment("Test comment");
  }

  @Test
  public void testUpdateOrAddCommentToPRWhenUpdatingComment() throws IOException {
    Mono<GHIssueComment> ghIssueCommentMono =
        this.githubClient.updateOrAddCommentToPR(
            "testRepo", 1, "Test comment", "[a-zA-Z]+\\s[\\d].*");
    ghIssueCommentMono.block();
    verify(this.gitHubMock, times(1)).getRepository("testOwner/testRepo");
    verify(this.ghRepoMock, times(1)).getPullRequest(1);
    verify(this.ghPullRequestMock, times(1)).getComments();
    verify(this.ghCommentMock1, times(1)).getBody();
    verify(this.ghCommentMock2, times(1)).getBody();
    verify(this.ghCommentMock1, times(0)).update("Test comment");
    verify(this.ghCommentMock2, times(1)).update("Test comment");
  }

  @Test
  public void testUpdateOrAddCommentToPRWhenAddingComment() throws IOException {
    Mono<GHIssueComment> ghIssueCommentMono =
        this.githubClient.updateOrAddCommentToPR(
            "testRepo", 1, "Test comment", "[a-z]+\\s[\\d]{2}.*");
    ghIssueCommentMono.block();
    verify(this.gitHubMock, times(2)).getRepository("testOwner/testRepo");
    verify(this.ghRepoMock, times(2)).getPullRequest(1);
    verify(this.ghPullRequestMock, times(1)).getComments();
    verify(this.ghCommentMock1, times(1)).getBody();
    verify(this.ghCommentMock2, times(1)).getBody();
    verify(this.ghCommentMock1, times(0)).update("Test comment");
    verify(this.ghCommentMock2, times(0)).update("Test comment");
  }

  @Test
  public void testAddCommentToCommit() throws IOException {
    githubClient.addCommentToCommit("testRepo", "shatest", "Test comment");
    verify(gitHubMock, times(1)).getRepository("testOwner/testRepo");
    verify(ghRepoMock, times(1)).getCommit("shatest");
    verify(ghCommitMock, times(1)).createComment("Test comment");
  }

  @Test
  public void testGetAuthorEmail() throws IOException {
    assertEquals("some@email.com", githubClient.getPRAuthorEmail("testRepo", 1));
    verify(gitHubMock, times(1)).getRepository("testOwner/testRepo");
    verify(ghRepoMock, times(1)).getPullRequest(1);
    verify(ghPullRequestMock, times(1)).getUser();
    verify(ghUserMock, times(1)).getEmail();
  }

  @Test
  public void testAddLabelToPR() throws IOException {
    githubClient.addLabelToPR("testRepo", 1, "translations-needed");
    verify(gitHubMock, times(1)).getRepository("testOwner/testRepo");
    verify(ghRepoMock, times(1)).getPullRequest(1);
    verify(ghPullRequestMock, times(1)).addLabels("translations-needed");
  }

  @Test
  public void testRemoveLabelFromPR() throws IOException {
    githubClient.removeLabelFromPR("testRepo", 1, "translations-needed");
    verify(gitHubMock, times(1)).getRepository("testOwner/testRepo");
    verify(ghRepoMock, times(1)).getPullRequest(1);
    verify(ghPullRequestMock, times(1)).removeLabel("translations-needed");
  }

  @Test
  public void testGetPRComments() throws IOException {
    List<GHIssueComment> comments = Lists.newArrayList(new GHIssueComment(), new GHIssueComment());
    when(ghPullRequestMock.getComments()).thenReturn(comments);

    assertEquals(comments, githubClient.getPRComments("testRepo", 1));
    verify(gitHubMock, times(1)).getRepository("testOwner/testRepo");
    verify(ghRepoMock, times(1)).getPullRequest(1);
    verify(ghPullRequestMock, times(1)).getComments();
  }

  @Test
  public void testClientRefreshWhenCredsInvalid()
      throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    assertEquals("mockSha", githubClient.getPRBaseCommit("testRepo", 1));
    when(gitHubMock.isCredentialValid()).thenReturn(false);
    doReturn(gitHubMock).when(githubClient).createGithubClient(isA(String.class));
    assertEquals("mockSha", githubClient.getPRBaseCommit("testRepo", 1));
    verify(githubClient, times(1)).createGithubClient("testRepo");
  }

  @Test
  public void testRetryLogicForAddCommentToPR() throws IOException {
    when(gitHubMock.getRepository(isA(String.class)))
        .thenThrow(new IOException("network issue"))
        .thenThrow(new IOException("network issue"))
        .thenReturn(ghRepoMock);

    Mono<GHIssueComment> ghIssueCommentMono =
        githubClient.addCommentToPR("testRepo", 1, "Test comment");
    ghIssueCommentMono.block();

    verify(gitHubMock, times(3)).getRepository("testOwner/testRepo");
    verify(ghPullRequestMock, times(1)).comment("Test comment");
  }

  @Test
  public void testAddLabelToPRWithRetry() throws IOException {

    when(gitHubMock.getRepository(isA(String.class)))
        .thenThrow(new IOException("network issue"))
        .thenThrow(new IOException("network issue"))
        .thenReturn(ghRepoMock);

    githubClient.addLabelToPR("testRepo", 1, "new-label");

    verify(gitHubMock, times(3)).getRepository("testOwner/testRepo");
    verify(ghPullRequestMock, times(1)).addLabels("new-label");
  }

  @Test
  public void testIsLabelAppliedToPRWithRetry() throws IOException {
    GHLabel ghLabelMock = Mockito.mock(GHLabel.class);
    when(ghLabelMock.getName()).thenReturn("bug");
    when(ghPullRequestMock.getLabels())
        .thenThrow(new GithubException("some error"))
        .thenReturn(List.of(ghLabelMock));
    assertTrue(githubClient.isLabelAppliedToPR("testOwner/testRepo", 1, "bug"));
  }

  @Test
  public void testgetPrFilePatches() throws IOException {
    this.githubClient.getPrFilePatches("testRepo", 1);
    verify(this.gitHubMock, times(1)).getRepository("testOwner/testRepo");
    verify(this.ghRepoMock, times(1)).getPullRequest(1);
    verify(this.ghPullRequestMock, times(1)).listFiles();
  }

  @Test
  public void testAddReviewCommentsToPR() throws IOException {
    stubReviewBuilder();

    githubClient.addReviewCommentsToPR(
        "testRepo",
        1,
        List.of(new GithubClient.ReviewComment("Placeholder issue", "src/main/strings.xml", 42)),
        "commitSha");

    verify(gitHubMock, times(1)).getRepository("testOwner/testRepo");
    verify(ghRepoMock, times(1)).getPullRequest(1);

    InOrder inOrder = inOrder(ghPullRequestMock, ghPullRequestReviewBuilderMock);
    inOrder.verify(ghPullRequestMock, times(1)).createReview();
    inOrder.verify(ghPullRequestReviewBuilderMock, times(1)).commitId("commitSha");
    inOrder
        .verify(ghPullRequestReviewBuilderMock, times(1))
        .event(GHPullRequestReviewEvent.COMMENT);
    inOrder
        .verify(ghPullRequestReviewBuilderMock, times(1))
        .body("I18N source string validation findings:");
    inOrder
        .verify(ghPullRequestReviewBuilderMock, times(1))
        .singleLineComment("Placeholder issue", "src/main/strings.xml", 42);
    inOrder.verify(ghPullRequestReviewBuilderMock, times(1)).create();
  }

  @Test
  public void testAddReviewCommentsToPRSubmitsTheReviewSoThatCommentsArePublished()
      throws IOException {
    stubReviewBuilder();

    githubClient.addReviewCommentsToPR(
        "testRepo",
        1,
        List.of(new GithubClient.ReviewComment("Placeholder issue", "src/main/strings.xml", 42)),
        "commitSha");

    // Without an event the review is created as a PENDING draft: GitHub accepts the call but the
    // comments are only visible to the identity that created it, until the review is submitted
    verify(ghPullRequestReviewBuilderMock, times(1)).event(GHPullRequestReviewEvent.COMMENT);
    verify(ghPullRequestReviewBuilderMock, never()).event(GHPullRequestReviewEvent.PENDING);
    verify(ghPullRequestReviewBuilderMock, times(1)).create();
  }

  @Test
  public void testAddReviewCommentsToPRWithMultipleComments() throws IOException {
    stubReviewBuilder();

    githubClient.addReviewCommentsToPR(
        "testRepo",
        1,
        List.of(
            new GithubClient.ReviewComment("Comment 1", "src/main/strings.xml", 10),
            new GithubClient.ReviewComment("Comment 2", "src/main/strings.xml", 20),
            new GithubClient.ReviewComment("Comment 3", "src/main/other.xml", 30)),
        "commitSha");

    verify(ghPullRequestMock, times(1)).createReview();
    verify(ghPullRequestReviewBuilderMock, times(1))
        .singleLineComment("Comment 1", "src/main/strings.xml", 10);
    verify(ghPullRequestReviewBuilderMock, times(1))
        .singleLineComment("Comment 2", "src/main/strings.xml", 20);
    verify(ghPullRequestReviewBuilderMock, times(1))
        .singleLineComment("Comment 3", "src/main/other.xml", 30);
    // A single review is created, holding all the comments
    verify(ghPullRequestReviewBuilderMock, times(1)).create();
  }

  @Test
  public void testAddReviewCommentsToPRWithNullComments() throws IOException {
    List<GithubClient.ReviewComment> postedComments =
        githubClient.addReviewCommentsToPR("testRepo", 1, null, "commitSha");

    assertTrue(postedComments.isEmpty());
    verify(gitHubMock, never()).getRepository(anyString());
    verify(ghPullRequestMock, never()).createReview();
  }

  @Test
  public void testAddReviewCommentsToPRWithEmptyComments() throws IOException {
    List<GithubClient.ReviewComment> postedComments =
        githubClient.addReviewCommentsToPR("testRepo", 1, List.of(), "commitSha");

    assertTrue(postedComments.isEmpty());
    verify(gitHubMock, never()).getRepository(anyString());
    verify(ghPullRequestMock, never()).createReview();
  }

  @Test
  public void testAddReviewCommentsToPRWithRetry() throws IOException {
    stubReviewBuilder();
    when(gitHubMock.getRepository(isA(String.class)))
        .thenThrow(new IOException("network issue"))
        .thenThrow(new IOException("network issue"))
        .thenReturn(ghRepoMock);

    githubClient.addReviewCommentsToPR(
        "testRepo",
        1,
        List.of(new GithubClient.ReviewComment("Placeholder issue", "src/main/strings.xml", 42)),
        "commitSha");

    verify(gitHubMock, times(3)).getRepository("testOwner/testRepo");
    verify(ghPullRequestReviewBuilderMock, times(1)).create();
  }

  @Test
  public void testAddReviewCommentsToPR_ThrowsExceptionWhenRetriesExhausted() throws IOException {
    stubReviewBuilder();
    when(ghPullRequestReviewBuilderMock.create()).thenThrow(new IOException("network issue"));

    assertThrows(
        IllegalStateException.class,
        () ->
            githubClient.addReviewCommentsToPR(
                "testRepo",
                1,
                List.of(
                    new GithubClient.ReviewComment(
                        "Placeholder issue", "src/main/strings.xml", 42)),
                "commitSha"));

    // One initial attempt plus maxRetries
    verify(ghPullRequestReviewBuilderMock, times(4)).create();
    verify(meterRegistryMock, times(1))
        .counter(
            "Mojito.GitHubClient.RetriesExhausted",
            "repository",
            "testRepo",
            "operation",
            "addReviewCommentsToPR");
    verify(counterMock, times(1)).increment();
  }

  @Test
  public void testAddReviewCommentsToPRSkipsCommentAlreadyOnTheSameLine() throws IOException {
    stubReviewBuilder();
    stubExistingReviewComments(
        List.of(stubExistingReviewComment("Placeholder issue", "src/main/strings.xml", 42, 42)));

    githubClient.addReviewCommentsToPR(
        "testRepo",
        1,
        List.of(
            new GithubClient.ReviewComment("Placeholder issue", "src/main/strings.xml", 42),
            new GithubClient.ReviewComment("Another issue", "src/main/strings.xml", 42)),
        "commitSha");

    verify(ghPullRequestReviewBuilderMock, never())
        .singleLineComment("Placeholder issue", "src/main/strings.xml", 42);
    verify(ghPullRequestReviewBuilderMock, times(1))
        .singleLineComment("Another issue", "src/main/strings.xml", 42);
    verify(ghPullRequestReviewBuilderMock, times(1)).create();
    verify(meterRegistryMock, times(1))
        .counter("Mojito.GitHubClient.DuplicatedReviewCommentsSkipped", "repository", "testRepo");
    verify(counterMock, times(1)).increment(1);
  }

  @Test
  public void testAddReviewCommentsToPRPostsCommentWhenExistingOneIsOnAnotherLineOrFile()
      throws IOException {
    stubReviewBuilder();
    stubExistingReviewComments(
        List.of(
            stubExistingReviewComment("Placeholder issue", "src/main/strings.xml", 41, 41),
            stubExistingReviewComment("Placeholder issue", "src/main/other.xml", 42, 42)));

    githubClient.addReviewCommentsToPR(
        "testRepo",
        1,
        List.of(new GithubClient.ReviewComment("Placeholder issue", "src/main/strings.xml", 42)),
        "commitSha");

    verify(ghPullRequestReviewBuilderMock, times(1))
        .singleLineComment("Placeholder issue", "src/main/strings.xml", 42);
    verify(ghPullRequestReviewBuilderMock, times(1)).create();
  }

  @Test
  public void testAddReviewCommentsToPRPostsCommentWhenExistingOneHasAnotherBody()
      throws IOException {
    stubReviewBuilder();
    stubExistingReviewComments(
        List.of(stubExistingReviewComment("Another issue", "src/main/strings.xml", 42, 42)));

    githubClient.addReviewCommentsToPR(
        "testRepo",
        1,
        List.of(new GithubClient.ReviewComment("Placeholder issue", "src/main/strings.xml", 42)),
        "commitSha");

    verify(ghPullRequestReviewBuilderMock, times(1))
        .singleLineComment("Placeholder issue", "src/main/strings.xml", 42);
    verify(ghPullRequestReviewBuilderMock, times(1)).create();
  }

  @Test
  public void testAddReviewCommentsToPRSkipsCommentMatchingOutdatedExistingComment()
      throws IOException {
    stubReviewBuilder();
    // An outdated comment has no current line, only the original one it was posted on
    stubExistingReviewComments(
        List.of(stubExistingReviewComment("Placeholder issue", "src/main/strings.xml", 0, 42)));

    githubClient.addReviewCommentsToPR(
        "testRepo",
        1,
        List.of(new GithubClient.ReviewComment("Placeholder issue", "src/main/strings.xml", 42)),
        "commitSha");

    verify(ghPullRequestMock, never()).createReview();
    verify(ghPullRequestReviewBuilderMock, never()).create();
  }

  @Test
  public void testAddReviewCommentsToPRSkipsDuplicatesWithinTheProvidedComments()
      throws IOException {
    stubReviewBuilder();

    githubClient.addReviewCommentsToPR(
        "testRepo",
        1,
        List.of(
            new GithubClient.ReviewComment("Placeholder issue", "src/main/strings.xml", 42),
            new GithubClient.ReviewComment("Placeholder issue", "src/main/strings.xml", 42)),
        "commitSha");

    verify(ghPullRequestReviewBuilderMock, times(1))
        .singleLineComment("Placeholder issue", "src/main/strings.xml", 42);
    verify(ghPullRequestReviewBuilderMock, times(1)).create();
  }

  @Test
  public void testAddReviewCommentsToPRSkipsReviewWhenAllCommentsAlreadyExist() throws IOException {
    stubReviewBuilder();
    stubExistingReviewComments(
        List.of(
            stubExistingReviewComment("Comment 1", "src/main/strings.xml", 10, 10),
            stubExistingReviewComment("Comment 2", "src/main/other.xml", 20, 20)));

    githubClient.addReviewCommentsToPR(
        "testRepo",
        1,
        List.of(
            new GithubClient.ReviewComment("Comment 1", "src/main/strings.xml", 10),
            new GithubClient.ReviewComment("Comment 2", "src/main/other.xml", 20)),
        "commitSha");

    verify(ghPullRequestMock, never()).createReview();
    verify(ghPullRequestReviewBuilderMock, never()).create();
    verify(counterMock, times(1)).increment(2);
  }

  @Test
  public void testAddReviewCommentsToPRReturnsThePostedComments() throws IOException {
    stubReviewBuilder();
    GithubClient.ReviewComment comment1 =
        new GithubClient.ReviewComment("Comment 1", "src/main/strings.xml", 10);
    GithubClient.ReviewComment comment2 =
        new GithubClient.ReviewComment("Comment 2", "src/main/other.xml", 20);

    List<GithubClient.ReviewComment> postedComments =
        githubClient.addReviewCommentsToPR("testRepo", 1, List.of(comment1, comment2), "commitSha");

    assertEquals(List.of(comment1, comment2), postedComments);
  }

  @Test
  public void testAddReviewCommentsToPRReturnsOnlyTheCommentsThatWereNotSkipped()
      throws IOException {
    stubReviewBuilder();
    stubExistingReviewComments(
        List.of(stubExistingReviewComment("Placeholder issue", "src/main/strings.xml", 42, 42)));
    GithubClient.ReviewComment alreadyPosted =
        new GithubClient.ReviewComment("Placeholder issue", "src/main/strings.xml", 42);
    GithubClient.ReviewComment newComment =
        new GithubClient.ReviewComment("Another issue", "src/main/strings.xml", 42);

    List<GithubClient.ReviewComment> postedComments =
        githubClient.addReviewCommentsToPR(
            "testRepo", 1, List.of(alreadyPosted, newComment), "commitSha");

    assertEquals(List.of(newComment), postedComments);
  }

  @Test
  public void testAddReviewCommentsToPRReturnsNoCommentsWhenAllCommentsAlreadyExist()
      throws IOException {
    stubReviewBuilder();
    stubExistingReviewComments(
        List.of(stubExistingReviewComment("Comment 1", "src/main/strings.xml", 10, 10)));

    List<GithubClient.ReviewComment> postedComments =
        githubClient.addReviewCommentsToPR(
            "testRepo",
            1,
            List.of(new GithubClient.ReviewComment("Comment 1", "src/main/strings.xml", 10)),
            "commitSha");

    assertTrue(postedComments.isEmpty());
  }

  @Test
  public void testAreReviewCommentsResolvedWithNullComments() {
    assertFalse(githubClient.areReviewCommentsResolved("testRepo", 1, null));

    verifyNoInteractions(restTemplateMock);
  }

  @Test
  public void testAreReviewCommentsResolvedWithEmptyComments() {
    assertFalse(githubClient.areReviewCommentsResolved("testRepo", 1, List.of()));

    verifyNoInteractions(restTemplateMock);
  }

  @Test
  public void testAreReviewCommentsResolvedWhenAllCommentsAreOnResolvedThreads() {
    stubReviewThreadsQuery(
        reviewThreadsResponse(
            resolvedReviewThread("Comment 1", "src/main/strings.xml", 10, 10),
            resolvedReviewThread("Comment 2", "src/main/other.xml", 20, 20)));

    assertTrue(
        githubClient.areReviewCommentsResolved(
            "testRepo",
            1,
            List.of(
                new GithubClient.ReviewComment("Comment 1", "src/main/strings.xml", 10),
                new GithubClient.ReviewComment("Comment 2", "src/main/other.xml", 20))));
  }

  @Test
  public void testAreReviewCommentsResolvedWhenOneCommentIsOnAnUnresolvedThread() {
    stubReviewThreadsQuery(
        reviewThreadsResponse(
            resolvedReviewThread("Comment 1", "src/main/strings.xml", 10, 10),
            unresolvedReviewThread("Comment 2", "src/main/other.xml", 20, 20)));

    assertFalse(
        githubClient.areReviewCommentsResolved(
            "testRepo",
            1,
            List.of(
                new GithubClient.ReviewComment("Comment 1", "src/main/strings.xml", 10),
                new GithubClient.ReviewComment("Comment 2", "src/main/other.xml", 20))));
  }

  @Test
  public void testAreReviewCommentsResolvedWhenCommentIsNotOnThePullRequest() {
    stubReviewThreadsQuery(reviewThreadsResponse());

    assertFalse(
        githubClient.areReviewCommentsResolved(
            "testRepo",
            1,
            List.of(
                new GithubClient.ReviewComment("Placeholder issue", "src/main/strings.xml", 42))));
  }

  @Test
  public void testAreReviewCommentsResolvedWhenResolvedCommentIsOnAnotherLineOrFile() {
    stubReviewThreadsQuery(
        reviewThreadsResponse(
            resolvedReviewThread("Placeholder issue", "src/main/strings.xml", 41, 41),
            resolvedReviewThread("Placeholder issue", "src/main/other.xml", 42, 42)));

    assertFalse(
        githubClient.areReviewCommentsResolved(
            "testRepo",
            1,
            List.of(
                new GithubClient.ReviewComment("Placeholder issue", "src/main/strings.xml", 42))));
  }

  @Test
  public void testAreReviewCommentsResolvedWhenResolvedCommentHasAnotherBody() {
    stubReviewThreadsQuery(
        reviewThreadsResponse(
            resolvedReviewThread("Another issue", "src/main/strings.xml", 42, 42)));

    assertFalse(
        githubClient.areReviewCommentsResolved(
            "testRepo",
            1,
            List.of(
                new GithubClient.ReviewComment("Placeholder issue", "src/main/strings.xml", 42))));
  }

  @Test
  public void testAreReviewCommentsResolvedMatchesOutdatedResolvedComment() {
    // An outdated comment has no current line, only the original one it was posted on
    stubReviewThreadsQuery(
        reviewThreadsResponse(
            resolvedReviewThread("Placeholder issue", "src/main/strings.xml", 0, 42)));

    assertTrue(
        githubClient.areReviewCommentsResolved(
            "testRepo",
            1,
            List.of(
                new GithubClient.ReviewComment("Placeholder issue", "src/main/strings.xml", 42))));
  }

  @Test
  public void testAreReviewCommentsResolvedIgnoresLineEndingsAndSurroundingWhitespaceInTheBody() {
    stubReviewThreadsQuery(
        reviewThreadsResponse(
            resolvedReviewThread(
                "Placeholder issue:\r\nmissing placeholder\n", "src/main/strings.xml", 42, 42)));

    assertTrue(
        githubClient.areReviewCommentsResolved(
            "testRepo",
            1,
            List.of(
                new GithubClient.ReviewComment(
                    "Placeholder issue:\nmissing placeholder", "src/main/strings.xml", 42))));
  }

  @Test
  public void testAreReviewCommentsResolvedPaginatesTheReviewThreads() {
    stubReviewThreadsQuery(
        reviewThreadsResponse(
            "cursor1", resolvedReviewThread("Comment 1", "src/main/strings.xml", 10, 10)),
        reviewThreadsResponse(resolvedReviewThread("Comment 2", "src/main/other.xml", 20, 20)));

    assertTrue(
        githubClient.areReviewCommentsResolved(
            "testRepo",
            1,
            List.of(
                new GithubClient.ReviewComment("Comment 1", "src/main/strings.xml", 10),
                new GithubClient.ReviewComment("Comment 2", "src/main/other.xml", 20))));

    List<HttpEntity> requests = captureReviewThreadsRequests(2);
    assertNull(getQueryVariables(requests.get(0)).get("after"));
    assertEquals("cursor1", getQueryVariables(requests.get(1)).get("after"));
  }

  @Test
  public void testAreReviewCommentsResolvedStopsPaginatingWhenTheCursorIsMissing() {
    stubReviewThreadsQuery(
        reviewThreadsResponse(
            "", resolvedReviewThread("Comment 1", "src/main/strings.xml", 10, 10)));

    assertFalse(
        githubClient.areReviewCommentsResolved(
            "testRepo",
            1,
            List.of(new GithubClient.ReviewComment("Comment 2", "src/main/other.xml", 20))));

    captureReviewThreadsRequests(1);
  }

  @Test
  public void testAreReviewCommentsResolvedStopsPaginatingAfterTheMaximumNumberOfPages() {
    // Every page announces a next one, the client must not follow them indefinitely
    stubReviewThreadsQuery(
        reviewThreadsResponse(
            "cursor", resolvedReviewThread("Comment 1", "src/main/strings.xml", 10, 10)));

    assertFalse(
        githubClient.areReviewCommentsResolved(
            "testRepo",
            1,
            List.of(new GithubClient.ReviewComment("Comment 2", "src/main/other.xml", 20))));

    captureReviewThreadsRequests(20);
  }

  @Test
  public void testAreReviewCommentsResolvedWithRetry() {
    whenReviewThreadsQueried()
        .thenThrow(new RestClientException("network issue"))
        .thenReturn(
            ResponseEntity.ok(
                reviewThreadsResponse(
                    resolvedReviewThread("Placeholder issue", "src/main/strings.xml", 42, 42))));

    assertTrue(
        githubClient.areReviewCommentsResolved(
            "testRepo",
            1,
            List.of(
                new GithubClient.ReviewComment("Placeholder issue", "src/main/strings.xml", 42))));

    captureReviewThreadsRequests(2);
  }

  @Test
  public void testAreReviewCommentsResolved_ThrowsExceptionWhenRetriesExhausted() {
    whenReviewThreadsQueried().thenThrow(new RestClientException("network issue"));

    assertThrows(
        IllegalStateException.class,
        () ->
            githubClient.areReviewCommentsResolved(
                "testRepo",
                1,
                List.of(
                    new GithubClient.ReviewComment(
                        "Placeholder issue", "src/main/strings.xml", 42))));

    // One initial attempt plus maxRetries
    captureReviewThreadsRequests(4);
    verify(meterRegistryMock, times(1))
        .counter(
            "Mojito.GitHubClient.RetriesExhausted",
            "repository",
            "testRepo",
            "operation",
            "areReviewCommentsResolved");
    verify(counterMock, times(1)).increment();
  }

  @Test
  public void testAreReviewCommentsResolved_ThrowsExceptionWhenTheApiReturnsErrors()
      throws IOException {
    JsonNode errorResponse =
        objectMapper.readTree(
            """
            {"errors": [{"message": "Could not resolve to a Repository"}]}
            """);
    stubReviewThreadsQuery(errorResponse);

    assertThrows(
        IllegalStateException.class,
        () ->
            githubClient.areReviewCommentsResolved(
                "testRepo",
                1,
                List.of(
                    new GithubClient.ReviewComment(
                        "Placeholder issue", "src/main/strings.xml", 42))));
  }

  @Test
  public void testAreReviewCommentsResolved_ThrowsExceptionWhenTheResponseIsEmpty() {
    whenReviewThreadsQueried().thenReturn(new ResponseEntity<>(HttpStatus.OK));

    assertThrows(
        IllegalStateException.class,
        () ->
            githubClient.areReviewCommentsResolved(
                "testRepo",
                1,
                List.of(
                    new GithubClient.ReviewComment(
                        "Placeholder issue", "src/main/strings.xml", 42))));
  }

  @Test
  public void testAreReviewCommentsResolvedCallsTheGraphqlEndpointWithTheInstallationToken() {
    stubReviewThreadsQuery(reviewThreadsResponse());

    githubClient.areReviewCommentsResolved(
        "testRepo",
        1,
        List.of(new GithubClient.ReviewComment("Placeholder issue", "src/main/strings.xml", 42)));

    ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplateMock, times(1))
        .exchange(
            eq("https://api.github.com/graphql"),
            eq(HttpMethod.POST),
            requestCaptor.capture(),
            eq(JsonNode.class));

    HttpEntity<?> request = requestCaptor.getValue();
    assertEquals(
        List.of("Bearer installationToken"), request.getHeaders().get(HttpHeaders.AUTHORIZATION));
    Map<String, Object> variables = getQueryVariables(request);
    assertEquals("testOwner", variables.get("owner"));
    assertEquals("testRepo", variables.get("name"));
    assertEquals(1, variables.get("number"));
  }

  private OngoingStubbing<ResponseEntity<JsonNode>> whenReviewThreadsQueried() {
    return when(
        restTemplateMock.exchange(
            anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)));
  }

  /** Stubs the GraphQL calls, one response per page of review threads */
  private void stubReviewThreadsQuery(JsonNode... responses) {
    OngoingStubbing<ResponseEntity<JsonNode>> stubbing = whenReviewThreadsQueried();
    for (JsonNode response : responses) {
      stubbing = stubbing.thenReturn(ResponseEntity.ok(response));
    }
  }

  private List<HttpEntity> captureReviewThreadsRequests(int expectedCallCount) {
    ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplateMock, times(expectedCallCount))
        .exchange(anyString(), eq(HttpMethod.POST), requestCaptor.capture(), eq(JsonNode.class));
    return requestCaptor.getAllValues();
  }

  private Map<String, Object> getQueryVariables(HttpEntity<?> request) {
    return (Map<String, Object>) ((Map<String, Object>) request.getBody()).get("variables");
  }

  /** Builds a GraphQL response holding the given review threads, with no further page */
  private JsonNode reviewThreadsResponse(ObjectNode... reviewThreads) {
    return reviewThreadsResponse(null, reviewThreads);
  }

  /**
   * Builds a GraphQL response holding the given review threads, announcing a next page when a
   * cursor is provided
   */
  private JsonNode reviewThreadsResponse(String endCursor, ObjectNode... reviewThreads) {
    ObjectNode pageInfo = objectMapper.createObjectNode();
    pageInfo.put("hasNextPage", endCursor != null);
    pageInfo.put("endCursor", endCursor);

    ObjectNode reviewThreadsNode = objectMapper.createObjectNode();
    reviewThreadsNode.set("pageInfo", pageInfo);
    reviewThreadsNode.set(
        "nodes", objectMapper.createArrayNode().addAll(Arrays.asList(reviewThreads)));

    ObjectNode response = objectMapper.createObjectNode();
    response
        .putObject("data")
        .putObject("repository")
        .putObject("pullRequest")
        .set("reviewThreads", reviewThreadsNode);
    return response;
  }

  private ObjectNode resolvedReviewThread(String body, String path, int line, int originalLine) {
    return reviewThread(true, body, path, line, originalLine);
  }

  private ObjectNode unresolvedReviewThread(String body, String path, int line, int originalLine) {
    return reviewThread(false, body, path, line, originalLine);
  }

  /**
   * Builds a review thread node holding a single comment. A line that is not strictly positive
   * stands for the null Github returns when the comment is outdated.
   */
  private ObjectNode reviewThread(
      boolean isResolved, String body, String path, int line, int originalLine) {
    ObjectNode comment = objectMapper.createObjectNode();
    comment.put("path", path);
    comment.put("body", body);
    if (line > 0) {
      comment.put("line", line);
    } else {
      comment.putNull("line");
    }
    if (originalLine > 0) {
      comment.put("originalLine", originalLine);
    } else {
      comment.putNull("originalLine");
    }

    ObjectNode reviewThread = objectMapper.createObjectNode();
    reviewThread.put("isResolved", isResolved);
    reviewThread.putObject("comments").set("nodes", objectMapper.createArrayNode().add(comment));
    return reviewThread;
  }

  private void stubExistingReviewComments(List<GHPullRequestReviewComment> existingComments)
      throws IOException {
    PagedIterable<GHPullRequestReviewComment> reviewCommentsMock =
        Mockito.mock(PagedIterable.class);
    when(reviewCommentsMock.toList()).thenReturn(existingComments);
    when(ghPullRequestMock.listReviewComments()).thenReturn(reviewCommentsMock);
  }

  private GHPullRequestReviewComment stubExistingReviewComment(
      String body, String path, int line, int originalLine) {
    GHPullRequestReviewComment reviewCommentMock = Mockito.mock(GHPullRequestReviewComment.class);
    when(reviewCommentMock.getBody()).thenReturn(body);
    when(reviewCommentMock.getPath()).thenReturn(path);
    when(reviewCommentMock.getLine()).thenReturn(line);
    when(reviewCommentMock.getOriginalLine()).thenReturn(originalLine);
    return reviewCommentMock;
  }

  private void stubReviewBuilder() throws IOException {
    when(ghPullRequestMock.createReview()).thenReturn(ghPullRequestReviewBuilderMock);
    when(ghPullRequestReviewBuilderMock.commitId(anyString()))
        .thenReturn(ghPullRequestReviewBuilderMock);
    when(ghPullRequestReviewBuilderMock.event(isA(GHPullRequestReviewEvent.class)))
        .thenReturn(ghPullRequestReviewBuilderMock);
    when(ghPullRequestReviewBuilderMock.body(anyString()))
        .thenReturn(ghPullRequestReviewBuilderMock);
    when(ghPullRequestReviewBuilderMock.singleLineComment(anyString(), anyString(), anyInt()))
        .thenReturn(ghPullRequestReviewBuilderMock);
  }

  @Test
  public void testAddCommentToPR_ThrowsException() throws IOException {
    when(this.ghPullRequestMock.comment(anyString())).thenThrow(GithubException.class);

    Mono<GHIssueComment> ghIssueCommentMono =
        this.githubClient.addCommentToPR("testRepo", 1, "Test comment");
    assertThrows(IllegalStateException.class, ghIssueCommentMono::block);
  }

  @Configuration
  @ConfigurationProperties("l10n.github")
  static class TestConfig {

    @Bean
    public GithubClient getGithubClient() throws NoSuchAlgorithmException, InvalidKeySpecException {
      MeterRegistry meterRegistryMock = Mockito.mock(MeterRegistry.class);
      GithubClient ghClient =
          Mockito.spy(new GithubClient("testAppId", "someKey", "testOwner", meterRegistryMock));
      PrivateKey privateKeyMock = Mockito.mock(PrivateKey.class);
      doReturn(privateKeyMock).when(ghClient).getSigningKey();
      return ghClient;
    }
  }

  @ParameterizedTest(name = "now={0}, expiresAt={1} -> refresh={2}")
  @CsvSource({
    "100, 131, false", // > 30s away
    "100, 130, true", // exactly 30s away -> refresh
    "100, 120, true", // 20s away -> refresh
    "100,  90, true" // already expired -> refresh
  })
  void testShouldRefreshToken(long now, long expiry, boolean expected) {
    long nowMs = TimeUnit.SECONDS.toMillis(now);
    long expiryMs = TimeUnit.SECONDS.toMillis(expiry);
    boolean requiresRefresh = githubClient.shouldRefreshToken(expiryMs, nowMs);
    assertEquals(expected, requiresRefresh);
  }
}
