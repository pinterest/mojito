export async function uploadImage(
  id: string,
  data: ArrayBuffer,
): Promise<void> {
  const response = await fetch(`/api/images/${id}`, {
    method: "PUT",
    headers: {
      "X-CSRF-TOKEN": APP_CONFIG.csrfToken,
    },
    body: data,
  });

  if (!response.ok) {
    console.error("Failed to upload image", response);
    throw new Error(
      `Error network response: ${response.status} ${response.statusText}`,
    );
  }
}
