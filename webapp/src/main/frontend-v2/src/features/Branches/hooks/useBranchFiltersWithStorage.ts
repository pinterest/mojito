import { useEffect, useState } from "react";
import type { StatusFilter } from "../BranchFilters";

interface BranchFiltersState {
  queryText: string;
  statusFilter: StatusFilter[];
  createdBefore: Date | null;
  createdAfter: Date | null;
}

interface BranchFiltersActions {
  setQueryText: (value: string) => void;
  setStatusFilter: (value: StatusFilter[]) => void;
  setCreatedBefore: (value: Date | null) => void;
  setCreatedAfter: (value: Date | null) => void;
}

interface BranchFiltersActions {
  setQueryText: (value: string) => void;
  setStatusFilter: (value: StatusFilter[]) => void;
  setCreatedBefore: (value: Date | null) => void;
  setCreatedAfter: (value: Date | null) => void;
}

type UseBranchFiltersReturn = BranchFiltersState & BranchFiltersActions;

const STORAGE_KEY = "branchFilters";

const getDefaultFiltersState = (username: string): BranchFiltersState => ({
  queryText: username,
  statusFilter: [],
  createdBefore: null,
  createdAfter: null,
});

const loadFiltersFromStorage = (username: string): BranchFiltersState => {
  try {
    const storedData = sessionStorage.getItem(STORAGE_KEY);

    if (!storedData) {
      return getDefaultFiltersState(username);
    }

    const parsedData = JSON.parse(storedData);

    return {
      queryText: parsedData.queryText,
      statusFilter: Array.isArray(parsedData.statusFilter)
        ? parsedData.statusFilter
        : [],
      createdBefore: parsedData.createdBefore
        ? new Date(parsedData.createdBefore)
        : null,
      createdAfter: parsedData.createdAfter
        ? new Date(parsedData.createdAfter)
        : null,
    };
  } catch (error) {
    console.warn("Failed to load branch filters from session storage:", error);
    return getDefaultFiltersState(username);
  }
};

const saveFiltersToStorage = (filters: BranchFiltersState): void => {
  try {
    const dataToStore = {
      queryText: filters.queryText,
      statusFilter: filters.statusFilter,
      createdBefore: filters.createdBefore?.toISOString(),
      createdAfter: filters.createdAfter?.toISOString(),
    };

    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(dataToStore));
  } catch (error) {
    console.warn("Failed to save branch filters to session storage:", error);
  }
};

export const useBranchFiltersWithStorage = (
  username: string,
): UseBranchFiltersReturn => {
  const [filtersState, setFiltersState] = useState<BranchFiltersState>(() =>
    loadFiltersFromStorage(username),
  );

  useEffect(() => {
    saveFiltersToStorage(filtersState);
  }, [filtersState]);

  const setQueryText = (queryText: string) => {
    setFiltersState((prev) => ({ ...prev, queryText }));
  };

  const setStatusFilter = (statusFilter: StatusFilter[]) => {
    setFiltersState((prev) => ({ ...prev, statusFilter }));
  };

  const setCreatedBefore = (createdBefore: Date | null) => {
    setFiltersState((prev) => ({ ...prev, createdBefore }));
  };

  const setCreatedAfter = (createdAfter: Date | null) => {
    setFiltersState((prev) => ({ ...prev, createdAfter }));
  };

  const actions: BranchFiltersActions = {
    setQueryText,
    setStatusFilter,
    setCreatedBefore,
    setCreatedAfter,
  };

  return { ...filtersState, ...actions };
};
