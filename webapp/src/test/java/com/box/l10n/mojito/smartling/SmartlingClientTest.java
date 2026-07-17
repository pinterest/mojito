package com.box.l10n.mojito.smartling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.smartling.request.Binding;
import com.box.l10n.mojito.smartling.request.Bindings;
import com.box.l10n.mojito.smartling.response.*;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.util.retry.Retry;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
      SmartlingClientTest.class,
      SmartlingClientConfiguration.class,
      ObjectMapper.class,
      SmartlingTestConfig.class
    })
@EnableConfigurationProperties
public class SmartlingClientTest {

  static Logger logger = LoggerFactory.getLogger(SmartlingClient.class);

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired(required = false)
  SmartlingClient smartlingClient;

  @Autowired SmartlingTestConfig smartlingTestConfig;

  @Before
  public void init() {
    Assume.assumeNotNull(smartlingClient);
  }

  @Test
  public void testGetSourceStrings() throws SmartlingClientException {
    Assume.assumeNotNull(smartlingTestConfig.projectId);
    Assume.assumeNotNull(smartlingTestConfig.fileUri);

    logger.debug("Test getSourceStrings");
    Items<StringInfo> sourceStrings =
        smartlingClient.getSourceStrings(
            smartlingTestConfig.projectId, smartlingTestConfig.fileUri, 0, 500);

    sourceStrings.getItems().stream().map(item -> item.getHashcode()).forEach(logger::debug);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetSourceStringsTruncatesResponseBodyOnError() throws Exception {
    HttpClient httpClient = mock(HttpClient.class);
    HttpResponse<String> httpResponse = (HttpResponse<String>) mock(HttpResponse.class);
    SmartlingOAuth2TokenService tokenService = mock(SmartlingOAuth2TokenService.class);

    String responseBody = "x".repeat(SmartlingClient.ERROR_RESPONSE_SNIPPET_MAX_BYTES + 250);
    String truncatedResponseBody =
        responseBody.substring(0, SmartlingClient.ERROR_RESPONSE_SNIPPET_MAX_BYTES);

    when(tokenService.getAccessToken()).thenReturn("token");
    when(httpResponse.body()).thenReturn(responseBody);
    when(httpClient.send(
            ArgumentMatchers.any(HttpRequest.class),
            ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
        .thenReturn(httpResponse);

    try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
      mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(httpClient);

      SmartlingClient testSmartlingClient =
          new SmartlingClient(tokenService, Retry.backoff(1, Duration.ofMillis(1)));

      SmartlingClientException exception =
          Assert.assertThrows(
              SmartlingClientException.class,
              () -> testSmartlingClient.getSourceStrings("projectId", "fileUri", 0, 500));

      assertThat(exception.getMessage()).contains("Can't get source strings");
      assertThat(exception.getMessage())
          .contains("response body (first 1000 bytes): " + truncatedResponseBody);
      assertThat(truncatedResponseBody).hasSize(SmartlingClient.ERROR_RESPONSE_SNIPPET_MAX_BYTES);
      assertThat(exception.getMessage())
          .contains(responseBody.substring(SmartlingClient.ERROR_RESPONSE_SNIPPET_MAX_BYTES));
    }
  }

  @Test
  public void testGetSourceStringsStream() {
    Assume.assumeNotNull(smartlingTestConfig.projectId);
    Assume.assumeNotNull(smartlingTestConfig.fileUri);

    smartlingClient
        .getStringInfos(smartlingTestConfig.projectId, smartlingTestConfig.fileUri)
        .forEach(
            stringInfo -> {
              if (stringInfo.getKeys().size() == 1) {
                logger.debug(
                    "hashcode: {}\nvariant: {}\nparsed string: {}\n stringtext: {}\nkeys:",
                    stringInfo.getHashcode(),
                    stringInfo.getStringVariant(),
                    stringInfo.getParsedStringText(),
                    stringInfo.getStringText());

                stringInfo.getKeys().stream()
                    .forEach(
                        key -> logger.debug("key: {}, file: {}", key.getKey(), key.getFileUri()));

                logger.debug("\n\n");
              }
            });
  }

  @Test
  public void testDownloadGlossaryFile() {
    Assume.assumeNotNull(smartlingClient);
    Assume.assumeNotNull(smartlingTestConfig.accountId);
    Assume.assumeNotNull(smartlingTestConfig.glossaryId);

    String glossaryContent =
        smartlingClient.downloadGlossaryFile(
            smartlingTestConfig.accountId, smartlingTestConfig.glossaryId);

    assertNotNull(glossaryContent);
    assertTrue(glossaryContent.startsWith("<?xml version='1.0' encoding='UTF-8'?>"));
  }

  @Test
  public void testDownloadSourceGlossaryFile() {
    Assume.assumeNotNull(smartlingClient);
    Assume.assumeNotNull(smartlingTestConfig.accountId);
    Assume.assumeNotNull(smartlingTestConfig.glossaryId);

    String glossaryContent =
        smartlingClient.downloadSourceGlossaryFile(
            smartlingTestConfig.accountId, smartlingTestConfig.glossaryId, "fr-FR");

    assertNotNull(glossaryContent);
    assertTrue(glossaryContent.startsWith("<?xml version='1.0' encoding='UTF-8'?>"));
    assertTrue(glossaryContent.contains("xml:lang=\"fr-FR\""));
  }

  @Test
  public void testDownloadGlossaryFileWithTranslations() throws Exception {
    Assume.assumeNotNull(smartlingClient);
    Assume.assumeNotNull(smartlingTestConfig.accountId);
    Assume.assumeNotNull(smartlingTestConfig.glossaryId);

    String glossaryContent =
        smartlingClient.downloadGlossaryFileWithTranslations(
            smartlingTestConfig.accountId, smartlingTestConfig.glossaryId, "fr-FR", "en-US");

    assertNotNull(glossaryContent);
    assertTrue(glossaryContent.startsWith("<?xml version='1.0' encoding='UTF-8'?>"));
    assertTrue(glossaryContent.contains("xml:lang=\"en-US\""));
    assertTrue(glossaryContent.contains("<langSet xml:lang=\"fr-FR\""));
  }

  //  @Ignore
  @Test
  public void testRefreshToken() throws InterruptedException {
    Assume.assumeNotNull(smartlingTestConfig.projectId);
    Assume.assumeNotNull(smartlingTestConfig.fileUri);

    smartlingClient
        .getStringInfos(smartlingTestConfig.projectId, smartlingTestConfig.fileUri)
        .forEach(
            stringInfo -> {
              if (stringInfo.getKeys().size() == 1) {
                logger.debug(
                    "hashcode: {}\nvariant: {}\nparsed string: {}\n stringtext: {}\nkeys:",
                    stringInfo.getHashcode(),
                    stringInfo.getStringVariant(),
                    stringInfo.getParsedStringText(),
                    stringInfo.getStringText());

                stringInfo.getKeys().stream()
                    .forEach(
                        key -> logger.debug("key: {}, file: {}", key.getKey(), key.getFileUri()));

                logger.debug("\n\n");
              }
            });

    System.out.println("Sleeping until token is almost expired");
    Thread.sleep(460 * 1000L);

    for (int i = 0; i < 350; i++) {
      smartlingClient
          .getStringInfos(smartlingTestConfig.projectId, smartlingTestConfig.fileUri)
          .forEach(
              stringInfo -> {
                if (stringInfo.getKeys().size() == 1) {
                  logger.debug(
                      "hashcode: {}\nvariant: {}\nparsed string: {}\n stringtext: {}\nkeys:",
                      stringInfo.getHashcode(),
                      stringInfo.getStringVariant(),
                      stringInfo.getParsedStringText(),
                      stringInfo.getStringText());

                  stringInfo.getKeys().stream()
                      .forEach(
                          key -> logger.debug("key: {}, file: {}", key.getKey(), key.getFileUri()));

                  logger.debug("\n\n");
                }
              });

      System.out.println("Sleeping for token to expire and get refreshed");
      Thread.sleep(100L);
    }
  }

  @Test
  public void testUploadFile() {
    Assume.assumeNotNull(smartlingTestConfig.projectId);
    uploadFile(smartlingTestConfig.projectId, "strings.xml");
  }

  private void uploadFile(String projectId, String fileName) {
    smartlingClient.uploadFile(
        projectId,
        fileName,
        "android",
        "<resources>\n"
            + "    <string name=\"hello\">Hello</string>\n"
            + "    <string name=\"bye\">Bye</string>\n"
            + "</resources>\n",
        null,
        null,
        null);
  }

  @Test
  public void testUploadDownloadAndDeleteFile() {
    Assume.assumeNotNull(smartlingTestConfig.projectId);
    String fileName = testIdWatcher.getEntityName("") + "-string.xml";

    uploadFile(smartlingTestConfig.projectId, fileName);
    try {
      String result =
          smartlingClient.downloadFile(
              smartlingTestConfig.projectId,
              "fr-FR",
              fileName,
              false,
              SmartlingClient.RetrievalType.PUBLISHED);

      Assert.assertEquals(
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<resources>\n" + "</resources>\n",
          result);
    } finally {
      smartlingClient.deleteFile(smartlingTestConfig.projectId, fileName);
    }
  }

  @Test
  public void testUploadDownloadAndDeleteLocalizedFile() {
    Assume.assumeNotNull(smartlingTestConfig.projectId);

    FileUploadResponse response;
    String downloadFile;
    String fileName = "strings.xml";

    uploadFile(smartlingTestConfig.projectId, fileName);

    String content =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources>\n"
            + "    <string name=\"hello\">Hola</string>\n"
            + "    <string name=\"bye\">Adios</string>\n"
            + "</resources>";

    response =
        smartlingClient.uploadLocalizedFile(
            smartlingTestConfig.projectId, fileName, "android", "es-MX", content, null, null);

    downloadFile =
        smartlingClient.downloadFile(
            smartlingTestConfig.projectId,
            "es-MX",
            fileName,
            false,
            SmartlingClient.RetrievalType.PENDING);

    assertThat(response.getCode()).isEqualTo("SUCCESS");
    assertThat(downloadFile.trim()).isEqualTo(content);

    smartlingClient.deleteFile(smartlingTestConfig.projectId, fileName);
  }

  @Test
  public void testUploadContextPNGUpperCase() throws IOException {
    Assume.assumeNotNull(smartlingTestConfig.projectId);
    ClassPathResource classPathResource = new ClassPathResource("/com/box/l10n/mojito/img/1.png");
    byte[] content = ByteStreams.toByteArray(classPathResource.getInputStream());
    Context createdContext =
        smartlingClient.uploadContext(
            smartlingTestConfig.projectId, "caseissuewithpng.PNG", content);

    Context context =
        smartlingClient.getContext(smartlingTestConfig.projectId, createdContext.getContextUid());

    assertNotNull(context.getContextUid());
    Assert.assertEquals(createdContext.getContextUid(), context.getContextUid());
  }

  @Test
  public void testCRUDContext() throws IOException {
    Assume.assumeNotNull(smartlingTestConfig.projectId);

    ClassPathResource classPathResource = new ClassPathResource("/com/box/l10n/mojito/img/1.png");
    byte[] content = ByteStreams.toByteArray(classPathResource.getInputStream());
    Context createdContext =
        smartlingClient.uploadContext(smartlingTestConfig.projectId, "image1.png", content);

    assertNotNull(createdContext.getContextUid());

    Context context =
        smartlingClient.getContext(smartlingTestConfig.projectId, createdContext.getContextUid());

    assertNotNull(context.getContextUid());
    Assert.assertEquals(createdContext.getContextUid(), context.getContextUid());

    smartlingClient.deleteContext(smartlingTestConfig.projectId, context.getContextUid());

    SmartlingClientException contextNotFoundException =
        Assert.assertThrows(
            SmartlingClientException.class,
            () ->
                smartlingClient.getContext(smartlingTestConfig.projectId, context.getContextUid()));
    Assert.assertTrue(
        contextNotFoundException
            .getMessage()
            .contains(String.format("Can't get context: %s", context.getContextUid())));
  }

  @Test
  public void testGetFiles() {
    Assume.assumeNotNull(smartlingTestConfig.projectId);
    Items<File> files = smartlingClient.getFiles(smartlingTestConfig.projectId);
    files.getItems().stream().forEach(f -> logger.debug(f.getFileUri()));
  }

  @Test
  public void testGetStringInfosFromFile() {
    Assume.assumeNotNull(smartlingTestConfig.projectId);
    Items<File> files = smartlingClient.getFiles(smartlingTestConfig.projectId);
    Stream<StringInfo> stringInfosFromFiles =
        files.getItems().stream()
            .flatMap(
                file ->
                    smartlingClient.getStringInfos(
                        smartlingTestConfig.projectId, file.getFileUri()));
    stringInfosFromFiles.forEach(stringInfo -> logger.debug(stringInfo.getHashcode()));
  }

  @Test
  public void unwrapRootValueToDeserialize() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);

    String response =
        "{\n"
            + "  \"response\": {\n"
            + "    \"code\": \"SUCCESS\",\n"
            + "    \"data\": {\n"
            + "      \"accessToken\": \"b816424c-2e95-11e7-93ae-92361f002671\",\n"
            + "      \"expiresIn\": 480,\n"
            + "      \"refreshExpiresIn\": 3660,\n"
            + "      \"refreshToken\": \"c0a6f410-2e95-11e7-93ae-92361f002671\",\n"
            + "      \"tokenType\": \"Bearer\"\n"
            + "    }\n"
            + "  }\n"
            + "}";

    AuthenticationResponse authenticationResponse =
        objectMapper.readValue(response, AuthenticationResponse.class);
    Assert.assertEquals(
        "b816424c-2e95-11e7-93ae-92361f002671", authenticationResponse.getData().getAccessToken());
  }

  @Test
  public void bindingsSerialization() throws JsonProcessingException {
    Bindings bindings = new Bindings();
    Binding binding = new Binding();
    binding.setContextUid("c1");
    binding.setStringHashcode("h1");
    bindings.getBindings().add(binding);

    Binding binding2 = new Binding();
    binding2.setContextUid("c1");
    binding2.setStringHashcode("h1");
    bindings.getBindings().add(binding2);

    ObjectMapper objectMapper = new ObjectMapper();
    String str = objectMapper.writeValueAsString(bindings);
    Assert.assertEquals(
        "{\"bindings\":[{\"contextUid\":\"c1\",\"stringHashcode\":\"h1\"},{\"contextUid\":\"c1\",\"stringHashcode\":\"h1\"}]}",
        str);
  }
}
