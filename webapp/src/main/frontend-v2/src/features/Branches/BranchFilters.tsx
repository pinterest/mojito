import React from "react";
import { DatePicker, Flex, Input, Select } from "antd";
import dayjs from "dayjs";
import { useTranslation } from "react-i18next";

export type StatusFilter = "empty" | "deleted";

export interface BranchFiltersProps {
  queryText: string;
  onQueryTextChange: (value: string) => void;
  statusFilter: StatusFilter[];
  onStatusFilterChange: (values: StatusFilter[]) => void;
  createdBefore: Date | null;
  onCreatedBeforeChange: (date: Date | null) => void;
  createdAfter: Date | null;
  onCreatedAfterChange: (date: Date | null) => void;
  isLoading?: boolean;
}

const BranchFilters: React.FC<BranchFiltersProps> = ({
  queryText,
  statusFilter,
  createdBefore,
  createdAfter,
  isLoading = false,
  onQueryTextChange,
  onStatusFilterChange,
  onCreatedBeforeChange,
  onCreatedAfterChange,
}) => {
  const { t } = useTranslation("branch");

  const branchStatusOptions: { label: string; value: StatusFilter }[] = [
    { label: t("branches.status.empty"), value: "empty" },
    { label: t("branches.status.deleted"), value: "deleted" },
  ];

  return (
    <Flex
      gap={16}
      style={{ marginBottom: "1rem" }}
      justify='space-between'
      wrap={true}
    >
      <Input.Search
        placeholder={t("branches.search")}
        value={queryText}
        loading={isLoading}
        allowClear={true}
        style={{ width: "20rem" }}
        onChange={(e) => onQueryTextChange(e.target.value)}
      />

      <Flex gap={8} align='center' wrap={true} style={{ flexGrow: 1 }}>
        <Select
          placeholder={t("branches.status")}
          allowClear={true}
          mode='multiple'
          style={{ minWidth: "10rem" }}
          onChange={(values) => onStatusFilterChange(values)}
          value={statusFilter}
          options={branchStatusOptions}
        />
        <DatePicker
          placeholder={t("branches.createdBefore")}
          style={{ width: "8rem" }}
          allowClear={true}
          value={createdBefore ? dayjs(createdBefore) : null}
          onChange={(e) => {
            if (!e) {
              onCreatedBeforeChange(null);
              return;
            }

            onCreatedBeforeChange(new Date(e.toISOString()));
          }}
        />
        <DatePicker
          placeholder={t("branches.createdAfter")}
          style={{ width: "8rem" }}
          value={createdAfter ? dayjs(createdAfter) : null}
          allowClear={true}
          onChange={(e) => {
            if (!e) {
              onCreatedAfterChange(null);
              return;
            }

            onCreatedAfterChange(new Date(e.toISOString()));
          }}
        />
      </Flex>
    </Flex>
  );
};

export default BranchFilters;
