package com.box.l10n.mojito.xliff;

import static org.junit.Assert.*;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * @author jaurambault
 */
public class XliffUtilsTest {

  XliffUtils xliffUtils;

  final String xliff =
      "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
          + "<file original=\"en.properties\" source-language=\"en\" target-language=\"fr-fr\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
          + "<body>\n"
          + "<trans-unit id=\"\" resname=\"fake\">\n"
          + "<source xml:lang=\"en\">fake</source>\n"
          + "<target xml:lang=\"\">fake</target>\n"
          + "<note annotates=\"target\" from=\"automation\">MUST REVIEW\n"
          + "Text unit for id: , Skipping it...</note>\n"
          + "</trans-unit>\n"
          + "</body>\n"
          + "</file>\n"
          + "</xliff>";

  @Before
  public void setUp() {
    this.xliffUtils = new XliffUtils();
  }

  @Test
  public void testGetXliffTargetLanguage() throws XPathExpressionException {
    assertEquals("fr-fr", this.xliffUtils.getTargetLanguage(this.xliff));
  }

  @Test
  public void testGetXliffTargetLanguageMissingAttribute()
      throws XPathExpressionException,
          ParserConfigurationException,
          IOException,
          TransformerException,
          SAXException {
    String xliffWithoutTargetLanguage = this.xliffUtils.removeTargetLanguageAttribute(xliff);

    assertEquals(null, this.xliffUtils.getTargetLanguage(xliffWithoutTargetLanguage));
  }

  @Test
  public void testRemoveTargetLanguageAttribute()
      throws XPathExpressionException,
          ParserConfigurationException,
          IOException,
          TransformerException,
          SAXException {
    String newXliff = this.xliffUtils.removeTargetLanguageAttribute(this.xliff);
    assertFalse(newXliff.contains("target-language=\"fr-fr\""));
  }

  String binUnit(String id, String sourceHref, String targetHref) {
    return "<bin-unit id=\""
        + id
        + "\" mime-type=\"image/png\" restype=\"uri\">\n"
        + "<bin-source>\n"
        + "<external-file href=\""
        + sourceHref
        + "\"/>\n"
        + "</bin-source>\n"
        + "<bin-target>\n"
        + "<external-file href=\""
        + targetHref
        + "\"/>\n"
        + "</bin-target>\n"
        + "<note>The course cover art</note>\n"
        + "</bin-unit>\n";
  }

  String xliffWithBody(String bodyContent) {
    return "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
        + "<file original=\"course[1]\" source-language=\"en\" target-language=\"fr-fr\" datatype=\"plaintext\">\n"
        + "<body>\n"
        + "<trans-unit id=\"course[25].name\">\n"
        + "<source>Media Planner Practice Exam</source>\n"
        + "<target/>\n"
        + "</trans-unit>\n"
        + bodyContent
        + "</body>\n"
        + "</file>\n"
        + "</xliff>";
  }

  @Test
  public void testContainsBinUnitElement()
      throws ParserConfigurationException, IOException, SAXException {
    String xliffWithBinUnit =
        this.xliffWithBody(
            this.binUnit("course[25].picture.target_url", "https://cdn.test.com/source.png", ""));

    assertTrue(this.xliffUtils.containsBinUnitElement(xliffWithBinUnit));
  }

  @Test
  public void testContainsBinUnitElementWithMultipleBinUnits()
      throws ParserConfigurationException, IOException, SAXException {
    String xliffWithBinUnits =
        this.xliffWithBody(
            this.binUnit("course[25].picture.target_url", "https://cdn.test.com/picture.png", "")
                + this.binUnit(
                    "course[25].hero_picture.target_url", "https://cdn.test.com/hero.png", ""));

    assertTrue(this.xliffUtils.containsBinUnitElement(xliffWithBinUnits));
  }

  @Test
  public void testContainsBinUnitElementWhenMissing()
      throws ParserConfigurationException, IOException, SAXException {
    assertFalse(this.xliffUtils.containsBinUnitElement(this.xliff));
  }

  @Test
  public void testContainsBinUnitElementWhenOnlyBinUnitChildElementsArePresent()
      throws ParserConfigurationException, IOException, SAXException {
    String xliffWithoutBinUnit =
        this.xliffWithBody(
            "<bin-source>\n"
                + "<external-file href=\"https://cdn.test.com/source.png\"/>\n"
                + "</bin-source>\n");

    assertFalse(this.xliffUtils.containsBinUnitElement(xliffWithoutBinUnit));
  }

  @Test
  public void testContainsBinUnitElementWhenElementNameOnlyContainsBinUnit()
      throws ParserConfigurationException, IOException, SAXException {
    String xliffWithoutBinUnit = this.xliffWithBody("<bin-unit-group id=\"course[25]\"/>\n");

    assertFalse(this.xliffUtils.containsBinUnitElement(xliffWithoutBinUnit));
  }

  @Test(expected = SAXException.class)
  public void testContainsBinUnitElementWithInvalidXml()
      throws ParserConfigurationException, IOException, SAXException {
    this.xliffUtils.containsBinUnitElement("<xliff><file><bin-unit></file></xliff>");
  }

  @Test(expected = SAXException.class)
  public void testContainsBinUnitElementWithDoctypeIsRejected()
      throws ParserConfigurationException, IOException, SAXException {
    String xliffWithDoctype =
        "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
            + this.xliffWithBody(this.binUnit("course[25].picture.target_url", "&xxe;", ""));

    this.xliffUtils.containsBinUnitElement(xliffWithDoctype);
  }

  @Test(expected = SAXException.class)
  public void testContainsBinUnitElementWithEmptyContent()
      throws ParserConfigurationException, IOException, SAXException {
    this.xliffUtils.containsBinUnitElement("");
  }

  @Test(expected = SAXException.class)
  public void testRemoveTargetLanguageAttributeWithDoctypeIsRejected()
      throws XPathExpressionException,
          ParserConfigurationException,
          IOException,
          TransformerException,
          SAXException {
    String xliffWithDoctype =
        "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>" + this.xliff;

    this.xliffUtils.removeTargetLanguageAttribute(xliffWithDoctype);
  }

  @Test
  public void testRemoveBinUnitElements()
      throws ParserConfigurationException, IOException, SAXException, TransformerException {
    String xliffWithBinUnits =
        this.xliffWithBody(
            this.binUnit("course[25].picture.target_url", "https://cdn.test.com/picture.png", "")
                + this.binUnit(
                    "course[25].hero_picture.target_url", "https://cdn.test.com/hero.png", ""));

    String updatedXliff = this.xliffUtils.removeBinUnitElements(xliffWithBinUnits);

    assertFalse(this.xliffUtils.containsBinUnitElement(updatedXliff));
    assertTrue(updatedXliff.contains("<trans-unit id=\"course[25].name\">"));
  }

  @Test
  public void testRemoveBinUnitElementsWithoutBinUnit()
      throws ParserConfigurationException, IOException, SAXException, TransformerException {
    String updatedXliff = this.xliffUtils.removeBinUnitElements(this.xliff);

    assertFalse(this.xliffUtils.containsBinUnitElement(updatedXliff));
    assertEquals("fr-fr", this.xliffUtils.getTargetLanguage(updatedXliff));
  }

  @Test(expected = SAXException.class)
  public void testRemoveBinUnitElementsWithInvalidXml()
      throws ParserConfigurationException, IOException, SAXException, TransformerException {
    this.xliffUtils.removeBinUnitElements("<xliff><file><bin-unit></file></xliff>");
  }

  @Test(expected = SAXException.class)
  public void testRemoveBinUnitElementsWithDoctypeIsRejected()
      throws ParserConfigurationException, IOException, SAXException, TransformerException {
    String xliffWithDoctype =
        "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
            + this.xliffWithBody(this.binUnit("course[25].picture.target_url", "&xxe;", ""));

    this.xliffUtils.removeBinUnitElements(xliffWithDoctype);
  }

  @Test(expected = SAXException.class)
  public void testRemoveBinUnitElementsWithEmptyContent()
      throws ParserConfigurationException, IOException, SAXException, TransformerException {
    this.xliffUtils.removeBinUnitElements("");
  }

  @Test
  public void testReplaceTargetMediaTargetUrlsUpdatesPictureUrl() throws Exception {
    String content =
        this.xliffWithBody(
            this.binUnit(
                "course[25].picture.target_url",
                "https://src.example.com/pic.png",
                "https://old.example.com/pic.png"));

    String result =
        this.xliffUtils.replaceTargetMediaTargetUrls(
            25, content, "https://new.example.com/pic.png", null, null);

    assertTrue(result.contains("https://new.example.com/pic.png"));
    assertFalse(result.contains("https://old.example.com/pic.png"));
  }

  @Test
  public void testReplaceTargetMediaTargetUrlsRemovesPictureBinUnitWhenUrlIsNull()
      throws Exception {
    String content =
        this.xliffWithBody(
            this.binUnit("course[25].picture.target_url", "https://src.example.com/pic.png", ""));

    String result = this.xliffUtils.replaceTargetMediaTargetUrls(25, content, null, null, null);

    assertFalse(this.xliffUtils.containsBinUnitElement(result));
  }

  @Test
  public void testReplaceTargetMediaTargetUrlsRemovesPictureBinUnitWhenUrlIsEmpty()
      throws Exception {
    String content =
        this.xliffWithBody(
            this.binUnit("course[25].picture.target_url", "https://src.example.com/pic.png", ""));

    String result = this.xliffUtils.replaceTargetMediaTargetUrls(25, content, "", null, null);

    assertFalse(this.xliffUtils.containsBinUnitElement(result));
  }

  @Test
  public void testReplaceTargetMediaTargetUrlsUpdatesHeroPictureUrl() throws Exception {
    String content =
        this.xliffWithBody(
            this.binUnit(
                "course[25].hero_picture.target_url",
                "https://src.example.com/hero.png",
                "https://old.example.com/hero.png"));

    String result =
        this.xliffUtils.replaceTargetMediaTargetUrls(
            25, content, null, "https://new.example.com/hero.png", null);

    assertTrue(result.contains("https://new.example.com/hero.png"));
    assertFalse(result.contains("https://old.example.com/hero.png"));
  }

  @Test
  public void testReplaceTargetMediaTargetUrlsRemovesHeroPictureBinUnitWhenUrlIsNull()
      throws Exception {
    String content =
        this.xliffWithBody(
            this.binUnit(
                "course[25].hero_picture.target_url", "https://src.example.com/hero.png", ""));

    String result = this.xliffUtils.replaceTargetMediaTargetUrls(25, content, null, null, null);

    assertFalse(this.xliffUtils.containsBinUnitElement(result));
  }

  @Test
  public void testReplaceTargetMediaTargetUrlsUpdatesMobileHeroPictureUrlWhenBinUnitExists()
      throws Exception {
    String content =
        this.xliffWithBody(
            this.binUnit(
                    "course[25].hero_picture.target_url", "https://src.example.com/hero.png", "")
                + this.binUnit(
                    "course[25].hero_picture.mobile_target_url",
                    "https://src.example.com/mobile.png",
                    "https://old.example.com/mobile.png"));

    String result =
        this.xliffUtils.replaceTargetMediaTargetUrls(
            25, content, null, null, "https://new.example.com/mobile.png");

    assertTrue(result.contains("https://new.example.com/mobile.png"));
    assertFalse(result.contains("https://old.example.com/mobile.png"));
  }

  @Test
  public void testReplaceTargetMediaTargetUrlsCreatesMobileBinUnitFromHeroWhenMissing()
      throws Exception {
    String content =
        this.xliffWithBody(
            this.binUnit(
                "course[25].hero_picture.target_url",
                "https://src.example.com/hero.png",
                "https://hero.example.com/hero.png"));

    String result =
        this.xliffUtils.replaceTargetMediaTargetUrls(
            25,
            content,
            null,
            "https://hero.example.com/hero.png",
            "https://new.example.com/mobile.png");

    assertTrue(result.contains("course[25].hero_picture.mobile_target_url"));
    assertTrue(result.contains("https://new.example.com/mobile.png"));
  }

  @Test
  public void testReplaceTargetMediaTargetUrlsMobileSkippedWhenNoHeroBinUnit() throws Exception {
    String content =
        this.xliffWithBody(
            this.binUnit(
                "course[25].picture.target_url",
                "https://src.example.com/pic.png",
                "https://pic.example.com/pic.png"));

    String result =
        this.xliffUtils.replaceTargetMediaTargetUrls(
            25,
            content,
            "https://pic.example.com/pic.png",
            null,
            "https://new.example.com/mobile.png");

    assertFalse(result.contains("course[25].hero_picture.mobile_target_url"));
  }

  @Test
  public void testReplaceTargetMediaTargetUrlsRemovesMobileBinUnitWhenUrlIsNull() throws Exception {
    String content =
        this.xliffWithBody(
            this.binUnit(
                    "course[25].hero_picture.target_url", "https://src.example.com/hero.png", "")
                + this.binUnit(
                    "course[25].hero_picture.mobile_target_url",
                    "https://src.example.com/mobile.png",
                    ""));

    String result = this.xliffUtils.replaceTargetMediaTargetUrls(25, content, null, null, null);

    assertFalse(result.contains("course[25].hero_picture.mobile_target_url"));
  }

  @Test
  public void testReplaceTargetMediaTargetUrlsAllThreeUrlsReplaced() throws Exception {
    String content =
        this.xliffWithBody(
            this.binUnit(
                    "course[25].picture.target_url",
                    "https://src.example.com/pic.png",
                    "https://old.example.com/pic.png")
                + this.binUnit(
                    "course[25].hero_picture.target_url",
                    "https://src.example.com/hero.png",
                    "https://old.example.com/hero.png")
                + this.binUnit(
                    "course[25].hero_picture.mobile_target_url",
                    "https://src.example.com/mobile.png",
                    "https://old.example.com/mobile.png"));

    String result =
        this.xliffUtils.replaceTargetMediaTargetUrls(
            25,
            content,
            "https://new.example.com/pic.png",
            "https://new.example.com/hero.png",
            "https://new.example.com/mobile.png");

    assertTrue(result.contains("https://new.example.com/pic.png"));
    assertTrue(result.contains("https://new.example.com/hero.png"));
    assertTrue(result.contains("https://new.example.com/mobile.png"));
    assertFalse(result.contains("https://old.example.com/pic.png"));
    assertFalse(result.contains("https://old.example.com/hero.png"));
    assertFalse(result.contains("https://old.example.com/mobile.png"));
  }

  @Test
  public void testReplaceTargetMediaTargetUrlsReturnsSameContentWhenNothingChanged()
      throws Exception {
    String content = this.xliff;

    String result = this.xliffUtils.replaceTargetMediaTargetUrls(25, content, null, null, null);

    assertEquals(content, result);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testReplaceTargetMediaTargetUrlsThrowsOnNullContent() throws Exception {
    this.xliffUtils.replaceTargetMediaTargetUrls(25, null, null, null, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testReplaceTargetMediaTargetUrlsThrowsOnBlankContent() throws Exception {
    this.xliffUtils.replaceTargetMediaTargetUrls(25, "   ", null, null, null);
  }

  @Test(expected = SAXException.class)
  public void testReplaceTargetMediaTargetUrlsRejectsDoctype() throws Exception {
    String xliffWithDoctype =
        "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
            + this.xliffWithBody(this.binUnit("course[25].picture.target_url", "&xxe;", ""));

    this.xliffUtils.replaceTargetMediaTargetUrls(
        25, xliffWithDoctype, "https://new.example.com/pic.png", null, null);
  }
}
