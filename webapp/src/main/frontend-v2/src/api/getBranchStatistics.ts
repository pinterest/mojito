import type { BranchStatistics } from "@/types/branchStatistics";
import type { Page } from "@/types/page";

export async function getBranchStatistics({
    branchId,
    createdByUserName,
    deleted,
    page = 0,
    size = 10,
}: {
    branchId?: number;
    createdByUserName?: string;
    deleted?: boolean;
    page?: number;
    size?: number;
}): Promise<Page<BranchStatistics>> {
    const searchParams = new URLSearchParams();
    searchParams.append("branchId", branchId ? branchId.toString() : "");
    searchParams.append("page", page.toString());
    searchParams.append("size", size.toString());

    if (createdByUserName) {
        searchParams.append("createdByUserName", createdByUserName);
    }
    if (deleted !== undefined) {
        searchParams.append("deleted", deleted.toString());
    }

    const response = await fetch(
        `/api/branchStatistics?${searchParams.toString()}`,
    );
    if (!response.ok) {
        console.error("Failed to fetch branch statistics", response);
        throw new Error(
            `Error network response: ${response.status} ${response.statusText}`,
        );
    }
    return response.json() satisfies Promise<Page<BranchStatistics>>;
}
