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

/**
 * PagerDuty client for creating (triggering) and resolving incidents.
 *
 * @author mattwilshire
 */
public class PagerDutyClient {

  static Logger logger = LoggerFactory.getLogger(PagerDutyClient.class);

  static final String BASE_URL = "https://events.pagerduty.com";
  static final String ENQUEUE_PATH = "/v2/enqueue";

  public static final int MAX_RETRIES = 3;
  public static String exceptionTemplate =
      "PagerDuty request failed: Status Code: '{0}', Response Body: '{1}'";

  private HttpClient httpClient = HttpClient.newHttpClient();
  private final String integrationKey;

  public PagerDutyClient(String integrationKey) {
    this.integrationKey = integrationKey;
  }

  public PagerDutyClient(String integrationKey, HttpClient httpClient) {
    // Constructor for unit tests
    this.integrationKey = integrationKey;
    this.httpClient = httpClient;
  }

  /**
   * Trigger incident using a deduplication key and send PagerDutyPayload with it. The client will
   * attempt to send the event request MAX_RETRIES times if an internal error is received from the
   * server. If the max number of retries is reached, a bad request is received or the payload
   * cannot be serialized a PagerDutyException is thrown.
   */
  public void triggerIncident(String dedupKey, PagerDutyPayload payload) throws PagerDutyException {
    sendPayload(dedupKey, payload, PagerDutyPostData.EventAction.TRIGGER);
  }

  /**
   * Resolve incident using a deduplication key. The client will attempt to send the event request
   * MAX_RETRIES times if an internal error is received from the server. If the max number of
   * retries is reached, a bad request is received or the payload cannot be serialized a
   * PagerDutyException is thrown.
   */
  public void resolveIncident(String dedupKey) throws PagerDutyException {
    sendPayload(dedupKey, null, PagerDutyPostData.EventAction.RESOLVE);
  }

  private void sendPayload(
      String dedupKey, PagerDutyPayload payload, PagerDutyPostData.EventAction eventAction)
      throws PagerDutyException {

    if (integrationKey == null || integrationKey.isEmpty())
      throw new PagerDutyException("Integration key should not be null or empty.");

    if (dedupKey == null || dedupKey.isEmpty())
      throw new PagerDutyException("Deduplication key should not be null or empty.");

    PagerDutyPostData postBody = new PagerDutyPostData(integrationKey, dedupKey);
    postBody.setPayload(payload);
    postBody.setEventAction(eventAction);

    try {
      HttpRequest request = buildRequest(postBody.serialize());
      sendRequest(request);
    } catch (JsonProcessingException e) {
      throw new PagerDutyException("Failed to serialize PagerDutyPostRequest to a JSON string.", e);
    }
  }

  private void sendRequest(HttpRequest request) throws PagerDutyException {
    // Retry sending PD notifications if they fail
    for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
      try {
        HttpResponse<String> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();
        if (statusCode == 200 || statusCode == 202) return;

        // Either the max retries was hit or a bad request, there is no recovering from a 400 status
        // code, exit without retries
        if (attempt == MAX_RETRIES || statusCode == 400) {
          logger.error(MessageFormat.format(exceptionTemplate, statusCode, response.body()));
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
