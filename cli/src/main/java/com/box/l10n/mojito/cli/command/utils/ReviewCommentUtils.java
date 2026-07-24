package com.box.l10n.mojito.cli.command.utils;

import com.box.l10n.mojito.github.GithubClient;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utilities for inspecting generated GitHub PR review comments. Mirrors {@link SarifUtils} so that
 * review comments can be validated against GitHub's modified lines using the same tooling as SARIF
 * output.
 */
public class ReviewCommentUtils {

  /**
   * Builds a map of file path to the set of line numbers targeted by the given review comments.
   * This is the review-comment equivalent of {@link SarifUtils#buildFileToLineNumberMap} and can be
   * diffed against GitHub's modified-lines map to find comments GitHub would drop.
   *
   * @param reviewComments the comments about to be posted to GitHub
   * @return map of file path to the set of line numbers referenced by those comments
   */
  public static Map<String, Set<Integer>> buildFileToLineNumberMap(
      List<GithubClient.ReviewComment> reviewComments) {
    return reviewComments.stream()
        .collect(
            Collectors.toMap(
                GithubClient.ReviewComment::getPath,
                comment -> {
                  Set<Integer> lines = new HashSet<>();
                  lines.add(comment.getLine());
                  return lines;
                },
                (set1, set2) -> {
                  Set<Integer> merged = new HashSet<>(set1);
                  merged.addAll(set2);
                  return merged;
                }));
  }
}
