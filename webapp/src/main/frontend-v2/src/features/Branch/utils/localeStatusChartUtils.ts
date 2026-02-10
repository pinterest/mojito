import type { TFunction } from "i18next";
import type { BranchTextUnitStatusDto } from "@/types/branchTextUnitStatus";
import type { TextUnitStatus } from "@/types/textUnitStatus";
import {
  getColorForTextUnitStatus,
  geti18nKeyForTextUnitStatus,
  orderTextUnitStatuses,
} from "@/features/Branch/utils/textUnitStatusVisualization";

export interface LocaleStatusMap {
  [locale: string]: {
    [status in TextUnitStatus]?: number;
  };
}

export interface ChartDataset {
  label: string;
  data: number[];
  backgroundColor: string;
}

export interface ChartData {
  labels: string[];
  datasets: ChartDataset[];
}

export function extractLocales(
  branchTextUnitStatus: BranchTextUnitStatusDto,
): string[] {
  return Object.keys(branchTextUnitStatus.localeTextUnitStatus);
}

export function extractAvailableStatuses(
  branchTextUnitStatus: BranchTextUnitStatusDto,
): TextUnitStatus[] {
  const allStatuses = Object.values(branchTextUnitStatus.localeTextUnitStatus)
    .flat()
    .map((tu) => tu.status ?? ("TRANSLATION_NEEDED" as TextUnitStatus));

  const uniqueStatuses = Array.from(new Set(allStatuses));
  return orderTextUnitStatuses(uniqueStatuses);
}

export type TextUnitStatusCountDict = {
  [status in TextUnitStatus]: number;
};

export function buildTextUnitCountDictionary(
  branchTextUnitStatus: BranchTextUnitStatusDto,
): TextUnitStatusCountDict {
  return Object.values(branchTextUnitStatus.localeTextUnitStatus).reduce(
    (statusCountDict, textUnits) => {
      textUnits.forEach((textUnit) => {
        const status = textUnit?.status ?? "TRANSLATION_NEEDED";
        statusCountDict[status] = (statusCountDict[status] || 0) + 1;
      });

      return statusCountDict;
    },
    {} as TextUnitStatusCountDict,
  );
}

export function createLocaleToTextUnitStatusMap(
  branchTextUnitStatus: BranchTextUnitStatusDto,
): LocaleStatusMap {
  return Object.entries(branchTextUnitStatus.localeTextUnitStatus).reduce(
    (acc, [locale, textUnitsInLocale]) => {
      if (!acc[locale]) {
        acc[locale] = {};
      }

      textUnitsInLocale.forEach((textUnit) => {
        const status =
          textUnit?.status ?? ("TRANSLATION_NEEDED" as TextUnitStatus);
        acc[locale][status] = (acc[locale][status] || 0) + 1;
      });

      return acc;
    },
    {} as LocaleStatusMap,
  );
}

export function transformToChartData(
  locales: string[],
  statuses: TextUnitStatus[],
  localeStatusMap: LocaleStatusMap,
  t: TFunction<string, string>,
): ChartData {
  return {
    labels: locales,
    datasets: statuses.map((status) => {
      return {
        label: t(geti18nKeyForTextUnitStatus(status)),
        data: locales.map((locale) => localeStatusMap[locale]?.[status] ?? 0),
        backgroundColor: getColorForTextUnitStatus(status),
      };
    }),
  };
}
