import { useMemo } from "react";
import { useTranslation } from "react-i18next";

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
  const { t } = useTranslation("branch");
  return useMemo(() => {
    const locales = extractLocales(branchTextUnitStatus);
    const statuses = extractAvailableStatuses(branchTextUnitStatus);
    const localeStatusMap =
      createLocaleToTextUnitStatusMap(branchTextUnitStatus);

    return transformToChartData(locales, statuses, localeStatusMap, t);
  }, [branchTextUnitStatus, t]);
}
