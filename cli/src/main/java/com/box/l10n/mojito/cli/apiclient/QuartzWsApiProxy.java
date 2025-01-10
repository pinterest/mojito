package com.box.l10n.mojito.cli.apiclient;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuartzWsApiProxy extends QuartzWsApi {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(QuartzWsApiProxy.class);

  public QuartzWsApiProxy(ApiClient apiClient) {
    super(apiClient);
  }

  public void deleteAllDynamicJobs() throws ApiException {
    logger.debug("deleteAllDynamicJobs");
    super.deleteAllDynamicJobs();
  }

  public List<String> getAllDynamicJobs() throws ApiException {
    logger.debug("getAllDynamicJobs");
    return super.getAllDynamicJobs();
  }
}
