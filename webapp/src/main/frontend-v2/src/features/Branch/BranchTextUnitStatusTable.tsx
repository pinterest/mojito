import { Button, Table, type TableProps } from "antd";
import { ZoomInOutlined } from "@ant-design/icons";
import React from "react";
import { useTranslation } from "react-i18next";

import { getTextUnitScreenshotMap } from "./utils/textUnitStatusVisualization";

import type {
  BranchTextUnitStatusDto,
  TextUnitStatusDto,
} from "@/api/types/branchTextUnitStatus";
import type { BranchStatistics } from "@/types/branchStatistics";

interface TableDataType {
  key: React.Key;
  name: string;
  screenshot?: string | undefined;
  translatedCount: number;
  totalCount: number;
}

interface TextUnitTableProps {
  branchTextUnitStatus?: BranchTextUnitStatusDto;
  branchStats?: BranchStatistics;
  onPreviewImage?: (screenshotUrl: string) => void;
}

const TextUnitTable: React.FC<TextUnitTableProps> = ({
  branchTextUnitStatus,
  branchStats,
  onPreviewImage,
}) => {
  const { t } = useTranslation("branch");

  const screenshotMap = getTextUnitScreenshotMap(branchStats!);
  const processedTextUnits = new Map<
    number,
    { name: string; translated: number; total: number }
  >();

  Object.values(branchTextUnitStatus?.localeTextUnitStatus ?? [])
    .flat()
    .forEach((textUnit: TextUnitStatusDto) => {
      const id = textUnit.textUnitId ?? 0;
      const isTranslated =
        textUnit.status === "APPROVED" || textUnit.status === "OVERRIDDEN";

      const existing = processedTextUnits.get(id);
      if (existing) {
        existing.total++;
        if (isTranslated) existing.translated++;
      } else {
        processedTextUnits.set(id, {
          name: textUnit.textUnitName,
          translated: isTranslated ? 1 : 0,
          total: 1,
        });
      }
    });

  const tableData: TableDataType[] = Array.from(processedTextUnits.entries())
    .map(([textUnitId, data]) => {
      return {
        key: textUnitId,
        name: data.name,
        screenshot: screenshotMap.get(textUnitId)?.src,
        translatedCount: data.translated,
        totalCount: data.total,
      };
    })
    .sort((a, b) => a.name.localeCompare(b.name));

  const columns: TableProps<TableDataType>["columns"] = [
    {
      title: t("textunit.name"),
      dataIndex: "name",
      key: "name",
      width: "60%",
    },
    {
      title: t("textunit.screenshot"),
      dataIndex: "screenshot",
      key: "screenshot",
      render: (screenshot: string | undefined) => {
        if (!screenshot) return <span>❌</span>;

        return (
          <Button
            icon={<ZoomInOutlined />}
            onClick={() => screenshot && onPreviewImage?.(screenshot)}
            aria-label='Preview image'
          >
            {t("preview.screenshot")}
          </Button>
        );
      },
      width: "20%",
    },
    {
      title: t("textunit.translatedCount"),
      key: "translatedCount",
      width: "20%",
      render: (_, record) => (
        <span>
          {record.translatedCount} / {record.totalCount}
        </span>
      ),
    },
  ];

  return (
    <div style={{ padding: "2rem" }}>
      <Table<TableDataType> columns={columns} dataSource={tableData} />
    </div>
  );
};

export default React.memo(TextUnitTable);
