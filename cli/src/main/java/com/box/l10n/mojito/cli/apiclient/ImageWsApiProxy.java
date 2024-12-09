package com.box.l10n.mojito.cli.apiclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageWsApiProxy extends ImageWsApi {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(ImageWsApiProxy.class);

  public ImageWsApiProxy(ApiClient apiClient) {
    super(apiClient);
  }

  @Override
  public void uploadImage(byte[] body, String imageName) throws ApiException {
    logger.debug("Upload image with name = {}", imageName);
    super.uploadImage(body, imageName);
  }
}
