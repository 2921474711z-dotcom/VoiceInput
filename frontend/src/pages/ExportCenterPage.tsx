import { Download, FileArchive, FileJson, FileText, RefreshCcw } from "lucide-react";
import { useEffect, useState } from "react";
import { useToast } from "../components/ToastProvider";
import { getExportDownloadUrl, getExports } from "../services/api";
import { downloadFileFromUrl } from "../services/download";
import type { ExportRecordResponse, ExportType } from "../types/api";

const exportTypes: Array<{ label: string; value?: ExportType }> = [
  { label: "全部格式" },
  { label: "DOCX", value: "DOCX" },
  { label: "Markdown", value: "MARKDOWN" },
  { label: "TXT", value: "TXT" },
  { label: "JSON", value: "JSON" }
];

const iconMap: Record<ExportType, typeof FileText> = {
  DOCX: FileArchive,
  MARKDOWN: FileText,
  TXT: FileText,
  JSON: FileJson
};

export function ExportCenterPage() {
  const toast = useToast();
  const [items, setItems] = useState<ExportRecordResponse[]>([]);
  const [exportType, setExportType] = useState<ExportType | undefined>();
  const [loading, setLoading] = useState(false);

  async function load() {
    setLoading(true);
    try {
      setItems(await getExports({ exportType }));
    } catch (error) {
      console.error(error);
      toast.error("加载导出中心失败", error instanceof Error ? error.message : "请稍后重试");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, [exportType]);

  async function download(record: ExportRecordResponse) {
    try {
      await downloadFileFromUrl(getExportDownloadUrl(record.id), record.fileName);
    } catch (error) {
      console.error(error);
      toast.error("下载失败", error instanceof Error ? error.message : "请稍后重试");
    }
  }

  function formatBytes(value?: number) {
    if (!value) return "0 B";
    if (value < 1024) return `${value} B`;
    if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
    return `${(value / 1024 / 1024).toFixed(2)} MB`;
  }

  function formatDate(value?: string) {
    if (!value) return "-";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
  }

  return (
    <div className="export-center-layout">
      <section className="panel export-center-main">
        <div className="panel-title-row">
          <div>
            <h3 className="panel-title">导出中心</h3>
            <p className="summary-note">所有文件都由后端生成并写入对象存储，列表来自数据库导出记录。</p>
          </div>
          <div className="action-row">
            <select className="select-input" value={exportType ?? ""} onChange={(event) => setExportType((event.target.value || undefined) as ExportType | undefined)}>
              {exportTypes.map((item) => (
                <option key={item.label} value={item.value ?? ""}>{item.label}</option>
              ))}
            </select>
            <button className="secondary-button" type="button" onClick={load} disabled={loading}>
              <RefreshCcw size={16} />
              刷新
            </button>
          </div>
        </div>

        <div className="export-record-list">
          {items.length === 0 ? (
            <div className="empty-state">暂无导出文件。请先在历史详情或工作台结果中生成 DOCX、Markdown、TXT 或 JSON。</div>
          ) : items.map((item) => {
            const Icon = iconMap[item.exportType] ?? FileText;
            return (
              <div key={item.id} className="export-record-card">
                <div className="export-record-icon"><Icon size={22} /></div>
                <div className="export-record-body">
                  <strong>{item.fileName}</strong>
                  <div className="file-meta">
                    {item.taskTitle || item.taskId} · {item.exportType} · {item.contentSource === "PROOFREAD" ? "人工校对版" : "模型输出版"}
                  </div>
                  <div className="file-meta">
                    {formatBytes(item.sizeBytes)} · {formatDate(item.createdAt)} · {item.status}
                  </div>
                  {item.errorMessage ? <div className="error-note">{item.errorMessage}</div> : null}
                </div>
                <button className="primary-link-button" type="button" onClick={() => download(item)} disabled={item.status !== "SUCCESS"}>
                  <Download size={16} />
                  下载
                </button>
              </div>
            );
          })}
        </div>
      </section>
    </div>
  );
}
