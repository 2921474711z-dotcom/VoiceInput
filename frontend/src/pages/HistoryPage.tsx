import { Copy, Download, RefreshCcw, Trash2 } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useToast } from "../components/ToastProvider";
import { deleteHistory, getHistory, getHistoryDetail, getMarkdownDownloadUrl, getTemplates, reoptimizeTask } from "../services/api";
import type { ConfigTemplateResponse, SceneType, TaskDetailResponse, TaskStatus, TaskSummaryResponse } from "../types/api";

const sceneOptions: Array<{ label: string; value?: SceneType }> = [
  { label: "全部场景" },
  { label: "会议纪要", value: "MEETING_MINUTES" },
  { label: "工作汇报", value: "WORK_REPORT" },
  { label: "正式表达", value: "FORMAL_EXPRESSION" },
  { label: "Markdown 笔记", value: "MARKDOWN_NOTE" },
  { label: "代码注释", value: "CODE_COMMENT" },
  { label: "聊天回复", value: "CHAT_REPLY" }
];

const statusOptions: Array<{ label: string; value?: TaskStatus }> = [
  { label: "全部状态" },
  { label: "处理成功", value: "SUCCESS" },
  { label: "处理中", value: "PROCESSING" },
  { label: "处理失败", value: "FAILED" }
];

export function HistoryPage() {
  const toast = useToast();
  const [items, setItems] = useState<TaskSummaryResponse[]>([]);
  const [detail, setDetail] = useState<TaskDetailResponse | null>(null);
  const [templates, setTemplates] = useState<ConfigTemplateResponse[]>([]);
  const [detailTemplateId, setDetailTemplateId] = useState("");
  const [keyword, setKeyword] = useState("");
  const [sceneType, setSceneType] = useState<SceneType | undefined>();
  const [status, setStatus] = useState<TaskStatus | undefined>();
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [sort, setSort] = useState("time");
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [size] = useState(10);
  const [tab, setTab] = useState<"optimized" | "raw">("optimized");

  const selectedTemplateName = useMemo(
    () => templates.find((item) => item.id === detailTemplateId)?.name ?? detail?.templateName ?? "未命名模板",
    [detail?.templateName, detailTemplateId, templates]
  );

  async function loadHistory(overrides?: {
    keyword?: string;
    sceneType?: SceneType;
    status?: TaskStatus;
    sort?: string;
    page?: number;
    startDate?: string;
    endDate?: string;
  }) {
    const data = await getHistory({
      keyword,
      sceneType,
      status,
      sort,
      page,
      size,
      startDate,
      endDate,
      ...overrides
    });
    setItems(data.items);
    setTotal(data.total);
    if (data.items.length > 0) {
      const current = detail ? data.items.find((item) => item.id === detail.id) ?? data.items[0] : data.items[0];
      const selected = await getHistoryDetail(current.id);
      setDetail(selected);
      setDetailTemplateId(selected.templateId ?? "");
    } else {
      setDetail(null);
      setDetailTemplateId("");
    }
  }

  useEffect(() => {
    getTemplates()
      .then(setTemplates)
      .catch((error) => {
        console.error(error);
        toast.error("加载模板失败", error instanceof Error ? error.message : "请稍后重试");
      });
  }, []);

  useEffect(() => {
    loadHistory().catch((error) => {
      console.error(error);
      toast.error("加载历史记录失败", error instanceof Error ? error.message : "请稍后重试");
    });
  }, [page]);

  async function reloadFromFirstPage(overrides?: Parameters<typeof loadHistory>[0]) {
    if (page !== 0) {
      setPage(0);
      return;
    }
    await loadHistory({ page: 0, ...overrides });
  }

  async function handleApplyFilter() {
    await reloadFromFirstPage();
  }

  async function handleSelect(id: string) {
    try {
      const data = await getHistoryDetail(id);
      setDetail(data);
      setDetailTemplateId(data.templateId ?? "");
    } catch (error) {
      console.error(error);
      toast.error("读取详情失败", error instanceof Error ? error.message : "请稍后重试");
    }
  }

  async function handleDelete() {
    if (!detail) {
      return;
    }
    const confirmed = window.confirm(`确认删除记录《${detail.title ?? detail.fileName}》吗？`);
    if (!confirmed) {
      return;
    }
    try {
      await deleteHistory(detail.id);
      await loadHistory();
      toast.success("删除成功", "历史记录已移除");
    } catch (error) {
      console.error(error);
      toast.error("删除失败", error instanceof Error ? error.message : "请稍后重试");
    }
  }

  async function handleReoptimize() {
    if (!detail) {
      return;
    }
    try {
      await reoptimizeTask(detail.id, detailTemplateId || detail.templateId);
      await loadHistory();
      toast.success("再次处理已提交", `模板：${selectedTemplateName}`);
    } catch (error) {
      console.error(error);
      toast.error("再次处理失败", error instanceof Error ? error.message : "请稍后重试");
    }
  }

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(detail?.optimizedText ?? "");
      toast.success("复制成功", "优化后文本已写入剪贴板");
    } catch (error) {
      console.error(error);
      toast.error("复制失败", error instanceof Error ? error.message : "浏览器未允许访问剪贴板");
    }
  }

  async function handleDownloadMarkdown() {
    if (!detail) {
      return;
    }
    try {
      const response = await fetch(getMarkdownDownloadUrl(detail.id));
      if (!response.ok) {
        throw new Error("导出请求失败");
      }
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `${detail.title ?? detail.fileName ?? "history"}.md`;
      link.click();
      URL.revokeObjectURL(url);
      toast.success("导出成功", "Markdown 文件已开始下载");
    } catch (error) {
      console.error(error);
      toast.error("导出失败", error instanceof Error ? error.message : "请稍后重试");
    }
  }

  function formatScene(scene?: SceneType) {
    return sceneOptions.find((item) => item.value === scene)?.label ?? scene ?? "-";
  }

  function formatDateTime(value: unknown) {
    if (Array.isArray(value) && value.length >= 6) {
      const [year, month, day, hour, minute, second, nano = 0] = value as number[];
      return new Date(year, month - 1, day, hour, minute, second, Math.floor(nano / 1_000_000)).toLocaleString();
    }
    if (typeof value === "string" || typeof value === "number") {
      const date = new Date(value);
      return Number.isNaN(date.getTime()) ? "-" : date.toLocaleString();
    }
    return "-";
  }

  return (
    <div className="history-layout">
      <aside className="panel filter-panel">
        <h3 className="panel-title">搜索</h3>
        <input className="text-input" value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="搜索标题、内容或关键字..." />
        <h4 className="section-label">场景类型</h4>
        <div className="option-stack">
          {sceneOptions.map((item) => (
            <label key={item.label} className="checkbox-row">
              <input
                type="radio"
                checked={sceneType === item.value}
                onChange={() => {
                  setSceneType(item.value);
                  reloadFromFirstPage({ sceneType: item.value }).catch((error) => {
                    console.error(error);
                    toast.error("场景筛选失败", error instanceof Error ? error.message : "请稍后重试");
                  });
                }}
              />
              <span>{item.label}</span>
            </label>
          ))}
        </div>
        <h4 className="section-label">状态</h4>
        <div className="option-stack">
          {statusOptions.map((item) => (
            <label key={item.label} className="checkbox-row">
              <input
                type="radio"
                checked={status === item.value}
                onChange={() => {
                  setStatus(item.value);
                  reloadFromFirstPage({ status: item.value }).catch((error) => {
                    console.error(error);
                    toast.error("状态筛选失败", error instanceof Error ? error.message : "请稍后重试");
                  });
                }}
              />
              <span>{item.label}</span>
            </label>
          ))}
        </div>
        <h4 className="section-label">时间范围</h4>
        <div className="option-stack">
          <input className="text-input" type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
          <input className="text-input" type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
        </div>
        <div className="filter-actions">
          <button
            className="secondary-button"
            type="button"
            onClick={() => {
              setKeyword("");
              setSceneType(undefined);
              setStatus(undefined);
              setSort("time");
              setStartDate("");
              setEndDate("");
              reloadFromFirstPage({
                keyword: "",
                sceneType: undefined,
                status: undefined,
                sort: "time",
                startDate: "",
                endDate: ""
              }).catch((error) => {
                console.error(error);
                toast.error("重置筛选失败", error instanceof Error ? error.message : "请稍后重试");
              });
            }}
          >
            重置
          </button>
          <button className="primary-button" type="button" onClick={handleApplyFilter}>
            应用筛选
          </button>
        </div>
      </aside>

      <section className="panel history-table-panel">
        <div className="panel-title-row">
          <h3 className="panel-title">历史记录</h3>
          <select
            className="select-input"
            value={sort}
            onChange={(e) => {
              const nextSort = e.target.value;
              setSort(nextSort);
              reloadFromFirstPage({ sort: nextSort }).catch((error) => {
                console.error(error);
                toast.error("排序失败", error instanceof Error ? error.message : "请稍后重试");
              });
            }}
          >
            <option value="time">按时间排序</option>
            <option value="duration">按时长排序</option>
            <option value="words">按字数排序</option>
          </select>
        </div>
        <div className="table-list">
          {items.map((item) => (
            <button key={item.id} type="button" className={`table-row ${detail?.id === item.id ? "selected" : ""}`} onClick={() => handleSelect(item.id)}>
              <div className="table-title">
                {item.title ?? item.fileName}
                <div className="file-meta">{item.templateName ?? "未命名模板"}</div>
              </div>
              <div>{formatScene(item.sceneType)}</div>
              <div>{formatDateTime(item.createdAt)}</div>
              <div>{((item.totalDurationMs ?? 0) / 1000).toFixed(2)}s</div>
              <div>{item.optimizedWordCount ?? 0}</div>
              <div className={`status-dot ${item.status.toLowerCase()}`}>{item.status}</div>
            </button>
          ))}
        </div>
        <div className="pager">
          <button className="secondary-button" type="button" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>
            上一页
          </button>
          <span>第 {page + 1} 页 / 共 {Math.max(1, Math.ceil(total / size))} 页</span>
          <button className="secondary-button" type="button" disabled={(page + 1) * size >= total} onClick={() => setPage((value) => value + 1)}>
            下一页
          </button>
        </div>
      </section>

      <aside className="panel detail-panel">
        {detail ? (
          <>
            <h3 className="panel-title">{detail.title ?? detail.fileName}</h3>
            <div className="detail-meta">
              <span>{formatDateTime(detail.createdAt)}</span>
              <span>{formatScene(detail.sceneType)}</span>
              <span>{((detail.totalDurationMs ?? 0) / 1000).toFixed(2)}s</span>
            </div>
            <label>
              本次再次处理模板
              <select className="select-input" value={detailTemplateId} onChange={(e) => setDetailTemplateId(e.target.value)}>
                <option value="">沿用原模板</option>
                {templates.map((template) => (
                  <option key={template.id} value={template.id}>
                    {template.name}{template.defaultTemplate ? "（默认）" : ""}
                  </option>
                ))}
              </select>
            </label>
            <div className="text-meta">当前记录模板：{detail.templateName ?? "未命名模板"}</div>
            <div className="tabs-title">
              <button className={`tab-button ${tab === "optimized" ? "active" : ""}`} type="button" onClick={() => setTab("optimized")}>
                优化后文本
              </button>
              <button className={`tab-button ${tab === "raw" ? "active" : ""}`} type="button" onClick={() => setTab("raw")}>
                原始识别文本
              </button>
            </div>
            <div className="text-view compact">{tab === "optimized" ? detail.optimizedText : detail.rawText}</div>
            <div className="action-grid">
              <button className="secondary-button" type="button" onClick={handleCopy}>
                <Copy size={16} />
                复制
              </button>
              <button className="secondary-button" type="button" onClick={handleReoptimize}>
                <RefreshCcw size={16} />
                再次处理
              </button>
              <button className="secondary-link-button" type="button" onClick={handleDownloadMarkdown}>
                <Download size={16} />
                转 Markdown
              </button>
              <button className="danger-button" type="button" onClick={handleDelete}>
                <Trash2 size={16} />
                删除
              </button>
            </div>
          </>
        ) : (
          <div className="empty-state">当前没有可查看的历史记录。</div>
        )}
      </aside>
    </div>
  );
}
