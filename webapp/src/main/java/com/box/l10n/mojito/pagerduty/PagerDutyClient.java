package com.box.l10n.mojito.pagerduty;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PagerDutyClient {

  static Logger logger = LoggerFactory.getLogger(PagerDutyClient.class);

  static final String BASE_URL = "https://events.pagerduty.com";
  static final String ENQUEUE_PATH = "/v2/enqueue";

  private HttpClient httpClient = HttpClient.newHttpClient();

  private static final int MAX_RETRIES = 3;
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

        if (response.statusCode() == 400) {
          throw new PagerDutyException(
              "PagerDuty BAD REQUEST: status code '"
                  + statusCode
                  + "', response body: "
                  + response.body());
        }

        if (attempt == MAX_RETRIES) {
          throw new PagerDutyException(
              "PagerDuty request failed: Status code '"
                  + statusCode
                  + "', response body: "
                  + response.body());
        }
      } catch (IOException | InterruptedException e) {
        logger.error("Failed to send PagerDuty request: ", e);
      }
    }
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
