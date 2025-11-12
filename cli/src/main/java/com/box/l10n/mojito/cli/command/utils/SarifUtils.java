package com.box.l10n.mojito.cli.command.utils;

import com.box.l10n.mojito.sarif.model.Region;
import com.box.l10n.mojito.sarif.model.Sarif;
import java.util.HashSet;
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
                  Set<Integer> merged = new HashSet<>(set1);
                  merged.addAll(set2);
                  return merged;
                }));
  }

  public static Map<String, Set<Integer>> removeFilePathPrefix(
      Map<String, Set<Integer>> fileToLineNumberMap, String prefixToRemove) {
    if (prefixToRemove == null || prefixToRemove.isEmpty()) {
      return fileToLineNumberMap;
    }

    return fileToLineNumberMap.entrySet().stream()
        .collect(
            Collectors.toMap(
                entry -> {
                  String filePath = entry.getKey();
                  if (filePath.startsWith(prefixToRemove)) {
                    return filePath.substring(prefixToRemove.length());
                  } else {
                    return filePath;
                  }
                },
                Map.Entry::getValue));
  }
}
