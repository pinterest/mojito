import type { PollableTask } from "./types/pollableTask";

export async function getPollableTask({
  pollableTaskId,
}: {
  pollableTaskId: number;
}): Promise<PollableTask> {
  const response = await fetch(
    `/api/pollableTasks/${encodeURIComponent(pollableTaskId)}`,
  );
  if (!response.ok) {
    console.error("Failed to fetch pollable task", response);
    throw new Error(
      `Error network response: ${response.status} ${response.statusText}`,
    );
  }
  return response.json() satisfies Promise<PollableTask>;
}
