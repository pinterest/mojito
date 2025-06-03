package com.box.l10n.mojito.cli.command.checks;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public abstract class AbstractPlaceholderDescriptionCheck {

  public abstract Set<CliCheckResult.CheckFailure> checkCommentForDescriptions(
      String source, String comment);

  public Optional<CliCheckResult.CheckFailure> getCheckFailure(String placeholder) {
    CliCheckResult.CheckFailure checkFailure = null;
    if (StringUtils.isNumeric(placeholder)) {
      checkFailure =
          new CliCheckResult.CheckFailure(
              "MISSING_DESCRIPTION_FOR_NUMERIC_PLACEHOLDER",
              "Missing description for placeholder number "
                  + QUOTE_MARKER
                  + placeholder
                  + QUOTE_MARKER
                  + " in comment. Please add a description in the string comment in the form "
                  + QUOTE_MARKER
                  + placeholder
                  + ":<description>"
                  + QUOTE_MARKER);
    } else if (!placeholder.trim().isEmpty()) {
      checkFailure =
          new CliCheckResult.CheckFailure(
              "MISSING_NAMED_PLACEHOLDER_DESCRIPTION",
              "Missing description for placeholder with name "
                  + QUOTE_MARKER
                  + placeholder
                  + QUOTE_MARKER
                  + " in comment. Please add a description in the string comment in the form "
                  + QUOTE_MARKER
                  + placeholder
                  + ":<description>"
                  + QUOTE_MARKER);
    }
    return Optional.ofNullable(checkFailure);
  }

  protected boolean isPlaceholderDescriptionMissingInComment(String comment, String placeholder) {
    return StringUtils.isBlank(comment)
        || !Pattern.compile(Pattern.quote(placeholder) + ":.+").matcher(comment).find();
  }
}
