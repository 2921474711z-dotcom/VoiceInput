# VoiceInput Pro

`VoiceInput Pro` 是一套面向办公与技术场景的智能语音输入工作台，目标不是只把音频转成文字，而是把“上传、识别、优化、复核、沉淀、导出”做成一条完整链路。

项目目录包括：

- 前端应用：`frontend`
- Java 后端：`backend`
- 视觉参考：`frontDesign`
- Docker 编排：`docker-compose.yml`
- 演示材料：`docs/presentation`

## 技术栈

- 前端：React + Vite + TypeScript
- 后端：Spring Boot 3 + Spring Data JPA + Flyway
- 数据与中间件：PostgreSQL + Redis + MinIO
- 模型接入：OpenAI 兼容接口 / Xiaomi MiMo Token Plan
- 部署方式：Docker Compose

## 目录结构

```text
E:\Desktop\work
├── frontDesign
├── frontend
├── backend
├── docs\presentation
├── docker-compose.yml
├── docker-compose.local.yml
├── .env.example
└── README.md
```

## 演示材料

仓库已附带一份可直接用于答辩、验收或课堂汇报的演示稿：

- `docs/presentation/VoiceInput-Pro-平台介绍与验收演示稿.pptx`

这份 PPT 重点覆盖：

- 平台定位与适用场景
- 工作台、历史记录、导出中心、热词管理、模型配置等核心页面
- 从上传音频到结果落库、导出交付的完整处理链路
- 技术架构、部署方式、验收亮点和演示路径

如需重新生成演示稿，可执行：

```powershell
py -3 docs/presentation/build_voiceinput_platform_ppt.py
```

说明：

- 脚本默认读取 `.tmp/pptx-assets` 下的页面截图作为插图素材。
- 如需重新抓取页面截图，请先确保本地站点正常运行。

## 环境准备

### 1. Docker Desktop

- 需要安装 Docker Desktop
- Docker 数据目录建议统一落在 `E:\DockerData\VoiceInputPro`

### 2. Java / Node

- Java 17+
- Node.js 20+
- npm 10+

### 3. Maven

如需本地构建后端，请使用：

- Maven 配置目录：`E:\Maven`
- Maven 本地仓库：`E:\maven-repository`

## 配置文件

先复制环境变量模板：

```powershell
Copy-Item .env.example .env
```

再按实际情况补充以下配置：

- PostgreSQL 连接信息
- Redis 地址
- MinIO 账号、密码与桶名
- ASR 提供方、模型与 API Key
- LLM 提供方、模型与 API Key
- 成本计费参数

## 模型与 ASR 配置说明

当前项目按 OpenAI 兼容接口设计，至少需要配置：

- `ASR_BASE_URL`
- `ASR_API_KEY`
- `ASR_MODEL`
- `LLM_BASE_URL`
- `LLM_API_KEY`
- `LLM_MODEL`

默认推荐：

- ASR：`mimo-v2.5`
- LLM：`mimo-v2.5-pro`

当前默认方案为 Xiaomi MiMo Token Plan：

- `ASR_PROVIDER=xiaomi-mimo`
- `ASR_BASE_URL=https://token-plan-cn.xiaomimimo.com`
- `ASR_MODEL=mimo-v2.5`
- `LLM_PROVIDER=xiaomi-mimo`
- `LLM_BASE_URL=https://token-plan-cn.xiaomimimo.com`
- `LLM_MODEL=mimo-v2.5-pro`

补充说明：

- 代码会自行补 `/v1/...`，因此 `.env` 里的 Base URL 不要重复写 `/v1`
- ASR 实现不是 `audio/transcriptions`
- 当前方案实际使用的是 `chat/completions + input_audio(base64)`
- 后端会把上传的本地音频转成 Base64 Data URL，再发送给模型

## 启动方式

项目默认交付目标是：

`别人只要具备 Docker Desktop，并配置好自己的 API Key，就可以直接启动整套系统。`

### 标准启动

推荐命令：

```powershell
docker compose up -d --build
```

说明：

- 第一次执行时可能下载 Maven 和 npm 依赖，属于正常现象
- 当前 `Dockerfile` 已启用 BuildKit 缓存挂载，后续重复构建会复用容器内 Maven 与 npm 缓存
- 默认交付方式不依赖宿主机的 `E:\maven-repository`

这条命令会自动完成：

- 构建前端镜像
- 构建后端镜像
- 启动 PostgreSQL
- 启动 Redis
- 启动 MinIO
- 启动前端与后端服务

### 查看服务状态

```powershell
docker compose ps
```

启动后可访问：

- 前端：[http://localhost:5173](http://localhost:5173)
- 后端健康检查：[http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- MinIO Console：[http://localhost:9001](http://localhost:9001)

### 当前机器的本地兜底模式

如果当前开发机存在 Docker Hub 拉取问题，或希望复用本机现编产物，可以使用：

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build
```

说明：

- 这是当前开发机的本地兜底方案
- 不作为默认交付给其他人的启动方式
- 对外仍以 `docker compose up -d --build` 为标准

## 本地开发

### 前端

```powershell
cd frontend
npm install
npm run dev
```

### 后端

```powershell
cd backend
mvn spring-boot:run "-Dmaven.repo.local=E:\maven-repository"
```

## 验证流程

建议按以下路径验证：

1. 进入工作台，选择一个场景
2. 上传音频文件
3. 点击“开始处理”
4. 查看原始识别文本和优化后文本
5. 保存到历史记录
6. 在热词管理中新增术语并启用
7. 回到工作台重新处理
8. 在历史页查看详情并导出 Markdown / DOCX
9. 在模型配置中调整识别与优化策略
10. 在统计页查看耗时、成本、场景分布和热词命中率

## 交付说明

对外提供项目时，应满足以下条件：

- 拿到代码后只需安装 Docker Desktop
- 复制 `.env.example` 为 `.env`
- 填写自己的 `ASR_API_KEY` 与 `LLM_API_KEY`
- 执行 `docker compose up -d --build`
- 即可启动并联调整套系统

这意味着：

- 默认启动方式不能依赖本机 Maven 仓库
- 默认启动方式不能依赖预先构建好的 `dist` 或 `classes`
- 默认启动方式不能依赖开发机私有缓存镜像

## 常见问题

### 1. 模型接口连不上

- 检查 `ASR_BASE_URL`、`LLM_BASE_URL`
- 检查 `ASR_API_KEY`、`LLM_API_KEY`
- 检查容器是否可以访问外网

### 2. MinIO 桶不存在

- 后端启动时会自动检查桶
- 如果失败，请检查 MinIO 连接配置和凭证

### 3. 数据库启动失败

- 检查 `POSTGRES_USER`、`POSTGRES_PASSWORD`
- 检查端口是否冲突

### 4. 任务一直不完成

- 检查后端日志
- 检查 Redis 连接
- 检查模型配置是否完整

### 5. 成本统计异常

- 检查 `.env` 里的价格配置
- 检查模型返回结果是否包含足够的 usage 信息
