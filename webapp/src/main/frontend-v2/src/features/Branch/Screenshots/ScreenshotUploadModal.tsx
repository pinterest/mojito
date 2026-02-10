import { Button, message, Modal } from "antd";
import React, { memo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";

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

interface ScreenshotUploadData {
  files: File[];
  screenshotToTextUnitMap: Map<string, number[]>;
}

const ScreenshotUpload: React.FC<BranchDetailsProps> = ({
  isOpen,
  setIsOpen,
  branchStats,
}) => {
  const dataToUpload = useRef<ScreenshotUploadData>({
    files: [],
    screenshotToTextUnitMap: new Map(),
  });

  const [canUpload, setCanUpload] = useState(false);

  const { t } = useTranslation("branch");
  const queryClient = useQueryClient();

  const uploadScreenshotsMutation = useMutation({
    mutationFn: async ({
      files,
      screenshotToTextUnitMap,
    }: ScreenshotUploadData): Promise<void> => {
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
        throw new Error("Error uploading images");
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
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: [
          "branchTextUnitStatus",
          {
            branchName: branchStats.branch.name,
            repoName: branchStats.branch.repository.name,
          },
        ],
        exact: true,
      });

      await queryClient.invalidateQueries({
        queryKey: ["branchStatistics", { branchName: branchStats.branch.name }],
        exact: true,
      });

      setIsOpen(false);
    },
    onError: (error) => {
      console.error("Error uploading screenshots", error);
      message.error(t("schreenshotUploadFailed"));
    },
  });

  const handleOk = async () => {
    const { files, screenshotToTextUnitMap } = dataToUpload.current;
    uploadScreenshotsMutation.mutate({ files, screenshotToTextUnitMap });
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
      title={t("screenshotUploadModal.title")}
      open={isOpen}
      onCancel={handleCancel}
      confirmLoading={uploadScreenshotsMutation.status === "pending"}
      width='80vw'
      footer={[
        <Button key='back' onClick={handleCancel}>
          {t("screenshotUploadModal.cancel")}
        </Button>,
        <Button
          disabled={!canUpload}
          loading={uploadScreenshotsMutation.status === "pending"}
          key='submit'
          type='primary'
          onClick={handleOk}
        >
          {t("screenshotUploadModal.submit")}
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
