package com.box.l10n.mojito.okapi;

import static com.box.l10n.mojito.service.assetExtraction.AssetExtractionService.PRIMARY_BRANCH;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchStatistic;
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
 * <p>When the end document is reached, this step will artificially return a NO_OP event. This
 * results in the pipeline going into a NO_OP slide (continuously sending NO_OP events), we will
 * read from our virtual event queue (VEQ) during this no op phase and once the VEQ is empty we can
 * return the end document event.
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

  private final Queue<Event> virtualEvents = new LinkedList<>();
  private boolean endDocumentReached = false;
  private Event endDocumentEvent = null;
  private final HashSet<String> sourceTextUnitMD5s = new HashSet<>();
  private final List<Long> branchIdsWithTextUnitsAdded = new ArrayList<>();

  private final LinkedList<TextUnitDTO> textUnitQueue = new LinkedList<>();

  private final Asset asset;

  public AppendTextUnitsStep(
      Asset asset, RepositoryLocale repositoryLocale, InheritanceMode inheritanceMode) {
    this.asset = asset;
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
    if (!endDocumentReached) {
      switch (event.getEventType()) {
        case EventType.TEXT_UNIT:
          return this.handleSourceTextUnit(event);
        case EventType.END_DOCUMENT:
          // Reached the end of the document, process text units from branches into the virtual
          // event queue.
          endDocumentReached = true;
          this.handleEndDocument();
          endDocumentEvent = event;
          return new Event(EventType.NO_OP);
      }

    } else {
      if (!virtualEvents.isEmpty()) {
        return virtualEvents.poll();
      }
      return endDocumentEvent;
    }

    return event;
  }

  /**
   * Reached the end of the document. Fetch all text units from branches that are fully translated
   * and append them into the textUnitQueue. These text units will be processed into Okapi events in
   * the virtual queue. When the NO_OP slide occurs (end document, no_op returned) this step will
   * read from the virtualEventQueue.
   */
  public void handleEndDocument() {
    List<Branch> branches =
        branchRepository
            .findByRepositoryIdAndDeletedFalseAndNameNotNullAndNameNot(
                this.asset.getRepository().getId(), PRIMARY_BRANCH)
            .stream()
            .filter(
                branch ->
                    branchStatisticRepository.findByBranch(branch) != null
                        && branchStatisticRepository.findByBranch(branch).getForTranslationCount()
                            == 0)
            .toList();

    for (Branch branch : branches) {
      /**
       * NOTE: Plurals will be returned by the root locale, e.g English -> 'one' and 'other' This is
       * fine for this use case, when we reach a plural pop them both off, we will use the target
       * locale to get the CLDRs and inject events for them to the pipeline. see {@link
       * BranchStatisticService#getTextUnitDTOSForTmTextUnitIds}
       */
      boolean addedTU = false;
      for (TextUnitDTO textUnit : branchStatisticService.getTextUnitDTOsForBranch(branch)) {
        String md5 =
            textUnitUtils.computeTextUnitMD5(
                textUnit.getName(), textUnit.getSource(), textUnit.getComment());
        if (sourceTextUnitMD5s.contains(md5)) continue;
        textUnitQueue.add(textUnit);
        addedTU = true;
      }

      if (addedTU) {
        BranchStatistic branchStatistic = branch.getBranchStatistic();
        branchStatistic.setInLocalizedAsset(true);
        branchStatisticRepository.save(branchStatistic);
        logger.info("Updated 'inLocalizedAsset' field for branch name '{}'", branch.getName());
      }
    }

    processTextUnitQueue();
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
        // Should not have reached a plural form that is other, other will be packaged with plural
        // form 'one'.
        // If we find this it means the POFilter extracted a plural to 'one', 'few', 'many' but not
        // other (expected), we don't have a match for this md5 but can confirm it would have
        // matched if its 'one' form is missing.
        if (textUnitDTO.getPluralForm().equals("other")) continue;
        virtualEvents.addAll(
            textUnitDTOConverter.toOkapiPluralEvents(
                List.of(textUnitDTO, textUnitQueue.poll()), this.getTargetLocale()));
      } else {
        virtualEvents.add(textUnitToOkapiEvent(textUnitDTO));
      }
    }
  }

  @Override
  public boolean isDone() {
    return endDocumentReached && virtualEvents.isEmpty();
  }

  public Event textUnitToOkapiEvent(TextUnitDTO textUnitDTO) {
    return new Event(
        EventType.TEXT_UNIT, textUnitDTOConverter.toOkapiTU(textUnitDTO, this.getTargetLocale()));
  }
}
