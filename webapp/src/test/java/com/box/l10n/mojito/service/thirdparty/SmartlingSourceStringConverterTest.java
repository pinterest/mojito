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
}
