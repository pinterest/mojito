import { Flex, Image } from "antd";
import React from "react";

import BranchDetailCard from "./BranchDetailCard";
import LocaleStatusBarChart from "./Charts/LocaleStatusBarChart";
import TextUnitStatusDoughnut from "./Charts/TextUnitStatusDoughnut";
import BranchTextUnitStatusTable from "./BranchTextUnitStatusTable";

import type { BranchTextUnitStatusDto } from "@/api/types/branchTextUnitStatus";
import type { BranchStatistics } from "@/types/branchStatistics";

interface BranchDetailsProps {
  branchStats: BranchStatistics;
  branchTextUnitStatus: BranchTextUnitStatusDto;
}

const BranchDetails: React.FC<BranchDetailsProps> = ({
  branchStats,
  branchTextUnitStatus,
}) => {
  const [imagePreview, setImagePreview] = React.useState<string | undefined>();

  return (
    <>
      <BranchDetailCard
        branchStats={branchStats}
        onPreviewImage={setImagePreview}
      />

      <Flex gap='large' orientation='vertical'>
        <div className='chart-container'>
          <TextUnitStatusDoughnut
            className='doughnut-chart-container'
            branchTextUnitStatus={branchTextUnitStatus}
          />
          <LocaleStatusBarChart
            className='bar-chart-container'
            branchTextUnitStatus={branchTextUnitStatus}
          />
        </div>

        <Image
          className='d-none'
          src={imagePreview}
          preview={{
            open: Boolean(imagePreview),
            onOpenChange: () => setImagePreview(undefined),
          }}
        />

        <div>
          <BranchTextUnitStatusTable
            branchTextUnitStatus={branchTextUnitStatus}
            branchStats={branchStats}
            onPreviewImage={(url) => {
              setImagePreview(url);
            }}
          />
        </div>
      </Flex>
    </>
  );
};

export default BranchDetails;
