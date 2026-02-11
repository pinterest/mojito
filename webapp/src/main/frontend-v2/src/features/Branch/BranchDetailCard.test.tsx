import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import "@testing-library/jest-dom";

import BranchDetailCard from "./BranchDetailCard";
import type { BranchStatistics } from "@/types/branchStatistics";

// Mock components
vi.mock("./Screenshots/ScreenshotUploadModal", () => ({
  default: () => <div data-testid='screenshot-upload-modal' />,
}));

vi.mock("@/components/navigation/PullRequestLink", () => ({
  default: ({
    repoName,
    branchName,
  }: {
    repoName: string;
    branchName: string;
  }) => (
    <div data-testid='pull-request-link'>
      {repoName}/{branchName}
    </div>
  ),
}));

vi.mock("@/utils/formatDate", () => ({
  displayDate: (date: Date) => date.toISOString().split("T")[0],
}));

vi.mock("./utils/textUnitStatusVisualization", () => ({
  getTextUnitScreenshotMap: () => new Map(),
}));

const createMockBranchStats = (
  overrides: Partial<BranchStatistics> = {},
): BranchStatistics => ({
  id: 1,
  createdDate: 1640995200000,
  totalCount: 100,
  forTranslationCount: 20,
  branch: {
    id: 1,
    createdDate: 1640995200000,
    name: "feature/test-branch",
    deleted: false,
    repository: {
      id: 1,
      createdDate: 1640995200000,
      name: "test-repo",
      sourceLocale: { id: 1 },
      manualScreenshotRun: { id: 1, createdDate: null },
    },
    createdByUser: {
      id: 1,
      createdDate: 1640995200000,
      username: "testuser",
      commonName: "Test User",
    },
    screenshots: [],
  },
  branchTextUnitStatistics: [
    {
      id: 1,
      tmTextUnit: {
        id: 1,
        createdDate: 1640995200000,
        name: "test.key",
        content: "Test content",
      },
      forTranslationCount: 10,
      totalCount: 50,
    },
  ],
  ...overrides,
});

const setUserRole = (role: string) => {
  Object.defineProperty(window, "APP_CONFIG", {
    value: { user: { role } },
    writable: true,
  });
};

describe("BranchDetailCard", () => {
  beforeEach(() => {
    setUserRole("ROLE_USER");
  });

  describe("Basic rendering", () => {
    it("renders branch details correctly", () => {
      const mockBranchStats = createMockBranchStats();

      render(<BranchDetailCard branchStats={mockBranchStats} />);

      expect(screen.getByText("branchDetails")).toBeInTheDocument();
      expect(
        screen.getByText("test-repo/feature/test-branch"),
      ).toBeInTheDocument();
      expect(screen.getByText("testuser")).toBeInTheDocument();
    });
  });

  describe("Delete functionality", () => {
    it("shows delete button for admin users when branch is not deleted", () => {
      setUserRole("ROLE_ADMIN");
      const mockBranchStats = createMockBranchStats({
        branch: { ...createMockBranchStats().branch, deleted: false },
      });

      render(<BranchDetailCard branchStats={mockBranchStats} />);

      expect(screen.getByText("branch.delete.btn")).toBeInTheDocument();
    });

    it("does not show delete button for non-admin users", () => {
      const mockBranchStats = createMockBranchStats();

      render(<BranchDetailCard branchStats={mockBranchStats} />);

      expect(screen.queryByText("branch.delete.btn")).not.toBeInTheDocument();
    });

    it("calls onBranchDelete when delete is confirmed", async () => {
      setUserRole("ROLE_ADMIN");
      const user = userEvent.setup();
      const mockOnBranchDelete = vi.fn();
      const mockBranchStats = createMockBranchStats();

      render(
        <BranchDetailCard
          branchStats={mockBranchStats}
          onBranchDelete={mockOnBranchDelete}
        />,
      );

      const deleteButton = screen.getByText("branch.delete.btn");
      await user.click(deleteButton);

      const confirmButton = screen.getByText("branch.delete.yes");
      await user.click(confirmButton);

      expect(mockOnBranchDelete).toHaveBeenCalledWith(1, 1);
    });
  });

  describe("Branch status alerts", () => {
    it("shows deleted branch alert when branch is deleted", () => {
      const mockBranchStats = createMockBranchStats({
        branch: { ...createMockBranchStats().branch, deleted: true },
      });

      render(<BranchDetailCard branchStats={mockBranchStats} />);

      expect(screen.getByText("branch.alert.deleted")).toBeInTheDocument();
    });
  });

  describe("Screenshot upload functionality", () => {
    const createMockStatsWithMultipleTextUnits = () =>
      createMockBranchStats({
        branchTextUnitStatistics: [
          {
            id: 1,
            tmTextUnit: {
              id: 1,
              createdDate: 1640995200000,
              name: "test.key",
              content: "Test",
            },
            forTranslationCount: 10,
            totalCount: 50,
          },
          {
            id: 2,
            tmTextUnit: {
              id: 2,
              createdDate: 1640995200000,
              name: "test.key2",
              content: "Test2",
            },
            forTranslationCount: 5,
            totalCount: 25,
          },
        ],
      });

    it("shows screenshot upload warning when screenshots are missing", () => {
      const mockBranchStats = createMockStatsWithMultipleTextUnits();

      render(<BranchDetailCard branchStats={mockBranchStats} />);

      expect(screen.getByText("screenshotsNotUploaded")).toBeInTheDocument();
      expect(screen.getByText("uploadScreenshot")).toBeInTheDocument();
    });

    it("opens screenshot upload modal when upload button is clicked", async () => {
      const user = userEvent.setup();
      const mockBranchStats = createMockStatsWithMultipleTextUnits();

      render(<BranchDetailCard branchStats={mockBranchStats} />);

      const uploadButton = screen.getByText("uploadScreenshot");
      await user.click(uploadButton);

      expect(screen.getByTestId("screenshot-upload-modal")).toBeInTheDocument();
    });
  });

  describe("Progress calculation", () => {
    it("returns 100% progress when totalCount is 0", () => {
      const mockBranchStats = createMockBranchStats({
        totalCount: 0,
        forTranslationCount: 0,
      });

      render(<BranchDetailCard branchStats={mockBranchStats} />);

      const progressBar = screen.getByRole("progressbar");
      expect(progressBar).toHaveAttribute("aria-valuenow", "100");
    });
  });
});
