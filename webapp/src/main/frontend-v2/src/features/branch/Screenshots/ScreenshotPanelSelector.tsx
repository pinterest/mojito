import React, { memo, useMemo } from "react";
import type { RcFile } from "antd/es/upload";
import { Empty, Table, type TableProps } from "antd";
import { useTranslation } from "react-i18next";

interface ScreenshotPanelSelectorProps {
  textUnits: { id: number; name: string }[];
  file: RcFile;
  textUnitToScreenshotNameMap: Map<number, string>;

  onTextUnitScreenshotChange?: (textUnitIds: number[]) => void;
}

interface TableDataType {
  key: number;
  name: string;
  screenshotName?: string | undefined;
}

const ScreenshotPanelSelector: React.FC<ScreenshotPanelSelectorProps> = ({
  file,
  textUnitToScreenshotNameMap,
  textUnits,
  onTextUnitScreenshotChange,
}) => {
  const { t } = useTranslation("branch");
  const tableData: TableDataType[] = useMemo(
    () =>
      textUnits
        .filter((textUnit) => {
          return (
            !textUnitToScreenshotNameMap.get(textUnit.id) ||
            textUnitToScreenshotNameMap.get(textUnit.id) === file.name
          );
        })
        .map((textUnit) => ({
          key: textUnit.id,
          name: textUnit.name,
          screenshotName: textUnitToScreenshotNameMap.get(textUnit.id),
        })),
    [textUnits, textUnitToScreenshotNameMap, file],
  );

  const [selectedRowKeys, setSelectedRowKeys] = React.useState<number[]>([]);

  const columns: TableProps<TableDataType>["columns"] = [
    {
      title: t("textunit.name"),
      dataIndex: "name",
      key: "name",
    },
    {
      title: t("textunit.screenshot"),
      dataIndex: "screenshot",
      key: "screenshot",
      width: "30%",
      render: (_, record) =>
        record.screenshotName || t("textunit.noScreenshot"),
    },
  ];

  return (
    <div className='p-1'>
      <Table<TableDataType>
        columns={columns}
        dataSource={tableData}
        pagination={{ pageSize: 5 }}
        locale={{
          emptyText: () => {
            return (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={t(
                  "screenshotUploadModal.noTextUnitsWithoutScreenshot",
                )}
              />
            );
          },
        }}
        rowSelection={{
          selectedRowKeys: selectedRowKeys,
          onChange: (newSelectedRowKeys: React.Key[]) => {
            setSelectedRowKeys(newSelectedRowKeys as number[]);
            onTextUnitScreenshotChange?.(newSelectedRowKeys as number[]);
          },
        }}
      />
    </div>
  );
};

export default memo(ScreenshotPanelSelector);
