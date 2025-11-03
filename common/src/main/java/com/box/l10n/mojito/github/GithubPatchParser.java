package com.box.l10n.mojito.github;

import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class GithubPatchParser {

  public Set<Integer> getAddedLines(String patch) {
    Set<Integer> changedLines = new HashSet<>();
    if (patch == null) return changedLines;

    String[] lines = patch.lines().toArray(String[]::new);
    int newLineNum = 0;

    for (String line : lines) {
      if (isHunkHeader(line)) {
        // Get the new file hunk header: @@ -old,+new @@
        String[] parts = line.split(" ");
        String newFileRange = parts[2]; // "+50,7"
        String[] newFileParts = newFileRange.substring(1).split(","); // remove '+'
        newLineNum = Integer.parseInt(newFileParts[0]) - 1;
      } else if (isLineAddition(line)) {
        newLineNum++;
        changedLines.add(newLineNum);
      } else if (!isLineRemoval(line)) {
        newLineNum++;
      }
    }

    return changedLines;
  }

  private static boolean isLineRemoval(String line) {
    return line.startsWith("-") && !line.startsWith("---");
  }

  private static boolean isLineAddition(String line) {
    return line.startsWith("+") && !line.startsWith("+++");
  }

  private boolean isHunkHeader(String line) {
    return line.startsWith("@@");
  }
}
