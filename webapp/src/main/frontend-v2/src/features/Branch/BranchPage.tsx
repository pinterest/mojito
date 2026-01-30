import { useQuery } from "@tanstack/react-query";
import { Flex, Result, Spin } from "antd";
import React from "react";

import "./index.css";

import { getBranchStatistics } from "@/api/getBranchStatistics";
import { getBranchTextUnitStatus } from "@/api/getBranchTextUnitStatus";
import BranchDetails from "@/features/Branch/BranchDetails";
import { useBranchName } from "@/hooks/useBranchName";
import { useRepoName } from "@/hooks/useRepoName";
import type { BranchStatistics } from "@/types/branchStatistics";
import type { BranchTextUnitStatusDto } from "@/types/branchTextUnitStatus";
import type { Page } from "@/types/page";

const BranchPage: React.FC = () => {
    const branchName = useBranchName();
    const repoName = useRepoName();

    const branchTextUnitStatus = useQuery<BranchTextUnitStatusDto>({
        queryKey: ["branchTextUnitStatus", { branchName, repoName }],
        queryFn: () =>
            getBranchTextUnitStatus({
                branchName,
                repoName,
            }),
    });

    const branchStatsQuery = useQuery<Page<BranchStatistics>>({
        queryKey: ["branchStatistics", { branchName }],
        queryFn: () =>
            getBranchStatistics({
                branchId: branchTextUnitStatus.data!.branchId,
            }),
        enabled: branchTextUnitStatus.isSuccess,
    });

    if (branchStatsQuery.isLoading || branchTextUnitStatus.isLoading) {
        return (
            <Flex justify="center" style={{ margin: "2vh" }}>
                <Spin />
            </Flex>
        );
    }

    const branch = branchStatsQuery.data?.content?.[0];
    if (
        branchStatsQuery.isError ||
        branchTextUnitStatus.isError ||
        !branch ||
        !branchTextUnitStatus.data
    ) {
        return (
            <Result
                status="500"
                subTitle="Branch details could not be loaded. Please try again later."
            />
        );
    }

    return (
        <BranchDetails
            branchStats={branch}
            branchTextUnitStatus={branchTextUnitStatus.data}
        />
    );
};

export default BranchPage;
