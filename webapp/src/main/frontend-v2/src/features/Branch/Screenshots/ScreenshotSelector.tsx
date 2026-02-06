import React, { memo, useMemo, useState } from "react";
import { Button, Collapse, type CollapseProps } from "antd";
import type { RcFile } from "antd/es/upload";

import ScreenshotPanelSelector from "./ScreenshotPanelSelector";
import { getTextUnitScreenshotMap } from "../utils/textUnitStatusVisualization";

import type { BranchStatistics } from "@/types/branchStatistics";
import { useTranslation } from "react-i18next";

interface ScreenshotSelectorProps {
  branchStats?: BranchStatistics;
  files: RcFile[];

  onPreviewImage?: (screenshot: RcFile) => void;
  onScreenshotTextUnitAssignment: (
    screenshotToTextUnitMap: Map<string, number[]>,
  ) => void;
}

const ScreenshotSelector: React.FC<ScreenshotSelectorProps> = ({
  branchStats,
  files,
  onScreenshotTextUnitAssignment,
  onPreviewImage,
}) => {
  const { t } = useTranslation("branch");
  const textUnits: { id: number; name: string }[] = useMemo(() => {
    const uniqueTextUnits = new Map();
    (branchStats?.branchTextUnitStatistics ?? []).forEach((textUnitStat) => {
      uniqueTextUnits.set(textUnitStat.tmTextUnit.id, {
        id: textUnitStat.tmTextUnit.id,
        name: textUnitStat.tmTextUnit.name,
      });
    });
    return Array.from(uniqueTextUnits.values());
  }, [branchStats]);

  const [userScreenshotToTextUnitMap, setUserScreenshotToTextUnitMap] =
    useState<Map<string, number[]>>(new Map());

  const serverScreenshotToTextUnitMap = useMemo(() => {
    if (!branchStats) return new Map();

    return new Map(
      [...getTextUnitScreenshotMap(branchStats).entries()].map(
        ([textUnitId, screenshot]) => {
          return [textUnitId, screenshot.src];
        },
      ),
    );
  }, [branchStats]);

  const handleRowSelectionChange = (textUnitIds: number[], file: RcFile) => {
    const newMap = new Map(userScreenshotToTextUnitMap);
    newMap.set(file.name, textUnitIds);
    setUserScreenshotToTextUnitMap(newMap);
    onScreenshotTextUnitAssignment(newMap);
  };

  const textUnitToScreenshotNameMap = useMemo(() => {
    const map = new Map<number, string>();
    serverScreenshotToTextUnitMap.forEach((screenshotSrc, textUnitId) => {
      const screenshotName = screenshotSrc;
      map.set(textUnitId, screenshotName);
    });

    userScreenshotToTextUnitMap.forEach((textUnitIds, screenshotName) => {
      textUnitIds.forEach((textUnitId) => {
        map.set(textUnitId, screenshotName);
      });
    });
    return map;
  }, [userScreenshotToTextUnitMap, serverScreenshotToTextUnitMap]);

  const collapseItems: CollapseProps["items"] = files.map((file) => ({
    key: file.name,
    label: file.name,
    extra: (
      <Button
        onClick={(e) => {
          e.stopPropagation();
          onPreviewImage?.(file);
        }}
      >
        {t("preview")}
      </Button>
    ),
    children: (
      <ScreenshotPanelSelector
        file={file}
        textUnitToScreenshotNameMap={textUnitToScreenshotNameMap}
        textUnits={textUnits}
        onTextUnitScreenshotChange={(textUnitIds) =>
          handleRowSelectionChange(textUnitIds, file)
        }
      />
    ),
  }));

  return (
    <div className='p-1'>
      <Collapse items={collapseItems}></Collapse>
    </div>
  );
};

export default memo(ScreenshotSelector);
