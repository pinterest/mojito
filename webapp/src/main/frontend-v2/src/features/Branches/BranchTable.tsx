import React, { memo } from "react";
import { Link } from "react-router";
import { Table, type TableColumnsType } from "antd";

import type { BranchStatistics } from "@/types/branchStatistics";
import { displayDate } from "@/utils/formatDate";

interface BranchDatum {
  key: string;
  branchName: string;
  createdDate: string;
  repo: string;
  author: string;
  screenshotCount: number;
}

interface BranchTableProps {
  isLoading: boolean;
  stats: BranchStatistics[];
}

const BranchTable: React.FC<BranchTableProps> = ({ isLoading, stats }) => {
  const columns: TableColumnsType<BranchDatum> = [
    {
      title: "Repository",
      dataIndex: "repo",
      key: "repo",
      sorter: (a, b) => a.repo.localeCompare(b.repo),
    },
    {
      title: "Name",
      dataIndex: "branchName",
      key: "branchName",
      render: (_, record) => (
        <Link
          to={`/branch?branchName=${record.branchName}&repoName=${record.repo}`}
        >
          {record.branchName}
        </Link>
      ),
      sorter: (a, b) => a.branchName.localeCompare(b.branchName),
    },
    {
      title: "Created Date",
      dataIndex: "createdDate",
      key: "createdDate",
      sorter: (a, b) => {
        const date1 = new Date(a.createdDate);
        const date2 = new Date(b.createdDate);
        return date1.getTime() - date2.getTime();
      },
    },
    {
      title: "Author",
      dataIndex: "author",
      key: "author",
      sorter: (a, b) => a.author.localeCompare(b.author),
    },
    {
      title: "Screenshot Count",
      dataIndex: "screenshotCount",
      key: "screenshotCount",
      sorter: (a, b) => (a.screenshotCount ?? 0) - (b.screenshotCount ?? 0),
    },
  ];

  const data: BranchDatum[] = stats.map(
    (stat): BranchDatum => ({
      key: String(stat.branch.id),
      branchName: stat.branch.name,
      repo: stat.branch.repository.name,
      createdDate: displayDate(new Date(stat.createdDate)),
      author: stat.branch.createdByUser.username,
      screenshotCount: stat.branch.screenshots.reduce((count, screenshot) => {
        return count + (screenshot.textUnits.length ? 1 : 0);
      }, 0),
    }),
  );

  return (
    <Table
      columns={columns}
      dataSource={data}
      loading={isLoading}
      sortDirections={["descend", "ascend"]}
      pagination={{
        pageSize: 10,
        showSizeChanger: true,
        showQuickJumper: true,
      }}
    />
  );
};

export default memo(BranchTable);
