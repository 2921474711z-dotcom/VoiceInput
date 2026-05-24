import { Copy, Download, Eraser, RefreshCcw, Save } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { useToast } from "../components/ToastProvider";
import { prepareAudioUpload } from "../services/audio";
import { createTask, getConfig, getMarkdownDownloadUrl, getTaskDetail, getTemplates, reoptimizeTask, saveTaskToHistory, uploadAudio } from "../services/api";
import type { AppConfigResponse, ConfigTemplateResponse, SceneType, TaskDetailResponse, UploadAssetResponse } from "../types/api";

const scenes: Array<{ value: SceneType; label: string; description: string }> = [
  { value: "MEETING_MINUTES", label: "会议纪要", description: "输出议题、结论和待办事项" },
  { value: "WORK_REPORT", label: "工作汇报", description: "输出分点总结、风险和下一步" },
  { value: "FORMAL_EXPRESSION", label: "正式表达", description: "减少口语化，统一表达风格" },
  { value: "MARKDOWN_NOTE", label: "Markdown 笔记", description: "整理为可直接归档的 Markdown 内容" },
  { value: "CODE_COMMENT", label: "代码注释", description: "保留技术术语和英文命名" },
  { value: "CHAT_REPLY", label: "聊天回复", description: "保持简洁自然，适合直接发送" }
];

function buildTemplatePreview(template?: ConfigTemplateResponse | null) {
  if (!template) {
    return [];
  }
  return [
    `ASR：${template.config.asrModelRoute}`,
    `LLM：${template.config.llmModelRoute}`,
    `语言：${template.config.languageType}`,
    `领域：${template.config.domainModel}`,
    `输出：${template.config.outputFormat}`,
    `目标：${template.config.optimizationGoal}`
  ];
}

export function WorkbenchPage() {
  const toast = useToast();
  const [scene, setScene] = useState<SceneType>("MEETING_MINUTES");
  const [upload, setUpload] = useState<UploadAssetResponse | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [task, setTask] = useState<TaskDetailResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("准备就绪，先选择模板再开始处理");
  const [templates, setTemplates] = useState<ConfigTemplateResponse[]>([]);
  const [activeConfig, setActiveConfig] = useState<AppConfigResponse | null>(null);
  const [selectedTemplateId, setSelectedTemplateId] = useState("");
  const selectedScene = scenes.find((item) => item.value === scene) ?? scenes[0];
  const selectedTemplate = useMemo(
    () => templates.find((item) => item.id === selectedTemplateId) ?? null,
    [selectedTemplateId, templates]
  );
  const taskScene = task ? scenes.find((item) => item.value === task.sceneType) : null;
  const notifiedStatusRef = useRef("");

  useEffect(() => {
    Promise.all([getTemplates(), getConfig()])
      .then(([templateData, configData]) => {
        setTemplates(templateData);
        setActiveConfig(configData);
        const preferredTemplateId =
          configData.defaultTemplateId ||
          templateData.find((item) => item.defaultTemplate)?.id ||
          templateData[0]?.id ||
          "";
        setSelectedTemplateId(preferredTemplateId);
      })
      .catch((error) => {
        console.error(error);
        toast.error("加载模板失败", error instanceof Error ? error.message : "请稍后重试");
      });
  }, []);

  useEffect(() => {
    if (!task || !["PENDING", "PROCESSING"].includes(task.status)) {
      return;
    }
    const timer = window.setInterval(async () => {
      try {
        const detail = await getTaskDetail(task.id);
        setTask(detail);
        setMessage(
          detail.status === "SUCCESS"
            ? "处理完成"
            : detail.status === "FAILED"
              ? detail.errorMessage ?? "处理失败"
              : "处理中..."
        );
      } catch (error) {
        console.error(error);
      }
    }, 2000);
    return () => window.clearInterval(timer);
  }, [task]);

  useEffect(() => {
    if (!task) {
      notifiedStatusRef.current = "";
      return;
    }
    const marker = `${task.id}:${task.status}`;
    if (notifiedStatusRef.current === marker) {
      return;
    }
    if (task.status === "SUCCESS") {
      toast.success("处理完成", `结果来自模板：${task.templateName ?? "未命名模板"}`);
      notifiedStatusRef.current = marker;
    }
    if (task.status === "FAILED") {
      toast.error("处理失败", task.errorMessage ?? "请检查模型配置或音频内容后重试");
      notifiedStatusRef.current = marker;
    }
  }, [task, toast]);

  async function handleChooseFile(event: React.ChangeEvent<HTMLInputElement>) {
    const selected = event.target.files?.[0];
    if (!selected) {
      return;
    }

    setLoading(true);
    setMessage("正在准备音频文件...");
    try {
      const prepared = await prepareAudioUpload(selected);
      const uploaded = await uploadAudio(prepared.file, prepared.durationSeconds);
      setFile(prepared.file);
      setUpload(uploaded);
      setTask(null);
      setMessage(prepared.normalized ? "音频已标准化并上传，可以开始处理" : "音频已上传，可以开始处理");
      toast.success("音频上传成功", prepared.normalized ? "已自动转成标准 WAV" : "可以直接开始处理");
    } catch (error) {
      console.error(error);
      toast.error("上传失败", error instanceof Error ? error.message : "请重新选择音频");
      setMessage("上传失败，请重新选择音频");
    } finally {
      setLoading(false);
      event.target.value = "";
    }
  }

  async function handleProcess() {
    if (!upload) {
      toast.info("还没有上传音频", "先上传音频，再开始处理");
      return;
    }
    if (!selectedTemplateId) {
      toast.info("还没有选择模板", "先选择本次处理模板");
      return;
    }
    setLoading(true);
    try {
      const summary = await createTask(upload.id, scene, selectedTemplateId);
      const detail = await getTaskDetail(summary.id);
      setTask(detail);
      setMessage(`已按“${selectedTemplate?.name ?? "未命名模板"}”创建任务，正在处理...`);
      toast.info("任务已创建", `场景：${selectedScene.label}，模板：${selectedTemplate?.name ?? "未命名模板"}`);
    } catch (error) {
      console.error(error);
      toast.error("开始处理失败", error instanceof Error ? error.message : "请稍后重试");
    } finally {
      setLoading(false);
    }
  }

  async function handleSaveHistory() {
    if (!task) {
      return;
    }
    try {
      const detail = await saveTaskToHistory(task.id);
      setTask(detail);
      setMessage("结果已保存到历史记录");
      toast.success("保存成功", "当前结果已进入历史记录");
    } catch (error) {
      console.error(error);
      toast.error("保存失败", error instanceof Error ? error.message : "请稍后重试");
    }
  }

  async function handleReoptimize() {
    if (!task) {
      return;
    }
    setLoading(true);
    try {
      const summary = await reoptimizeTask(task.id, selectedTemplateId || task.templateId);
      const detail = await getTaskDetail(summary.id);
      setTask(detail);
      setMessage("已提交再次处理任务");
      toast.success("再次处理已提交", `模板：${selectedTemplate?.name ?? task.templateName ?? "原模板"}`);
    } catch (error) {
      console.error(error);
      toast.error("再次处理失败", error instanceof Error ? error.message : "请稍后重试");
    } finally {
      setLoading(false);
    }
  }

  async function handleCopy(text?: string, label = "内容") {
    try {
      await navigator.clipboard.writeText(text ?? "");
      setMessage("内容已写入剪贴板");
      toast.success("复制成功", `${label}已复制`);
    } catch (error) {
      console.error(error);
      toast.error("复制失败", error instanceof Error ? error.message : "浏览器未允许访问剪贴板");
    }
  }

  function handleSceneSelect(nextScene: SceneType) {
    setScene(nextScene);
    const next = scenes.find((item) => item.value === nextScene);
    if (!next) {
      return;
    }
    setMessage(task ? `场景已切换到“${next.label}”，重新开始处理后才会按新场景生效` : `场景已切换到“${next.label}”`);
  }

  function handleClear() {
    setUpload(null);
    setFile(null);
    setTask(null);
    setMessage("当前工作台已清空");
    toast.info("已清空", "上传记录和处理结果已重置");
  }

  async function handleDownloadMarkdown() {
    if (!task) {
      toast.info("暂无可导出的内容", "先完成一次处理");
      return;
    }
    try {
      const response = await fetch(getMarkdownDownloadUrl(task.id));
      if (!response.ok) {
        throw new Error("导出请求失败");
      }
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `${task.title ?? task.fileName ?? "result"}.md`;
      link.click();
      URL.revokeObjectURL(url);
      toast.success("导出成功", "Markdown 文件已开始下载");
    } catch (error) {
      console.error(error);
      toast.error("导出失败", error instanceof Error ? error.message : "请稍后重试");
    }
  }

  return (
    <div className="workbench-grid">
      <aside className="panel scene-panel">
        <h3 className="panel-title">输入场景</h3>
        <div className="scene-list">
          {scenes.map((item) => (
            <button
              key={item.value}
              className={`scene-item ${scene === item.value ? "active" : ""}`}
              type="button"
              onClick={() => handleSceneSelect(item.value)}
            >
              <span>{item.label}</span>
              <small>{item.description}</small>
            </button>
          ))}
        </div>
      </aside>

      <section className="workbench-main">
        <div className="panel upload-panel">
          <div className="upload-box">
            <div className="panel-title-row">
              <h3 className="panel-title">上传音频</h3>
              <span className="status-badge">{message}</span>
            </div>

            <label className="upload-dropzone">
              <input type="file" accept="audio/*" hidden onChange={handleChooseFile} />
              <div className="upload-copy">点击上传或拖拽音频文件到此处</div>
              <div className="upload-hint">支持 mp3、wav、m4a、aac、flac、ogg。m4a/aac 会优先自动转成标准 WAV。</div>
            </label>

            {upload ? (
              <div className="file-row">
                <div>
                  <div className="file-name">{upload.fileName}</div>
                  <div className="file-meta">
                    {(upload.sizeBytes / 1024 / 1024).toFixed(2)} MB · {upload.durationSeconds ?? 0}s
                    {file?.type === "audio/wav" ? " · 已标准化" : ""}
                  </div>
                </div>
                <button type="button" className="icon-button" onClick={handleClear}>
                  ×
                </button>
              </div>
            ) : null}
          </div>

          <div className="upload-actions">
            <label>
              本次模板
              <select
                className="select-input"
                value={selectedTemplateId}
                onChange={(event) => {
                  setSelectedTemplateId(event.target.value);
                  const nextTemplate = templates.find((item) => item.id === event.target.value);
                  if (nextTemplate) {
                    setMessage(`本次处理将使用模板“${nextTemplate.name}”`);
                  }
                }}
              >
                <option value="">请选择模板</option>
                {templates.map((template) => (
                  <option key={template.id} value={template.id}>
                    {template.name}{template.defaultTemplate ? "（默认）" : ""}
                  </option>
                ))}
              </select>
            </label>
            <button className="primary-button" type="button" onClick={handleProcess} disabled={!upload || loading || !selectedTemplateId}>
              开始处理
            </button>
            <button className="secondary-button" type="button" onClick={handleClear}>
              <Eraser size={16} />
              清空
            </button>
          </div>
        </div>

        <div className="panel">
          <div className="panel-title-row">
            <h3 className="panel-title">本次设置</h3>
          </div>
          <div className="text-meta">当前场景：{selectedScene.label}</div>
          <div className="text-meta">
            当前模板：{selectedTemplate?.name ?? "未选择"}
            {activeConfig?.defaultTemplateName ? `，系统默认模板：${activeConfig.defaultTemplateName}` : ""}
          </div>
          <div className="template-inline-preview">
            {buildTemplatePreview(selectedTemplate).map((item) => (
              <span key={item} className="scene-tag">{item}</span>
            ))}
          </div>
        </div>

        <div className="result-grid">
          <div className="panel text-panel">
            <div className="panel-title-row">
              <h3 className="panel-title">原始识别文本</h3>
              <button className="inline-action" type="button" onClick={() => handleCopy(task?.rawText, "原始文本")}>
                <Copy size={14} />
                复制原文
              </button>
            </div>
            <div className="text-view">{task?.rawText || "处理完成后，这里显示原始识别文本。"}</div>
            <div className="text-meta">字数：{task?.rawWordCount ?? 0}</div>
          </div>

          <div className="panel text-panel">
            <div className="panel-title-row">
              <h3 className="panel-title">优化后文本</h3>
              <button className="inline-action" type="button" onClick={() => handleCopy(task?.optimizedText, "优化后文本")}>
                <Copy size={14} />
                复制优化文
              </button>
            </div>
            <div className="text-view">{task?.optimizedText || "处理完成后，这里显示模板整理后的结果。"}</div>
            <div className="text-meta">
              {task ? `结果模板：${task.templateName ?? "未命名模板"}` : "模板会影响模型、风格、输出结构和热词修正。"}
            </div>
            <div className="action-row">
              <button className="secondary-button" type="button" onClick={handleReoptimize} disabled={!task || loading}>
                <RefreshCcw size={16} />
                再次处理
              </button>
              <button className="secondary-button" type="button" onClick={handleSaveHistory} disabled={!task}>
                <Save size={16} />
                保存到历史
              </button>
              <button className="primary-link-button" type="button" onClick={handleDownloadMarkdown}>
                <Download size={16} />
                导出 Markdown
              </button>
            </div>
          </div>
        </div>

        <div className="stats-strip">
          <div className="stat-card">
            <span>处理耗时</span>
            <strong>{((task?.totalDurationMs ?? 0) / 1000).toFixed(2)}s</strong>
          </div>
          <div className="stat-card">
            <span>字数</span>
            <strong>{task?.optimizedWordCount ?? 0} 字</strong>
          </div>
          <div className="stat-card">
            <span>估算成本</span>
            <strong>¥ {(task?.estimatedCost ?? 0).toFixed(4)}</strong>
          </div>
          <div className="stat-card">
            <span>热词命中</span>
            <strong>{task?.hotwordHitCount ?? 0} 次</strong>
          </div>
        </div>
      </section>
    </div>
  );
}
