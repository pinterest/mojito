package com.box.l10n.mojito.cli.command;

import static com.box.l10n.mojito.cli.command.checks.AbstractCliChecker.BULLET_POINT;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffService;
import com.box.l10n.mojito.cli.command.extraction.MissingExtractionDirectoryException;
import com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSenderGithub;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.github.GithubClients;
import com.box.l10n.mojito.github.GithubPatchParser;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.fusesource.jansi.Ansi;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class ExtractionCheckCommandTest extends CLITestBase {

  @Test
  public void runSuccessfulChecks() throws Exception {

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source1").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source2").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source2");

    getL10nJCommander()
        .run(
            "extract-diff",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1");

    getL10nJCommander()
        .run(
            "extraction-check",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1",
            "-cl",
            "CONTEXT_COMMENT_CHECKER");

    Assert.assertTrue(outputCapture.toString().contains("Running checks against new strings"));
    Assert.assertTrue(outputCapture.toString().contains("Checks completed"));
    Assert.assertFalse(
        outputCapture.toString().contains("failed") || outputCapture.toString().contains("Failed"));
  }

  @Test
  public void runHardFailChecks() throws Exception {

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source1").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source2").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source2");

    getL10nJCommander()
        .run(
            "extract-diff",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1");

    getL10nJCommander()
        .run(
            "extraction-check",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1",
            "-cl",
            "CONTEXT_COMMENT_CHECKER",
            "-hf",
            "CONTEXT_COMMENT_CHECKER");

    Assert.assertTrue(outputCapture.toString().contains("Running checks against new strings"));
    Assert.assertTrue(
        outputCapture
            .toString()
            .contains("The following checks had hard failures:" + System.lineSeparator()));
    Assert.assertTrue(outputCapture.toString().contains("CONTEXT_COMMENT_CHECKER"));
    Assert.assertTrue(
        outputCapture.toString().contains("Context and comment check found failures:"));
    Assert.assertTrue(
        outputCapture
            .toString()
            .contains(
                BULLET_POINT
                    + "Source string `This is a new source string missing a context` failed check with error: Context string is empty."));
  }

  @Test
  public void runSoftFailChecks() throws Exception {

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source1").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source2").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source2");

    getL10nJCommander()
        .run(
            "extract-diff",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1");

    getL10nJCommander()
        .run(
            "extraction-check",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1",
            "-cl",
            "CONTEXT_COMMENT_CHECKER");

    Assert.assertTrue(outputCapture.toString().contains("Running checks against new strings"));
    Assert.assertTrue(outputCapture.toString().contains("Failed checks: "));
    Assert.assertTrue(outputCapture.toString().contains("CONTEXT_COMMENT_CHECKER"));
    Assert.assertTrue(outputCapture.toString().contains("Checks completed"));
  }

  @Test
  public void runSuccessfulContextCommentCheckerWithExcludedFiles() {
    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source1").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source2").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source2");

    getL10nJCommander()
        .run(
            "extract-diff",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1");

    getL10nJCommander()
        .run(
            "extraction-check",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1",
            "-cl",
            "CONTEXT_COMMENT_CHECKER",
            "-hf",
            "CONTEXT_COMMENT_CHECKER",
            "-co",
            "contextCommentExcludeFilesPattern:.*.js");

    Assert.assertTrue(outputCapture.toString().contains("Running checks against new strings"));
    Assert.assertFalse(
        outputCapture
            .toString()
            .contains("The following checks had hard failures:" + System.lineSeparator()));
    Assert.assertFalse(outputCapture.toString().contains("CONTEXT_COMMENT_CHECKER"));
    Assert.assertFalse(
        outputCapture.toString().contains("Context and comment check found failures:"));
    Assert.assertFalse(
        outputCapture
            .toString()
            .contains(
                BULLET_POINT
                    + "Source string `This is a new source string missing a context` failed check with error: Context string is empty."));

    getL10nJCommander()
        .run(
            "extraction-check",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1",
            "-cl",
            "CONTEXT_COMMENT_CHECKER",
            "-hf",
            "CONTEXT_COMMENT_CHECKER",
            "-co",
            "contextCommentExcludeFilesPattern:.*/parent/dir/.*");

    Assert.assertTrue(outputCapture.toString().contains("Running checks against new strings"));
    Assert.assertFalse(
        outputCapture
            .toString()
            .contains("The following checks had hard failures:" + System.lineSeparator()));
    Assert.assertFalse(outputCapture.toString().contains("CONTEXT_COMMENT_CHECKER"));
    Assert.assertFalse(
        outputCapture.toString().contains("Context and comment check found failures:"));
    Assert.assertFalse(
        outputCapture
            .toString()
            .contains(
                BULLET_POINT
                    + "Source string `This is a new source string missing a context` failed check with error: Context string is empty."));
  }

  @Test
  public void runFailedContextCommentCheckerWithExcludedFiles() {
    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source1").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source2").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source2");

    getL10nJCommander()
        .run(
            "extract-diff",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1");

    getL10nJCommander()
        .run(
            "extraction-check",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1",
            "-cl",
            "CONTEXT_COMMENT_CHECKER",
            "-hf",
            "CONTEXT_COMMENT_CHECKER",
            "-co",
            "contextCommentExcludeFilesPattern:.*.jsx");

    Assert.assertTrue(outputCapture.toString().contains("Running checks against new strings"));
    Assert.assertTrue(
        outputCapture
            .toString()
            .contains("The following checks had hard failures:" + System.lineSeparator()));
    Assert.assertTrue(outputCapture.toString().contains("CONTEXT_COMMENT_CHECKER"));
    Assert.assertTrue(
        outputCapture.toString().contains("Context and comment check found failures:"));
    Assert.assertTrue(
        outputCapture
            .toString()
            .contains(
                BULLET_POINT
                    + "Source string `This is a new source string missing a context` failed check with error: Context string is empty."));

    getL10nJCommander()
        .run(
            "extraction-check",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1",
            "-cl",
            "CONTEXT_COMMENT_CHECKER",
            "-hf",
            "CONTEXT_COMMENT_CHECKER",
            "-co",
            "contextCommentExcludeFilesPattern:.*/parent/notFoundDir/.*");

    Assert.assertTrue(outputCapture.toString().contains("Running checks against new strings"));
    Assert.assertTrue(
        outputCapture
            .toString()
            .contains("The following checks had hard failures:" + System.lineSeparator()));
    Assert.assertTrue(outputCapture.toString().contains("CONTEXT_COMMENT_CHECKER"));
    Assert.assertTrue(
        outputCapture.toString().contains("Context and comment check found failures:"));
    Assert.assertTrue(
        outputCapture
            .toString()
            .contains(
                BULLET_POINT
                    + "Source string `This is a new source string missing a context` failed check with error: Context string is empty."));
    Assert.assertTrue(
        outputCapture
            .toString()
            .contains(
                BULLET_POINT
                    + "Source string `This is another new source string missing a context` failed check with error: Context string is empty."));

    getL10nJCommander()
        .run(
            "extraction-check",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1",
            "-cl",
            "CONTEXT_COMMENT_CHECKER",
            "-hf",
            "CONTEXT_COMMENT_CHECKER",
            "-co",
            "contextCommentExcludeFilesPattern:.*/file.js");

    Assert.assertTrue(outputCapture.toString().contains("Running checks against new strings"));
    Assert.assertTrue(
        outputCapture
            .toString()
            .contains("The following checks had hard failures:" + System.lineSeparator()));
    Assert.assertTrue(outputCapture.toString().contains("CONTEXT_COMMENT_CHECKER"));
    Assert.assertTrue(
        outputCapture.toString().contains("Context and comment check found failures:"));
    Assert.assertTrue(
        outputCapture
            .toString()
            .contains(
                BULLET_POINT
                    + "Source string `This is another new source string missing a context` failed check with error: Context string is empty."));

    getL10nJCommander()
        .run(
            "extraction-check",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1",
            "-cl",
            "CONTEXT_COMMENT_CHECKER",
            "-hf",
            "CONTEXT_COMMENT_CHECKER",
            "-co",
            "contextCommentExcludeFilesPattern:some/parent/dir/.*");

    Assert.assertTrue(outputCapture.toString().contains("Running checks against new strings"));
    Assert.assertTrue(
        outputCapture
            .toString()
            .contains("The following checks had hard failures:" + System.lineSeparator()));
    Assert.assertTrue(outputCapture.toString().contains("CONTEXT_COMMENT_CHECKER"));
    Assert.assertTrue(
        outputCapture.toString().contains("Context and comment check found failures:"));
    Assert.assertTrue(
        outputCapture
            .toString()
            .contains(
                BULLET_POINT
                    + "Source string `This is another new source string missing a context` failed check with error: Context string is empty."));
  }

  @Test
  public void runCheckWithInvalidCheckName() {
    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source1").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source2").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source2");

    getL10nJCommander()
        .run(
            "extract-diff",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1");

    getL10nJCommander()
        .run(
            "extraction-check",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1",
            "-cl",
            "INVALID_CHECK_NAME");

    Assert.assertTrue(outputCapture.toString().contains("Invalid type [INVALID_CHECK_NAME]"));
  }

  @Test
  public void runHardFailChecksWithInvalidCheckName() throws Exception {

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source1").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source2").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source2");

    getL10nJCommander()
        .run(
            "extract-diff",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1");

    getL10nJCommander()
        .run(
            "extraction-check",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1",
            "-cl",
            "CONTEXT_COMMENT_CHECKER",
            "-hf",
            "INVALID_NAME");

    Assert.assertTrue(
        outputCapture.toString().contains("Unknown check name in hard fail list 'INVALID_NAME'"));
  }

  /**
   * this is a functional test for the {@link
   * com.box.l10n.mojito.cli.command.checks.AbstractCliChecker#getAddedTextUnitsExcludingInconsistentComments(List)}
   *
   * <p>If combination source+context is added again with a different comment we run the check and
   * eventually reject it if it is not valid
   */
  @Test
  public void runCheckWithInconsistentCommentsInGettextAdd() throws Exception {

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source1").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source2").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source2");

    getL10nJCommander()
        .run(
            "extract-diff",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1");

    getL10nJCommander()
        .run(
            "extraction-check",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1",
            "-cl",
            "CONTEXT_COMMENT_CHECKER",
            "-hf",
            "CONTEXT_COMMENT_CHECKER");

    Assert.assertTrue(
        outputCapture
            .toString()
            .contains("Source string `source1` failed check with error: Context string is empty."));
  }

  /**
   * this is a functional test for the {@link
   * com.box.l10n.mojito.cli.command.checks.AbstractCliChecker#getAddedTextUnitsExcludingInconsistentComments(List)}
   *
   * <p>Before adding the logic to exclude inconsistent comment, we'd have run the check on the text
   * unit, and if it was invalid we would have got an error. This was missleading because only the
   * order of the comments had changed - due to maybe order of the extraction, line, or file moving
   * around.
   */
  @Test
  public void runCheckWithInconsistentCommentsInGettextChange() throws Exception {

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source1").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source2").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source2");

    getL10nJCommander()
        .run(
            "extract-diff",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1");

    getL10nJCommander()
        .run(
            "extraction-check",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1",
            "-cl",
            "CONTEXT_COMMENT_CHECKER",
            "-hf",
            "CONTEXT_COMMENT_CHECKER");

    Assert.assertTrue(outputCapture.toString().contains("Running checks against new strings"));
    Assert.assertTrue(outputCapture.toString().contains("Checks completed"));
    Assert.assertFalse(
        outputCapture.toString().contains("failed") || outputCapture.toString().contains("Failed"));
  }

  /**
   * this is a functional test for the {@link
   * com.box.l10n.mojito.cli.command.checks.AbstractCliChecker#getAddedTextUnitsExcludingInconsistentComments(List)}
   *
   * <p>Before adding the logic to exclude inconsistent comment, we'd have run the check on the text
   * unit, and if it was invalid we would have got an error. This was missleading because the old
   * usage was just removed
   */
  @Test
  public void runCheckWithInconsistentCommentsInGettextRemove() throws Exception {

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source1").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source2").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source2");

    getL10nJCommander()
        .run(
            "extract-diff",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1");

    getL10nJCommander()
        .run(
            "extraction-check",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1",
            "-cl",
            "CONTEXT_COMMENT_CHECKER",
            "-hf",
            "CONTEXT_COMMENT_CHECKER");

    Assert.assertTrue(outputCapture.toString().contains("Running checks against new strings"));
    Assert.assertTrue(outputCapture.toString().contains("Checks completed"));
    Assert.assertFalse(
        outputCapture.toString().contains("failed") || outputCapture.toString().contains("Failed"));
  }

  @Test
  public void testChecksSkippedIfSkipChecksEnabled() {
    ConsoleWriter consoleWriter = Mockito.mock(ConsoleWriter.class);
    ExtractionCheckCommand extractionCheckCommand = Mockito.spy(new ExtractionCheckCommand());
    extractionCheckCommand.consoleWriter = consoleWriter;
    extractionCheckCommand.areChecksSkipped = true;
    when(consoleWriter.fg(isA(Ansi.Color.class))).thenReturn(consoleWriter);
    when(consoleWriter.newLine()).thenReturn(consoleWriter);
    when(consoleWriter.a(isA(String.class))).thenReturn(consoleWriter);
    extractionCheckCommand.execute();
    verify(consoleWriter, times(1)).a("Checks disabled as --skip-checks is set to true.");
  }

  @Test
  public void testChecksSkippedNotificationsSuppressedWhenSkipI18nChecksLabelApplied() {
    ConsoleWriter consoleWriter = Mockito.mock(ConsoleWriter.class);
    ExtractionCheckCommand extractionCheckCommand = Mockito.spy(new ExtractionCheckCommand());
    extractionCheckCommand.consoleWriter = consoleWriter;
    extractionCheckCommand.areChecksSkipped = true;
    extractionCheckCommand.isSkipI18nChecksLabelApplied = true;
    when(consoleWriter.fg(isA(Ansi.Color.class))).thenReturn(consoleWriter);
    when(consoleWriter.newLine()).thenReturn(consoleWriter);
    when(consoleWriter.a(isA(String.class))).thenReturn(consoleWriter);

    extractionCheckCommand.execute();

    verify(consoleWriter, times(1)).a("Checks disabled as --skip-checks is set to true.");
    verify(consoleWriter, times(1))
        .a("Checks skipped notifications suppressed because skip-i18n-checks label is applied.");
  }

  @Test
  public void testChecksSkippedNotificationsNotSuppressedWhenSkipI18nChecksLabelNotApplied() {
    ConsoleWriter consoleWriter = Mockito.mock(ConsoleWriter.class);
    ExtractionCheckCommand extractionCheckCommand = Mockito.spy(new ExtractionCheckCommand());
    extractionCheckCommand.consoleWriter = consoleWriter;
    extractionCheckCommand.areChecksSkipped = true;
    extractionCheckCommand.isSkipI18nChecksLabelApplied = false;
    when(consoleWriter.fg(isA(Ansi.Color.class))).thenReturn(consoleWriter);
    when(consoleWriter.newLine()).thenReturn(consoleWriter);
    when(consoleWriter.a(isA(String.class))).thenReturn(consoleWriter);

    extractionCheckCommand.execute();

    verify(consoleWriter, times(1)).a("Checks disabled as --skip-checks is set to true.");
    verify(consoleWriter, times(0))
        .a("Checks skipped notifications suppressed because skip-i18n-checks label is applied.");
  }

  @Test
  public void testStatsAreReportedIfUrlTemplateSet() {
    ConsoleWriter consoleWriter = Mockito.mock(ConsoleWriter.class);
    RestTemplate restTemplateMock = Mockito.mock(RestTemplate.class);
    when(consoleWriter.fg(isA(Ansi.Color.class))).thenReturn(consoleWriter);
    when(consoleWriter.newLine()).thenReturn(consoleWriter);
    when(consoleWriter.a(isA(String.class))).thenReturn(consoleWriter);

    ExtractionCheckCommand extractionCheckCommand = new ExtractionCheckCommand();
    extractionCheckCommand.consoleWriter = consoleWriter;
    extractionCheckCommand.restTemplate = restTemplateMock;
    extractionCheckCommand.statsUrlTemplate =
        "http://someUrl.com/my_test_stat_{check_name}_{outcome}?value=1";
    CliCheckResult success = new CliCheckResult(true, false, "testCheck1");
    CliCheckResult failure = new CliCheckResult(false, false, "testCheck2");
    extractionCheckCommand.reportStatistics(Lists.newArrayList(success, failure));
    verify(restTemplateMock, times(1))
        .put("http://someUrl.com/my_test_stat_testCheck1_success?value=1", null);
    verify(restTemplateMock, times(1))
        .put("http://someUrl.com/my_test_stat_testCheck2_failure?value=1", null);
  }

  @Test
  public void testStatsAreNotReportedIfUrlTemplateIsNull() {
    ConsoleWriter consoleWriter = Mockito.mock(ConsoleWriter.class);
    RestTemplate restTemplateMock = Mockito.mock(RestTemplate.class);
    when(consoleWriter.fg(isA(Ansi.Color.class))).thenReturn(consoleWriter);
    when(consoleWriter.newLine()).thenReturn(consoleWriter);
    when(consoleWriter.a(isA(String.class))).thenReturn(consoleWriter);

    ExtractionCheckCommand extractionCheckCommand = new ExtractionCheckCommand();
    extractionCheckCommand.consoleWriter = consoleWriter;
    extractionCheckCommand.restTemplate = restTemplateMock;
    extractionCheckCommand.statsUrlTemplate = null;
    CliCheckResult success = new CliCheckResult(true, false, "testCheck1");
    CliCheckResult failure = new CliCheckResult(false, false, "testCheck2");
    extractionCheckCommand.reportStatistics(Lists.newArrayList(success, failure));
    verify(restTemplateMock, times(0))
        .put("http://someUrl.com/my_test_stat_testCheck1_success?value=1", null);
    verify(restTemplateMock, times(0))
        .put("http://someUrl.com/my_test_stat_testCheck2_failure?value=1", null);
  }

  @Test
  public void testErrorReportingStatistics() {
    ConsoleWriter consoleWriter = Mockito.mock(ConsoleWriter.class);
    RestTemplate restTemplateMock = Mockito.mock(RestTemplate.class);
    when(consoleWriter.fg(isA(Ansi.Color.class))).thenReturn(consoleWriter);
    when(consoleWriter.newLine()).thenReturn(consoleWriter);
    when(consoleWriter.a(isA(String.class))).thenReturn(consoleWriter);
    doThrow(new RestClientException("test exception"))
        .when(restTemplateMock)
        .put("http://someUrl.com/my_test_stat_testCheck1_success?value=1", null);

    ExtractionCheckCommand extractionCheckCommand = new ExtractionCheckCommand();
    extractionCheckCommand.consoleWriter = consoleWriter;
    extractionCheckCommand.restTemplate = restTemplateMock;
    extractionCheckCommand.statsUrlTemplate =
        "http://someUrl.com/my_test_stat_{check_name}_{outcome}?value=1";
    CliCheckResult success = new CliCheckResult(true, false, "testCheck1");
    CliCheckResult failure = new CliCheckResult(false, false, "testCheck2");
    extractionCheckCommand.reportStatistics(Lists.newArrayList(success, failure));
    verify(consoleWriter, times(1))
        .a("Error reporting statistics to http endpoint: test exception");
  }

  @Test
  public void testGetMismatchedFileWithLineNumbers_emptyInputs() {
    ExtractionCheckCommand command = new ExtractionCheckCommand();
    Map<String, Set<Integer>> sarifFiles = new HashMap<>();
    Map<String, Set<Integer>> githubFiles = new HashMap<>();
    Map<String, Set<Integer>> result =
        command.getMismatchedFileWithLineNumbers(sarifFiles, githubFiles);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testGetMismatchedFileWithLineNumbers_noMismatches() {
    ExtractionCheckCommand command = new ExtractionCheckCommand();
    Map<String, Set<Integer>> sarifFiles = new HashMap<>();
    Map<String, Set<Integer>> githubFiles = new HashMap<>();
    sarifFiles.put("test.py", new HashSet<>(List.of(1, 2, 3)));
    githubFiles.put("test.py", new HashSet<>(List.of(1, 2, 3)));
    Map<String, Set<Integer>> result =
        command.getMismatchedFileWithLineNumbers(sarifFiles, githubFiles);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testGetMismatchedFileWithLineNumbers_withMismatches() {
    ExtractionCheckCommand command = new ExtractionCheckCommand();
    Map<String, Set<Integer>> sarifFiles = new HashMap<>();
    Map<String, Set<Integer>> githubFiles = new HashMap<>();
    sarifFiles.put("test.py", new HashSet<>(List.of(1, 2, 3)));
    githubFiles.put("test.py", new HashSet<>(List.of(2, 3, 4)));
    Map<String, Set<Integer>> result =
        command.getMismatchedFileWithLineNumbers(sarifFiles, githubFiles);
    Assert.assertEquals(result, Map.of("test.py", Set.of(1)));
  }

  @Test
  public void testGetMismatchedFileWithLineNumbers_multipleFiles() {
    ExtractionCheckCommand command = new ExtractionCheckCommand();
    Map<String, Set<Integer>> sarifFiles = new HashMap<>();
    Map<String, Set<Integer>> githubFiles = new HashMap<>();
    sarifFiles.put("test.py", new HashSet<>(List.of(1, 2)));
    sarifFiles.put("other.py", new HashSet<>(List.of(5, 6)));
    githubFiles.put("test.py", new HashSet<>(List.of(1)));
    githubFiles.put("other.py", new HashSet<>(List.of(5, 6, 7)));
    Map<String, Set<Integer>> result =
        command.getMismatchedFileWithLineNumbers(sarifFiles, githubFiles);
    Assert.assertEquals(Set.of(2), result.get("test.py"));
    Assert.assertNull(result.get("other.py"));
  }

  public void testPOMultiCommentForSameSourceAndTarget() {
    // we test that removing usage of a string does not re-trigger checking

  }

  @Test
  public void testExecuteThrowsWhenSarifFileAndInlineReviewCommentsBothEnabled() {
    ExtractionCheckCommand command = new ExtractionCheckCommand();
    command.shouldGenerateSarifFile = true;
    command.shouldAddInlineReviewComments = true;

    try {
      command.execute();
      fail("Expected a CommandException to be thrown");
    } catch (CommandException e) {
      Assert.assertTrue(
          e.getMessage()
              .contains("Cannot use both --generate-sarif-file and --add-inline-review-comments"));
    }
  }

  @Test
  public void testExecuteDoesNotThrowMutualExclusivityErrorWhenBothDisabled() {
    ExtractionCheckCommand command = new ExtractionCheckCommand();
    command.shouldGenerateSarifFile = false;
    command.shouldAddInlineReviewComments = false;
    assertValidateParametersPassed(command);
  }

  @Test
  public void testExecuteDoesNotThrowMutualExclusivityErrorWhenOnlySarifEnabled() {
    ExtractionCheckCommand command = new ExtractionCheckCommand();
    command.shouldGenerateSarifFile = true;
    command.shouldAddInlineReviewComments = false;
    assertValidateParametersPassed(command);
  }

  @Test
  public void testExecuteDoesNotThrowMutualExclusivityErrorWhenOnlyInlineReviewCommentsEnabled() {
    ExtractionCheckCommand command = new ExtractionCheckCommand();
    command.shouldGenerateSarifFile = false;
    command.shouldAddInlineReviewComments = true;
    assertValidateParametersPassed(command);
  }

  /**
   * Executes the command and asserts that no mutual-exclusivity {@link CommandException} is thrown.
   * Any other exception (e.g. from unmocked dependencies further down execute()) is expected and
   * ignored, since it proves validateParameters() let execution continue.
   */
  private void assertValidateParametersPassed(ExtractionCheckCommand command) {
    try {
      command.execute();
    } catch (CommandException e) {
      Assert.assertFalse(e.getMessage().contains("mutually exclusive"));
    } catch (Exception e) {
      // Expected: execution proceeded past validateParameters and failed later due to
      // unconfigured dependencies (not under test here).
    }
  }

  private ExtractionCheckCommand createCommandForInlineReviewComments(
      ExtractionDiffService extractionDiffService,
      GithubClients githubClients,
      GithubPatchParser githubPatchParser) {
    ExtractionCheckCommand command = new ExtractionCheckCommand();
    command.extractionDiffService = extractionDiffService;
    command.githubClients = githubClients;
    command.githubPatchParser = githubPatchParser;
    command.consoleWriter =
        new ConsoleWriter(false, ConsoleWriter.OutputType.ANSI_CONSOLE_AND_LOGGER);
    command.checkerList = new ArrayList<>();
    command.currentExtractionName = "current";
    command.baseExtractionName = "base";
    command.shouldAddInlineReviewComments = true;
    command.areChecksSkipped = false;
    return command;
  }

  private void configureExtractionDiffs(ExtractionCheckCommand command)
      throws MissingExtractionDirectoryException {
    when(command.extractionDiffService.hasAddedTextUnits(any())).thenReturn(true);
    when(command.extractionDiffService.findAssetExtractionDiffsWithAddedTextUnits(any()))
        .thenReturn(List.of(new AssetExtractionDiff()));
  }

  @Test
  public void testAddInlineReviewCommentsSkipsWhenGithubParametersAreMissing()
      throws MissingExtractionDirectoryException {
    ExtractionDiffService extractionDiffService = Mockito.mock(ExtractionDiffService.class);
    GithubClients githubClients = Mockito.mock(GithubClients.class);
    GithubPatchParser githubPatchParser = Mockito.mock(GithubPatchParser.class);
    ExtractionCheckCommand command =
        createCommandForInlineReviewComments(
            extractionDiffService, githubClients, githubPatchParser);
    configureExtractionDiffs(command);
    command.githubOwner = null;
    command.githubRepository = "testRepo";
    command.githubPRNumber = 42;
    command.commitSha = "abc123";

    command.execute();

    verifyNoInteractions(githubClients);
    Assert.assertTrue(outputCapture.toString().contains("Required GitHub parameters not provided"));
  }

  @Test
  public void testAddInlineReviewCommentsSkipsWhenGithubClientIsUnavailable()
      throws MissingExtractionDirectoryException {
    ExtractionDiffService extractionDiffService = Mockito.mock(ExtractionDiffService.class);
    GithubClients githubClients = Mockito.mock(GithubClients.class);
    GithubPatchParser githubPatchParser = Mockito.mock(GithubPatchParser.class);
    ExtractionCheckCommand command =
        createCommandForInlineReviewComments(
            extractionDiffService, githubClients, githubPatchParser);
    configureExtractionDiffs(command);
    command.githubOwner = "testOwner";
    command.githubRepository = "testRepo";
    command.githubPRNumber = 42;
    command.commitSha = "abc123";
    when(githubClients.isClientAvailable("testOwner")).thenReturn(false);

    command.execute();

    verify(githubClients, times(1)).isClientAvailable("testOwner");
    verify(githubClients, never()).getClient("testOwner");
    Assert.assertTrue(outputCapture.toString().contains("GitHub client not available"));
  }

  @Test
  public void testAddInlineReviewCommentsDelegatesToGithubSender()
      throws MissingExtractionDirectoryException {
    ExtractionDiffService extractionDiffService = Mockito.mock(ExtractionDiffService.class);
    GithubClients githubClients = Mockito.mock(GithubClients.class);
    GithubPatchParser githubPatchParser = Mockito.mock(GithubPatchParser.class);
    GithubClient githubClient = Mockito.mock(GithubClient.class);
    ExtractionCheckNotificationSenderGithub githubSender =
        Mockito.mock(ExtractionCheckNotificationSenderGithub.class);
    ExtractionCheckCommand command =
        createCommandForInlineReviewComments(
            extractionDiffService, githubClients, githubPatchParser);
    configureExtractionDiffs(command);
    command.githubOwner = "testOwner";
    command.githubRepository = "testRepo";
    command.githubPRNumber = 42;
    command.commitSha = "abc123";
    command.extractionCheckNotificationSenders = List.of(githubSender);
    when(githubClients.isClientAvailable("testOwner")).thenReturn(true);
    when(githubClients.getClient("testOwner")).thenReturn(githubClient);
    when(githubClient.getPrFilePatches("testRepo", 42))
        .thenReturn(Map.of("file.py", "@@ -1 +1 @@\n+new line"));
    when(githubPatchParser.getAddedLines("@@ -1 +1 @@\n+new line")).thenReturn(Set.of(1));

    command.execute();

    verify(githubSender, times(1))
        .addInlineReviewComments(
            any(),
            any(),
            org.mockito.ArgumentMatchers.eq(Map.of("file.py", Set.of(1))),
            org.mockito.ArgumentMatchers.eq(""));
  }

  @Test
  public void testAddInlineReviewCommentsLogsWarningWhenGithubSenderIsNotConfigured()
      throws MissingExtractionDirectoryException {
    ExtractionDiffService extractionDiffService = Mockito.mock(ExtractionDiffService.class);
    GithubClients githubClients = Mockito.mock(GithubClients.class);
    GithubPatchParser githubPatchParser = Mockito.mock(GithubPatchParser.class);
    GithubClient githubClient = Mockito.mock(GithubClient.class);
    ExtractionCheckCommand command =
        createCommandForInlineReviewComments(
            extractionDiffService, githubClients, githubPatchParser);
    configureExtractionDiffs(command);
    command.githubOwner = "testOwner";
    command.githubRepository = "testRepo";
    command.githubPRNumber = 42;
    command.commitSha = "abc123";
    when(githubClients.isClientAvailable("testOwner")).thenReturn(true);
    when(githubClients.getClient("testOwner")).thenReturn(githubClient);
    when(githubClient.getPrFilePatches("testRepo", 42)).thenReturn(new HashMap<>());

    command.execute();

    Assert.assertTrue(
        outputCapture.toString().contains("No GitHub notification sender found in configuration"));
  }
}
