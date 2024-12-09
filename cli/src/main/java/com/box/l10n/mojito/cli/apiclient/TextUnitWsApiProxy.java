package com.box.l10n.mojito.cli.apiclient;

import com.box.l10n.mojito.cli.model.GitBlameWithUsage;
import com.box.l10n.mojito.cli.model.GitBlameWithUsageGitBlameWithUsage;
import com.box.l10n.mojito.cli.model.PollableTask;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextUnitWsApiProxy extends TextUnitWsApi {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(TextUnitWsApiProxy.class);

  public TextUnitWsApiProxy(ApiClient apiClient) {
    super(apiClient);
  }

  @Override
  public List<GitBlameWithUsageGitBlameWithUsage> getGitBlameWithUsages(
      List<Long> repositoryIds,
      List<String> repositoryNames,
      Long tmTextUnitId,
      String usedFilter,
      String statusFilter,
      Boolean doNotTranslateFilter,
      Integer limit,
      Integer offset)
      throws ApiException {
    logger.debug("getGitBlameWithUsages");
    return super.getGitBlameWithUsages(
        repositoryIds,
        repositoryNames,
        tmTextUnitId,
        usedFilter,
        statusFilter,
        doNotTranslateFilter,
        limit,
        offset);
  }

  @Override
  public PollableTask saveGitBlameWithUsages(List<GitBlameWithUsage> body) throws ApiException {
    logger.debug("saveGitBlameWithUsages");
    return super.saveGitBlameWithUsages(body);
  }
}
