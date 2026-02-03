import { useLocation } from "react-router";

export function useBranchName(): string {
  const location = useLocation();
  return location.search
    ? new URLSearchParams(location.search).get("branchName") || ""
    : "";
}
