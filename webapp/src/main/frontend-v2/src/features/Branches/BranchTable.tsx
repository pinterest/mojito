import React, { memo } from "react";
import { Link } from "react-router";
import { useTranslation } from "react-i18next";
import { ConfigProvider, Empty, Table, type TableColumnsType } from "antd";

import type { BranchStatistics } from "@/types/branchStatistics";
import { displayDate } from "@/utils/formatDate";
import type { Page } from "@/types/page";

interface BranchDatum {
  key: string;
  branchName: string;
  createdDate: string;
  repo: string;
  author?: string;
  screenshotCount: number;
}

interface BranchTableProps {
  isLoading: boolean;
  statsPage?: Page<BranchStatistics> | undefined;

  onPageChange?: (page: number, pageSize: number) => void;
}

const PAGE_SIZE_OPTIONS = ["10", "20", "50", "100"];

const BranchTable: React.FC<BranchTableProps> = ({
  isLoading,
  statsPage,
  onPageChange,
}) => {
  const { t } = useTranslation("branch");

  const columns: TableColumnsType<BranchDatum> = [
    {
      title: t("branches.repository"),
      dataIndex: "repo",
      key: "repo",
      sorter: (a, b) => a.repo.localeCompare(b.repo),
    },
    {
      title: t("branches.name"),
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
      title: t("branches.createdDate"),
      dataIndex: "createdDate",
      key: "createdDate",
      sorter: (a, b) => {
        const date1 = new Date(a.createdDate);
        const date2 = new Date(b.createdDate);
        return date1.getTime() - date2.getTime();
      },
    },
    {
      title: t("branches.author"),
      dataIndex: "author",
      key: "author",
      sorter: (a, b) => (a?.author ?? "").localeCompare(b?.author ?? ""),
    },
    {
      title: t("branches.screenshotCount"),
      dataIndex: "screenshotCount",
      key: "screenshotCount",
      sorter: (a, b) => (a.screenshotCount ?? 0) - (b.screenshotCount ?? 0),
    },
  ];

  const data: BranchDatum[] =
    statsPage?.content.map(
      (stat): BranchDatum => ({
        key: String(stat.branch.id),
        branchName: stat.branch.name,
        repo: stat.branch.repository.name,
        createdDate: displayDate(new Date(stat.createdDate)),
        author: stat.branch?.createdByUser?.username,
        screenshotCount: stat.branch.screenshots.reduce((count, screenshot) => {
          return count + (screenshot.textUnits.length ? 1 : 0);
        }, 0),
      }),
    ) ?? [];

  return (
    <ConfigProvider
      renderEmpty={() => {
        return (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={t("branches.noFilterResults")}
          />
        );
      }}
    >
      <Table
        columns={columns}
        dataSource={data}
        loading={isLoading}
        sortDirections={["descend", "ascend"]}
        pagination={{
          pageSizeOptions: PAGE_SIZE_OPTIONS,
          pageSize: statsPage?.size,
          total: statsPage?.totalElements ?? 0,
          showSizeChanger: true,
          onChange: (page, pageSize) => {
            onPageChange?.(page, pageSize);
          },
        }}
      />
    </ConfigProvider>
  );
};

export default memo(BranchTable);
