package com.box.l10n.mojito.okapi.filters;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.filters.xliff.XLIFFFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

public class XliffFilter extends XLIFFFilter {
  public static final String FILTER_CONFIG_ID = "okf_xliff@mojito";

  private String usagesKeyRegex;

  boolean markBlankOrNoTextAsNotTranslatable = true;

  @Override
  public String getName() {
    return FILTER_CONFIG_ID;
  }

  @Override
  public void open(RawDocument input) {
    super.open(input);
    FilterOptions filterOptions = input.getAnnotation(FilterOptions.class);
    if (filterOptions != null) {
      filterOptions.getString("usagesKeyRegexp", s -> usagesKeyRegex = s);
      filterOptions.getBoolean(
          "blankOrNoTextNotTranslatable", b -> this.markBlankOrNoTextAsNotTranslatable = b);
    }
  }

  public String getAttributeValue(String xmlContent) {
    String regex = String.format("%s=\\\"([^\\\"]+)\\\"", this.usagesKeyRegex);
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(xmlContent);
    if (matcher.find()) {
      return matcher.group(matcher.groupCount());
    }
    return null;
  }

  void markNotTranslatableIfBlankOrNoText(ITextUnit textUnit) {
    if (this.markBlankOrNoTextAsNotTranslatable) {
      TextContainer sourceTextContainer = textUnit.getSource();
      TextContainer textContainerClone = sourceTextContainer.clone();
      String source = sourceTextContainer.toString();
      if (source.indexOf('&') >= 0) {
        source = StringEscapeUtils.unescapeHtml4(source);
      }
      if (StringUtils.isBlank(source.replace('\u00A0', ' ')) || !textContainerClone.hasText()) {
        textUnit.setIsTranslatable(false);
      }
    }
  }

  @Override
  public Event next() {
    Event event = super.next();
    if (!event.isTextUnit()) {
      return event;
    }
    if (this.usagesKeyRegex != null) {
      ITextUnit textUnit = event.getTextUnit();
      String usage = this.getAttributeValue(textUnit.getSkeleton().toString());
      if (usage != null) {
        UsagesAnnotation usagesAnnotation = new UsagesAnnotation(Set.of(usage));
        textUnit.setAnnotation(usagesAnnotation);
      }
    }
    this.markNotTranslatableIfBlankOrNoText(event.getTextUnit());
    return event;
  }
}
