import { useMemo } from "react";

import {
    type ChartData,
    createLocaleToTextUnitStatusMap,
    extractAvailableStatuses,
    extractLocales,
    transformToChartData,
} from "../utils/localeStatusChartUtils";

import type { BranchTextUnitStatusDto } from "@/types/branchTextUnitStatus";

export function useLocaleTextUnitStatusChartData(
    branchTextUnitStatus: BranchTextUnitStatusDto,
): ChartData {
    return useMemo(() => {
        const locales = extractLocales(branchTextUnitStatus);
        const statuses = extractAvailableStatuses(branchTextUnitStatus);
        const localeStatusMap =
            createLocaleToTextUnitStatusMap(branchTextUnitStatus);

        return transformToChartData(locales, statuses, localeStatusMap);
    }, [branchTextUnitStatus]);
}
