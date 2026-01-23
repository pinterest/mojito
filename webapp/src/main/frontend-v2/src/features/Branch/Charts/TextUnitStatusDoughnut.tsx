import { Typography } from "antd";
import React, { memo } from "react";

import { getColorForTextUnitStatus } from "../utils/textUnitStatusVisualization";

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
            <Typography.Title level={4} className="text-center">
                Text Units by Status
            </Typography.Title>

            <DoughnutGraph
                data={{
                    labels: extractAvailableStatuses(branchTextUnitStatus),
                    datasets: [
                        {
                            label: "Translation Status",
                            data: Object.values(textUnitStatusCountMap),
                            backgroundColor: Object.keys(
                                textUnitStatusCountMap,
                            ).map(getColorForTextUnitStatus),
                        },
                    ],
                }}
            />
        </div>
    );
};

export default memo(TextUnitStatusChart);
