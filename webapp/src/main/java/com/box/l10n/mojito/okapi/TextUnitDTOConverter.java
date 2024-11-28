package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.po.PoPluralRule;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.annotation.Note;
import net.sf.okapi.common.annotation.NoteAnnotation;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import org.springframework.stereotype.Component;

@Component
public class TextUnitDTOConverter {

  private final String CONTEXT_DELIMETER = "---";
  private final String poMsgSkeletonStart = "\nmsgid \"%s\"\nmsgstr \"";
  private final String poMsgSkeletonnPlaceholder = "[#$$self$]";
  private final String poMsgSkeletonEnd = "\"\n";

  private final String poMsgPluralSkeleton = "\nmsgid_plural \"%s\"\n";

  public TextUnit toOkapiTU(TextUnitDTO textUnitDTO, LocaleId localeId) {
    TextUnit tu = new TextUnit(textUnitDTO.getTmTextUnitId().toString(), textUnitDTO.getSource());
    tu.setName(textUnitDTO.getName());

    addDefaults(tu);
    addContext(tu);
    addComment(textUnitDTO, tu);
    addSkeleton(tu, localeId, textUnitDTO.getComment());

    return tu;
  }

  // Only handles english source as of now.
  public List<Event> toOkapiPluralEvents(List<TextUnitDTO> pluralTextUnitDTOs, LocaleId locale) {
    List<Event> events = new ArrayList<>();

    PoPluralRule pluralRule = PoPluralRule.fromBcp47Tag(locale.toBCP47());
    List<String> forms = pluralRule.getCldrForms().stream().toList();

    TextUnitDTO singular =
        pluralTextUnitDTOs.stream()
            .filter(tu -> tu.getPluralForm().equals("one"))
            .findFirst()
            .orElse(null);

    TextUnitDTO plural =
        pluralTextUnitDTOs.stream()
            .filter(tu -> tu.getPluralForm().equals("other"))
            .findFirst()
            .orElse(null);

    assert singular != null;
    assert plural != null;

    TextUnit singularTU = toOkapiTU(singular, locale);
    TextUnit pluralTU = toOkapiTU(plural, locale);

    events.add(createPluralStartEvent(singular, plural));

    /**
     * We have _one and _other from the DB. Just need to replace the _other with the CLDRs for the
     * target locale to simulate POFilter.
     */
    for (int i = 0; i < forms.size(); i++) {
      TextUnit textUnit;
      if (forms.get(i).equals("one")) {
        textUnit = singularTU.clone();
      } else {
        textUnit = pluralTU.clone();
        textUnit.setName(textUnit.getName().replace("_other", "_" + forms.get(i)));
      }
      textUnit.setSkeleton(createSkeletonForPluralEntry(Integer.toString(i), textUnit, locale));
      events.add(new Event(EventType.TEXT_UNIT, textUnit));
    }

    events.add(createPluralEndEvent(singular, plural));
    return events;
  }

  private void addContext(TextUnit textUnit) {
    if (!textUnit.getName().contains(CONTEXT_DELIMETER)) return;
    String context = textUnit.getName().split("---")[1];
    textUnit.setProperty(new Property("context", context));
  }

  private void addComment(TextUnitDTO textUnitDTO, TextUnit textUnit) {
    NoteAnnotation noteAnnotation = new NoteAnnotation();
    noteAnnotation.add(new Note(textUnitDTO.getComment()));
    textUnit.setAnnotation(noteAnnotation);
  }

  private void addDefaults(TextUnit textUnit) {
    textUnit.setMimeType("application/x-gettext");
    textUnit.setIsTranslatable(true);
    textUnit.setPreserveWhitespaces(true);
  }

  private void addSkeleton(TextUnit textUnit, LocaleId localeId, String comment) {

    StringBuilder skeletonString = new StringBuilder();
    if (!Strings.isNullOrEmpty(comment)) {
      skeletonString.append("\n#. ").append(comment.replaceAll("\n", "\n#. "));
    }

    if (textUnit.getName().contains(CONTEXT_DELIMETER)) {
      String context = textUnit.getName().split("---")[1].trim();
      skeletonString.append("\nmsgctxt \"").append(context).append("\"");
    }

    skeletonString.append(String.format(poMsgSkeletonStart, textUnit.getSource().toString()));

    GenericSkeletonPart start = new GenericSkeletonPart(skeletonString.toString());

    GenericSkeletonPart translationPlaceholder =
        getTranslationPlaceholderSkeleton(textUnit, localeId);

    GenericSkeletonPart end = new GenericSkeletonPart(poMsgSkeletonEnd);

    GenericSkeleton skeleton = new GenericSkeleton();
    skeleton.add(start);
    skeleton.add(translationPlaceholder);
    skeleton.add(end);

    textUnit.setSkeleton(skeleton);
  }

  private GenericSkeletonPart getTranslationPlaceholderSkeleton(
      TextUnit textUnit, LocaleId localeId) {
    GenericSkeletonPart skeletonPart = new GenericSkeletonPart(poMsgSkeletonnPlaceholder);
    skeletonPart.setParent(textUnit);
    skeletonPart.setLocale(localeId);
    return skeletonPart;
  }

  private Event createPluralStartEvent(TextUnitDTO singular, TextUnitDTO plural) {
    StringBuilder startGroupSkeleton = new StringBuilder();
    StartGroup startGroup = new StartGroup();
    startGroup.setId("o" + singular.getTmTextUnitId().toString());
    startGroup.setType("x-gettext-plurals");
    startGroup.setMimeType("application/x-gettext");
    startGroup.setIsTranslatable(true);
    startGroup.setPreserveWhitespaces(false);

    if (!Strings.isNullOrEmpty(singular.getComment())) {
      startGroupSkeleton.append("\n#. ").append(singular.getComment().replaceAll("\n", "\n#. "));
    }

    if (singular.getName().contains(CONTEXT_DELIMETER)) {
      String context = singular.getName().split("---")[1];
      context = context.split("_" + singular.getPluralForm())[0].trim();
      startGroupSkeleton.append("\nmsgctxt \"").append(context).append("\"");
    }

    startGroupSkeleton.append(String.format("\nmsgid \"%s\"", singular.getSource()));
    startGroupSkeleton.append(String.format(poMsgPluralSkeleton, plural.getSource()));
    startGroup.setSkeleton(new GenericSkeleton(startGroupSkeleton.toString()));
    return new Event(EventType.START_GROUP, startGroup);
  }

  private Event createPluralEndEvent(TextUnitDTO singular, TextUnitDTO plural) {
    Ending ending = new Ending();
    ending.setSkeleton(new GenericSkeleton(""));
    ending.setId("o" + singular.getTmTextUnitId().toString());
    return new Event(EventType.END_GROUP, ending);
  }

  private GenericSkeleton createSkeletonForPluralEntry(
      String index, TextUnit textUnit, LocaleId localeId) {
    GenericSkeleton gs = new GenericSkeleton();

    gs.add(new GenericSkeletonPart("msgstr[" + index + "] \""));
    gs.add(getTranslationPlaceholderSkeleton(textUnit, localeId));
    gs.add(new GenericSkeletonPart(poMsgSkeletonEnd));
    return gs;
  }
}
