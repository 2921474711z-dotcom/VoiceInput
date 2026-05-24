import type {
  AppConfigRequest,
  AppConfigResponse,
  ConfigTemplateResponse,
  HistoryPageResponse,
  HotwordCategoryResponse,
  HotwordResponse,
  PagedHotwordResponse,
  SceneType,
  TaskDetailResponse,
  TaskStatus,
  TaskSummaryResponse,
  UploadAssetResponse,
  UsageStatsResponse
} from "../types/api";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, init);
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: "请求失败" }));
    throw new Error(error.message ?? "请求失败");
  }
  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) {
    return response.json();
  }
  return response as unknown as T;
}

export async function uploadAudio(file: File, durationSeconds?: number) {
  const formData = new FormData();
  formData.append("file", file);
  if (typeof durationSeconds === "number") {
    formData.append("durationSeconds", durationSeconds.toFixed(2));
  }
  return request<UploadAssetResponse>("/uploads/audio", {
    method: "POST",
    body: formData
  });
}

export async function createTask(uploadId: string, sceneType: SceneType, templateId: string) {
  return request<TaskSummaryResponse>("/tasks/process", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ uploadId, sceneType, templateId })
  });
}

export async function getTaskDetail(id: string) {
  return request<TaskDetailResponse>(`/tasks/${id}`);
}

export async function saveTaskToHistory(id: string) {
  return request<TaskDetailResponse>(`/tasks/${id}/save`, { method: "POST" });
}

export async function reoptimizeTask(id: string, templateId?: string) {
  return request<TaskSummaryResponse>(`/tasks/${id}/reoptimize`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ templateId })
  });
}

export async function getHistory(params: {
  keyword?: string;
  sceneType?: SceneType;
  status?: TaskStatus;
  startDate?: string;
  endDate?: string;
  sort?: string;
  page?: number;
  size?: number;
}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") query.append(key, String(value));
  });
  return request<HistoryPageResponse>(`/history?${query.toString()}`);
}

export async function getHistoryDetail(id: string) {
  return request<TaskDetailResponse>(`/history/${id}`);
}

export async function deleteHistory(id: string) {
  return request<void>(`/history/${id}`, { method: "DELETE" });
}

export function getMarkdownDownloadUrl(id: string) {
  return `${API_BASE_URL}/history/${id}/export-markdown`;
}

export async function getHotwordCategories() {
  return request<HotwordCategoryResponse[]>("/hotwords/categories");
}

export async function getHotwords(params: { categoryId?: number; keyword?: string; page?: number; size?: number }) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") query.append(key, String(value));
  });
  return request<PagedHotwordResponse>(`/hotwords?${query.toString()}`);
}

export async function createHotword(payload: unknown) {
  return request<HotwordResponse>("/hotwords", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
}

export async function updateHotword(id: string, payload: unknown) {
  return request<HotwordResponse>(`/hotwords/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
}

export async function deleteHotword(id: string) {
  return request<void>(`/hotwords/${id}`, { method: "DELETE" });
}

export async function importHotwords(file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return request<void>("/hotwords/import", { method: "POST", body: formData });
}

export function getHotwordExportUrl() {
  return `${API_BASE_URL}/hotwords/export`;
}

export async function getConfig() {
  return request<AppConfigResponse>("/config");
}

export async function updateConfig(payload: AppConfigRequest) {
  return request<AppConfigResponse>("/config", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
}

export async function resetConfig() {
  return request<AppConfigResponse>("/config/reset", { method: "POST" });
}

export async function getTemplates() {
  return request<ConfigTemplateResponse[]>("/config/templates");
}

export async function createTemplate(payload: unknown) {
  return request<ConfigTemplateResponse>("/config/templates", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
}

export async function applyTemplate(id: string) {
  return request<AppConfigResponse>(`/config/templates/${id}/apply`, { method: "POST" });
}

export async function setDefaultTemplate(id: string) {
  return request<void>(`/config/templates/${id}/default`, { method: "POST" });
}

export async function deleteTemplate(id: string) {
  return request<void>(`/config/templates/${id}`, { method: "DELETE" });
}

export async function getUsageStats(params: { sceneType?: SceneType; from?: string; to?: string }) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") query.append(key, String(value));
  });
  return request<UsageStatsResponse>(`/stats/usage?${query.toString()}`);
}
