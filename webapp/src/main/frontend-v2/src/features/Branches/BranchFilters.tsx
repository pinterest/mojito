import React from "react";
import { DatePicker, Flex, Input, Select } from "antd";
import dayjs from "dayjs";

export type StatusFilter = "empty" | "deleted";

const branchStatusOptions: { label: string; value: StatusFilter }[] = [
  { label: "Include empty", value: "empty" },
  { label: "Include deleted", value: "deleted" },
];

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
  return (
    <Flex gap={16} className='m-1' justify='space-between'>
      <Input.Search
        placeholder='Search by user name or branch name'
        value={queryText}
        loading={isLoading}
        allowClear={true}
        style={{ width: "20rem" }}
        onChange={(e) => onQueryTextChange(e.target.value)}
      />

      <Flex
        gap={8}
        align='center'
        justify='flex-end'
        wrap={true}
        style={{ flexGrow: 1 }}
      >
        <Select
          placeholder='Branch status'
          allowClear={true}
          mode='multiple'
          style={{ minWidth: "10rem" }}
          onChange={(values) => onStatusFilterChange(values)}
          value={statusFilter}
          options={branchStatusOptions}
        />
        <DatePicker
          placeholder='Created before'
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
          placeholder='Created after'
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
