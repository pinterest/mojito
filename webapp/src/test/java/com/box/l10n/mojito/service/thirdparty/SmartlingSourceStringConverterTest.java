package com.box.l10n.mojito.service.thirdparty;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import org.junit.Test;

public class SmartlingSourceStringConverterTest {
  @Test
  public void testConvert_ConvertsJavaPlaceholdersToPositional() {
    SmartlingSourceStringConverter converter = new SmartlingSourceStringConverter();

    String input = "Hello %s, you have %d messages and %.2f dollars";
    String expected = "Hello %1$s, you have %2$d messages and %3$.2f dollars";

    assertThat(
            converter.convert(
                input, Collections.singletonList("smartling-placeholder-format=java")))
        .isEqualTo(expected);
  }

  @Test
  public void testConvert_ReturnsSameInputString() {
    SmartlingSourceStringConverter converter = new SmartlingSourceStringConverter();

    String input = "Text without extra spaces or HTML tags";

    assertThat(converter.convert(input, Collections.emptyList())).isEqualTo(input);
  }

  @Test
  public void testConvert_ReturnsSameInputHtmlString() {
    SmartlingSourceStringConverter converter = new SmartlingSourceStringConverter();

    String input = "<div id='test'>Text</div>";

    assertThat(converter.convert(input, Collections.emptyList())).isEqualTo(input);
  }

  @Test
  public void testConvert_RemovesExtraSpaces() {
    SmartlingSourceStringConverter converter = new SmartlingSourceStringConverter();

    String input = "  Text with    extra spaces. ";
    String expected = "Text with extra spaces.";

    assertThat(converter.convert(input, Collections.emptyList())).isEqualTo(expected);
  }

  @Test
  public void testConvert_RemovesExtraSpacesInHtmlString() {
    SmartlingSourceStringConverter converter = new SmartlingSourceStringConverter();

    String input = "  <div   id='test'>  Text with    spaces  </div> ";

    String expected = "<div   id='test'> Text with spaces </div>";

    assertThat(converter.convert(input, Collections.emptyList())).isEqualTo(expected);
  }

  @Test
  public void testConvert_RemovesExtraSpacesInTextWithDoubleQuotes() {
    SmartlingSourceStringConverter converter = new SmartlingSourceStringConverter();

    String input = "  Text with    extra spaces and \"double quotes\". ";
    String expected = "Text with extra spaces and \"double quotes\".";

    assertThat(converter.convert(input, Collections.emptyList())).isEqualTo(expected);
  }

  @Test
  public void testConvert_RemovesExtraSpacesInContentOfHtmlElementWithDoubleQuotes() {
    SmartlingSourceStringConverter converter = new SmartlingSourceStringConverter();

    String input = "  <div   id='test'>  Text with    spaces and \"double quotes\"  </div>. ";
    String expected = "<div   id='test'> Text with spaces and &quot;double quotes&quot; </div>.";

    assertThat(converter.convert(input, Collections.emptyList())).isEqualTo(expected);
  }

  @Test
  public void testConvert_RemovesExtraSpacesInHtmlTagWithDoubleQuotes() {
    SmartlingSourceStringConverter converter = new SmartlingSourceStringConverter();

    String input = "  <div   id=\"test\">  Text with    spaces and \"double quotes\"  </div>. ";
    String expected = "<div id=\"test\"> Text with spaces and &quot;double quotes&quot; </div>.";

    assertThat(converter.convert(input, Collections.emptyList())).isEqualTo(expected);
  }

  @Test
  public void testConvert_RemovesExtraSpacesInRegularMultilineString() {
    SmartlingSourceStringConverter converter = new SmartlingSourceStringConverter();

    String input =
        """
        Multiline
        text
            with spaces
        """;
    String expected =
        """
        Multiline
        text
         with spaces""";

    assertThat(converter.convert(input, Collections.emptyList())).isEqualTo(expected);
  }

  @Test
  public void testConvert_ConvertsNewlinesInSingleQuotedAttributeValuesToLiteralBackslashN() {
    SmartlingSourceStringConverter converter = new SmartlingSourceStringConverter();

    String input =
        "<img src='https://i.image.com/c701359316f68ba1ea145dd251fd3571.png' "
            + "style='display: block;\n"
            + "                width:auto;\n"
            + "                max-width: 70%;\n"
            + "                height:auto;\n"
            + "                margin-left: auto;\n"
            + "               margin-right: auto;'>";

    String expected =
        "<img src='https://i.image.com/c701359316f68ba1ea145dd251fd3571.png' "
            + "style='display: block;\\n"
            + "                width:auto;\\n"
            + "                max-width: 70%;\\n"
            + "                height:auto;\\n"
            + "                margin-left: auto;\\n"
            + "               margin-right: auto;'>";

    assertThat(converter.convert(input, Collections.emptyList())).isEqualTo(expected);
  }

  @Test
  public void testConvert_EscapesNewlinesInSingleQuotedAttributesEvenIfValueContainsDoubleQuote() {
    SmartlingSourceStringConverter converter = new SmartlingSourceStringConverter();

    String input = "<img style='foo\"bar\n    baz'>";
    String expected = "<img style='foo\"bar\\n    baz'>";

    assertThat(converter.convert(input, Collections.emptyList())).isEqualTo(expected);
  }

  @Test
  public void testConvert_DoesNotDoubleEscapeHtmlEntities() {
    SmartlingSourceStringConverter converter = new SmartlingSourceStringConverter();

    String input = "<p>&quot;&quot;</p>";

    assertThat(converter.convert(input, Collections.emptyList())).isEqualTo(input);
  }

  @Test
  public void testConvert_UnescapesNbspEntities() {
    SmartlingSourceStringConverter converter = new SmartlingSourceStringConverter();

    String input = "<p>&nbsp;&nbsp;</p>";
    String expected = "<p>\u00A0\u00A0</p>";

    assertThat(converter.convert(input, Collections.emptyList())).isEqualTo(expected);
  }

  @Test
  public void testConvert_UnescapesNbspButKeepsOtherEntities() {
    SmartlingSourceStringConverter converter = new SmartlingSourceStringConverter();

    String input = "<p>&nbsp;&quot;</p>";
    String expected = "<p>\u00A0&quot;</p>";

    assertThat(converter.convert(input, Collections.emptyList())).isEqualTo(expected);
  }

  @Test
  public void testConvert_UnescapesNbspButKeepsOtherEntitiesInPlainText() {
    SmartlingSourceStringConverter converter = new SmartlingSourceStringConverter();

    String input = "Company&nbsp;&amp;&nbsp;  Co";
    String expected = "Company\u00A0&amp;\u00A0 Co";

    assertThat(converter.convert(input, Collections.emptyList())).isEqualTo(expected);
  }
}
