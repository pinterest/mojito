package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CliCheckerExecutor {

  private final List<AbstractCliChecker> cliCheckerList;

  private final Pattern excludedFilesPattern;

  public CliCheckerExecutor(List<AbstractCliChecker> cliCheckerList, Pattern excludedFilesPattern) {
    this.cliCheckerList = cliCheckerList;
    this.excludedFilesPattern = excludedFilesPattern;
  }

  public List<CliCheckResult> executeChecks(List<AssetExtractionDiff> assetExtractionDiffs) {
    return cliCheckerList.stream()
        .map(
            check -> {
              if (check.getCliCheckerType() == CliCheckerType.CONTEXT_COMMENT_CHECKER) {
                ((ContextAndCommentCliChecker) check)
                    .setExcludedFilesPattern(this.excludedFilesPattern);
              }
              return check.run(assetExtractionDiffs);
            })
        .collect(Collectors.toList());
  }
}
