import { Typography } from "antd";
import React, { memo } from "react";
import { useTranslation } from "react-i18next";

import {
  getColorForTextUnitStatus,
  geti18nKeyForTextUnitStatus,
} from "../utils/textUnitStatusVisualization";

import { extractAvailableStatuses } from "../utils/localeStatusChartUtils";
import type { BranchTextUnitStatusDto } from "@/types/branchTextUnitStatus";
import type { TextUnitStatus } from "@/types/textUnitStatus";
import DoughnutGraph from "@/components/visualization/DoughnutGraph";

interface BranchDetailsProps {
  branchTextUnitStatus: BranchTextUnitStatusDto;

  className?: string;
}

const TextUnitStatusChart: React.FC<BranchDetailsProps> = ({
  branchTextUnitStatus,
  className,
}) => {
  const { t } = useTranslation("branch");
  const sortedStatuses = extractAvailableStatuses(branchTextUnitStatus);

  const textUnitStatusCountMap = Object.values(
    branchTextUnitStatus.localeTextUnitStatus,
  ).reduce(
    (acc, textUnitsInLocale) => {
      textUnitsInLocale.forEach((textUnit) => {
        const status = textUnit?.status ?? "TRANSLATION_NEEDED";
        acc[status] = (acc[status] || 0) + 1;
      });
      return acc;
    },
    {} as Record<TextUnitStatus, number>,
  );

  return (
    <div className={className}>
      <Typography.Title level={4} className='text-center'>
        {t("textUnitsByStatus")}
      </Typography.Title>

      <DoughnutGraph
        data={{
          labels: sortedStatuses.map((status) =>
            t(geti18nKeyForTextUnitStatus(status)),
          ),
          datasets: [
            {
              label: t("translationStatus"),
              data: sortedStatuses.map(
                (status) => textUnitStatusCountMap[status] || 0,
              ),
              backgroundColor: sortedStatuses.map(getColorForTextUnitStatus),
            },
          ],
        }}
      />
    </div>
  );
};

export default memo(TextUnitStatusChart);
