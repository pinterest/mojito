package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrintfLikeIntegrityCheckerTest {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(PrintfLikeIntegrityCheckerTest.class);

  @Test
  public void testGetPlaceholder() throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String string = "%1$ld개 파일과 %2$@개 폴더가 있습니다 %d %1$-04d %1$04d %2$.2ld";

    Set<String> placeholders = checker.getPlaceholders(string);

    Set<String> expected = new HashSet<>();
    expected.add("%1$ld");
    expected.add("%2$@");
    expected.add("%d");
    expected.add("%1$-04d");
    expected.add("%1$04d");
    expected.add("%2$.2ld");

    logger.debug("expected: {}", expected);
    logger.debug("actual: {}", placeholders);

    assertEquals(expected, placeholders);
  }

  @Test
  public void testMacSinglePlaceholderCheckWorks() throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "There are %@ files";
    String target = "Il y a %@ fichiers";

    checker.check(source, target);
  }

  @Test
  public void testMacMultiplePlaceholdersCheckWorks() throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "There are %1$ld files and %2$@ folders";
    String target = "Il y a %1$ld fichiers et %2$@ dossiers";

    checker.check(source, target);
  }

  @Test
  public void testMacPlaceholdersCheckWithRemovedSpacesWorks1()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "%1$@ of %2$@ %3$@";
    String target = "%1$@/%2$@%3$@";

    checker.check(source, target);
  }

  @Test
  public void testMacPlaceholdersCheckWithRemovedSpacesWorks2()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "%1$@ %2$@:";
    String target = "%1$@%2$@:";

    checker.check(source, target);
  }

  @Test
  public void testMacMultiplePlaceholdersAndTranslationCheckWorks()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "There are %1$ld files and %2$@ folders";
    String target = "%1$ld개 파일과 %2$@개 폴더가 있습니다";

    checker.check(source, target);
  }

  @Test
  public void testMacTranslationAndMultiplePlaceholdersCheckWorks()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "%1$ld files, %2$@ folders";
    String target = "파일%1$ld, 폴더%2$@";

    checker.check(source, target);
  }

  @Test
  public void testMacPlaceholderCheckWorksWithDifferentOrder()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "There are %1$lld files and %2$.2ld folders";
    String target = "Il y a %2$.2ld dossiers et %1$lld fichiers";

    checker.check(source, target);
  }

  @Test
  public void testMacPlaceholderCheckFailsIfDifferentPlaceholdersCount()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "There are %1$04d files and %2$@ folders";
    String target = "Il y a %1$04d fichiers";

    try {
      checker.check(source, target);
      fail("PrintfLikeIntegrityCheckerException must be thrown");
    } catch (PrintfLikeIntegrityCheckerException e) {
      assertEquals(e.getMessage(), "Placeholders in source and target are different");
    }
  }

  @Test
  public void testMacPlaceholderCheckFailsIfSamePlaceholdersCountButSomeRepeatedOrMissing()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "There are %1$-04d files and %2$@ folders";
    String target = "Il y a %1$-04d fichiers et %1$-04d dossiers";

    try {
      checker.check(source, target);
      fail("PrintfLikeIntegrityCheckerException must be thrown");
    } catch (PrintfLikeIntegrityCheckerException e) {
      assertEquals(e.getMessage(), "Placeholders in source and target are different");
    }
  }

  @Test
  public void testMacPlaceholderCheckFailsIfSamePlaceholdersCountButSpecifierModified()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "There are %1$.1ld files and %2$.2ld folders";
    String target = "Il y a %1$.1ld fichiers et %1$.1ld dossiers";

    try {
      checker.check(source, target);
      fail("PrintfLikeIntegrityCheckerException must be thrown");
    } catch (PrintfLikeIntegrityCheckerException e) {
      assertEquals(e.getMessage(), "Placeholders in source and target are different");
    }
  }

  @Test
  public void testAndroidSinglePlaceholderCheckWorks() throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "There are %d files";
    String target = "Il y a %d fichiers";

    checker.check(source, target);
  }

  @Test
  public void testAndroidMultiplePlaceholdersCheckWorks()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "There are %1$s files and %2$d folders";
    String target = "Il y a %1$s fichiers et %2$d dossiers";

    checker.check(source, target);
  }

  @Test
  public void testAndroidMultiplePlaceholdersAndTranslationCheckWorks()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "There are %1$s files and %2$d folders";
    String target = "%1$s개 파일과 %2$d개 폴더가 있습니다";

    checker.check(source, target);
  }

  @Test
  public void testAndroidTranslationAndMultiplePlaceholdersCheckWorks()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "%1$s files, %2$d folders";
    String target = "파일%1$s, 폴더%2$d";

    checker.check(source, target);
  }

  @Test
  public void testAndroidPlaceholderCheckWorksWithDifferentOrder()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "There are %1$d files and %2$d folders";
    String target = "Il y a %2$d dossiers et %1$d fichiers";

    checker.check(source, target);
  }

  @Test
  public void testAndroidPlaceholderCheckFailsIfDifferentPlaceholdersCount()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "There are %1$d files and %2$d folders";
    String target = "Il y a %1$d fichiers";

    try {
      checker.check(source, target);
      fail("PrintfLikeIntegrityCheckerException must be thrown");
    } catch (PrintfLikeIntegrityCheckerException e) {
      assertEquals(e.getMessage(), "Placeholders in source and target are different");
    }
  }

  @Test
  public void testAndroidPlaceholderCheckFailsIfSamePlaceholdersCountButSomeRepeatedOrMissing()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "There are %1$d files and %2$d folders";
    String target = "Il y a %1$d fichiers et %1$d dossiers";

    try {
      checker.check(source, target);
      fail("PrintfLikeIntegrityCheckerException must be thrown");
    } catch (PrintfLikeIntegrityCheckerException e) {
      assertEquals(e.getMessage(), "Placeholders in source and target are different");
    }
  }

  @Test
  public void testAndroidPlaceholderCheckFailsIfSamePlaceholdersCountButSpecifierModified()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "There are %1$d files and %2$d folders";
    String target = "Il y a %1$d fichiers et %2$s dossiers";

    try {
      checker.check(source, target);
      fail("PrintfLikeIntegrityCheckerException must be thrown");
    } catch (PrintfLikeIntegrityCheckerException e) {
      assertEquals(e.getMessage(), "Placeholders in source and target are different");
    }
  }

  @Test
  public void testSpecifierWithRemovedSpace() throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "%1$s of %2$s GB";
    String target = "%1$s/%2$sGB";

    checker.check(source, target);
  }

  @Test
  public void testIncorrectlyModifiedSpecifier() throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "%1.1f GB";
    String target = "%1,1f Go";

    try {
      checker.check(source, target);
      fail("PrintfLikeIntegrityCheckerException must be thrown");
    } catch (PrintfLikeIntegrityCheckerException e) {
      assertEquals(e.getMessage(), "Placeholders in source and target are different");
    }
  }

  @Test
  public void testSourceNonPositionalMatchesTargetPositional_SingleString()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "Select %s answers to proceed";
    String target = "Kies %1$s antwoorde om voort te gaan";

    checker.check(source, target);
  }

  @Test
  public void testSourceNonPositionalMatchesTargetPositional_SingleInteger()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "You have %d messages";
    String target = "Du har %1$d beskeder";

    checker.check(source, target);
  }

  @Test
  public void testSourceNonPositionalMatchesTargetPositional_WithPrecision()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "Battery: %.1f percent";
    String target = "Batterie: %1$.1f prozent";

    checker.check(source, target);
  }

  @Test
  public void testSourcePositionalMatchesTargetNonPositional()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "Select %1$s answers";
    String target = "Kies %s antwoorde";

    checker.check(source, target);
  }

  @Test
  public void testNonPositionalSourceWithWrongTypeInTarget()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "Select %s answers";
    String target = "Kies %1$d antwoorde";

    try {
      checker.check(source, target);
      fail("PrintfLikeIntegrityCheckerException must be thrown");
    } catch (PrintfLikeIntegrityCheckerException e) {
      assertEquals(e.getMessage(), "Placeholders in source and target are different");
    }
  }

  @Test
  public void testNonPositionalSourceWithMissingPlaceholderInTarget()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "Select %s answers";
    String target = "Kies antwoorde";

    try {
      checker.check(source, target);
      fail("PrintfLikeIntegrityCheckerException must be thrown");
    } catch (PrintfLikeIntegrityCheckerException e) {
      assertEquals(e.getMessage(), "Placeholders in source and target are different");
    }
  }

  @Test
  public void testNonPositionalSourceWithExtraPlaceholderInTarget()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "Select answers";
    String target = "Kies %1$s antwoorde";

    try {
      checker.check(source, target);
      fail("PrintfLikeIntegrityCheckerException must be thrown");
    } catch (PrintfLikeIntegrityCheckerException e) {
      assertEquals(e.getMessage(), "Placeholders in source and target are different");
    }
  }

  @Test
  public void testMultipleNonPositionalPlaceholdersCountMismatch()
      throws PrintfLikeIntegrityCheckerException {

    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "There are %s files and %d folders";
    String target = "Il y a %1$s fichiers";

    try {
      checker.check(source, target);
      fail("PrintfLikeIntegrityCheckerException must be thrown");
    } catch (PrintfLikeIntegrityCheckerException e) {
      assertEquals(e.getMessage(), "Placeholders in source and target are different");
    }
  }

  @Test
  public void testRawPercentageSymbolInText() throws PrintfLikeIntegrityCheckerException {
    // Raw percentage at end of string (no conversion character after %)
    // This should not be treated as a placeholder
    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "Progress: 95%";
    String target = "Progrès: 95%";

    checker.check(source, target);
  }

  @Test
  public void testRawPercentageWithPlaceholder() throws PrintfLikeIntegrityCheckerException {
    // Combination of real placeholder (%s) and raw percentage (95%)
    // The 95% has ')' after it which is not a valid conversion character
    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "Loading %s... (95%)";
    String target = "Chargement %1$s... (95%)";

    checker.check(source, target);
  }

  @Test
  public void testEscapedPercentage() throws PrintfLikeIntegrityCheckerException {
    // Double %% is the escaped form and should be treated as a placeholder
    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "95%% complete";
    String target = "95%% terminé";

    checker.check(source, target);
  }

  @Test
  public void testPercentageFollowedByPunctuation() throws PrintfLikeIntegrityCheckerException {
    // Percentage followed by punctuation (not a conversion character)
    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "Discount: 50%!";
    String target = "Réduction: 50%!";

    checker.check(source, target);
  }

  @Test
  public void testDigitPercentageNoLongerMatchesAsPlaceholder()
      throws PrintfLikeIntegrityCheckerException {
    // Fixed: "95% of" should NOT extract "% o" anymore
    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "95% of users";
    String target = "95% des utilisateurs";

    checker.check(source, target); // NOW PASSES
  }

  @Test
  public void testEuropeanPercentageFormat() throws PrintfLikeIntegrityCheckerException {
    // Fixed: European format with space before % should not match
    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "Progress: 95 % done";
    String target = "Progrès: 95 % fait";

    checker.check(source, target); // NOW PASSES
  }

  @Test
  public void testCommonPercentagePhrases() throws PrintfLikeIntegrityCheckerException {
    // Real-world phrases that were causing false positives
    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String[][] tests = {
      {"95% complete", "95% terminé"},
      {"100% done", "100% fait"},
      {"50% of files", "50% des fichiers"},
      {"Battery: 85% charged", "Batterie: 85% chargée"}
    };

    for (String[] test : tests) {
      checker.check(test[0], test[1]); // All PASS
    }
  }

  @Test
  public void testFloatingPointPercentage() throws PrintfLikeIntegrityCheckerException {
    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "Loading 3.14% of data";
    String target = "Chargement 3.14% des données";

    checker.check(source, target); // PASSES
  }

  @Test
  public void testMultiplePercentagesInText() throws PrintfLikeIntegrityCheckerException {
    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "Between 50% and 75% complete";
    String target = "Entre 50% et 75% complet";

    checker.check(source, target); // PASSES
  }

  @Test
  public void testDigitPercentageMixedWithPlaceholder() throws PrintfLikeIntegrityCheckerException {
    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "Loading %s... 95% complete";
    String target = "Chargement %1$s... 95% complet";

    checker.check(source, target); // PASSES - only %s matched
  }

  @Test
  public void testSpaceFlagStillWorks() throws PrintfLikeIntegrityCheckerException {
    // REGRESSION: Space flag must still work
    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "Value: % d";
    String target = "Valor: % d";

    checker.check(source, target); // MUST PASS
  }

  @Test
  public void testSpaceFlagAtStartStillWorks() throws PrintfLikeIntegrityCheckerException {
    // REGRESSION: Space flag at start must still work
    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "% d items";
    String target = "% d elementos";

    checker.check(source, target); // MUST PASS
  }

  @Test
  public void testSpaceFlagInContextStillWorks() throws PrintfLikeIntegrityCheckerException {
    // REGRESSION: Space flag in context must still work
    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "Test % d value";
    String target = "Prueba % d valor";

    checker.check(source, target); // MUST PASS
  }

  @Test
  public void testNegativePercentage() throws PrintfLikeIntegrityCheckerException {
    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "Change: -5% decrease";
    String target = "Changement: -5% diminution";

    checker.check(source, target); // PASSES
  }

  @Test
  public void testPercentageWithCurrencySymbol() throws PrintfLikeIntegrityCheckerException {
    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "Price: $95% off";
    String target = "Prix: $95% de rabais";

    checker.check(source, target); // PASSES
  }

  @Test
  public void testZeroPercentage() throws PrintfLikeIntegrityCheckerException {
    PrintfLikeIntegrityChecker checker = new PrintfLikeIntegrityChecker();
    String source = "0% complete";
    String target = "0% completo";

    checker.check(source, target); // PASSES
  }
}
