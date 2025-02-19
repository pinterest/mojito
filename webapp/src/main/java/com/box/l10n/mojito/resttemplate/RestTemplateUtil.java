package com.box.l10n.mojito.resttemplate;

import com.box.l10n.mojito.rest.resttemplate.ResttemplateConfig;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("RestTemplateUtilWebapp")
public class RestTemplateUtil {

  @Autowired ResttemplateConfig resttemplateConfig;

  public String getURIForResource(String resourcePath) {

    StringBuilder uri = new StringBuilder();

    if (resourcePath.startsWith(resttemplateConfig.getScheme())) {
      uri.append(resourcePath);
    } else {
      uri.append(resttemplateConfig.getScheme()).append("://").append(resttemplateConfig.getHost());

      if (resttemplateConfig.getPort() != 80) {
        uri.append(":").append(resttemplateConfig.getPort());
      }

      if (!Strings.isNullOrEmpty(resttemplateConfig.getContextPath())) {
        uri.append(resttemplateConfig.getContextPath());
      }

      uri.append("/").append(resourcePath);
    }

    return uri.toString();
  }
}
