package com.box.l10n.mojito.service.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TextUnitConverterGettextTest {

  TextUnitConverterGettext converter;

  @BeforeEach
  public void setup() {
    converter = new TextUnitConverterGettext();
  }

  @Test
  public void testConversion() {
    TextUnitDTO textUnit = new TextUnitDTO();
    String source = "This is a test source string";
    String context = "com.box.l10n.mojito.service.converter.TextUnitConverterGettext.sourceString";
    textUnit.setName(source + " --- " + context);
    textUnit.setSource(source);
    textUnit.setComment("L10N: Test string only used for tests.");

    // Should be well formatted
    assertEquals(
        "#. L10N: Test string only used for tests.\n"
            + "msgctxt \"com.box.l10n.mojito.service.converter.TextUnitConverterGettext.sourceString\"\n"
            + "msgid \"This is a test source string\"\n"
            + "msgstr \"\"\n\n",
        converter.convert(textUnit));

    // Remove context
    textUnit.setName(source);
    assertEquals(
        "#. L10N: Test string only used for tests.\n"
            + "msgid \"This is a test source string\"\n"
            + "msgstr \"\"\n\n",
        converter.convert(textUnit));

    // Remove comment
    textUnit.setComment(null);
    assertEquals(
        "msgid \"This is a test source string\"\n" + "msgstr \"\"\n\n",
        converter.convert(textUnit));

    // Add context without comment
    textUnit.setName(source + " --- " + context);
    assertEquals(
        "msgctxt \"com.box.l10n.mojito.service.converter.TextUnitConverterGettext.sourceString\"\n"
            + "msgid \"This is a test source string\"\n"
            + "msgstr \"\"\n\n",
        converter.convert(textUnit));

    // Test escapes correctly
    textUnit.setComment("L10N: The \"quotes\" here should not be escaped");
    textUnit.setName("Name of the string \"here\"");
    textUnit.setSource("Name of the string \"here\"");

    assertEquals(
        "#. L10N: The \"quotes\" here should not be escaped\n"
            + "msgid \"Name of the string \\\"here\\\"\"\n"
            + "msgstr \"\"\n\n",
        converter.convert(textUnit));
  }

  @Test
  public void testPluralConversion() {
    TextUnitDTO textUnit = new TextUnitDTO();

    String singular = "{count} book";
    String plural = "{count} books";

    String context = "com.box.l10n.mojito.service.converter.TextUnitConverterGettext.sourceString";
    textUnit.setName(singular + " --- " + context + " _other");
    textUnit.setSource(plural);
    textUnit.setComment("L10N: Test string only used for tests.");
    textUnit.setPluralForm("other");
    textUnit.setPluralFormOther(plural + " _other");

    assertEquals(
        "#. L10N: Test string only used for tests.\n"
            + "msgctxt \"com.box.l10n.mojito.service.converter.TextUnitConverterGettext.sourceString\"\n"
            + "msgid \"{count} book\"\n"
            + "msgid_plural \"{count} books\"\n"
            + "msgstr[0] \"\"\n"
            + "msgstr[1] \"\"\n\n",
        converter.convert(textUnit));

    textUnit.setComment(null);
    assertEquals(
        "msgctxt \"com.box.l10n.mojito.service.converter.TextUnitConverterGettext.sourceString\"\n"
            + "msgid \"{count} book\"\n"
            + "msgid_plural \"{count} books\"\n"
            + "msgstr[0] \"\"\n"
            + "msgstr[1] \"\"\n\n",
        converter.convert(textUnit));

    textUnit.setName(singular + " _other");
    assertEquals(
        "msgid \"{count} book\"\n"
            + "msgid_plural \"{count} books\"\n"
            + "msgstr[0] \"\"\n"
            + "msgstr[1] \"\"\n\n",
        converter.convert(textUnit));
  }
}
