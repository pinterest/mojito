import { Form, Tabs, type TabsProps, Upload, type UploadProps } from "antd";
import { useTranslation } from "react-i18next";
import React, { useState } from "react";
import { InboxOutlined } from "@ant-design/icons";
import type { RcFile } from "antd/es/upload";

import ScreenshotSelector from "./ScreenshotSelector";

import type { BranchStatistics } from "@/types/branchStatistics";

const { Dragger } = Upload;

interface ScreenshotWizardProps {
  branchStats: BranchStatistics;

  onDataChange?: (
    files: RcFile[],
    screenshotToTextUnitMap: Map<string, number[]>,
  ) => void;
}

const ScreenshotWizard: React.FC<ScreenshotWizardProps> = ({
  branchStats,
  onDataChange,
}) => {
  const [activeTab, setActiveTab] = useState<string>("ScreenshotUpload");
  const [files, setFiles] = useState<RcFile[]>([]);

  const { t } = useTranslation("branch");

  const uploadProps: UploadProps = {
    name: "file",
    multiple: true,
    onChange(info) {
      setFiles(info.fileList.map((file) => file.originFileObj as RcFile));
    },
    beforeUpload: () => {
      return false;
    },
  };

  const items: TabsProps["items"] = [
    {
      key: "ScreenshotUpload",
      label: t("screenshotUploadTabTitle"),
      children: (
        <>
          <Dragger {...uploadProps}>
            <p className='ant-upload-drag-icon'>
              <InboxOutlined />
            </p>
            <p className='ant-upload-text'>{t("screenshotDragUploadText")}</p>
          </Dragger>
        </>
      ),
    },
    {
      key: "TextUnitSelection",
      label: t("textUnitSelectionTabTitle"),
      disabled: files.length === 0,
      children: (
        <>
          <ScreenshotSelector
            branchStats={branchStats}
            files={files}
            onScreenshotTextUnitAssignment={(map) => {
              onDataChange?.(files, map);
            }}
          />
        </>
      ),
    },
  ];

  return (
    <Form>
      <> {JSON.stringify(files)}</>
      <Tabs
        activeKey={activeTab}
        defaultActiveKey='ScreenshotUpload'
        items={items}
        onChange={setActiveTab}
      />
    </Form>
  );
};

export default ScreenshotWizard;
