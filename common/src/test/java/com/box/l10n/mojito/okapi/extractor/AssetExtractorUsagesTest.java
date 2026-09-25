package com.box.l10n.mojito.okapi.extractor;

import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class AssetExtractorUsagesTest {

  static final String STRINGS_XML =
      """
      <?xml version="1.0" encoding="utf-8"?>
      <resources>
          <!-- Title of merge option within Edit board page -->
          <string name="merge_board">Merge board</string>

          <string name="board_reorder_changes_saved">Changes saved!</string>

          <plurals name="people">
              <item quantity="one">%1$d person</item>
              <item quantity="other">%1$d people</item>
          </plurals>
      </resources>
      """;

  AssetExtractor assetExtractor = new AssetExtractor();

  @Test
  public void addUsagesFromNameDeclarationLine() {
    AssetExtractorTextUnit mergeBoard = createTextUnit("merge_board", null);
    AssetExtractorTextUnit changesSaved = createTextUnit("board_reorder_changes_saved", null);

    assetExtractor.addUsagesFromNameDeclarationLine(
        List.of(mergeBoard, changesSaved), STRINGS_XML, "res/values/strings.xml");

    Assertions.assertThat(mergeBoard.getUsages()).containsExactly("res/values/strings.xml:4");
    Assertions.assertThat(changesSaved.getUsages()).containsExactly("res/values/strings.xml:6");
  }

  @Test
  public void addUsagesFromNameDeclarationLineForPluralForms() {
    AssetExtractorTextUnit one = createTextUnit("people_one", "one");
    AssetExtractorTextUnit other = createTextUnit("people_other", "other");

    assetExtractor.addUsagesFromNameDeclarationLine(
        List.of(one, other), STRINGS_XML, "res/values/strings.xml");

    Assertions.assertThat(one.getUsages()).containsExactly("res/values/strings.xml:8");
    Assertions.assertThat(other.getUsages()).containsExactly("res/values/strings.xml:8");
  }

  @Test
  public void addUsagesFromNameDeclarationLineKeepsUsagesFromFilter() {
    AssetExtractorTextUnit textUnit = createTextUnit("merge_board", null);
    textUnit.setUsages(Set.of("app/src/main/java/EditBoard.java:214"));

    assetExtractor.addUsagesFromNameDeclarationLine(
        List.of(textUnit), STRINGS_XML, "res/values/strings.xml");

    Assertions.assertThat(textUnit.getUsages())
        .containsExactly("app/src/main/java/EditBoard.java:214");
  }

  @Test
  public void addUsagesFromNameDeclarationLineWhenNameIsSingleQuoted() {
    AssetExtractorTextUnit textUnit = createTextUnit("merge_board", null);

    assetExtractor.addUsagesFromNameDeclarationLine(
        List.of(textUnit),
        """
        <resources>
            <string name='merge_board'>Merge board</string>
        </resources>
        """,
        "res/values/strings.xml");

    Assertions.assertThat(textUnit.getUsages()).containsExactly("res/values/strings.xml:2");
  }

  @Test
  public void addUsagesFromNameDeclarationLineKeepsFirstDeclaration() {
    AssetExtractorTextUnit textUnit = createTextUnit("merge_board", null);

    assetExtractor.addUsagesFromNameDeclarationLine(
        List.of(textUnit),
        """
        <resources>
            <string name="merge_board">Merge board</string>
            <string name="merge_board">Merge board</string>
        </resources>
        """,
        "res/values/strings.xml");

    Assertions.assertThat(textUnit.getUsages()).containsExactly("res/values/strings.xml:2");
  }

  @Test
  public void addUsagesFromNameDeclarationLineWhenNameIsNotDeclared() {
    AssetExtractorTextUnit textUnit = createTextUnit("key1", null);

    assetExtractor.addUsagesFromNameDeclarationLine(
        List.of(textUnit), "key1=value1\n", "test.properties");

    Assertions.assertThat(textUnit.getUsages()).isNull();
  }

  @Test
  public void addUsagesFromNameDeclarationLineWhenNoName() {
    AssetExtractorTextUnit textUnit = createTextUnit(null, null);

    assetExtractor.addUsagesFromNameDeclarationLine(
        List.of(textUnit), STRINGS_XML, "res/values/strings.xml");

    Assertions.assertThat(textUnit.getUsages()).isNull();
  }

  @Test
  public void getNameWithoutPluralForm() {
    Assertions.assertThat(
            assetExtractor.getNameWithoutPluralForm(createTextUnit("people_one", "one")))
        .isEqualTo("people");
    Assertions.assertThat(
            assetExtractor.getNameWithoutPluralForm(createTextUnit("people_one", null)))
        .isEqualTo("people_one");
    Assertions.assertThat(assetExtractor.getNameWithoutPluralForm(createTextUnit("one", "one")))
        .isEqualTo("one");
    Assertions.assertThat(assetExtractor.getNameWithoutPluralForm(createTextUnit(null, "one")))
        .isNull();
  }

  AssetExtractorTextUnit createTextUnit(String name, String pluralForm) {
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setName(name);
    assetExtractorTextUnit.setPluralForm(pluralForm);
    return assetExtractorTextUnit;
  }
}
