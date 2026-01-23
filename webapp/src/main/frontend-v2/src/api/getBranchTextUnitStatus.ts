import type { BranchTextUnitStatusDto } from "@/types/branchTextUnitStatus";

export async function getBranchTextUnitStatus({
    branchName,
    repoName,
}: {
    branchName: string;
    repoName: string;
}): Promise<BranchTextUnitStatusDto> {
    const searchParams = new URLSearchParams();
    searchParams.append("branchName", branchName);
    searchParams.append("repositoryName", repoName);

    const response = await fetch(
        `/api/branch/textUnitStatus?${searchParams.toString()}`,
    );
    if (!response.ok) {
        console.error("Failed to fetch branch text unit status", response);
        throw new Error("Error network response");
    }
    return response.json() satisfies Promise<BranchTextUnitStatusDto>;
}
