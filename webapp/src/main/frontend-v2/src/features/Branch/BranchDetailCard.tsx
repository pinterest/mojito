import { Alert, Button, Card, Descriptions, Flex, Progress } from "antd";
import React, { memo, useMemo } from "react";

import type { BranchStatistics } from "@/types/branchStatistics";
import PullRequestLink from "@/components/PullRequestLink";
import { displayDate } from "@/utils/formatDate";

interface BranchDetailsProps {
    branchStats: BranchStatistics;
}

const BranchDetailCard: React.FC<BranchDetailsProps> = ({ branchStats }) => {
    const progressPercent = useMemo(() => {
        if (!branchStats.branchTextUnitStatistics) return 0;

        const totalCount = branchStats.totalCount;
        const forTranslationCount = branchStats.forTranslationCount;

        if (totalCount === 0) return 100;

        return Number(
            (((totalCount - forTranslationCount) / totalCount) * 100).toFixed(
                2,
            ),
        );
    }, [branchStats]);

    const lastUpdatedDate = useMemo(() => {
        if (!branchStats.branchTextUnitStatistics) return null;

        const lastUpdatedTimestamp =
            branchStats.branchTextUnitStatistics.reduce((acc, val) => {
                return Math.max(acc, val.tmTextUnit?.createdDate || 0);
            }, 0);

        return lastUpdatedTimestamp ? new Date(lastUpdatedTimestamp) : null;
    }, [branchStats]);

    const hasScreenshots = branchStats.branch.screenshots.length > 0;

    return (
        <Flex orientation="vertical" gap="small">
            {!hasScreenshots && (
                <div className="m-1">
                    <Alert
                        title="Screenshots not uploaded"
                        type="warning"
                        showIcon={true}
                        action={
                            <Button onClick={() => {}}>
                                Upload Screenshot
                            </Button>
                        }
                    />
                </div>
            )}

            <Card className="m-1">
                <Descriptions title="Branch Details" bordered>
                    <Descriptions.Item label="Branch">
                        <PullRequestLink
                            repoName={branchStats.branch.repository.name}
                            branchName={branchStats.branch.name}
                        />
                    </Descriptions.Item>
                    <Descriptions.Item label="Created Date">
                        {displayDate(new Date(branchStats.branch.createdDate))}
                    </Descriptions.Item>
                    <Descriptions.Item label="Updated Date">
                        {displayDate(
                            lastUpdatedDate ||
                                new Date(branchStats.branch.createdDate),
                        )}
                    </Descriptions.Item>
                    <Descriptions.Item label="Author">
                        {branchStats.branch.createdByUser.username}
                    </Descriptions.Item>

                    <Descriptions.Item label="Progression">
                        <Progress percent={progressPercent ?? 0}></Progress>
                    </Descriptions.Item>
                </Descriptions>
            </Card>
        </Flex>
    );
};

export default memo(BranchDetailCard);
