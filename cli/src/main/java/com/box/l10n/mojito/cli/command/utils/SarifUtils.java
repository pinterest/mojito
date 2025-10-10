package com.box.l10n.mojito.cli.command.utils;

import com.box.l10n.mojito.sarif.model.Region;
import com.box.l10n.mojito.sarif.model.Sarif;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SarifUtils {

  public static Map<String, Set<Integer>> buildFileToLineNumberMap(Sarif sarif) {
    return sarif.getRuns().stream()
        .flatMap(run -> run.getResults().stream())
        .flatMap(
            result ->
                result.getLocations().stream()
                    .map(
                        location -> {
                          String fileName =
                              location.getPhysicalLocation().getArtifactLocation().getUri();
                          Region region = location.getPhysicalLocation().getRegion();
                          int startLine = region.startLine;
                          int endLine = region.endLine;
                          return Map.entry(
                              fileName,
                              IntStream.rangeClosed(startLine, endLine)
                                  .boxed()
                                  .collect(Collectors.toSet()));
                        }))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (set1, set2) -> {
                  Set<Integer> merged = new java.util.HashSet<>(set1);
                  merged.addAll(set2);
                  return merged;
                }));
  }
}
