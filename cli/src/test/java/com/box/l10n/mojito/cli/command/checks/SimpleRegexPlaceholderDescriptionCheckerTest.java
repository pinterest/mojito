package com.box.l10n.mojito.cli.command.checks;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;

import com.box.l10n.mojito.regex.PlaceholderRegularExpressions;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SimpleRegexPlaceholderDescriptionCheckerTest {

  private SimpleRegexPlaceholderDescriptionChecker simpleRegexPlaceholderDescriptionChecker;

  @Before
  public void setup() {
    simpleRegexPlaceholderDescriptionChecker =
        new SimpleRegexPlaceholderDescriptionChecker(
            PlaceholderRegularExpressions.SIMPLE_PRINTF_REGEX);
  }

  @Test
  public void testSuccess() {
    String source = "There is %1 books on %1 shelves";
    String comment = "Test comment %1:The number of books and shelves";
    Set<String> failures =
        simpleRegexPlaceholderDescriptionChecker
            .checkCommentForDescriptions(source, comment)
            .stream()
            .map(CliCheckResult.CheckFailure::failureMessage)
            .collect(Collectors.toSet());
    Assert.assertTrue(failures.isEmpty());
  }

  @Test
  public void testFailure() {
    String source = "There is %1 books";
    String comment = "Test comment";
    Set<String> failures =
        simpleRegexPlaceholderDescriptionChecker
            .checkCommentForDescriptions(source, comment)
            .stream()
            .map(CliCheckResult.CheckFailure::failureMessage)
            .collect(Collectors.toSet());
    Assert.assertEquals(1, failures.size());
    Assert.assertTrue(
        failures.contains(
            "Missing description for placeholder with name "
                + QUOTE_MARKER
                + "%1"
                + QUOTE_MARKER
                + " in comment. Please add a description in the string comment in the form `%1:<description>`"));
  }

  @Test
  public void testNullComment() {
    String source = "There is %1 books";
    String comment = null;
    Set<String> failures =
        simpleRegexPlaceholderDescriptionChecker
            .checkCommentForDescriptions(source, comment)
            .stream()
            .map(CliCheckResult.CheckFailure::failureMessage)
            .collect(Collectors.toSet());
    Assert.assertEquals(1, failures.size());
    Assert.assertTrue(
        failures.contains(
            "Missing description for placeholder with name "
                + QUOTE_MARKER
                + "%1"
                + QUOTE_MARKER
                + " in comment. Please add a description in the string comment in the form `%1:<description>`"));
  }

  @Test
  public void testFailureWithMultiplePlaceholders() {
    String source = "There is %1 books and %2 shelves";
    String comment = "Test comment %1:The number of books";
    Set<String> failures =
        simpleRegexPlaceholderDescriptionChecker
            .checkCommentForDescriptions(source, comment)
            .stream()
            .map(CliCheckResult.CheckFailure::failureMessage)
            .collect(Collectors.toSet());
    Assert.assertEquals(1, failures.size());
    Assert.assertTrue(
        failures.contains(
            "Missing description for placeholder with name "
                + QUOTE_MARKER
                + "%2"
                + QUOTE_MARKER
                + " in comment. Please add a description in the string comment in the form `%2:<description>`"));
  }

  @Test
  public void testNoSpecifierRegexSuccess() {
    String source = "There is %d books on %d shelves";
    String comment = "Test comment %d:The number of books and shelves";
    simpleRegexPlaceholderDescriptionChecker.setPattern(
        Pattern.compile(PlaceholderRegularExpressions.PLACEHOLDER_NO_SPECIFIER_REGEX.getRegex()));
    Set<String> failures =
        simpleRegexPlaceholderDescriptionChecker
            .checkCommentForDescriptions(source, comment)
            .stream()
            .map(CliCheckResult.CheckFailure::failureMessage)
            .collect(Collectors.toSet());
    Assert.assertTrue(failures.isEmpty());
  }

  @Test
  public void testNoSpecifierRegexFailure() {
    String source = "There is %d books on %d shelves";
    String comment = "Test comment";
    simpleRegexPlaceholderDescriptionChecker.setPattern(
        Pattern.compile(PlaceholderRegularExpressions.PLACEHOLDER_NO_SPECIFIER_REGEX.getRegex()));
    Set<String> failures =
        simpleRegexPlaceholderDescriptionChecker
            .checkCommentForDescriptions(source, comment)
            .stream()
            .map(CliCheckResult.CheckFailure::failureMessage)
            .collect(Collectors.toSet());
    Assert.assertEquals(1, failures.size());
    Assert.assertTrue(
        failures.contains(
            "Missing description for placeholder with name "
                + QUOTE_MARKER
                + "%d"
                + QUOTE_MARKER
                + " in comment. Please add a description in the string comment in the form `%d:<description>`"));
  }

  @Test
  public void testNoSpecifierRegexFailureWithMultiplePlaceholders() {
    String source = "There is %d books and %s shelves";
    String comment = "Test comment %d:The number of books";
    simpleRegexPlaceholderDescriptionChecker.setPattern(
        Pattern.compile(PlaceholderRegularExpressions.PLACEHOLDER_NO_SPECIFIER_REGEX.getRegex()));
    Set<String> failures =
        simpleRegexPlaceholderDescriptionChecker
            .checkCommentForDescriptions(source, comment)
            .stream()
            .map(CliCheckResult.CheckFailure::failureMessage)
            .collect(Collectors.toSet());
    Assert.assertEquals(1, failures.size());
    Assert.assertTrue(
        failures.contains(
            "Missing description for placeholder with name "
                + QUOTE_MARKER
                + "%s"
                + QUOTE_MARKER
                + " in comment. Please add a description in the string comment in the form `%s:<description>`"));
  }
}
