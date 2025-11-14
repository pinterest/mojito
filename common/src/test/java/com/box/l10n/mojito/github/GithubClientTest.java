package com.box.l10n.mojito.github;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
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
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.PagedIterator;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {GithubClientTest.class, GithubClientTest.TestConfig.class})
@EnableConfigurationProperties
public class GithubClientTest {

  @Autowired(required = false)
  GithubClient githubClient;

  @Autowired TestConfig testConfig;

  @Mock GitHub gitHubMock;

  @Mock GHAppInstallationToken ghAppInstallationTokenMock;

  @Mock GHRepository ghRepoMock;

  @Mock GHPullRequest ghPullRequestMock;

  @Mock GHCommit ghCommitMock;

  @Mock GHCommitPointer ghCommitPointerMock;

  @Mock GHUser ghUserMock;

  @Mock GHIssueComment ghCommentMock1;

  @Mock GHIssueComment ghCommentMock2;

  @Mock MeterRegistry meterRegistryMock;

  @Mock Counter counterMock;

  @Before
  public void setup() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    Assume.assumeNotNull(githubClient);
    githubClient.gitHubClient = gitHubMock;
    githubClient.githubAppInstallationToken = ghAppInstallationTokenMock;
    githubClient.maxRetries = 3;
    githubClient.retryMinBackoff = Duration.ofMillis(1);
    githubClient.retryMaxBackoff = Duration.ofMillis(10);
    when(this.meterRegistryMock.counter(
            anyString(), anyString(), anyString(), anyString(), anyString()))
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
