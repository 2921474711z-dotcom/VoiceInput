import { Download, Plus, Trash2, Upload } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useToast } from "../components/ToastProvider";
import { createHotword, deleteHotword, getHotwordCategories, getHotwordExportUrl, getHotwords, importHotwords, updateHotword } from "../services/api";
import type { HotwordCategoryResponse, HotwordResponse } from "../types/api";

const sceneChoices = [
  { value: "MEETING_MINUTES", label: "会议纪要" },
  { value: "WORK_REPORT", label: "工作汇报" },
  { value: "FORMAL_EXPRESSION", label: "正式表达" },
  { value: "MARKDOWN_NOTE", label: "Markdown 笔记" },
  { value: "CODE_COMMENT", label: "代码注释" },
  { value: "CHAT_REPLY", label: "聊天回复" }
];

export function HotwordsPage() {
  const toast = useToast();
  const [categories, setCategories] = useState<HotwordCategoryResponse[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<number | undefined>();
  const [items, setItems] = useState<HotwordResponse[]>([]);
  const [keyword, setKeyword] = useState("");
  const [current, setCurrent] = useState<HotwordResponse | null>(null);
  const [editorMode, setEditorMode] = useState<"create" | "edit">("create");
  const [page, setPage] = useState(0);

  const initialForm = useMemo(
    () => ({
      recognizedTerm: "",
      standardTerm: "",
      categoryId: 0,
      scenes: ["MEETING_MINUTES"],
      enabled: true,
      samples: [{ sampleBefore: "" }]
    }),
    []
  );

  const [form, setForm] = useState(initialForm);

  async function loadCategories() {
    const data = await getHotwordCategories();
    setCategories(data);
    if (!selectedCategory && data.length > 1) {
      setSelectedCategory(data[1].id);
    }
  }

  async function loadHotwords(overrides?: { categoryId?: number; keyword?: string; page?: number }) {
    const data = await getHotwords({
      categoryId: selectedCategory,
      keyword,
      page,
      size: 20,
      ...overrides
    });
    setItems(data.items);
    if (data.items.length === 0) {
      setCurrent(null);
      return;
    }
    if (!current && editorMode !== "create") {
      selectHotword(data.items[0]);
      return;
    }
    const matched = current ? data.items.find((item) => item.id === current.id) : null;
    if (!matched && editorMode !== "create") {
      selectHotword(data.items[0]);
    }
  }

  useEffect(() => {
    loadCategories().catch((error) => {
      console.error(error);
      toast.error("加载热词分类失败", error instanceof Error ? error.message : "请稍后重试");
    });
  }, []);

  useEffect(() => {
    loadHotwords().catch((error) => {
      console.error(error);
      toast.error("加载热词列表失败", error instanceof Error ? error.message : "请稍后重试");
    });
  }, [selectedCategory, page, keyword]);

  function selectHotword(item: HotwordResponse) {
    setEditorMode("edit");
    setCurrent(item);
    setForm({
      recognizedTerm: item.recognizedTerm,
      standardTerm: item.standardTerm,
      categoryId: item.categoryId,
      scenes: item.scenes,
      enabled: item.enabled,
      samples: item.samples.length ? item.samples.map((sample) => ({ sampleBefore: sample.sampleBefore })) : [{ sampleBefore: "" }]
    });
  }

  async function handleSave() {
    if (!form.recognizedTerm.trim() || !form.standardTerm.trim() || !form.categoryId || form.scenes.length === 0) {
      toast.info("信息未填写完整", "请补全识别词、标准词、分类和适用场景");
      return;
    }
    const payload = {
      ...form,
      samples: form.samples.filter((sample) => sample.sampleBefore.trim())
    };
    try {
      let saved: HotwordResponse;
      if (editorMode === "edit" && current) {
        saved = await updateHotword(current.id, payload);
      } else {
        saved = await createHotword(payload);
      }
      setPage(0);
      setSelectedCategory(saved.categoryId);
      await loadCategories();
      await loadHotwords({ categoryId: saved.categoryId, page: 0 });
      selectHotword(saved);
      toast.success(editorMode === "edit" ? "热词已更新" : "热词已新增", `${saved.recognizedTerm} -> ${saved.standardTerm}`);
    } catch (error) {
      toast.error(editorMode === "edit" ? "更新热词失败" : "新增热词失败", error instanceof Error ? error.message : "请稍后重试");
    }
  }

  async function handleDelete(id: string) {
    if (!window.confirm("确认删除该热词吗？")) return;
    try {
      await deleteHotword(id);
      setCurrent(null);
      setEditorMode("create");
      setForm(initialForm);
      await loadCategories();
      await loadHotwords({ page: 0 });
      toast.success("删除成功", "热词已移除");
    } catch (error) {
      toast.error("删除失败", error instanceof Error ? error.message : "请稍后重试");
    }
  }

  async function handleImport(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    try {
      await importHotwords(file);
      setPage(0);
      await loadCategories();
      await loadHotwords({ page: 0 });
      toast.success("导入成功", `${file.name} 已导入热词库`);
    } catch (error) {
      toast.error("导入失败", error instanceof Error ? error.message : "请检查 JSON 格式");
    } finally {
      event.target.value = "";
    }
  }

  function categoryName(categoryId: number) {
    return categories.find((item) => item.id === categoryId)?.name ?? `分类 ${categoryId}`;
  }

  function sceneLabel(scene: string) {
    return sceneChoices.find((item) => item.value === scene)?.label ?? scene;
  }

  async function handleExportHotwords() {
    try {
      const response = await fetch(getHotwordExportUrl());
      if (!response.ok) {
        throw new Error("导出请求失败");
      }
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "hotwords.json";
      link.click();
      URL.revokeObjectURL(url);
      toast.success("导出成功", "热词 JSON 已开始下载");
    } catch (error) {
      toast.error("导出失败", error instanceof Error ? error.message : "请稍后重试");
    }
  }

  return (
    <div className="hotword-layout">
      <aside className="panel category-panel">
        <div className="panel-title-row">
          <h3 className="panel-title">热词分类</h3>
          <button className="icon-button" type="button" onClick={() => { setCurrent(null); setEditorMode("create"); setForm(initialForm); }}>
            <Plus size={16} />
          </button>
        </div>
        <div className="category-list">
          {categories.filter((item) => item.code !== "ALL").map((item) => (
            <button key={item.id} type="button" className={`category-item ${selectedCategory === item.id ? "active" : ""}`} onClick={() => setSelectedCategory(item.id)}>
              <span>{item.name}</span>
              <strong>{item.count}</strong>
            </button>
          ))}
        </div>
      </aside>

      <section className="panel hotword-table-panel">
        <div className="panel-title-row">
          <input className="text-input" value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="搜索热词" />
          <div className="toolbar-row">
            <label className="secondary-button upload-inline">
              <Upload size={16} />
              导入热词
              <input hidden type="file" accept="application/json" onChange={handleImport} />
            </label>
            <button className="secondary-link-button" type="button" onClick={handleExportHotwords}>
              <Download size={16} />
              导出热词
            </button>
            <button className="primary-button" type="button" onClick={() => { setCurrent(null); setEditorMode("create"); setForm(initialForm); toast.info("已切换到新增模式", "保存时将创建新的热词，不会覆盖当前记录"); }}>
              <Plus size={16} />
              添加热词
            </button>
          </div>
        </div>

        <div className="table-list hotword-table">
          {items.map((item) => (
            <div key={item.id} className={`table-row hotword-record ${current?.id === item.id ? "selected" : ""}`}>
              <button type="button" className="table-row-main hotword-record-main" onClick={() => selectHotword(item)}>
                <div className="hotword-primary">
                  <div className="table-title">{item.recognizedTerm}</div>
                  <div className="hotword-arrow">修正为 {item.standardTerm}</div>
                </div>
                <div className="hotword-meta">
                  <span className="hotword-meta-label">分类</span>
                  <strong>{item.categoryName || categoryName(item.categoryId)}</strong>
                </div>
                <div className="hotword-scene-tags">
                  {item.scenes.map((scene) => (
                    <span key={`${item.id}-${scene}`} className="scene-tag">
                      {sceneLabel(scene)}
                    </span>
                  ))}
                </div>
                <div className={`pill ${item.enabled ? "success" : "warning"}`}>{item.enabled ? "已启用" : "已停用"}</div>
              </button>
              <button className="danger-button compact" type="button" onClick={() => handleDelete(item.id)}>
                删除
              </button>
            </div>
          ))}
        </div>
        <div className="pager">
          <button className="secondary-button" type="button" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>
            上一页
          </button>
          <span>第 {page + 1} 页</span>
          <button className="secondary-button" type="button" disabled={items.length < 20} onClick={() => setPage((value) => value + 1)}>
            下一页
          </button>
        </div>
      </section>

      <aside className="panel editor-panel">
        <h3 className="panel-title">{editorMode === "edit" ? "编辑热词" : "新增热词"}</h3>
        <div className="form-stack">
          <label>
            识别词
            <input className="text-input" value={form.recognizedTerm} onChange={(e) => setForm({ ...form, recognizedTerm: e.target.value })} />
          </label>
          <label>
            标准词
            <input className="text-input" value={form.standardTerm} onChange={(e) => setForm({ ...form, standardTerm: e.target.value })} />
          </label>
          <label>
            分类
            <select className="select-input" value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: Number(e.target.value) })}>
              <option value={0}>请选择分类</option>
              {categories.filter((item) => item.code !== "ALL").map((item) => (
                <option key={item.id} value={item.id}>
                  {item.name}
                </option>
              ))}
            </select>
          </label>
          <div>
            <div className="section-label">适用场景</div>
            <div className="checkbox-grid">
              {sceneChoices.map((scene) => (
                <label key={scene.value} className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={form.scenes.includes(scene.value)}
                    onChange={() =>
                      setForm((state) => ({
                        ...state,
                        scenes: state.scenes.includes(scene.value)
                          ? state.scenes.filter((item) => item !== scene.value)
                          : [...state.scenes, scene.value]
                      }))
                    }
                  />
                  <span>{scene.label}</span>
                </label>
              ))}
            </div>
          </div>
          <div className="toggle-row">
            <span>状态</span>
            <label className="checkbox-row">
              <input type="checkbox" checked={form.enabled} onChange={(e) => setForm({ ...form, enabled: e.target.checked })} />
              <span>{form.enabled ? "启用" : "停用"}</span>
            </label>
          </div>
          <div>
            <div className="section-label">样例效果</div>
            <div className="sample-list">
              {form.samples.map((sample, index) => (
                <div key={index} className="sample-item">
                  <textarea
                    className="textarea-input"
                    value={sample.sampleBefore}
                    onChange={(e) =>
                      setForm((state) => ({
                        ...state,
                        samples: state.samples.map((item, sampleIndex) =>
                          sampleIndex === index ? { ...item, sampleBefore: e.target.value } : item
                        )
                      }))
                    }
                  />
                  <button
                    className="danger-button compact"
                    type="button"
                    onClick={() =>
                      setForm((state) => ({
                        ...state,
                        samples: state.samples.length === 1
                          ? [{ sampleBefore: "" }]
                          : state.samples.filter((_, sampleIndex) => sampleIndex !== index)
                      }))
                    }
                  >
                    <Trash2 size={14} />
                    删除样例
                  </button>
                  <div className="sample-after">
                    {(sample.sampleBefore || "").split(form.recognizedTerm).join(form.standardTerm)}
                  </div>
                </div>
              ))}
              <button
                className="secondary-button"
                type="button"
                onClick={() => setForm((state) => ({ ...state, samples: [...state.samples, { sampleBefore: "" }] }))}
              >
                添加样例
              </button>
            </div>
          </div>
          <div className="action-row">
            <button className="secondary-button" type="button" onClick={() => { setCurrent(null); setEditorMode("create"); setForm(initialForm); }}>
              取消
            </button>
            <button className="primary-button" type="button" onClick={handleSave}>
              保存修改
            </button>
          </div>
        </div>
      </aside>
    </div>
  );
}
