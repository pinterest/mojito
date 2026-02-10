import type { ScreenshotRun } from "./types/screenshotRun";

export async function uploadScreenshot(
  screenshotRun: ScreenshotRun,
): Promise<ScreenshotRun> {
  const response = await fetch("/api/screenshots", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-TOKEN": APP_CONFIG.csrfToken,
    },
    body: JSON.stringify(screenshotRun),
  });

  if (!response.ok) {
    console.error("Failed to upload screenshot", response);
    throw new Error(
      `Error network response: ${response.status} ${response.statusText}`,
    );
  }
  return response.json() satisfies Promise<ScreenshotRun>;
}
