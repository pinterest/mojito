import React from "react";

import BranchDetailCard from "./BranchDetailCard";
import LocaleStatusBarChart from "./Charts/LocaleStatusBarChart";
import TextUnitStatusDoughnut from "./Charts/TextUnitStatusDoughnut";

import type { BranchStatistics } from "@/types/branchStatistics";
import type { BranchTextUnitStatusDto } from "@/types/branchTextUnitStatus";

interface BranchDetailsProps {
    branchStats: BranchStatistics;
    branchTextUnitStatus: BranchTextUnitStatusDto;
}

const BranchDetails: React.FC<BranchDetailsProps> = ({
    branchStats,
    branchTextUnitStatus,
}) => {
    return (
        <>
            <BranchDetailCard branchStats={branchStats}></BranchDetailCard>

            <div className="chart-container">
                <TextUnitStatusDoughnut
                    className="doughnut-chart-container"
                    branchTextUnitStatus={branchTextUnitStatus}
                />
                <LocaleStatusBarChart
                    className="bar-chart-container"
                    branchTextUnitStatus={branchTextUnitStatus}
                />
            </div>
        </>
    );
};

export default BranchDetails;
