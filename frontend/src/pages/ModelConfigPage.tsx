import { useEffect, useState } from "react";
import { useToast } from "../components/ToastProvider";
import {
  applyTemplate,
  createTemplate,
  deleteTemplate,
  getConfig,
  getTemplates,
  getUsageStats,
  resetConfig,
  setDefaultTemplate,
  updateConfig
} from "../services/api";
import type { AppConfigRequest, AppConfigResponse, ConfigTemplateResponse, SceneType, UsageStatsResponse } from "../types/api";

const templateSeed = { name: "", description: "", defaultTemplate: false };

const recognitionModelOptions = ["通用语音识别 v3", "快速语音识别 v3", "高精度语音识别 v3"];
const languageOptions = ["中文（普通话）", "中英混合", "英文"];
const domainOptions = ["通用领域", "会议场景", "技术表达", "客服沟通"];
const outputFormatOptions = ["纯文本", "结构化文本", "Markdown"];
const stabilityOptions = ["更快", "平衡", "更准确"];
const optimizationModelOptions = ["文本优化增强 v2", "结构化整理 v2", "正式表达增强 v2"];
const optimizationGoalOptions = ["会议纪要优化", "工作汇报优化", "正式表达优化", "Markdown 笔记优化", "代码注释优化", "聊天回复优化"];
const toneOptions = ["专业客观", "简洁直接", "正式严谨", "自然友好"];
const lengthOptions = ["精简", "适中", "详细"];
const costModeOptions = ["成本优先", "质量优先"];
const asrRouteOptions = ["mimo-v2.5", "mimo-v2-omni"];
const llmRouteOptions = ["mimo-v2.5-pro", "mimo-v2.5"];

const sceneLabelMap: Record<SceneType, string> = {
  MEETING_MINUTES: "会议纪要",
  WORK_REPORT: "工作汇报",
  FORMAL_EXPRESSION: "正式表达",
  MARKDOWN_NOTE: "Markdown 笔记",
  CODE_COMMENT: "代码注释",
  CHAT_REPLY: "聊天回复"
};

function formatSceneType(sceneType: string) {
  return sceneLabelMap[sceneType as SceneType] ?? sceneType;
}

function formatDateTime(value?: string) {
  if (!value) {
    return "未知时间";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });
}

function buildConfigSummary(config: AppConfigRequest) {
  return [
    { label: "ASR 模型", value: config.asrModelRoute },
    { label: "LLM 模型", value: config.llmModelRoute },
    { label: "识别策略", value: config.recognitionModel },
    { label: "语言类型", value: config.languageType },
    { label: "领域模型", value: config.domainModel },
    { label: "输出格式", value: config.outputFormat },
    { label: "稳定性", value: config.stabilityMode },
    { label: "优化目标", value: config.optimizationGoal },
    { label: "语气风格", value: config.toneStyle },
    { label: "长度偏好", value: config.lengthPreference },
    { label: "成本模式", value: config.costMode },
    { label: "热词修正", value: config.hotwordEnabled ? "启用" : "停用" }
  ];
}

function toConfigPayload(config: AppConfigResponse): AppConfigRequest {
  return {
    recognitionModel: config.recognitionModel,
    languageType: config.languageType,
    domainModel: config.domainModel,
    outputFormat: config.outputFormat,
    stabilityMode: config.stabilityMode,
    optimizationModel: config.optimizationModel,
    optimizationGoal: config.optimizationGoal,
    toneStyle: config.toneStyle,
    lengthPreference: config.lengthPreference,
    hotwordEnabled: config.hotwordEnabled,
    costMode: config.costMode,
    asrModelRoute: config.asrModelRoute,
    llmModelRoute: config.llmModelRoute
  };
}

export function ModelConfigPage() {
  const toast = useToast();
  const [tab, setTab] = useState<"config" | "templates" | "stats">("config");
  const [config, setConfig] = useState<AppConfigResponse | null>(null);
  const [templates, setTemplates] = useState<ConfigTemplateResponse[]>([]);
  const [stats, setStats] = useState<UsageStatsResponse | null>(null);
  const [templateForm, setTemplateForm] = useState(templateSeed);
  const [statsSceneType, setStatsSceneType] = useState("");
  const [statsFrom, setStatsFrom] = useState("");
  const [statsTo, setStatsTo] = useState("");

  async function loadAll() {
    const [configData, templateData, statsData] = await Promise.all([getConfig(), getTemplates(), getUsageStats({})]);
    setConfig(configData);
    setTemplates(templateData);
    setStats(statsData);
  }

  useEffect(() => {
    loadAll().catch((error) => {
      console.error(error);
      toast.error("加载配置失败", error instanceof Error ? error.message : "请稍后重试");
    });
  }, []);

  async function handleSaveConfig() {
    if (!config) {
      return;
    }
    try {
      const saved = await updateConfig(toConfigPayload(config));
      setConfig(saved);
      toast.success("系统默认配置已保存", "后续新任务会优先按这里的默认值创建模板或任务");
    } catch (error) {
      console.error(error);
      toast.error("保存配置失败", error instanceof Error ? error.message : "请稍后重试");
    }
  }

  async function handleReset() {
    try {
      const reset = await resetConfig();
      setConfig(reset);
      toast.success("已恢复默认配置");
    } catch (error) {
      console.error(error);
      toast.error("恢复默认失败", error instanceof Error ? error.message : "请稍后重试");
    }
  }

  async function handleCreateTemplate() {
    if (!config || !templateForm.name.trim()) {
      toast.info("模板名称未填写", "先填模板名称，再保存模板");
      return;
    }
    try {
      await createTemplate({
        ...templateForm,
        name: templateForm.name.trim(),
        description: templateForm.description.trim(),
        config: toConfigPayload(config),
        defaultTemplate: templateForm.defaultTemplate
      });
      setTemplateForm(templateSeed);
      await loadAll();
      toast.success("模板已保存", "这套模板可以在工作台直接选择");
    } catch (error) {
      console.error(error);
      toast.error("保存模板失败", error instanceof Error ? error.message : "请稍后重试");
    }
  }

  async function reloadStats() {
    try {
      setStats(await getUsageStats({
        sceneType: (statsSceneType || undefined) as SceneType | undefined,
        from: statsFrom || undefined,
        to: statsTo || undefined
      }));
      toast.success("统计已更新");
    } catch (error) {
      console.error(error);
      toast.error("加载统计失败", error instanceof Error ? error.message : "请稍后重试");
    }
  }

  if (!config || !stats) {
    return <div className="panel">加载中...</div>;
  }

  const currentSummary = buildConfigSummary(config);

  return (
    <div className="config-layout">
      <aside className="panel config-nav">
        <button className={`nav-tile ${tab === "config" ? "active" : ""}`} type="button" onClick={() => setTab("config")}>
          配置管理
        </button>
        <button className={`nav-tile ${tab === "templates" ? "active" : ""}`} type="button" onClick={() => setTab("templates")}>
          配置模板
        </button>
        <button className={`nav-tile ${tab === "stats" ? "active" : ""}`} type="button" onClick={() => setTab("stats")}>
          使用统计
        </button>
        <button className="secondary-button" type="button" onClick={handleReset}>
          恢复默认配置
        </button>
      </aside>

      <section className="config-main">
        {tab === "config" && (
          <div className="config-sections">
            <div className="panel config-note-panel">
              <h3 className="panel-title">配置管理</h3>
              <div className="summary-note">
                这里管理的是系统默认值，不是每次任务临时手填的参数。工作台真正执行时，优先使用“所选模板”里的完整快照；这里决定的是新模板的默认基线，以及没有指定模板时的系统默认走法。
              </div>
              <div className="runtime-grid">
                <div><span>ASR 提供方</span><strong>{config.asrProvider}</strong></div>
                <div><span>ASR 基地址</span><strong>{config.asrBaseUrl}</strong></div>
                <div><span>当前默认 ASR 路由</span><strong>{config.asrModelRoute}</strong></div>
                <div><span>LLM 提供方</span><strong>{config.llmProvider}</strong></div>
                <div><span>LLM 基地址</span><strong>{config.llmBaseUrl}</strong></div>
                <div><span>当前默认 LLM 路由</span><strong>{config.llmModelRoute}</strong></div>
                <div><span>系统默认模板</span><strong>{config.defaultTemplateName ?? "未设置"}</strong></div>
                <div><span>请求控制</span><strong>{config.modelTimeoutSeconds}s / {config.modelMaxRetries} 次重试</strong></div>
              </div>
            </div>

            <div className="panel">
              <h3 className="panel-title">默认模型与识别策略</h3>
              <div className="form-grid">
                <label>
                  默认 ASR 模型
                  <select className="select-input" value={config.asrModelRoute} onChange={(e) => setConfig({ ...config, asrModelRoute: e.target.value })}>
                    {asrRouteOptions.map((item) => <option key={item} value={item}>{item}</option>)}
                  </select>
                </label>
                <label>
                  默认 LLM 模型
                  <select className="select-input" value={config.llmModelRoute} onChange={(e) => setConfig({ ...config, llmModelRoute: e.target.value })}>
                    {llmRouteOptions.map((item) => <option key={item} value={item}>{item}</option>)}
                  </select>
                </label>
                <label>
                  识别策略
                  <select className="select-input" value={config.recognitionModel} onChange={(e) => setConfig({ ...config, recognitionModel: e.target.value })}>
                    {recognitionModelOptions.map((item) => <option key={item} value={item}>{item}</option>)}
                  </select>
                </label>
                <label>
                  语言类型
                  <select className="select-input" value={config.languageType} onChange={(e) => setConfig({ ...config, languageType: e.target.value })}>
                    {languageOptions.map((item) => <option key={item} value={item}>{item}</option>)}
                  </select>
                </label>
                <label>
                  领域模型
                  <select className="select-input" value={config.domainModel} onChange={(e) => setConfig({ ...config, domainModel: e.target.value })}>
                    {domainOptions.map((item) => <option key={item} value={item}>{item}</option>)}
                  </select>
                </label>
                <label>
                  输出格式
                  <select className="select-input" value={config.outputFormat} onChange={(e) => setConfig({ ...config, outputFormat: e.target.value })}>
                    {outputFormatOptions.map((item) => <option key={item} value={item}>{item}</option>)}
                  </select>
                </label>
                <label>
                  稳定性
                  <select className="select-input" value={config.stabilityMode} onChange={(e) => setConfig({ ...config, stabilityMode: e.target.value })}>
                    {stabilityOptions.map((item) => <option key={item} value={item}>{item}</option>)}
                  </select>
                </label>
              </div>
            </div>

            <div className="panel">
              <h3 className="panel-title">默认优化策略</h3>
              <div className="form-grid">
                <label>
                  优化模型
                  <select className="select-input" value={config.optimizationModel} onChange={(e) => setConfig({ ...config, optimizationModel: e.target.value })}>
                    {optimizationModelOptions.map((item) => <option key={item} value={item}>{item}</option>)}
                  </select>
                </label>
                <label>
                  优化目标
                  <select className="select-input" value={config.optimizationGoal} onChange={(e) => setConfig({ ...config, optimizationGoal: e.target.value })}>
                    {optimizationGoalOptions.map((item) => <option key={item} value={item}>{item}</option>)}
                  </select>
                </label>
                <label>
                  语气风格
                  <select className="select-input" value={config.toneStyle} onChange={(e) => setConfig({ ...config, toneStyle: e.target.value })}>
                    {toneOptions.map((item) => <option key={item} value={item}>{item}</option>)}
                  </select>
                </label>
                <label>
                  长度偏好
                  <select className="select-input" value={config.lengthPreference} onChange={(e) => setConfig({ ...config, lengthPreference: e.target.value })}>
                    {lengthOptions.map((item) => <option key={item} value={item}>{item}</option>)}
                  </select>
                </label>
                <label>
                  成本模式
                  <select className="select-input" value={config.costMode} onChange={(e) => setConfig({ ...config, costMode: e.target.value })}>
                    {costModeOptions.map((item) => <option key={item} value={item}>{item}</option>)}
                  </select>
                </label>
              </div>
              <div className="toggle-row">
                <span>默认启用热词修正</span>
                <label className="checkbox-row">
                  <input type="checkbox" checked={config.hotwordEnabled} onChange={(e) => setConfig({ ...config, hotwordEnabled: e.target.checked })} />
                  <span>{config.hotwordEnabled ? "启用" : "停用"}</span>
                </label>
              </div>
            </div>

            <div className="action-row">
              <button className="secondary-button" type="button" onClick={handleReset}>
                恢复默认
              </button>
              <button className="primary-button" type="button" onClick={handleSaveConfig}>
                保存系统默认值
              </button>
            </div>
          </div>
        )}

        {tab === "templates" && (
          <div className="panel">
            <h3 className="panel-title">配置模板</h3>
            <p className="summary-note">
              模板是用户真正拿来跑任务的处理预设。工作台开始处理时会选择模板，历史再次处理时默认沿用原模板，也可以临时切换别的模板。
            </p>

            <div className="form-grid">
              <label>
                模板名称
                <input className="text-input" value={templateForm.name} onChange={(e) => setTemplateForm({ ...templateForm, name: e.target.value })} />
              </label>
              <label>
                模板说明
                <input className="text-input" value={templateForm.description} onChange={(e) => setTemplateForm({ ...templateForm, description: e.target.value })} />
              </label>
            </div>

            <label className="checkbox-row">
              <input type="checkbox" checked={templateForm.defaultTemplate} onChange={(e) => setTemplateForm({ ...templateForm, defaultTemplate: e.target.checked })} />
              <span>创建后直接设为默认模板</span>
            </label>

            <div className="template-snapshot">
              <div className="template-snapshot-title">当前将保存的模板快照</div>
              <div className="template-summary-grid">
                {currentSummary.map((item) => (
                  <div key={item.label} className="template-summary-item">
                    <span>{item.label}</span>
                    <strong>{item.value}</strong>
                  </div>
                ))}
              </div>
            </div>

            <div className="action-row">
              <button className="primary-button" type="button" onClick={handleCreateTemplate}>
                保存当前配置为模板
              </button>
            </div>

            <div className="template-list">
              {templates.length === 0 ? (
                <div className="empty-state">还没有配置模板。先保存一套常用设置。</div>
              ) : templates.map((template) => (
                <div key={template.id} className="template-card">
                  <div className="template-card-head">
                    <div>
                      <strong>{template.name}</strong>
                      <p>{template.description || "未填写说明"}</p>
                    </div>
                    <div className="template-card-meta">
                      {template.defaultTemplate ? <span className="pill">默认模板</span> : null}
                      <span className="template-created-at">{formatDateTime(template.createdAt)}</span>
                    </div>
                  </div>

                  <div className="template-summary-grid">
                    {buildConfigSummary(template.config).map((item) => (
                      <div key={`${template.id}-${item.label}`} className="template-summary-item">
                        <span>{item.label}</span>
                        <strong>{item.value}</strong>
                      </div>
                    ))}
                  </div>

                  <div className="action-row">
                    <button
                      className="secondary-button"
                      type="button"
                      onClick={async () => {
                        try {
                          setConfig(await applyTemplate(template.id));
                          toast.success("模板已载入配置管理", template.name);
                        } catch (error) {
                          console.error(error);
                          toast.error("载入模板失败", error instanceof Error ? error.message : "请稍后重试");
                        }
                      }}
                    >
                      载入到配置管理
                    </button>
                    <button
                      className="secondary-button"
                      type="button"
                      onClick={async () => {
                        try {
                          await setDefaultTemplate(template.id);
                          await loadAll();
                          toast.success("默认模板已更新", template.name);
                        } catch (error) {
                          console.error(error);
                          toast.error("设置默认模板失败", error instanceof Error ? error.message : "请稍后重试");
                        }
                      }}
                    >
                      设为默认模板
                    </button>
                    <button
                      className="danger-button compact"
                      type="button"
                      onClick={async () => {
                        try {
                          await deleteTemplate(template.id);
                          await loadAll();
                          toast.success("模板已删除", template.name);
                        } catch (error) {
                          console.error(error);
                          toast.error("删除模板失败", error instanceof Error ? error.message : "请稍后重试");
                        }
                      }}
                    >
                      删除
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {tab === "stats" && (
          <div className="stats-board">
            <div className="panel">
              <div className="form-grid">
                <label>
                  场景筛选
                  <select className="select-input" value={statsSceneType} onChange={(e) => setStatsSceneType(e.target.value)}>
                    <option value="">全部场景</option>
                    <option value="MEETING_MINUTES">会议纪要</option>
                    <option value="WORK_REPORT">工作汇报</option>
                    <option value="FORMAL_EXPRESSION">正式表达</option>
                    <option value="MARKDOWN_NOTE">Markdown 笔记</option>
                    <option value="CODE_COMMENT">代码注释</option>
                    <option value="CHAT_REPLY">聊天回复</option>
                  </select>
                </label>
                <label>
                  开始日期
                  <input className="text-input" type="date" value={statsFrom} onChange={(e) => setStatsFrom(e.target.value)} />
                </label>
                <label>
                  结束日期
                  <input className="text-input" type="date" value={statsTo} onChange={(e) => setStatsTo(e.target.value)} />
                </label>
              </div>
              <div className="action-row">
                <button
                  className="secondary-button"
                  type="button"
                  onClick={() => {
                    setStatsSceneType("");
                    setStatsFrom("");
                    setStatsTo("");
                    getUsageStats({})
                      .then((data) => {
                        setStats(data);
                        toast.success("统计筛选已重置");
                      })
                      .catch((error) => {
                        console.error(error);
                        toast.error("重置统计失败", error instanceof Error ? error.message : "请稍后重试");
                      });
                  }}
                >
                  重置筛选
                </button>
                <button className="primary-button" type="button" onClick={reloadStats}>
                  应用筛选
                </button>
              </div>
            </div>

            <div className="stats-strip">
              <div className="stat-card"><span>平均识别耗时</span><strong>{stats.averageRecognitionSeconds}s</strong></div>
              <div className="stat-card"><span>平均优化耗时</span><strong>{stats.averageOptimizationSeconds}s</strong></div>
              <div className="stat-card"><span>平均总耗时</span><strong>{stats.averageTotalSeconds}s</strong></div>
              <div className="stat-card"><span>平均成本</span><strong>¥ {stats.averageCost}</strong></div>
              <div className="stat-card"><span>热词命中率</span><strong>{stats.hotwordHitRate}%</strong></div>
            </div>

            <div className="panel">
              <h3 className="panel-title">近 7 天趋势</h3>
              <div className="chart-list">
                {stats.trend.map((item) => (
                  <div key={item.date} className="chart-row">
                    <span>{item.date}</span>
                    <div className="bar-track">
                      <div className="bar-fill" style={{ width: `${Math.min(100, item.taskCount * 12)}%` }} />
                    </div>
                    <strong>{item.taskCount}</strong>
                  </div>
                ))}
              </div>
            </div>

            <div className="panel">
              <h3 className="panel-title">场景分布</h3>
              <div className="chart-list">
                {stats.sceneDistribution.map((item) => (
                  <div key={item.label} className="chart-row">
                    <span>{formatSceneType(item.label)}</span>
                    <div className="bar-track">
                      <div className="bar-fill secondary" style={{ width: `${Math.min(100, item.value * 12)}%` }} />
                    </div>
                    <strong>{item.value}</strong>
                  </div>
                ))}
              </div>
            </div>

            <div className="panel">
              <h3 className="panel-title">样本对比</h3>
              <div className="sample-compare-list">
                {stats.samples.map((sample) => (
                  <div key={sample.id} className="sample-compare-card">
                    <strong>{sample.title}</strong>
                    <div className="compare-grid">
                      <div><small>原始识别</small><p>{sample.rawText}</p></div>
                      <div><small>优化结果</small><p>{sample.optimizedText}</p></div>
                    </div>
                    <div className="improvement-note">{sample.improvement}</div>
                  </div>
                ))}
              </div>
              <p className="summary-note">{stats.conclusion}</p>
            </div>
          </div>
        )}
      </section>

      <aside className="panel preview-panel">
        <h3 className="panel-title">当前默认配置预览</h3>
        <div className="preview-list">
          <div><span>默认 ASR</span><strong>{config.asrModelRoute}</strong></div>
          <div><span>默认 LLM</span><strong>{config.llmModelRoute}</strong></div>
          <div><span>识别策略</span><strong>{config.recognitionModel}</strong></div>
          <div><span>语言类型</span><strong>{config.languageType}</strong></div>
          <div><span>优化目标</span><strong>{config.optimizationGoal}</strong></div>
          <div><span>热词修正</span><strong>{config.hotwordEnabled ? "启用" : "停用"}</strong></div>
          <div><span>默认模板</span><strong>{config.defaultTemplateName ?? "未设置"}</strong></div>
          <div><span>预计耗时</span><strong>{config.estimatedSeconds}s</strong></div>
          <div><span>预计成本</span><strong>¥ {config.estimatedCostPerMinute}/分钟</strong></div>
        </div>
      </aside>
    </div>
  );
}
