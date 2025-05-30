package com.box.l10n.mojito.cli.command;

import static com.box.l10n.mojito.cli.command.checks.AbstractCliChecker.BULLET_POINT;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.google.common.collect.Lists;
import java.util.List;
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

  public void testPOMultiCommentForSameSourceAndTarget() {
    // we test that removing usage of a string does not re-trigger checking

  }
}
