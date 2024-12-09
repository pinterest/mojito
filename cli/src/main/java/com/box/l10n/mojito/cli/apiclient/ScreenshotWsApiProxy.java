package com.box.l10n.mojito.cli.apiclient;

import com.box.l10n.mojito.cli.model.ScreenshotRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScreenshotWsApiProxy extends ScreenshotWsApi {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(ScreenshotWsApiProxy.class);

  public ScreenshotWsApiProxy(ApiClient apiClient) {
    super(apiClient);
  }

  @Override
  public ScreenshotRun createOrAddToScreenshotRun(ScreenshotRun body) throws ApiException {
    if (body.getRepository() != null) {
      logger.debug("Upload screenshots into repository = {}", body.getRepository().getName());
    } else {
      logger.debug("Upload screenshots for run with id = {}", body.getId());
    }
    return super.createOrAddToScreenshotRun(body);
  }
}
