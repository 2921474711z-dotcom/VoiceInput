import { Copy, Download, Edit3, Eraser, FileDown, Mic, RefreshCcw, Save, Square } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { useToast } from "../components/ToastProvider";
import { prepareAudioUpload } from "../services/audio";
import { createExport, createTask, createTextTask, getExportDownloadUrl, getMarkdownDownloadUrl, getTaskDetail, getTemplates, reoptimizeTask, saveProofread, saveTaskToHistory, uploadAudio } from "../services/api";
import { downloadFileFromUrl } from "../services/download";
import {
  buildRecordingFile,
  cleanLiveTranscript,
  getSpeechRecognitionConstructor,
  getSupportedRecordingMimeType,
  type SpeechRecognitionLike
} from "../services/liveSpeech";
import type { ConfigTemplateResponse, ExportType, SceneType, TaskDetailResponse, UploadAssetResponse } from "../types/api";

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
  const [inputMode, setInputMode] = useState<"upload" | "live">("upload");
  const [scene, setScene] = useState<SceneType>("MEETING_MINUTES");
  const [upload, setUpload] = useState<UploadAssetResponse | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [task, setTask] = useState<TaskDetailResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [recording, setRecording] = useState(false);
  const [liveDraft, setLiveDraft] = useState("");
  const [liveInterim, setLiveInterim] = useState("");
  const [message, setMessage] = useState("准备就绪，先选择模板再开始处理");
  const [templates, setTemplates] = useState<ConfigTemplateResponse[]>([]);
  const [selectedTemplateId, setSelectedTemplateId] = useState("");
  const [proofreadOpen, setProofreadOpen] = useState(false);
  const [proofreadRaw, setProofreadRaw] = useState("");
  const [proofreadOptimized, setProofreadOptimized] = useState("");
  const [proofreadMarkdown, setProofreadMarkdown] = useState("");
  const [exportType, setExportType] = useState<ExportType>("DOCX");
  const selectedScene = scenes.find((item) => item.value === scene) ?? scenes[0];
  const selectedTemplate = useMemo(
    () => templates.find((item) => item.id === selectedTemplateId) ?? null,
    [selectedTemplateId, templates]
  );
  const taskScene = task ? scenes.find((item) => item.value === task.sceneType) : null;
  const notifiedStatusRef = useRef("");
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const mediaStreamRef = useRef<MediaStream | null>(null);
  const speechRecognitionRef = useRef<SpeechRecognitionLike | null>(null);
  const recordingChunksRef = useRef<BlobPart[]>([]);
  const cancelRecordingRef = useRef(false);
  const liveDraftRef = useRef("");
  const liveInterimRef = useRef("");

  useEffect(() => {
    getTemplates()
      .then((templateData) => {
        setTemplates(templateData);
        const preferredTemplateId =
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
      syncProofreadDraft(null);
      return;
    }
    if (task.status === "SUCCESS") {
      syncProofreadDraft(task);
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

  useEffect(() => {
    return () => {
      stopLiveCapture();
    };
  }, []);

  function stopLiveCapture() {
    speechRecognitionRef.current?.abort();
    speechRecognitionRef.current = null;
    mediaStreamRef.current?.getTracks().forEach((track) => track.stop());
    mediaStreamRef.current = null;
  }

  async function submitRecordedAudio(recordedFile: File, transcript: string) {
    if (!selectedTemplateId) {
      toast.info("还没有选择模板", "先选择本次处理模板");
      return;
    }

    setLoading(true);
    setMessage("实时录音已结束，正在上传并创建任务...");
    try {
      const prepared = await prepareAudioUpload(recordedFile);
      const uploaded = await uploadAudio(prepared.file, prepared.durationSeconds);
      setFile(prepared.file);
      setUpload(uploaded);

      const cleanedTranscript = cleanLiveTranscript(transcript);
      if (!cleanedTranscript) {
        throw new Error("实时草稿为空，请重新录音或改用上传音频");
      }
      const summary = await createTextTask(uploaded.id, scene, selectedTemplateId, cleanedTranscript);
      const detail = await getTaskDetail(summary.id);
      setTask(detail);
      setMessage(`已按“${selectedTemplate?.name ?? "未命名模板"}”处理实时录音...`);
      toast.success("实时语音已提交", prepared.normalized ? "录音已转为标准 WAV 并进入处理链路" : "录音已进入处理链路");
    } catch (error) {
      console.error(error);
      toast.error("实时语音处理失败", error instanceof Error ? error.message : "请稍后重试");
      setMessage("实时语音处理失败，请重试");
    } finally {
      setLoading(false);
    }
  }

  async function handleStartLiveSpeech() {
    if (recording || loading) {
      return;
    }
    if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === "undefined") {
      toast.error("浏览器不支持实时录音", "请使用 Chrome 或 Edge 打开页面");
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mimeType = getSupportedRecordingMimeType();
      const recorder = new MediaRecorder(stream, mimeType ? { mimeType } : undefined);
      cancelRecordingRef.current = false;
      recordingChunksRef.current = [];
      mediaStreamRef.current = stream;
      mediaRecorderRef.current = recorder;

      recorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          recordingChunksRef.current.push(event.data);
        }
      };
      recorder.onstop = () => {
        const chunks = recordingChunksRef.current;
        recordingChunksRef.current = [];
        stopLiveCapture();
        setRecording(false);
        if (cancelRecordingRef.current) {
          cancelRecordingRef.current = false;
          return;
        }
        if (chunks.length === 0) {
          toast.error("没有录到音频", "请检查麦克风权限后重试");
          return;
        }
        void submitRecordedAudio(buildRecordingFile(chunks, recorder.mimeType || mimeType), `${liveDraftRef.current}${liveInterimRef.current}`);
      };

      const SpeechRecognitionCtor = getSpeechRecognitionConstructor();
      if (SpeechRecognitionCtor) {
        const recognition = new SpeechRecognitionCtor();
        recognition.continuous = true;
        recognition.interimResults = true;
        recognition.lang = "zh-CN";
        recognition.onresult = (event) => {
          let finalText = "";
          let interimText = "";
          for (let index = event.resultIndex; index < event.results.length; index += 1) {
            const result = event.results[index];
            if (result.isFinal) {
              finalText += result[0].transcript;
            } else {
              interimText += result[0].transcript;
            }
          }
          if (finalText) {
            setLiveDraft((current) => {
              const next = `${current}${finalText}`;
              liveDraftRef.current = next;
              return next;
            });
          }
          liveInterimRef.current = interimText;
          setLiveInterim(interimText);
        };
        recognition.onerror = (event) => {
          console.error(event);
          toast.info("实时草稿识别中断", "录音会继续保存，结束后仍会走正式处理");
        };
        recognition.onend = () => {
          if (mediaRecorderRef.current?.state === "recording") {
            try {
              recognition.start();
            } catch (error) {
              console.error(error);
            }
          }
        };
        speechRecognitionRef.current = recognition;
        recognition.start();
      } else {
        toast.info("浏览器不支持实时草稿", "录音会正常保存，结束后由后端正式识别");
      }

      setUpload(null);
      setFile(null);
      setTask(null);
      setLiveDraft("");
      setLiveInterim("");
      liveDraftRef.current = "";
      liveInterimRef.current = "";
      recorder.start(1000);
      setRecording(true);
      setMessage("正在实时录音，说完后点击“结束并处理”");
      toast.success("实时语音已开始", "页面会先显示草稿，结束后走正式处理链路");
    } catch (error) {
      console.error(error);
      stopLiveCapture();
      setRecording(false);
      toast.error("无法开始实时语音", error instanceof Error ? error.message : "请检查麦克风权限");
    }
  }

  function handleStopLiveSpeech() {
    if (mediaRecorderRef.current?.state === "recording") {
      setMessage("正在结束录音...");
      speechRecognitionRef.current?.stop();
      mediaRecorderRef.current.stop();
    }
  }

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

  function syncProofreadDraft(detail: TaskDetailResponse | null) {
    setProofreadRaw(detail?.proofreadRawText || detail?.rawText || "");
    setProofreadOptimized(detail?.proofreadOptimizedText || detail?.optimizedText || "");
    setProofreadMarkdown(detail?.proofreadMarkdownContent || detail?.markdownContent || "");
  }

  async function handleSaveProofread() {
    if (!task) {
      return;
    }
    try {
      await saveProofread(task.id, {
        rawText: proofreadRaw,
        optimizedText: proofreadOptimized,
        markdownContent: proofreadMarkdown
      });
      const detail = await getTaskDetail(task.id);
      setTask(detail);
      syncProofreadDraft(detail);
      toast.success("人工校对已保存", "后续导出 DOCX 会优先使用校对版");
    } catch (error) {
      console.error(error);
      toast.error("保存校对失败", error instanceof Error ? error.message : "请稍后重试");
    }
  }

  async function handleCreateExport() {
    if (!task) {
      toast.info("暂无可导出内容", "先完成一次处理");
      return;
    }
    try {
      const record = await createExport({
        taskId: task.id,
        exportType,
        contentSource: task.proofreadRevisionId ? "PROOFREAD" : "MODEL"
      });
      await downloadFileFromUrl(getExportDownloadUrl(record.id), record.fileName);
      toast.success("导出已生成", `${record.fileName} 已写入导出中心`);
    } catch (error) {
      console.error(error);
      toast.error("生成导出失败", error instanceof Error ? error.message : "请稍后重试");
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
    if (mediaRecorderRef.current?.state === "recording") {
      cancelRecordingRef.current = true;
      mediaRecorderRef.current.stop();
    }
    stopLiveCapture();
    setRecording(false);
    setLiveDraft("");
    setLiveInterim("");
    liveDraftRef.current = "";
    liveInterimRef.current = "";
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
            <div className="mode-switch">
              <button className={inputMode === "upload" ? "active" : ""} type="button" onClick={() => setInputMode("upload")} disabled={recording}>
                上传音频
              </button>
              <button className={inputMode === "live" ? "active" : ""} type="button" onClick={() => setInputMode("live")} disabled={loading}>
                实时说话
              </button>
            </div>
            <div className="panel-title-row">
              <h3 className="panel-title">上传音频</h3>
              <span className="status-badge">{message}</span>
            </div>

            <label className={`upload-dropzone ${inputMode === "upload" ? "" : "hidden-mode"}`}>
              <input type="file" accept="audio/*" hidden onChange={handleChooseFile} />
              <div className="upload-copy">点击上传或拖拽音频文件到此处</div>
              <div className="upload-hint">支持 mp3、wav、m4a、aac、flac、ogg。m4a/aac 会优先自动转成标准 WAV。</div>
            </label>

            {inputMode === "live" ? (
              <div className={`live-speech-card ${recording ? "recording" : ""}`}>
                <div className="live-meter" aria-hidden="true">
                  <span />
                  <span />
                  <span />
                </div>
                <div>
                  <div className="upload-copy">{recording ? "正在听你说话" : "点击开始，直接对着麦克风说话"}</div>
                  <div className="upload-hint">页面先显示实时草稿；结束后会自动上传录音，并复用现有识别、优化、热词、历史和导出流程。</div>
                </div>
                <div className="live-draft">
                  {liveDraft || liveInterim ? (
                    <>
                      <span>{liveDraft}</span>
                      <em>{liveInterim}</em>
                    </>
                  ) : (
                    <span className="muted-copy">实时草稿会显示在这里。浏览器不支持草稿时，仍会录音并在结束后正式识别。</span>
                  )}
                </div>
              </div>
            ) : null}

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

          <div className={`upload-actions ${inputMode === "live" ? "live-mode" : ""}`}>
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
            {inputMode === "live" && recording ? (
              <button className="primary-button live-stop-button" type="button" onClick={handleStopLiveSpeech}>
                <Square size={16} />
                结束并处理
              </button>
            ) : inputMode === "live" ? (
              <button className="primary-button live-record-button" type="button" onClick={handleStartLiveSpeech} disabled={loading || !selectedTemplateId}>
                <Mic size={16} />
                开始说话
              </button>
            ) : null}
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
          <div className="text-meta">当前模板：{selectedTemplate?.name ?? "未选择"}</div>
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
            <div className="proofread-panel">
              <button className="secondary-button" type="button" onClick={() => setProofreadOpen((value) => !value)} disabled={!task}>
                <Edit3 size={16} />
                {proofreadOpen ? "收起人工校对" : "人工校对"}
              </button>
              <select className="select-input" value={exportType} onChange={(event) => setExportType(event.target.value as ExportType)} disabled={!task}>
                <option value="DOCX">DOCX</option>
                <option value="MARKDOWN">Markdown</option>
                <option value="TXT">TXT</option>
                <option value="JSON">JSON</option>
              </select>
              <button className="primary-link-button" type="button" onClick={handleCreateExport} disabled={!task}>
                <FileDown size={16} />
                生成导出
              </button>
              {proofreadOpen && task ? (
                <div className="proofread-editor">
                  <label>
                    校对原始文本
                    <textarea className="text-input textarea-input" value={proofreadRaw} onChange={(event) => setProofreadRaw(event.target.value)} />
                  </label>
                  <label>
                    校对优化文本
                    <textarea className="text-input textarea-input" value={proofreadOptimized} onChange={(event) => setProofreadOptimized(event.target.value)} />
                  </label>
                  <label>
                    校对 Markdown
                    <textarea className="text-input textarea-input" value={proofreadMarkdown} onChange={(event) => setProofreadMarkdown(event.target.value)} />
                  </label>
                  <button className="primary-button" type="button" onClick={handleSaveProofread}>
                    <Save size={16} />
                    保存校对版本
                  </button>
                </div>
              ) : null}
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
