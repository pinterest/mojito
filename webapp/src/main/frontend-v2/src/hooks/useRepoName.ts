import { useLocation } from "react-router";

export function useRepoName(): string {
    const location = useLocation();
    return location.search
        ? new URLSearchParams(location.search).get("repoName") || ""
        : "";
}
