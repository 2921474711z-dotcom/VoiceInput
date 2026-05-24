function resolveFileName(contentDisposition: string | null, fallbackFileName: string) {
  if (!contentDisposition) {
    return fallbackFileName;
  }

  const utf8Match = contentDisposition.match(/filename\*\s*=\s*UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1]);
    } catch {
      return utf8Match[1];
    }
  }

  const plainMatch = contentDisposition.match(/filename\s*=\s*"?(?<name>[^";]+)"?/i);
  return plainMatch?.groups?.name || fallbackFileName;
}

export async function downloadFileFromUrl(url: string, fallbackFileName: string) {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error("下载请求失败");
  }

  const blob = await response.blob();
  const fileName = resolveFileName(response.headers.get("content-disposition"), fallbackFileName);
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = objectUrl;
  link.download = fileName;
  link.click();
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
}
