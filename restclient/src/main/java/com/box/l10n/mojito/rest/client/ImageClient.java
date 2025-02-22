package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.resttemplate.AuthenticatedRestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Client to upload images.
 *
 * @author jaurambault
 */
@Component
public class ImageClient {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(ImageClient.class);

  @Autowired private AuthenticatedRestTemplate authenticatedRestTemplate;

  /**
   * Uploads an image.
   *
   * @param name the image name
   * @param content the image content
   */
  public void uploadImage(String name, byte[] content) {
    logger.debug("Upload image with name = {}", name);
    this.authenticatedRestTemplate.put("/api/images/" + name, content);
  }
}
