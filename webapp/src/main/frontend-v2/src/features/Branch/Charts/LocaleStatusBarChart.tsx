import { Typography } from "antd";
import React, { memo } from "react";

import { useLocaleTextUnitStatusChartData } from "../hooks/useLocaleStatusChartData";

import type { BranchTextUnitStatusDto } from "@/types/branchTextUnitStatus";
import BarChartGraph from "@/components/visualization/BarChartGraph";

interface BranchDetailsProps {
    branchTextUnitStatus: BranchTextUnitStatusDto;
    className?: string;
}

const TextUnitStatusChart: React.FC<BranchDetailsProps> = ({
    branchTextUnitStatus,
    className,
}) => {
    const chartData = useLocaleTextUnitStatusChartData(branchTextUnitStatus);

    return (
        <div className={className}>
            <Typography.Title
                level={4}
                rootClassName="w-100"
                style={{ textAlign: "center" }}
                className="text-center"
            >
                Locale Text Unit Breakdown
            </Typography.Title>

            <BarChartGraph
                data={chartData}
                options={{
                    scales: { y: { stacked: true }, x: { stacked: true } },
                }}
            />
        </div>
    );
};

export default memo(TextUnitStatusChart);
