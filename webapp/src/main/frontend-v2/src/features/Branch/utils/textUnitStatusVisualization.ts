import type { BranchStatistics, Screenshot } from "@/types/branchStatistics";
import type { TextUnitStatus } from "@/types/textUnitStatus";

interface TextUnitStatusVisualDetail {
  color: string;
  i18nKey: string;
  order: number;
}

export const TEXT_UNIT_STATUS_TO_VISUAL_DETAIL: Record<
  TextUnitStatus,
  TextUnitStatusVisualDetail
> = {
  // Good states
  APPROVED: { color: "#76e67aff", i18nKey: "approved", order: 1 },
  OVERRIDDEN: { color: "#4caf50", i18nKey: "overridden", order: 2 },
  // Info states
  MT_REVIEW_NEEDED: {
    color: "#1b4bcf",
    i18nKey: "mtReviewNeeded",
    order: 3,
  },
  MT_TRANSLATED: {
    color: "#2ebec3",
    i18nKey: "mtTranslated",
    order: 4,
  },
  REVIEW_NEEDED: { color: "#3155ba", i18nKey: "reviewNeeded", order: 5 },
  // Failure states
  MANUALLY_REJECTED: {
    color: "#ff9800",
    i18nKey: "manuallyRejected",
    order: 6,
  },
  INTEGRITY_FAILURE: {
    color: "#f44336",
    i18nKey: "integrityFailure",
    order: 7,
  },
  // No progress state
  TRANSLATION_NEEDED: {
    color: "#a6a6a6ff",
    i18nKey: "translationNeeded",
    order: 8,
  },
};

function isValidTextUnitStatus(status: string): status is TextUnitStatus {
  return status in TEXT_UNIT_STATUS_TO_VISUAL_DETAIL;
}

export function getColorForTextUnitStatus(status: string): string {
  if (!isValidTextUnitStatus(status)) {
    throw new Error(`Unknown text unit status: ${status}`);
  }

  return TEXT_UNIT_STATUS_TO_VISUAL_DETAIL[status].color;
}

export function geti18nKeyForTextUnitStatus(status: string): string {
  if (!isValidTextUnitStatus(status)) {
    throw new Error(`Unknown text unit status: ${status}`);
  }
  return TEXT_UNIT_STATUS_TO_VISUAL_DETAIL[status].i18nKey;
}

export function orderTextUnitStatuses(
  statuses: TextUnitStatus[],
): TextUnitStatus[] {
  return statuses.sort((a, b) => {
    return (
      TEXT_UNIT_STATUS_TO_VISUAL_DETAIL[a].order -
      TEXT_UNIT_STATUS_TO_VISUAL_DETAIL[b].order
    );
  });
}

export function getTextUnitScreenshotMap(
  branchStats: BranchStatistics,
): Map<number, Screenshot> {
  const screenshotMap = new Map<number, Screenshot>();
  branchStats?.branch?.screenshots?.forEach((screenshot) => {
    screenshot.textUnits.forEach((textUnit) => {
      const textUnitId = textUnit.tmTextUnit.id;
      const existing = screenshotMap.get(textUnitId);

      // Only most recent screenshot is kept
      if (
        !existing ||
        new Date(screenshot.createdDate) >= new Date(existing.createdDate)
      ) {
        screenshotMap.set(textUnitId, screenshot);
      }
    });
  });
  return screenshotMap;
}
