# Config Template Page Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构 `/config` 页里的“配置模板”子页，让模板新增、编辑、列表、详情职责分离。

**Architecture:** 保留当前三段导航结构，只重做“配置模板”子页内部布局。上半区作为单一模板编辑器，下半区作为摘要式模板列表，详情在列表项内按需展开。

**Tech Stack:** React + TypeScript + Vite + 现有 CSS

---

### Task 1: 收紧模板页状态模型

**Files:**
- Modify: `E:\Desktop\work\frontend\src\pages\ModelConfigPage.tsx`

- [ ] 定义“新建态 / 编辑态 / 展开详情态”三组状态。
- [ ] 保证“修改模板”只影响上半区编辑器，不直接展开整页配置明细。
- [ ] 保证“取消修改”能回到新建态。

### Task 2: 重构模板编辑器区域

**Files:**
- Modify: `E:\Desktop\work\frontend\src\pages\ModelConfigPage.tsx`

- [ ] 把上半区明确成单一职责的模板编辑器。
- [ ] 保留模板名称、说明、默认模板开关、模板参数表单。
- [ ] 调整按钮语义为“保存为新模板 / 保存模板修改 / 取消修改”。

### Task 3: 重构模板列表区域

**Files:**
- Modify: `E:\Desktop\work\frontend\src\pages\ModelConfigPage.tsx`
- Modify: `E:\Desktop\work\frontend\src\styles\app.css`

- [ ] 模板列表默认只显示摘要信息。
- [ ] 详情默认折叠，点击后再展开完整配置项。
- [ ] 保留“查看详情 / 修改 / 载入到配置管理 / 设为默认模板 / 删除”操作。

### Task 4: 验证

**Files:**
- Modify: `E:\Desktop\work\frontend\src\pages\ModelConfigPage.tsx`
- Modify: `E:\Desktop\work\frontend\src\styles\app.css`

- [ ] 运行 `npm run build`
- [ ] 如需本地查看，运行 `docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build frontend backend`
- [ ] 确认 `/config` 页“配置模板”子页职责清晰，不再把新增、编辑、详情揉在一层
