package com.box.l10n.mojito.cli.apiclient;

import com.box.l10n.mojito.cli.model.AICheckRequest;
import com.box.l10n.mojito.cli.model.AICheckResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChecksWsApiProxy extends AiChecksWsApi {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(AiChecksWsApiProxy.class);

  public AiChecksWsApiProxy(ApiClient apiClient) {
    super(apiClient);
  }

  @Override
  public AICheckResponse executeAIChecks(AICheckRequest body) throws ApiException {
    logger.debug("Received request to execute AI checks");
    return super.executeAIChecks(body);
  }
}
