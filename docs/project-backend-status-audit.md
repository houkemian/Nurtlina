# Nurtlina 项目状态与后端审核

审核日期：2026-07-10  
审核范围：当前仓库代码、后端实现、Android 端后端接入、Firebase 配置、测试与后续工作。  
重点：后端是否满足 MVP 的账号、备份/同步基础、权益校验、导出/删除、远程配置与未来家庭共享基础。
**v2.0 重大变更**：已移除奶瓶计时器/状态机。Bottle 实体、bottles 表和相关 API 已废弃。数据模型简化为 FeedLog 为主的喂奶记录。

## 1. 总体结论

当前项目已经超过纯原型阶段，具备一个可继续推进的 Android 本地优先应用骨架，并且后端不是空壳：

- Android 端已经有 Room 本地数据、喂养/尿布/睡眠记录、通知、WorkManager 同步队列、Firebase Auth、FastAPI API 客户端，以及 FastAPI/Postgres 主同步仓库。
- 后端已经有 FastAPI + PostgreSQL + Alembic 初始 schema、Firebase ID token 验证、用户/默认家庭初始化、同步 push/pull、Google Play entitlement 基础校验、账号软删除、导出接口。
- Firebase 侧已有 Firestore rules、Functions、Firebase 配置文件。

**v2.0 状态**：产品已决定移除 Bottle（奶瓶计时器/状态机）相关所有代码和数据模型。Bottle 表、BottleStateMachine、BottleNotificationScheduler 等需要在 v2.0 重构中移除。

推荐方向：**继续按 FastAPI/Postgres-first 推进 MVP 后端闭环**，同时执行 v2.0 重构移除 Bottle 系统。

## 2. 当前已完成能力

### 2.1 Android 本地优先基础

已具备：

- Room 实体与 DAO 覆盖 Baby、FeedLog、DiaperLog、SleepLog、SyncQueue。
- 本地 repository 会写入业务数据并 enqueue 同步任务。
- WorkManager 会在有网络时执行同步队列。
- 核心喂奶记录在客户端处理，后端不阻塞记录创建。
- App startup 会尝试 Firebase 登录和 FastAPI `/me/init`，失败时可使用缓存 session 继续启动。

符合产品原则：

- 记录来源仍是本地数据。
- 后端失败不会直接阻断核心喂奶记录 flow。
- 同步是异步/后台行为。

### 2.2 FastAPI 后端基础

已具备：

- FastAPI app 入口、CORS、request id、统一异常处理。
- PostgreSQL async SQLAlchemy session。
- Alembic 初始 schema，包含：
  - users
  - families
  - family_members
  - babies
  - feed_logs
  - diaper_logs
  - sleep_logs
  - entitlements
  - sync_cursors
- Firebase Admin SDK 验证 ID token。
- 自动 provision 用户。
- `/api/v1/me/init` 创建默认 family 和 OWNER member。
- `/api/v1/me` 查询当前用户。
- `/api/v1/sync/feed-logs|diaper-logs|sleep-logs` 支持各实体 push。
- `/api/v1/sync/changes` 支持按 `updated_at > since` 拉取变更。
- 同步 push 有 family membership 校验。
- `/api/v1/billing/google-play/purchases` Google Play purchase token 验证框架。
- `/api/v1/entitlements/me` 返回当前 Pro 状态。
- `/api/v1/account/delete` 软删除用户及其默认家庭数据。
- `/api/v1/exports` 支持即时 CSV/JSON 导出。

**v2.0 待移除**：
- `bottles` 表和相关 Alembic migration。
- `/api/v1/sync/bottles` 同步端点。
- Bottle 相关的归属校验逻辑。
- Sync changes 返回中的 bottles 字段。

## 3. v2.0 重构任务

### P0：移除 Bottle 相关后端代码

| 任务 | 估时 |
|---|---|
| 移除 bottles 表 + Alembic migration | 1h |
| 移除 `/api/v1/sync/bottles` 端点 | 0.5h |
| 更新 `/api/v1/sync/changes` 返回不再包含 bottles | 0.5h |
| 更新 `sync_service` 移除 bottle 相关处理 | 1h |
| 更新 Firestore rules 移除 bottles 子集合 | 0.5h |
| 更新后端测试 | 1h |

## 4. 后端完成度评估

| 模块 | 当前状态 | 完成度 | 发布前要求 |
|---|---|---|---|
| Firebase Auth 登录 | Android 与 FastAPI 均已接入 | 中 | 真机验证登录失败/离线缓存 |
| 用户初始化 | `/me/init` 已有默认 family 创建 | 中 | 幂等与并发测试 |
| 本地优先 | Android Room + 队列已具备 | 中高 | 验证后端失败不影响所有核心操作 |
| FastAPI 同步 push | 已实现并补齐记录级归属校验 | 中高 | v2.0 移除 bottles push |
| FastAPI 同步 pull | 已取消不稳定分页并稳定排序 | 中高 | v2.0 移除 bottles 返回 |
| Entitlement 校验 | 已补 purchase token 绑定与 RTDN 更新 | 中高 | 部署层限制 RTDN push 来源/audience |
| 导出 | 已支持即时 CSV/JSON 导出 | 中高 | 大文件场景再接异步任务/GCS |
| 账号删除 | 已扩展多 family soft delete | 中高 | 若恢复 Firestore 业务数据需补 cleanup job |
| Remote Config | 依赖存在，边界未落地 | 低 | 定义允许/禁止配置 |

## 5. 推荐后续路线

### 阶段 1：v2.0 重构 — 移除 Bottle 系统

目标：从代码库中彻底移除 Bottle 相关代码，简化为纯喂奶记录模型。

### 阶段 2：发布前验收

- Android `bash gradlew testDebugUnitTest` 通过（2026-07-15 已验证）。
- Android `bash gradlew assembleDebug` 构建通过（2026-07-15 已验证）。
- 后端 `uv run pytest` 通过。
- 后端测试不能依赖生产 secret。
- Alembic migration 在空库可执行。
- Android 真机验证：
  - 无网络创建喂奶记录。
  - 有网络后同步。
  - 后端 500/超时不影响记录。
  - 通知权限拒绝后 app 可用。
  - 账号登录失败后本地基础功能可用。
  - 删除账号后本地和远端行为符合隐私说明。
- 文案检查：
  - 不出现安全保证、医疗背书或医学判断。
  - 保持 "tracking tool / based on public guidelines / not medical advice" 语义。

---

*审核日期：2026-07-10*
