import React, { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Flex, Input, Typography } from "antd";

import BranchTable from "./BranchTable";

import { getBranchStatistics } from "@/api/getBranchStatistics";
import type { BranchStatistics } from "@/types/branchStatistics";
import type { Page } from "@/types/page";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";

const BranchesPage: React.FC = () => {
  const [queryText, setQueryText] = useState(() => APP_CONFIG.user.username);

  const queryTextDebounced = useDebouncedValue(queryText, 300);

  const branchStatsQuery = useQuery<Page<BranchStatistics>>({
    queryKey: ["branchStatistics", { search: queryTextDebounced }],
    queryFn: () =>
      getBranchStatistics({
        search: queryTextDebounced,
      }),
  });

  const isLoading = branchStatsQuery.isLoading || branchStatsQuery.isFetching;

  return (
    <div className='m-1'>
      <Typography.Title level={3}>Branches</Typography.Title>

      <Flex gap={16} className='m-1' justify='space-between'>
        <Input.Search
          placeholder='Search by user name or branch name'
          value={queryText}
          onChange={(e) => setQueryText(e.target.value)}
          loading={isLoading}
        />
      </Flex>

      <BranchTable
        isLoading={isLoading}
        stats={branchStatsQuery.data?.content || []}
      />
    </div>
  );
};

export default BranchesPage;
