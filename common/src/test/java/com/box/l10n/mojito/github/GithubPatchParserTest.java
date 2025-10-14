package com.box.l10n.mojito.github;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.junit.Test;

public class GithubPatchParserTest {

  GithubPatchParser parser = new GithubPatchParser();

  @Test
  public void testGetAddedLines_NullPatch() {
    Set<Integer> result = parser.getAddedLines(null);
    assertTrue(result.isEmpty());
  }

  @Test
  public void testGetAddedLines_EmptyPatch() {
    Set<Integer> result = parser.getAddedLines("");
    assertTrue(result.isEmpty());
  }

  @Test
  public void testGetAddedLines_SingleAddition() {
    String patch =
        """
        @@ -1,1 +1,2 @@
        +added line
        """;
    Set<Integer> result = parser.getAddedLines(patch);
    assertEquals(Set.of(1), result);
  }

  @Test
  public void testGetAddedLines_MultipleAdditions() {
    String patch =
        """
        @@ -1,2 +1,3 @@
        +line1
        +line2
        """;
    Set<Integer> result = parser.getAddedLines(patch);
    assertEquals(Set.of(1, 2), result);
  }

  @Test
  public void testGetAddedLines_AdditionsAndRemovals() {
    String patch =
        """
        @@ -10,5 +10,6 @@
        -line removed
        +line added context
        """;
    Set<Integer> result = parser.getAddedLines(patch);
    assertEquals(Set.of(10), result);
  }

  @Test
  public void testGetAddedLines_IgnoresPatchMetaLines() {
    String patch =
        """
        @@ -2,2 +2,3 @@
        +++ b/file.txt
        +new line""";
    Set<Integer> result = parser.getAddedLines(patch);
    assertEquals(Set.of(3), result);
  }

  @Test
  public void testGetAddedLines_Edit_AndLargeInsertion() {
    String patch =
        """
        @@ -25,6 +25,7 @@ import com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificati
        import com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckThirdPartyNotificationService;
        import com.box.l10n.mojito.cli.command.utils.SarifFileGenerator;
        import com.box.l10n.mojito.cli.console.ConsoleWriter;
        +import com.box.l10n.mojito.github.GithubClient;
        import com.box.l10n.mojito.github.GithubClients;
        import com.box.l10n.mojito.github.GithubException;
        import com.box.l10n.mojito.json.ObjectMapper;
        @@ -368,9 +369,8 @@ public class ExtractionCheckCommand extends Command {
         }

         Sarif sarif = sarifFileGenerator.generateSarifFile(cliCheckerFailures, assetExtractionDiffs);
        -    String fileName = "i18n-checks.sarif.json";
         try {
        -      Path filePath = Paths.get(".", fileName);
        +      Path filePath = Paths.get(".", "i18n-checks.sarif.json");
           String sarifString = objectMapper.writeValueAsString(sarif);
           Files.writeString(filePath, sarifString);
           consoleWriter.fg(Ansi.Color.CYAN).newLine().a("SARIF file generated in cwd").println();
        @@ -379,6 +379,16 @@ public class ExtractionCheckCommand extends Command {
         }
        }

        +  private List<Integer> getLineNumbers() {
        +      if (!githubClients.isClientAvailable(githubOwner)) {
        +          return new ArrayList<>(){};
        +      }
        +
        +      GithubClient client = githubClients.getClient(githubOwner);
        +      client.get
        +      return new ArrayList<>(){};
        +  }
        +
        private boolean isSetGithubStatus() {
         return setGithubCommitStatus && githubClients.isClientAvailable(githubOwner);
        }""";
    Set<Integer> result = parser.getAddedLines(patch);
    assertEquals(Set.of(28, 373, 382, 383, 384, 385, 386, 387, 388, 389, 390, 391), result);
  }
}
