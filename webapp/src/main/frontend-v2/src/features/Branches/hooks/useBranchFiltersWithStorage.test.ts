import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { useBranchFiltersWithStorage } from "./useBranchFiltersWithStorage";
import type { StatusFilter } from "../BranchFilters";

describe("useBranchFiltersWithStorage", () => {
  const testUsername = "testuser";

  const mockSessionStorage = {
    getItem: vi.fn(),
    setItem: vi.fn(),
  };

  beforeEach(() => {
    Object.defineProperty(window, "sessionStorage", {
      value: mockSessionStorage,
      writable: true,
    });

    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe("initial state", () => {
    it("should return default state when no stored data", () => {
      mockSessionStorage.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useBranchFiltersWithStorage(testUsername),
      );

      expect(result.current.queryText).toBe("testuser");
      expect(result.current.statusFilter).toEqual([]);
      expect(result.current.createdBefore).toBeNull();
      expect(result.current.createdAfter).toBeNull();
    });

    it("should load state from session storage when available", () => {
      const storedData = {
        queryText: "stored-query",
        statusFilter: ["empty", "deleted"] as StatusFilter[],
        createdBefore: "2024-01-01T00:00:00.000Z",
        createdAfter: "2024-02-01T00:00:00.000Z",
      };

      mockSessionStorage.getItem.mockReturnValue(JSON.stringify(storedData));

      const { result } = renderHook(() =>
        useBranchFiltersWithStorage(testUsername),
      );

      expect(result.current.queryText).toBe("stored-query");
      expect(result.current.statusFilter).toEqual(["empty", "deleted"]);
      expect(result.current.createdBefore).toEqual(
        new Date("2024-01-01T00:00:00.000Z"),
      );
      expect(result.current.createdAfter).toEqual(
        new Date("2024-02-01T00:00:00.000Z"),
      );
    });

    it("should handle invalid JSON in session storage gracefully", () => {
      mockSessionStorage.getItem.mockReturnValue("invalid-json");

      const consoleSpy = vi.spyOn(console, "warn").mockImplementation(() => {});

      const { result } = renderHook(() =>
        useBranchFiltersWithStorage(testUsername),
      );

      expect(result.current.queryText).toBe("testuser");
      expect(result.current.statusFilter).toEqual([]);
      expect(result.current.createdBefore).toBeNull();
      expect(result.current.createdAfter).toBeNull();
      expect(consoleSpy).toHaveBeenCalledWith(
        "Failed to load branch filters from session storage:",
        expect.any(Error),
      );

      consoleSpy.mockRestore();
    });

    it("should handle non-array statusFilter in stored data", () => {
      const storedData = {
        queryText: "test",
        statusFilter: "invalid",
        createdBefore: null,
        createdAfter: null,
      };

      mockSessionStorage.getItem.mockReturnValue(JSON.stringify(storedData));

      const { result } = renderHook(() =>
        useBranchFiltersWithStorage(testUsername),
      );

      expect(result.current.statusFilter).toEqual([]);
    });
  });

  describe("state persistence", () => {
    it("should save state to session storage when state changes", () => {
      mockSessionStorage.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useBranchFiltersWithStorage(testUsername),
      );

      act(() => {
        result.current.setQueryText("new-query");
      });

      expect(mockSessionStorage.setItem).toHaveBeenCalledWith(
        "branchFilters",
        JSON.stringify({
          queryText: "new-query",
          statusFilter: [],
          createdBefore: undefined,
          createdAfter: undefined,
        }),
      );
    });

    it("should handle storage errors gracefully when saving", () => {
      mockSessionStorage.getItem.mockReturnValue(null);
      mockSessionStorage.setItem.mockImplementation(() => {
        throw new Error("Storage quota exceeded");
      });

      const consoleSpy = vi.spyOn(console, "warn").mockImplementation(() => {});

      const { result } = renderHook(() =>
        useBranchFiltersWithStorage(testUsername),
      );

      act(() => {
        result.current.setQueryText("new-query");
      });

      expect(consoleSpy).toHaveBeenCalledWith(
        "Failed to save branch filters to session storage:",
        expect.any(Error),
      );

      consoleSpy.mockRestore();
    });
  });

  describe("setter functions", () => {
    it("should update queryText", () => {
      mockSessionStorage.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useBranchFiltersWithStorage(testUsername),
      );

      act(() => {
        result.current.setQueryText("updated-query");
      });

      expect(result.current.queryText).toBe("updated-query");
    });

    it("should update statusFilter", () => {
      ({
        getItem: vi.fn(),
        setItem: vi.fn(),
      }).getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useBranchFiltersWithStorage(testUsername),
      );

      const newStatusFilter: StatusFilter[] = ["empty", "deleted"];

      act(() => {
        result.current.setStatusFilter(newStatusFilter);
      });

      expect(result.current.statusFilter).toEqual(newStatusFilter);
    });

    it("should update createdBefore", () => {
      mockSessionStorage.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useBranchFiltersWithStorage(testUsername),
      );

      const newDate = new Date("2024-01-01");

      act(() => {
        result.current.setCreatedBefore(newDate);
      });

      expect(result.current.createdBefore).toBe(newDate);
    });

    it("should update createdAfter", () => {
      mockSessionStorage.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useBranchFiltersWithStorage(testUsername),
      );

      const newDate = new Date("2024-02-01");

      act(() => {
        result.current.setCreatedAfter(newDate);
      });

      expect(result.current.createdAfter).toBe(newDate);
    });

    it("should handle null values for date setters", () => {
      mockSessionStorage.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useBranchFiltersWithStorage(testUsername),
      );

      act(() => {
        result.current.setCreatedBefore(new Date("2024-01-01"));
        result.current.setCreatedAfter(new Date("2024-02-01"));
      });

      act(() => {
        result.current.setCreatedBefore(null);
        result.current.setCreatedAfter(null);
      });

      expect(result.current.createdBefore).toBeNull();
      expect(result.current.createdAfter).toBeNull();
    });
  });

  describe("date serialization", () => {
    it("should serialize dates to ISO strings when saving", () => {
      mockSessionStorage.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useBranchFiltersWithStorage(testUsername),
      );

      const beforeDate = new Date("2024-01-01T10:00:00.000Z");
      const afterDate = new Date("2024-02-01T15:30:00.000Z");

      act(() => {
        result.current.setCreatedBefore(beforeDate);
        result.current.setCreatedAfter(afterDate);
      });

      expect(mockSessionStorage.setItem).toHaveBeenCalledWith(
        "branchFilters",
        JSON.stringify({
          queryText: "testuser",
          statusFilter: [],
          createdBefore: "2024-01-01T10:00:00.000Z",
          createdAfter: "2024-02-01T15:30:00.000Z",
        }),
      );
    });

    it("should deserialize ISO strings to Date objects when loading", () => {
      const storedData = {
        queryText: "test",
        statusFilter: [],
        createdBefore: "2024-01-01T10:00:00.000Z",
        createdAfter: "2024-02-01T15:30:00.000Z",
      };

      mockSessionStorage.getItem.mockReturnValue(JSON.stringify(storedData));

      const { result } = renderHook(() =>
        useBranchFiltersWithStorage(testUsername),
      );

      expect(result.current.createdBefore).toEqual(
        new Date("2024-01-01T10:00:00.000Z"),
      );
      expect(result.current.createdAfter).toEqual(
        new Date("2024-02-01T15:30:00.000Z"),
      );
    });
  });

  describe("multiple state updates", () => {
    it("should handle multiple state updates correctly", () => {
      mockSessionStorage.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useBranchFiltersWithStorage(testUsername),
      );

      act(() => {
        result.current.setQueryText("multi-test");
        result.current.setStatusFilter(["empty"]);
        result.current.setCreatedBefore(new Date("2024-01-01"));
      });

      expect(result.current.queryText).toBe("multi-test");
      expect(result.current.statusFilter).toEqual(["empty"]);
      expect(result.current.createdBefore).toEqual(new Date("2024-01-01"));
      expect(result.current.createdAfter).toBeNull();
    });

    it("should preserve previous state when updating an individual field", () => {
      const initialData = {
        queryText: "initial",
        statusFilter: ["deleted"] as StatusFilter[],
        createdBefore: "2024-01-01T00:00:00.000Z",
        createdAfter: null,
      };

      mockSessionStorage.getItem.mockReturnValue(JSON.stringify(initialData));

      const { result } = renderHook(() =>
        useBranchFiltersWithStorage(testUsername),
      );

      act(() => {
        result.current.setQueryText("updated");
      });

      expect(result.current.queryText).toBe("updated");
      expect(result.current.statusFilter).toEqual(["deleted"]);
      expect(result.current.createdBefore).toEqual(
        new Date("2024-01-01T00:00:00.000Z"),
      );
      expect(result.current.createdAfter).toBeNull();
    });
  });
});
