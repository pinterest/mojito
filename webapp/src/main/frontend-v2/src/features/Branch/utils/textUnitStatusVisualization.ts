import type { BranchStatistics, Screenshot } from "@/types/branchStatistics";
import type { TextUnitStatus } from "@/types/textUnitStatus";

interface TextUnitStatusVisualDetail {
    color: string;
    label: string;
    order: number;
}

export const TEXT_UNIT_STATUS_TO_VISUAL_DETAIL: Record<
    TextUnitStatus,
    TextUnitStatusVisualDetail
> = {
    // Good states
    APPROVED: { color: "#76e67aff", label: "Approved", order: 1 },
    OVERRIDDEN: { color: "#4caf50", label: "Overridden", order: 2 },
    // Info states
    MT_REVIEW_NEEDED: {
        color: "#00f7ffff",
        label: "MT Review Needed",
        order: 3,
    },
    MT_TRANSLATED: { color: "#00f7ffff", label: "MT Translated", order: 4 },
    REVIEW_NEEDED: { color: "#00f7ff6b", label: "Review Needed", order: 5 },
    // Failure states
    MANUALLY_REJECTED: {
        color: "#ff9800",
        label: "Manually Rejected",
        order: 6,
    },
    INTEGRITY_FAILURE: {
        color: "#f44336",
        label: "Integrity Failure",
        order: 7,
    },
    // No progress state
    TRANSLATION_NEEDED: {
        color: "#a6a6a6ff",
        label: "Translation Needed",
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

export function getLabelForTextUnitStatus(status: string): string {
    if (!isValidTextUnitStatus(status)) {
        throw new Error(`Unknown text unit status: ${status}`);
    }
    return TEXT_UNIT_STATUS_TO_VISUAL_DETAIL[status].label;
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
                new Date(screenshot.createdDate) >=
                    new Date(existing.createdDate)
            ) {
                screenshotMap.set(textUnitId, screenshot);
            }
        });
    });
    return screenshotMap;
}
