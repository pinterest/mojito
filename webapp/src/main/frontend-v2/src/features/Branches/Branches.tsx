import React from "react";
import { useTranslation } from "react-i18next";
import { useQuery } from "@tanstack/react-query";
import { Typography } from "antd";

import BranchTable from "./BranchTable";
import BranchFilters from "./BranchFilters";
import { useBranchFiltersWithStorage } from "./hooks/useBranchFiltersWithStorage";

import { getBranchStatistics } from "@/api/getBranchStatistics";
import type { BranchStatistics } from "@/types/branchStatistics";
import type { Page } from "@/types/page";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";

const BranchesPage: React.FC = () => {
  const {
    queryText,
    statusFilter,
    createdBefore,
    createdAfter,
    setQueryText,
    setStatusFilter,
    setCreatedBefore,
    setCreatedAfter,
  } = useBranchFiltersWithStorage(APP_CONFIG.user.username);

  const queryTextDebounced = useDebouncedValue(queryText, 300);
  const { t } = useTranslation("branch");

  const branchStatsQuery = useQuery<Page<BranchStatistics>>({
    queryKey: [
      "branchStatistics",
      {
        search: queryTextDebounced,
        statusFilter,
        createdBefore: createdBefore?.getTime(),
        createdAfter: createdAfter?.getTime(),
      },
    ],
    queryFn: () => {
      const isEmptyIncluded = statusFilter.includes("empty");
      const isDeletedIncluded = statusFilter.includes("deleted");
      return getBranchStatistics({
        search: queryTextDebounced,
        deleted: isDeletedIncluded,
        empty: isEmptyIncluded,
        createdBefore: createdBefore || undefined,
        createdAfter: createdAfter || undefined,
      });
    },
  });

  const isLoading = branchStatsQuery.isLoading || branchStatsQuery.isFetching;

  return (
    <div className='m-1'>
      <Typography.Title level={3}>{t("branches.title")}</Typography.Title>

      <BranchFilters
        queryText={queryText}
        isLoading={isLoading}
        statusFilter={statusFilter}
        createdBefore={createdBefore}
        createdAfter={createdAfter}
        onQueryTextChange={setQueryText}
        onStatusFilterChange={setStatusFilter}
        onCreatedBeforeChange={setCreatedBefore}
        onCreatedAfterChange={setCreatedAfter}
      />

      <BranchTable
        isLoading={isLoading}
        stats={branchStatsQuery.data?.content || []}
      />
    </div>
  );
};

export default BranchesPage;
