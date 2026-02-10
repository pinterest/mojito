import type { PollableTask } from "./types/pollableTask";

export async function deleteBranch({
  branchId,
  repositoryId,
}: {
  branchId: number;
  repositoryId: number;
}): Promise<PollableTask> {
  const response = await fetch(
    `/api/repositories/${encodeURIComponent(repositoryId)}/branches?branchId=${encodeURIComponent(branchId)}`,
    {
      method: "DELETE",
      headers: {
        "X-CSRF-TOKEN": APP_CONFIG.csrfToken,
      },
    },
  );

  if (!response.ok) {
    console.error("Failed to delete branch", response);
    throw new Error(
      `Error network response: ${response.status} ${response.statusText}`,
    );
  }
  return response.json() satisfies Promise<PollableTask>;
}
