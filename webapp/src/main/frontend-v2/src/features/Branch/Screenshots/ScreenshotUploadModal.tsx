import { Button, Modal } from "antd";
import React, { memo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";

import ScreenshotWizard from "./ScreenshotWizard";

import type { BranchStatistics } from "@/types/branchStatistics";

import "@/i18n";
import { uploadImage } from "@/api/uploadImage";
import type { ScreenshotRun } from "@/api/types/screenshotRun";
import { uploadScreenshot } from "@/api/uploadScreenshot";

interface BranchDetailsProps {
  isOpen: boolean;
  branchStats: BranchStatistics;
  setIsOpen: (open: boolean) => void;
  onPreviewImage?: (screenshotUrl: string) => void;
}

const ScreenshotUpload: React.FC<BranchDetailsProps> = ({
  isOpen,
  setIsOpen,
  branchStats,
}) => {
  const { t } = useTranslation("branch");
  const [isUploading, setIsUploading] = useState(false);
  const dataToUpload = useRef<{
    files: File[];
    screenshotToTextUnitMap: Map<string, number[]>;
  }>({ files: [], screenshotToTextUnitMap: new Map() });

  const [canUpload, setCanUpload] = useState(false);

  const handleOk = async () => {
    setIsUploading(true);
    const { files, screenshotToTextUnitMap } = dataToUpload.current;

    if (files.length === 0) {
      setIsUploading(false);
      setIsOpen(false);
      return;
    }

    const imageUuidByFileName = new Map(
      files.map((file) => [file.name, crypto.randomUUID()]),
    );

    const imageUploadPromises = await Promise.allSettled(
      files.map(async (file) => {
        const arrayBuffer = await file.arrayBuffer();
        return uploadImage(imageUuidByFileName.get(file.name)!, arrayBuffer);
      }),
    );

    const errorUploading = imageUploadPromises.some(
      (result) => result.status === "rejected",
    );

    if (errorUploading) {
      // TODO
      setIsUploading(false);
      return;
    }

    const screenshotRun: ScreenshotRun = {
      id: branchStats.branch.repository.manualScreenshotRun.id,
      name: undefined,
      repository: undefined,
      screenshots: Array.from(screenshotToTextUnitMap.entries()).map(
        ([screenshotName, textUnitIds]) => {
          return {
            locale: branchStats.branch.repository.sourceLocale,
            name: crypto.randomUUID(),
            branch: {
              id: branchStats.branch.id,
            },
            src: `api/images/${imageUuidByFileName.get(screenshotName)!}`,
            textUnits: textUnitIds.map((id) => ({ tmTextUnit: { id } })),
          };
        },
      ),
    };

    await uploadScreenshot(screenshotRun);
    setIsUploading(false);
    setIsOpen(false);
  };

  const handleCancel = () => {
    setIsOpen(false);
  };

  const handleDataStorage = (files: File[], map: Map<string, number[]>) => {
    dataToUpload.current = { files, screenshotToTextUnitMap: map };
    setCanUpload(files.length > 0 && map.size > 0);
  };

  return (
    <Modal
      title={t("uploadScreenshot")}
      open={isOpen}
      onCancel={handleCancel}
      confirmLoading={isUploading}
      width='80vw'
      footer={[
        <Button key='back' onClick={handleCancel}>
          {t("cancel")}
        </Button>,
        <Button
          disabled={!canUpload}
          key='submit'
          type='primary'
          onClick={handleOk}
        >
          {t("submit")}
        </Button>,
      ]}
    >
      <section className='m-1'>
        <ScreenshotWizard
          branchStats={branchStats}
          onDataChange={handleDataStorage}
        />
      </section>
    </Modal>
  );
};

export default memo(ScreenshotUpload);
