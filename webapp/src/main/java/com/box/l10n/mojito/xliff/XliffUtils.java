package com.box.l10n.mojito.xliff;

import com.google.common.base.Strings;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Utilities for working with XLIFF.
 *
 * @author jaurambault
 */
@Component
public class XliffUtils {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(XliffUtils.class);

  private static final String BIN_UNIT_ELEMENT_NAME = "bin-unit";

  private static final String BIN_TARGET_EXTERNAL_FILE_PATH = "bin-target/external-file";

  private static final String BIN_UNIT_ID_ATTRIBUTE_NAME = "id";

  private static final String EXTERNAL_FILE_HREF_ATTRIBUTE_NAME = "href";

  private static final String PICTURE_TARGET_ID_FORMAT = "course[%d].picture.target_url";

  private static final String HERO_PICTURE_TARGET_ID_FORMAT = "course[%d].hero_picture.target_url";

  private static final String HERO_PICTURE_MOBILE_TARGET_ID_FORMAT =
      "course[%d].hero_picture.mobile_target_url";

  private static final String TARGET_LANGUAGE_ATTRIBUTE_NAME = "target-language";

  /**
   * Gets the target language of the XLIFF by looking at the first "file" element.
   *
   * @param xliffContent xliff content from which to extract the target language
   * @return the target language or {@code null} if not found
   */
  public String getTargetLanguage(String xliffContent) {

    String targetLanguage = null;

    InputSource inputSource = new InputSource(new StringReader(xliffContent));
    XPath xPath = XPathFactory.newInstance().newXPath();

    SimpleNamespaceContext simpleNamespaceContext = new SimpleNamespaceContext();
    simpleNamespaceContext.bindNamespaceUri("xlf", "urn:oasis:names:tc:xliff:document:1.2");
    xPath.setNamespaceContext(simpleNamespaceContext);

    try {
      Node node =
          (Node)
              xPath.evaluate(
                  "/xlf:xliff/xlf:file[1]/@target-language", inputSource, XPathConstants.NODE);

      if (node != null) {
        targetLanguage = node.getTextContent();
      }

    } catch (XPathExpressionException xpee) {
      logger.debug("Can't extract target language from xliff", xpee);
    }
    return targetLanguage;
  }

  /**
   * Creates a {@link DocumentBuilderFactory} hardened against XXE and DOCTYPE-based attacks.
   *
   * @return a securely configured factory
   * @throws ParserConfigurationException if the parser does not support the required security
   *     features
   */
  private DocumentBuilderFactory newSecureDocumentBuilderFactory()
      throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory;
  }

  /**
   * Removes the {@code target-language} attribute from all elements in the given XML content.
   *
   * @param xmlContent the XML string to process
   * @return the XML string with the {@code target-language} attribute removed
   */
  public String removeTargetLanguageAttribute(String xmlContent)
      throws ParserConfigurationException,
          IOException,
          SAXException,
          XPathExpressionException,
          TransformerException {
    DocumentBuilderFactory dbFactory = newSecureDocumentBuilderFactory();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(new InputSource(new StringReader(xmlContent)));

    XPath xPath = XPathFactory.newInstance().newXPath();
    NodeList nodes =
        (NodeList)
            xPath
                .compile(String.format("//*[@%s]", TARGET_LANGUAGE_ATTRIBUTE_NAME))
                .evaluate(doc, XPathConstants.NODESET);

    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        ((org.w3c.dom.Element) node).removeAttribute(TARGET_LANGUAGE_ATTRIBUTE_NAME);
      }
    }

    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(doc), new StreamResult(writer));
    return writer.toString();
  }

  /**
   * Returns {@code true} if the given XML content contains at least one {@code bin-unit} element.
   *
   * @param xmlContent the XML string to inspect
   * @return {@code true} if a {@code bin-unit} element is present, {@code false} otherwise
   */
  public boolean containsBinUnitElement(String xmlContent)
      throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory factory = newSecureDocumentBuilderFactory();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = builder.parse(new InputSource(new StringReader(xmlContent)));
    return document.getElementsByTagName(BIN_UNIT_ELEMENT_NAME).getLength() > 0;
  }

  /**
   * Removes all {@code bin-unit} elements from the given XML content.
   *
   * @param xmlContent the XML string to process
   * @return the XML string with all {@code bin-unit} elements removed
   */
  public String removeBinUnitElements(String xmlContent)
      throws ParserConfigurationException, IOException, SAXException, TransformerException {
    DocumentBuilderFactory factory = newSecureDocumentBuilderFactory();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = builder.parse(new InputSource(new StringReader(xmlContent)));
    NodeList elements = document.getElementsByTagName(BIN_UNIT_ELEMENT_NAME);

    while (elements.getLength() > 0) {
      Node element = elements.item(0);
      element.getParentNode().removeChild(element);
    }

    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

    StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(document), new StreamResult(writer));
    return writer.toString();
  }

  /**
   * Returns the first {@code bin-unit} element in the document whose {@code id} attribute equals
   * {@code binUnitId}, or {@code null} if none matches.
   *
   * @param document the parsed DOM document to search
   * @param binUnitId the expected value of the {@code id} attribute
   * @return the matching {@code bin-unit} element, or {@code null}
   */
  private Element findBinUnitById(Document document, String binUnitId) {
    NodeList elements = document.getElementsByTagName(BIN_UNIT_ELEMENT_NAME);

    for (int i = 0; i < elements.getLength(); i++) {
      Element element = (Element) elements.item(i);
      if (element.hasAttribute(BIN_UNIT_ID_ATTRIBUTE_NAME)
          && Objects.equals(binUnitId, element.getAttribute(BIN_UNIT_ID_ATTRIBUTE_NAME))) {
        return element;
      }
    }

    return null;
  }

  /**
   * Converts a slash-separated element path into a namespace-agnostic relative XPath expression
   * using {@code local-name()} predicates, so it works regardless of the namespace prefix in use.
   *
   * <p>For example, {@code "body/trans-unit"} becomes {@code
   * "./*[local-name()='body']/*[local-name()='trans-unit']"}.
   *
   * @param elementPath slash-separated path of element names (e.g. {@code
   *     "bin-target/external-file"})
   * @return a relative XPath expression suitable for evaluation against a DOM {@link
   *     org.w3c.dom.Element}
   */
  private String toLocalNamePath(String elementPath) {
    StringBuilder xPathExpression = new StringBuilder(".");

    for (String elementName : elementPath.split("/")) {
      String trimmedElementName = elementName.trim();
      if (!trimmedElementName.isEmpty()) {
        xPathExpression.append("/*[local-name()='").append(trimmedElementName).append("']");
      }
    }

    return xPathExpression.toString();
  }

  private Element getBinTargetExternalFileElement(Element element) throws XPathExpressionException {
    XPath xPath = XPathFactory.newInstance().newXPath();
    return (Element)
        xPath.evaluate(
            toLocalNamePath(BIN_TARGET_EXTERNAL_FILE_PATH), element, XPathConstants.NODE);
  }

  private boolean updateBinTargetHrefByBinUnitId(
      Document document, String attributeValue, String nestedAttributeValue)
      throws XPathExpressionException {
    Element element = findBinUnitById(document, attributeValue);
    if (element == null) {
      logger.debug(
          "No element found for name: {} with attribute: {} having value: {}",
          BIN_UNIT_ELEMENT_NAME,
          BIN_UNIT_ID_ATTRIBUTE_NAME,
          attributeValue);
      return false;
    }

    Element nestedElement = getBinTargetExternalFileElement(element);
    if (nestedElement == null) {
      logger.debug("No element found for path: {}", BIN_TARGET_EXTERNAL_FILE_PATH);
      return false;
    }

    if (!nestedElement.hasAttribute(EXTERNAL_FILE_HREF_ATTRIBUTE_NAME)) {
      logger.debug(
          "No attribute: {} found on element for path: {}",
          EXTERNAL_FILE_HREF_ATTRIBUTE_NAME,
          BIN_TARGET_EXTERNAL_FILE_PATH);
      return false;
    }

    nestedElement.setAttribute(EXTERNAL_FILE_HREF_ATTRIBUTE_NAME, nestedAttributeValue);
    return true;
  }

  private boolean removeBinUnitElementById(Document document, String binUnitId) {
    Element element = findBinUnitById(document, binUnitId);
    if (element == null) {
      return false;
    }

    element.getParentNode().removeChild(element);
    return true;
  }

  private String toXml(Document document) throws TransformerException {
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

    StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(document), new StreamResult(writer));
    return writer.toString();
  }

  private Element ensureHeroPictureMobileBinUnitExists(Document document, int courseId) {
    String heroPictureId = String.format(HERO_PICTURE_TARGET_ID_FORMAT, courseId);
    String heroPictureMobileId = String.format(HERO_PICTURE_MOBILE_TARGET_ID_FORMAT, courseId);

    Element existingMobile = findBinUnitById(document, heroPictureMobileId);
    if (existingMobile != null) {
      return existingMobile;
    }

    Element heroPictureBinUnit = findBinUnitById(document, heroPictureId);
    if (heroPictureBinUnit == null) {
      return null;
    }

    Element heroPictureMobileBinUnit = (Element) heroPictureBinUnit.cloneNode(true);
    heroPictureMobileBinUnit.setAttribute(BIN_UNIT_ID_ATTRIBUTE_NAME, heroPictureMobileId);
    heroPictureBinUnit
        .getParentNode()
        .insertBefore(heroPictureMobileBinUnit, heroPictureBinUnit.getNextSibling());

    return heroPictureMobileBinUnit;
  }

  /**
   * Updates the picture, hero picture, and hero picture mobile target URLs in the {@code bin-unit}
   * elements of the given localized XLIFF content in a single DOM pass.
   *
   * <p>For each URL parameter:
   *
   * <ul>
   *   <li>If non-null and non-empty, the matching {@code bin-unit}'s {@code
   *       bin-target/external-file href} is updated.
   *   <li>If null or empty, the matching {@code bin-unit} element is removed entirely.
   * </ul>
   *
   * <p>For the mobile hero picture specifically, if no mobile {@code bin-unit} exists yet, one is
   * created by cloning the hero picture {@code bin-unit}. If the hero {@code bin-unit} is also
   * absent, the mobile URL is silently skipped.
   *
   * <p>Returns the original {@code localizedContent} string unchanged when no {@code bin-unit}
   * elements were modified or removed.
   *
   * @param courseId the course ID used to locate the {@code bin-unit} elements by their id
   * @param localizedContent the localized XLIFF content to update; must not be null or blank
   * @param pictureTargetUrl the new picture target URL, or {@code null}/empty to remove the element
   * @param heroPictureTargetUrl the new hero picture target URL, or {@code null}/empty to remove
   * @param heroPictureMobileTargetUrl the new mobile hero picture target URL, or {@code null}/empty
   *     to remove
   * @return the updated XLIFF content, or the original string if nothing changed
   * @throws IllegalArgumentException if {@code localizedContent} is null or blank
   */
  public String replaceTargetMediaTargetUrls(
      int courseId,
      String localizedContent,
      String pictureTargetUrl,
      String heroPictureTargetUrl,
      String heroPictureMobileTargetUrl)
      throws ParserConfigurationException,
          IOException,
          SAXException,
          XPathExpressionException,
          TransformerException {
    validateLocalizedContent(localizedContent);

    DocumentBuilderFactory factory = newSecureDocumentBuilderFactory();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = builder.parse(new InputSource(new StringReader(localizedContent)));

    boolean changed = false;

    if (!Strings.isNullOrEmpty(pictureTargetUrl)) {
      changed |=
          updateBinTargetHrefByBinUnitId(
              document, String.format(PICTURE_TARGET_ID_FORMAT, courseId), pictureTargetUrl);
    } else {
      changed |=
          removeBinUnitElementById(document, String.format(PICTURE_TARGET_ID_FORMAT, courseId));
    }

    if (!Strings.isNullOrEmpty(heroPictureTargetUrl)) {
      changed |=
          updateBinTargetHrefByBinUnitId(
              document,
              String.format(HERO_PICTURE_TARGET_ID_FORMAT, courseId),
              heroPictureTargetUrl);
    } else {
      changed |=
          removeBinUnitElementById(
              document, String.format(HERO_PICTURE_TARGET_ID_FORMAT, courseId));
    }

    if (!Strings.isNullOrEmpty(heroPictureMobileTargetUrl)) {
      Element mobileBinUnit = ensureHeroPictureMobileBinUnitExists(document, courseId);
      if (mobileBinUnit != null) {
        changed |=
            updateBinTargetHrefByBinUnitId(
                document,
                String.format(HERO_PICTURE_MOBILE_TARGET_ID_FORMAT, courseId),
                heroPictureMobileTargetUrl);
      } else {
        logger.debug(
            "Hero picture bin-unit not found, cannot create mobile bin-unit for courseId: {}",
            courseId);
      }
    } else {
      changed |=
          removeBinUnitElementById(
              document, String.format(HERO_PICTURE_MOBILE_TARGET_ID_FORMAT, courseId));
    }

    return changed ? toXml(document) : localizedContent;
  }

  private void validateLocalizedContent(String localizedContent) {
    if (localizedContent == null || localizedContent.trim().isEmpty()) {
      throw new IllegalArgumentException("localizedContent must not be null or blank");
    }
  }
}
