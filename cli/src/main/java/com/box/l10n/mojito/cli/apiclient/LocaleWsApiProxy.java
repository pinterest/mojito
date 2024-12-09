package com.box.l10n.mojito.cli.apiclient;

import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.model.Locale;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocaleWsApiProxy extends LocaleWsApi {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(LocaleWsApiProxy.class);

  public LocaleWsApiProxy(ApiClient apiClient) {
    super(apiClient);
  }

  public Locale getLocaleByBcp47Tag(String bcp47Tag) throws ApiException {
    logger.debug("Getting locale for BCP47 tag: {}", bcp47Tag);

    List<Locale> locales = this.getLocales(bcp47Tag);

    if (locales.size() != 1) {
      throw new CommandException("Could not find locale with BCP47 tag: " + bcp47Tag);
    }

    return locales.getFirst();
  }

  @Override
  public List<Locale> getLocales(String bcp47Tag) throws ApiException {
    logger.debug("Getting all locales");
    return super.getLocales(bcp47Tag);
  }
}
