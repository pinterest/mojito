package com.box.l10n.mojito.okapi;

import static com.box.l10n.mojito.service.assetExtraction.AssetExtractionService.PRIMARY_BRANCH;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.okapi.steps.AbstractMd5ComputationStep;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticService;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
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
  @Autowired TextUnitDTOConverter textUnitDTOConverter;

  private final Queue<Event> additionalEvents = new LinkedList<>();
  private boolean endDocumentProcessed = false;
  private Event endDocumentEvent = null;
  private final HashSet<String> sourceTextUnitMD5s = new HashSet<>();
  private final List<Long> branchIdsWithTextUnitsAdded = new ArrayList<>();

  private final LinkedList<TextUnitDTO> textUnitQueue = new LinkedList<>();

  public AppendTextUnitsStep(
      Asset asset, RepositoryLocale repositoryLocale, InheritanceMode inheritanceMode) {}

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
          branchRepository
              .findByRepositoryIdAndDeletedFalseAndNameNotNullAndNameNot(14L, PRIMARY_BRANCH)
              .stream()
              .filter(
                  branch ->
                      branchStatisticRepository.findByBranch(branch) != null
                          && branchStatisticRepository.findByBranch(branch).getForTranslationCount()
                              == 0)
              .toList();

      for (Branch branch : branches) {
        int textUnitsAdded = 0;
        for (TextUnitDTO textUnit : branchStatisticService.getTextUnitDTOsForBranch(branch)) {
          String md5 =
              textUnitUtils.computeTextUnitMD5(
                  textUnit.getName(), textUnit.getSource(), textUnit.getComment());
          if (sourceTextUnitMD5s.contains(md5)) continue;
          textUnitQueue.add(textUnit);
          textUnitsAdded++;
        }

        if (textUnitsAdded > 0) {
          branchIdsWithTextUnitsAdded.add(branch.getId());
        }
      }

      processTextUnitQueue();
      endDocumentEvent = event;
      return new Event(EventType.NO_OP);
    } else if (event.getEventType() == EventType.TEXT_UNIT && !endDocumentProcessed) {
      return this.handleSourceTextUnit(event);
    }
    return event;
  }

  /**
   * Text units that are originally in the source asset get passed here and their md5 is added to
   * the source text unit md5 list.
   *
   * @param event Okapi text unit event.
   * @return Okapi text unit event.
   */
  public Event handleSourceTextUnit(Event event) {
    super.handleTextUnit(event);
    sourceTextUnitMD5s.add(this.md5);
    return event;
  }

  public void processTextUnitQueue() {
    while (!textUnitQueue.isEmpty()) {
      TextUnitDTO textUnitDTO = textUnitQueue.poll();
      if (textUnitDTO.getPluralForm() != null) {
        // Should not have reached a plural form that is other, other will be packages with plural
        // form 'one'.
        // If we find this it means the POFilter extracted a plural to 'one', 'few', 'many' but not
        // other (expected)
        if (textUnitDTO.getPluralForm().equals("other")) continue;
        additionalEvents.addAll(
            textUnitDTOConverter.toOkapiPluralEvents(
                List.of(textUnitDTO, textUnitQueue.poll()), this.getTargetLocale()));

      } else {
        additionalEvents.add(textUnitToOkapiEvent(textUnitDTO));
      }
    }
  }

  @Override
  public boolean isDone() {
    return endDocumentProcessed && additionalEvents.isEmpty();
  }

  public Event textUnitToOkapiEvent(TextUnitDTO textUnitDTO) {
    return new Event(
        EventType.TEXT_UNIT, textUnitDTOConverter.toOkapiTU(textUnitDTO, this.getTargetLocale()));
  }
}
