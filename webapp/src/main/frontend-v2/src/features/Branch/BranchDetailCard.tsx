import { Alert, Button, Card, Descriptions, Flex, Progress } from "antd";
import React, { memo, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";

import ScreenshotUploadModal from "./Screenshots/ScreenshotUploadModal";
import { getTextUnitScreenshotMap } from "./utils/textUnitStatusVisualization";
import type { BranchStatistics } from "@/types/branchStatistics";
import PullRequestLink from "@/components/navigation/PullRequestLink";
import { displayDate } from "@/utils/formatDate";

import "@/i18n";

interface BranchDetailsProps {
  branchStats: BranchStatistics;
  onPreviewImage?: (screenshotUrl: string) => void;
}

const BranchDetailCard: React.FC<BranchDetailsProps> = ({
  branchStats,
  onPreviewImage,
}) => {
  const { t } = useTranslation("branch");
  const [isScreenshotModalOpen, setIsScreenshotModalOpen] = useState(false);

  const progressPercent = useMemo(() => {
    if (!branchStats.branchTextUnitStatistics) return 0;

    const totalCount = branchStats.totalCount;
    const forTranslationCount = branchStats.forTranslationCount;

    if (totalCount === 0) return 100;

    return Number(
      (((totalCount - forTranslationCount) / totalCount) * 100).toFixed(2),
    );
  }, [branchStats]);

  const lastUpdatedDate = useMemo(() => {
    if (!branchStats.branchTextUnitStatistics) return null;

    const lastUpdatedTimestamp = branchStats.branchTextUnitStatistics.reduce(
      (acc, val) => {
        return Math.max(acc, val.tmTextUnit?.createdDate || 0);
      },
      0,
    );

    return lastUpdatedTimestamp ? new Date(lastUpdatedTimestamp) : null;
  }, [branchStats]);

  const hasScreenshots =
    getTextUnitScreenshotMap(branchStats).size ===
    branchStats.branchTextUnitStatistics.length;

  return (
    <Flex orientation='vertical' gap='small'>
      {!hasScreenshots && (
        <div className='m-1'>
          <Alert
            title={t("screenshotsNotUploaded")}
            type='warning'
            showIcon={true}
            action={
              <Button onClick={() => setIsScreenshotModalOpen(true)}>
                {t("uploadScreenshot")}
              </Button>
            }
          />
        </div>
      )}

      {isScreenshotModalOpen && (
        <ScreenshotUploadModal
          isOpen={isScreenshotModalOpen}
          setIsOpen={setIsScreenshotModalOpen}
          branchStats={branchStats}
          onPreviewImage={onPreviewImage}
        />
      )}

      <Card className='m-1'>
        <Descriptions title={t("branchDetails")} bordered>
          <Descriptions.Item label={t("branch")}>
            <PullRequestLink
              repoName={branchStats.branch.repository.name}
              branchName={branchStats.branch.name}
            />
          </Descriptions.Item>
          <Descriptions.Item label={t("createdDate")}>
            {displayDate(new Date(branchStats.branch.createdDate))}
          </Descriptions.Item>
          <Descriptions.Item label={t("updatedDate")}>
            {displayDate(
              lastUpdatedDate || new Date(branchStats.branch.createdDate),
            )}
          </Descriptions.Item>
          <Descriptions.Item label={t("author")}>
            {branchStats.branch?.createdByUser?.username ?? "N/A"}
          </Descriptions.Item>

          <Descriptions.Item label={t("progression")}>
            <Progress percent={progressPercent ?? 0}></Progress>
          </Descriptions.Item>
        </Descriptions>
      </Card>
    </Flex>
  );
};

export default memo(BranchDetailCard);
