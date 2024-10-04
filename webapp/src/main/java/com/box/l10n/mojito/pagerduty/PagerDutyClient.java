package com.box.l10n.mojito.pagerduty;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PagerDutyClient {

  static Logger logger = LoggerFactory.getLogger(PagerDutyClient.class);

  static final String BASE_URL = "https://events.pagerduty.com";
  static final String ENQUEUE_PATH = "/v2/enqueue";

  private HttpClient httpClient = HttpClient.newHttpClient();

  public static final int MAX_RETRIES = 3;
  public static String exceptionTemplate =
      "PagerDuty request failed: Status Code: '{0}', Response Body: '{1}'";

  private final String integrationKey;

  public PagerDutyClient(String integrationKey) {
    this.integrationKey = integrationKey;
  }

  public PagerDutyClient(String integrationKey, HttpClient httpClient) {
    // Constructor for unit tests
    this.integrationKey = integrationKey;
    this.httpClient = httpClient;
  }

  public void triggerIncident(String dedupKey, PagerDutyPayload payload)
      throws JsonProcessingException, PagerDutyException {
    sendPayload(dedupKey, payload, PagerDutyPostRequest.EventAction.TRIGGER);
  }

  public void resolveIncident(String dedupKey) throws JsonProcessingException, PagerDutyException {
    sendPayload(dedupKey, null, PagerDutyPostRequest.EventAction.RESOLVE);
  }

  private void sendPayload(
      String dedupKey, PagerDutyPayload payload, PagerDutyPostRequest.EventAction eventAction)
      throws JsonProcessingException, PagerDutyException {

    if (integrationKey == null || integrationKey.isEmpty())
      throw new PagerDutyException("Integration key should not be null or empty.");

    if (dedupKey == null || dedupKey.isEmpty())
      throw new PagerDutyException("Deduplication key should not be null or empty.");

    PagerDutyPostRequest postBody = new PagerDutyPostRequest(integrationKey, dedupKey);
    postBody.setPayload(payload);
    postBody.setEventAction(eventAction);

    HttpRequest request = buildRequest(postBody.serialize());
    sendRequest(request);
  }

  private void sendRequest(HttpRequest request) throws PagerDutyException {
    // Retry sending PD notifications if they fail
    for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
      try {
        HttpResponse<String> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();

        if (statusCode == 200 || statusCode == 202) return;

        // Either the max retries was hit or bad request (not recovering from 400)
        if (attempt == MAX_RETRIES || statusCode == 400) {
          throwFormattedException(statusCode, response.body());
        }
      } catch (IOException | InterruptedException e) {
        if (attempt == MAX_RETRIES)
          throw new PagerDutyException("Failed to send PagerDuty request: ", e);
      }
    }
  }

  private void throwFormattedException(int statusCode, String response) throws PagerDutyException {
    throw new PagerDutyException(MessageFormat.format(exceptionTemplate, statusCode, response));
  }

  public HttpRequest buildRequest(String postBody) {
    return HttpRequest.newBuilder()
        .uri(URI.create(BASE_URL + ENQUEUE_PATH))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(postBody))
        .build();
  }
}
