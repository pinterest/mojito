package com.box.l10n.mojito.okapi;

import static com.box.l10n.mojito.service.assetExtraction.AssetExtractionService.PRIMARY_BRANCH;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.okapi.steps.AbstractMd5ComputationStep;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticService;
import com.box.l10n.mojito.service.tm.TranslatorWithInheritance;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.google.common.base.Strings;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.annotation.Note;
import net.sf.okapi.common.annotation.NoteAnnotation;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Append text units from external source.
 *
 * @author mattwilshire
 */
@Configurable
public class AppendTextUnitsStep extends AbstractMd5ComputationStep {

  static Logger logger = LoggerFactory.getLogger(AppendTextUnitsStep.class);

  @Autowired BranchRepository branchRepository;
  @Autowired BranchStatisticRepository branchStatisticRepository;
  @Autowired BranchStatisticService branchStatisticService;

  private final TranslatorWithInheritance translatorWithInheritance;
  private final Queue<Event> additionalEvents = new LinkedList<>();
  private boolean endDocumentProcessed = false;
  private Event endDocumentEvent = null;
  private final HashSet<Long> sourceTextUnitIds = new HashSet<>();

  private final LinkedList<TextUnitDTO> textUnitQueue = new LinkedList<>();

  public AppendTextUnitsStep(
      Asset asset, RepositoryLocale repositoryLocale, InheritanceMode inheritanceMode) {
    this.translatorWithInheritance =
        new TranslatorWithInheritance(asset, repositoryLocale, inheritanceMode);
  }

  @Override
  public String getName() {
    return "AppendTextUnitsStep";
  }

  @Override
  public String getDescription() {
    return "Append text units";
  }

  @Override
  public Event handleEvent(Event event) {
    if (endDocumentProcessed) {
      if (!additionalEvents.isEmpty()) {
        return additionalEvents.poll();
      }
      return endDocumentEvent;
    }

    if (event.getEventType() == EventType.END_DOCUMENT && !endDocumentProcessed) {
      endDocumentProcessed = true;

      List<Branch> branches =
          branchRepository.findByRepositoryIdAndDeletedFalseAndNameNotNullAndNameNot(
              13L, PRIMARY_BRANCH);

      branches =
          branches.stream()
              .filter(
                  branch ->
                      branchStatisticRepository.findByBranch(branch) != null
                          && branchStatisticRepository.findByBranch(branch).getForTranslationCount()
                              == 0)
              .toList();

      branches.forEach(
          branch -> {
            branchStatisticService
                .getTextUnitDTOsForBranch(branch)
                .forEach(
                    textUnitDTO -> {
                      if (sourceTextUnitIds.contains(textUnitDTO.getTmTextUnitId())) return;
                      textUnitQueue.add(textUnitDTO);
                    });
          });

      processTextUnitQueue();
      endDocumentEvent = event;
      return new Event(EventType.NO_OP);
    } else if (event.getEventType() == EventType.TEXT_UNIT && !endDocumentProcessed) {
      return this.handleTextUnit(event);
    }
    return event;
  }

  @Override
  public Event handleTextUnit(Event event) {
    super.handleTextUnit(event);
    TextUnitDTO textUnitDTO = translatorWithInheritance.getTextUnitDTO(md5);
    sourceTextUnitIds.add(textUnitDTO.getTmTextUnitId());
    return event;
  }

  public void processTextUnitQueue() {
    while (!textUnitQueue.isEmpty()) {
      TextUnitDTO textUnitDTO = textUnitQueue.poll();
      if (textUnitDTO.getPluralForm() != null) {
        handlePluralTextUnits(textUnitDTO);
      } else {
        additionalEvents.add(textUnitToOkapiEvent(textUnitDTO));
      }
    }
  }

  @Override
  public boolean isDone() {
    return endDocumentProcessed && additionalEvents.isEmpty();
  }

  public void handlePluralTextUnits(TextUnitDTO textUnitDTO) {
    // TODO: Handle many plural forms from pot header not just two..

    StringBuilder startGroupSkeleton = new StringBuilder();

    StartGroup startGroup = new StartGroup();
    startGroup.setId("o" + textUnitDTO.getTmTextUnitId().toString());
    startGroup.setType("x-gettext-plurals");
    startGroup.setMimeType("application/x-gettext");
    startGroup.setIsTranslatable(true);
    startGroup.setPreserveWhitespaces(false);

    if (!Strings.isNullOrEmpty(textUnitDTO.getComment())) {
      startGroupSkeleton.append("\n#. " + textUnitDTO.getComment());
    }

    if (textUnitDTO.getName().contains("---")) {
      String context = textUnitDTO.getName().split("---")[1];
      context = context.split("_" + textUnitDTO.getPluralForm())[0].trim();
      startGroupSkeleton.append("\nmsgctx \"" + context + "\"");
    }

    startGroupSkeleton.append("\nmsgid \"" + textUnitDTO.getSource() + "\"");

    TextUnitDTO other = textUnitQueue.poll();

    startGroupSkeleton.append("\nmsgid_plural \"" + other.getSource() + "\"\n");

    startGroup.setSkeleton(new GenericSkeleton(startGroupSkeleton.toString()));

    TextUnit textUnit = creatTextUnit(textUnitDTO);

    GenericSkeleton gs = new GenericSkeleton();
    gs.add(new GenericSkeletonPart("msgstr[0] \""));
    GenericSkeletonPart part = new GenericSkeletonPart("[#$$self$]");
    part.setParent(textUnit);
    part.setLocale(this.getTargetLocale());
    gs.add(part);
    gs.add(new GenericSkeletonPart("\"\n"));
    textUnit.setSkeleton(gs);

    TextUnit otherUnit = creatTextUnit(other);
    GenericSkeleton gs2 = new GenericSkeleton();
    gs2.add(new GenericSkeletonPart("msgstr[1] \""));
    GenericSkeletonPart part2 = new GenericSkeletonPart("[#$$self$]");
    part2.setParent(otherUnit);
    part2.setLocale(this.getTargetLocale());
    gs2.add(part2);
    gs2.add(new GenericSkeletonPart("\"\n"));
    otherUnit.setSkeleton(gs2);

    Event startEvent = new Event(EventType.START_GROUP, startGroup);
    Event mEvent = new Event(EventType.TEXT_UNIT, textUnit);
    Event nEvent = new Event(EventType.TEXT_UNIT, otherUnit);
    Ending ending = new Ending();
    ending.setSkeleton(new GenericSkeleton(""));
    ending.setId("o" + textUnitDTO.getTmTextUnitId().toString());
    Event endEvent = new Event(EventType.END_GROUP, ending);

    additionalEvents.add(startEvent);
    additionalEvents.add(mEvent);
    additionalEvents.add(nEvent);
    additionalEvents.add(endEvent);
  }

  public Event textUnitToOkapiEvent(TextUnitDTO textUnitDTO) {
    return new Event(EventType.TEXT_UNIT, creatTextUnit(textUnitDTO));
  }

  public TextUnit creatTextUnit(TextUnitDTO textUnitDTO) {
    TextUnit tu = new TextUnit("101", textUnitDTO.getSource());
    tu.setName(textUnitDTO.getName());

    if (textUnitDTO.getName().contains("---")) {
      tu.setProperty(new Property("context", textUnitDTO.getName().split("---")[1]));
    }

    NoteAnnotation noteAnnotation = new NoteAnnotation();
    noteAnnotation.add(new Note(textUnitDTO.getComment()));
    tu.setAnnotation(noteAnnotation);

    /* Skeleton generated by okapi, create one for our TU */
    GenericSkeleton genericSkeleton = new GenericSkeleton();
    genericSkeleton.add(
        new GenericSkeletonPart("\nmsgid \"" + tu.getSource().toString() + "\"\n" + "msgstr \""));

    GenericSkeletonPart part = new GenericSkeletonPart("[#$$self$]");
    part.setParent(tu);
    part.setLocale(this.getTargetLocale());

    genericSkeleton.add(part);
    genericSkeleton.add(new GenericSkeletonPart("\"\n"));
    tu.setSkeleton(genericSkeleton);

    tu.setMimeType("application/x-gettext");
    tu.setIsTranslatable(true);
    tu.setPreserveWhitespaces(true);
    return tu;
  }
}
