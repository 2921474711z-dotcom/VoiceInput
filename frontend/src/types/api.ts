export type SceneType =
  | "MEETING_MINUTES"
  | "WORK_REPORT"
  | "FORMAL_EXPRESSION"
  | "MARKDOWN_NOTE"
  | "CODE_COMMENT"
  | "CHAT_REPLY";

export type TaskStatus = "READY" | "PENDING" | "PROCESSING" | "SUCCESS" | "FAILED";

export interface UploadAssetResponse {
  id: string;
  fileName: string;
  sizeBytes: number;
  durationSeconds?: number;
  uploadedAt: string;
}

export interface TaskSummaryResponse {
  id: string;
  title?: string;
  sceneType: SceneType;
  status: TaskStatus;
  fileName: string;
  templateId?: string;
  templateName?: string;
  optimizedWordCount?: number;
  rawWordCount?: number;
  hotwordHitCount?: number;
  estimatedCost?: number;
  totalDurationMs?: number;
  savedToHistory?: boolean;
  createdAt: string;
  completedAt?: string;
}

export interface HotwordMatchResponse {
  recognizedTerm: string;
  standardTerm: string;
  hitCount: number;
}

export interface TaskDetailResponse {
  id: string;
  sourceTaskId?: string;
  versionIndex?: number;
  title?: string;
  summary?: string;
  sceneType: SceneType;
  status: TaskStatus;
  fileName: string;
  templateId?: string;
  templateName?: string;
  rawText?: string;
  optimizedText?: string;
  markdownContent?: string;
  rawWordCount?: number;
  optimizedWordCount?: number;
  hotwordHitCount?: number;
  estimatedCost?: number;
  recognitionDurationMs?: number;
  optimizationDurationMs?: number;
  totalDurationMs?: number;
  savedToHistory?: boolean;
  errorMessage?: string;
  proofreadRevisionId?: string;
  proofreadRawText?: string;
  proofreadOptimizedText?: string;
  proofreadMarkdownContent?: string;
  createdAt: string;
  completedAt?: string;
  hotwordMatches: HotwordMatchResponse[];
}

export type ExportType = "DOCX" | "MARKDOWN" | "TXT" | "JSON";
export type ExportContentSource = "MODEL" | "PROOFREAD";

export interface CreateExportRequest {
  taskId: string;
  exportType: ExportType;
  contentSource: ExportContentSource;
}

export interface ExportRecordResponse {
  id: string;
  taskId: string;
  taskTitle?: string;
  fileName: string;
  exportType: ExportType;
  contentType: string;
  contentSource: ExportContentSource;
  sizeBytes: number;
  status: string;
  errorMessage?: string;
  createdAt: string;
}

export interface ProofreadTaskRequest {
  rawText: string;
  optimizedText: string;
  markdownContent?: string;
}

export interface ProofreadTaskResponse {
  taskId: string;
  proofreadRevisionId: string;
  rawText: string;
  optimizedText: string;
  markdownContent?: string;
  proofreadAt: string;
}

export interface ModelConnectionTestResponse {
  id: string;
  target: "ASR" | "LLM";
  provider?: string;
  baseUrl?: string;
  modelName?: string;
  status: "SUCCESS" | "FAILED" | "NEEDS_AUDIO";
  message: string;
  durationMs: number;
  uploadId?: string;
  createdAt: string;
}

export interface HistoryPageResponse {
  items: TaskSummaryResponse[];
  total: number;
  page: number;
  size: number;
}

export interface HotwordCategoryResponse {
  id: number;
  code: string;
  name: string;
  icon?: string;
  sortOrder: number;
  enabled: boolean;
  count: number;
}

export interface HotwordSampleResponse {
  id: number;
  sampleBefore: string;
  sampleAfter: string;
}

export interface HotwordResponse {
  id: string;
  recognizedTerm: string;
  standardTerm: string;
  categoryId: number;
  categoryName: string;
  scenes: string[];
  enabled: boolean;
  samples: HotwordSampleResponse[];
  createdAt: string;
}

export interface PagedHotwordResponse {
  items: HotwordResponse[];
  total: number;
  page: number;
  size: number;
}

export interface AppConfigRequest {
  recognitionModel: string;
  languageType: string;
  domainModel: string;
  outputFormat: string;
  stabilityMode: string;
  optimizationModel: string;
  optimizationGoal: string;
  toneStyle: string;
  lengthPreference: string;
  hotwordEnabled: boolean;
  costMode: string;
  asrModelRoute: string;
  llmModelRoute: string;
}

export interface AppConfigResponse extends AppConfigRequest {
  asrProvider: string;
  asrBaseUrl: string;
  asrRuntimeModel: string;
  llmProvider: string;
  llmBaseUrl: string;
  llmRuntimeModel: string;
  modelTimeoutSeconds: number;
  modelMaxRetries: number;
  estimatedSeconds: number;
  estimatedCostPerMinute: number;
  defaultTemplateId?: string;
  defaultTemplateName?: string;
}

export interface ConfigTemplateRequest {
  name: string;
  description?: string;
  defaultTemplate: boolean;
  config: AppConfigRequest;
}

export interface ConfigTemplateResponse {
  id: string;
  name: string;
  description?: string;
  defaultTemplate: boolean;
  config: AppConfigRequest;
  createdAt: string;
}

export interface TrendPointResponse {
  date: string;
  taskCount: number;
  averageCost: number;
}

export interface DistributionPointResponse {
  label: string;
  value: number;
}

export interface SampleCompareResponse {
  id: string;
  title: string;
  rawText: string;
  optimizedText: string;
  improvement: string;
}

export interface UsageStatsResponse {
  averageRecognitionSeconds: number;
  averageOptimizationSeconds: number;
  averageTotalSeconds: number;
  averageWords: number;
  totalCost: number;
  averageCost: number;
  hotwordHitRate: number;
  trend: TrendPointResponse[];
  sceneDistribution: DistributionPointResponse[];
  samples: SampleCompareResponse[];
  conclusion: string;
}
