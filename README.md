# VoiceInput Pro

`VoiceInput Pro` 是一个面向办公与技术场景的智能语音输入工作台，包含：

- 前端工作台 `frontend`
- Java 后端 `backend`
- 参考设计稿 `frontDesign`
- Docker Compose 编排 `docker-compose.yml`

## 技术栈

- 前端：React + Vite + TypeScript
- 后端：Spring Boot 3 + Spring Data JPA + Flyway + PostgreSQL
- 中间件：PostgreSQL + Redis + MinIO
- 处理链路：后端异步任务队列 + OpenAI 兼容 ASR / LLM 接口

## 目录结构

```text
E:\Desktop\work
├─ frontDesign
├─ frontend
├─ backend
├─ docker-compose.yml
├─ .env.example
└─ README.md
```

## 环境准备

### 1. Docker Desktop

- 必须已安装 Docker Desktop
- Docker 数据目录建议统一落在 `E:\DockerData\VoiceInputPro`

### 2. Java 与 Node

- Java 17+
- Node.js 20+
- npm 10+

### 3. Maven

如果需要本地构建后端，请使用：

- Maven 配置目录：`E:\Maven`
- Maven 本地仓库：`E:\maven-repository`

## 配置文件

1. 复制一份环境变量模板：

```powershell
Copy-Item .env.example .env
```

2. 根据实际情况填写以下配置：

- PostgreSQL 数据库连接
- Redis 地址
- MinIO 账号、密码、桶名
- ASR 接口地址、模型与 Key
- LLM 接口地址、模型与 Key
- 成本价格参数

## 大模型与 ASR 配置说明

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

当前默认采用 Xiaomi MiMo Token Plan 一体方案：

- `ASR_PROVIDER=xiaomi-mimo`
- `ASR_BASE_URL=https://token-plan-cn.xiaomimimo.com`
- `ASR_MODEL=mimo-v2.5`
- `LLM_PROVIDER=xiaomi-mimo`
- `LLM_BASE_URL=https://token-plan-cn.xiaomimimo.com`
- `LLM_MODEL=mimo-v2.5-pro`

说明：

- 项目代码会自行补 `/v1/...`，所以 `.env` 中的 Base URL 不要重复写尾部 `/v1`
- ASR 实现不是 `audio/transcriptions`
- ASR 实际走的是 Xiaomi MiMo `chat/completions + input_audio(base64)` 方案
- 后端会把上传的本地音频转成 Base64 Data URL 后再发给 MiMo

如果改成其他兼容供应商，只要接口协议兼容即可。

## 启动方式

本项目默认交付目标是：

`别人只要有 Docker Desktop，并配置好自己的 API Key，就可以直接拉起运行。`

默认使用：

- `docker-compose.yml`：通用自包含启动方式，适合正常联网环境

仅当前这台机器的离线/缓存调试场景使用：

- `docker-compose.local.yml`：本机专用兜底覆盖文件，不作为默认交付方式

### 1. Docker 一键启动

默认推荐：

```powershell
docker compose up -d --build
```

这条命令会自动完成：

- 构建前端镜像
- 构建后端镜像
- 启动 PostgreSQL
- 启动 Redis
- 启动 MinIO
- 启动前端和后端服务

### 2. 查看服务状态

```powershell
docker compose ps
```

启动后访问：

- 前端：[http://localhost:5173](http://localhost:5173)
- 后端健康检查：[http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- MinIO Console：[http://localhost:9001](http://localhost:9001)

### 2. 当前机器离线兜底模式

如果当前机器存在 Docker Hub 拉取问题，或者希望复用本机缓存镜像与已编译产物，可以使用：

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build
```

说明：

- 这是当前开发机的兜底方案
- 不作为默认交付给他人的启动方式
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

1. 进入工作台，选择场景。
2. 上传音频文件。
3. 点击“开始处理”。
4. 查看原始识别文本和优化后文本。
5. 保存到历史。
6. 在热词管理中新增术语并启用。
7. 回到工作台重新处理。
8. 在历史页查看详情并导出 Markdown。
9. 在模型配置中调整识别与优化策略。
10. 在使用统计中查看耗时、成本、场景分布和热词命中率。

## 交付要求说明

对外提供项目时，应该保证以下条件成立：

- 别人拿到代码后，只需要安装 Docker Desktop
- 复制 `.env.example` 为 `.env`
- 填写自己的 `ASR_API_KEY` 和 `LLM_API_KEY`
- 执行 `docker compose up -d --build`
- 就可以直接跑起整套系统

也就是说：

- 默认启动方式不能依赖本机 Maven 仓库
- 默认启动方式不能依赖预先构建好的 dist 或 classes
- 默认启动方式不能依赖你这台机器的缓存镜像

## 常见问题

### 1. 模型接口连不上

- 检查 `ASR_BASE_URL`、`LLM_BASE_URL`
- 检查 `ASR_API_KEY`、`LLM_API_KEY`
- 检查容器是否可访问外网

### 2. MinIO 桶不存在

- 后端启动时会自动检查桶
- 如果失败，请检查 MinIO 连接配置和凭证

### 3. 数据库启动失败

- 检查 `POSTGRES_USER`、`POSTGRES_PASSWORD`
- 检查端口是否冲突

### 4. 处理任务一直不完成

- 检查后端日志
- 检查 Redis 连接
- 检查模型配置是否完整

### 5. 成本统计不准确

- 检查 `.env` 中的价格配置
- 检查模型返回结果是否包含足够 usage 信息
