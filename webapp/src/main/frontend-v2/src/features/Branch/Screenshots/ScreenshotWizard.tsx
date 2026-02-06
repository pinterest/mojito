import { Form, Image, Tabs, type TabsProps, Upload } from "antd";
import { useTranslation } from "react-i18next";
import React, { useEffect, useState } from "react";
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
  const [previewImageSrc, setPreviewImageSrc] = useState<string>("");
  const [previewFile, setPreviewFile] = useState<RcFile | null>(null);

  const { t } = useTranslation("branch");

  useEffect(() => {
    if (!previewFile || !(previewFile instanceof Blob)) {
      console.error("Not a Blob/File:", previewFile);
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setPreviewImageSrc("");
      return;
    }

    const url = URL.createObjectURL(previewFile);
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setPreviewImageSrc(url);

    return () => URL.revokeObjectURL(url);
  }, [previewFile]);

  const items: TabsProps["items"] = [
    {
      key: "ScreenshotUpload",
      label: t("screenshotUploadTabTitle"),
      children: (
        <>
          <Dragger
            name='file'
            multiple={true}
            onChange={(info) => {
              setFiles(
                info.fileList.map((file) => file.originFileObj as RcFile),
              );
            }}
            beforeUpload={() => {
              return false;
            }}
          >
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
            onPreviewImage={setPreviewFile}
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
      <Image
        className='d-none'
        src={previewImageSrc}
        preview={{
          open: Boolean(previewImageSrc),
          onOpenChange: () => {
            setPreviewFile(null);
          },
        }}
      />
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
