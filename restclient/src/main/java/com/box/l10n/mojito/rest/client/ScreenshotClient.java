package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.apiclient.ScreenshotWsApi;
import com.box.l10n.mojito.rest.apiclient.model.ScreenshotRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Client to upload screenshots.
 *
 * @author jaurambault
 */
@Component
public class ScreenshotClient {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(ScreenshotClient.class);

  @Autowired private ScreenshotWsApi screenshotWsApi;

  public ScreenshotRun createOrAddToScreenshotRun(ScreenshotRun body) {
    if (body.getRepository() != null) {
      logger.debug("Upload screenshots into repository = {}", body.getRepository().getName());
    } else {
      logger.debug("Upload screenshots for run with id = {}", body.getId());
    }
    return this.screenshotWsApi.createOrAddToScreenshotRun(body);
  }
}
