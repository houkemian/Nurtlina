# Nurtlina 项目状态与后端审核

审核日期：2026-06-07  
审核范围：当前仓库代码、后端实现、Android 端后端接入、Firebase 配置、测试与后续工作。  
重点：后端是否满足 MVP 的账号、备份/同步基础、权益校验、导出/删除、远程配置与未来家庭共享基础。

## 1. 总体结论

当前项目已经超过纯原型阶段，具备一个可继续推进的 Android 本地优先应用骨架，并且后端不是空壳：

- Android 端已经有 Room 本地数据、瓶子/喂养/尿布/睡眠记录、通知、WorkManager 同步队列、Firebase Auth、FastAPI API 客户端，以及 FastAPI/Postgres 主同步仓库。
- 后端已经有 FastAPI + PostgreSQL + Alembic 初始 schema、Firebase ID token 验证、用户/默认家庭初始化、同步 push/pull、Google Play entitlement 基础校验、账号软删除、导出接口占位。
- Firebase 侧已有 Firestore rules、Functions scaffold、Firebase 配置文件。

但当前状态仍应判断为 **MVP 后端集成中期，而不是可发布的后端闭环**。最大的架构问题是 **后端通道并存且职责未收敛**：

- Android 的后台队列 `SyncQueueProcessor` 会通过 FastAPI 推送本地变更。
- Android 的 `SyncRepository` 绑定已切换为 `ApiSyncRepository`，设置页/登录页手动同步会 flush FastAPI 队列并通过 `/sync/changes` 拉取后端变更。
- FastAPI 后端也有自己的 PostgreSQL 数据模型与同步接口。

这意味着项目已经开始收敛到 FastAPI/Postgres-first，但仍需清理或冻结旧 Firestore 同步实现，避免后续误用造成数据分裂、冲突策略不一致、权限模型不一致、测试覆盖重复但不闭环的问题。

推荐方向：**MVP 选择一种主同步后端并删减另一条路径的产品依赖**。

- 如果目标是一人快速上线 Android MVP：建议 Firebase-first，即 Auth + Firestore + Remote Config + Functions 做权益/后台任务；FastAPI 暂时降级为未来版本候选。
- 如果目标是掌控数据与导出/订阅校验并长期使用 Postgres：建议 FastAPI/Postgres-first，同时移除 Android 业务同步对 Firestore 的依赖，仅保留 Firebase Auth、Crashlytics、Analytics、Remote Config。

基于当前代码投入，仓库里 FastAPI 后端已经具备基础雏形，但 Android 端仍明显混合两套后端。因此后续首要任务不是继续堆新功能，而是收敛后端职责。

## 2. 当前已完成能力

### 2.1 Android 本地优先基础

已具备：

- Room 实体与 DAO 覆盖 Baby、Bottle、FeedLog、DiaperLog、SleepLog、SyncQueue。
- 本地 repository 会写入业务数据并 enqueue 同步任务。
- WorkManager 会在有网络时执行同步队列。
- 核心瓶子计时规则在客户端计算，后端不阻塞瓶子创建或状态变更。
- App startup 会尝试 Firebase 登录和 FastAPI `/me/init`，失败时可使用缓存 session 继续启动。

符合产品原则：

- 活跃计时器来源仍是本地数据。
- 后端失败不会直接阻断核心瓶子 timer flow。
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
  - bottles
  - feed_logs
  - diaper_logs
  - sleep_logs
  - entitlements
  - sync_cursors
- Firebase Admin SDK 验证 ID token。
- 自动 provision 用户。
- `/api/v1/me/init` 创建默认 family 和 OWNER member。
- `/api/v1/me` 查询当前用户。
- `/api/v1/sync/*` 支持各实体 push。
- `/api/v1/sync/changes` 支持按 `updated_at > since` 拉取变更。
- 同步 push 有 family membership 校验。
- 奶瓶同步冲突包含 `updated_at` 和状态优先级规则。
- `/api/v1/billing/google-play/purchases` 有 Google Play purchase token 验证框架。
- `/api/v1/entitlements/me` 可返回当前 Pro 状态。
- `/api/v1/account/delete` 可软删除用户及其默认家庭数据。
- `/api/v1/exports` 接口存在，但当前为 stub。

### 2.3 Firebase 基础

已具备：

- Firebase Auth 用于 Android 登录。
- Firestore rules 限制用户、family、member、baby care records 的访问范围。
- Firestore rules 对同步记录要求基础 metadata。
- Firestore rules 对更新使用 `updatedAt` 防止较旧写入覆盖较新写入。
- Firebase Functions scaffold 存在。

### 2.4 后端测试现状

已具备：

- 后端有健康检查测试。
- 后端有瓶子规则相关测试。
- 后端有同步冲突优先级测试。
- Android 端有 ExpiryCalculator、BottleStateMachine、SyncQueueWriter、AuthTokenInterceptor、AppStartupCoordinator、EntitlementCacheRepository 等测试。

测试已经覆盖一部分关键规则，但还没有覆盖完整后端闭环。

## 3. 主要风险与缺口

### P0：后端同步路径不统一（已开始收敛）

此前存在两条同步路径：

- Firestore 直连：`FirebaseSyncRepository`
- FastAPI/Postgres：`SyncQueueProcessor` + `BackendApiService`

当前已选择 FastAPI/Postgres-first：

- `SyncRepository` 绑定已由 `FirebaseSyncRepository` 替换为 `ApiSyncRepository`。
- `ApiSyncRepository.syncAll()` 会先 flush 本地 sync queue，再调用 FastAPI `/api/v1/sync/changes` 拉取远端变更并合并到 Room。
- `SyncWorker` 与 `WorkManagerSyncManager.syncNow()` 已改为委托 `SyncRepository`，后台同步、手动同步和登录后同步使用同一主通道。
- `FirebaseSyncRepository` 旧实现仍存在于代码中，但不再作为产品同步路径绑定。

风险：

- 同一个本地记录可能被写入 Firestore，也可能被写入 FastAPI/Postgres。
- 手动同步、后台同步、登录后同步的实际行为不一致。
- 两边冲突策略不完全一致。
- 两边权限/数据模型不完全一致。
- 用户删除、导出、恢复、权益校验无法形成单一可信源。

剩余建议：

- 删除或显式标注冻结 `FirebaseSyncRepository`，避免未来误绑定。
- 继续修复下方 FastAPI family ownership 校验和 pull cursor 稳定性问题。

### P0：FastAPI 同步入库缺少部分归属校验（已修复）

FastAPI push 入口会校验当前用户是 `family_id` 的成员。现已在 sync repository 入库前补齐记录级归属校验：

- incoming record 的 `family_id` 必须等于 request envelope 的 `family_id`，不再由 service 层覆盖 payload family。
- bottle/feed/diaper/sleep 的 `baby_id` 必须属于同一个 family。
- feed_log 的 `bottle_id` 如果存在，也必须属于同一个 family。
- 已存在记录如果 ID 属于其他 family，会返回 `rejected`，不会按主键取出后更新。
- 删除型变更通过 `deleted_at` tombstone 软删除合并，不会硬删或覆盖其他 family 数据。

新增 `test_sync_ownership.py` 覆盖 payload family mismatch、跨 family existing record、child baby/bottle 归属校验和软删除合并。

### P0：Android 手动同步和后台同步语义不一致（已修复）

设置页/登录页和 WorkManager 后台同步已统一到 `ApiSyncRepository.syncAll()`。该入口会 flush 本地 FastAPI sync queue，并执行 `/api/v1/sync/changes` pull。

当前状态：

- `SyncRepository` 与 `SyncManager` 已收敛到 FastAPI/Postgres 主通道。
- 手动同步会 flush 同步队列并执行 pull。
- UI 展示的 sync state 来自 `ApiSyncRepository.observeSyncState()`。

### P1：FastAPI pull 分页游标不够稳

当前 `/sync/changes` 对每个实体按 `updated_at > since` 拉取，超过 500 条时返回 `next_cursor = max(updated_at)`。

风险：

- 多实体共用一个 timestamp cursor，可能在大量同时间戳记录时漏数据。
- 没有稳定 tie-breaker，例如 `(updated_at, id)`。
- next cursor 不是按实体独立推进，客户端实现复杂后容易丢记录。

建议：

- 为每类实体使用独立 cursor，或统一变更日志表。
- cursor 使用 `(updatedAt, id)` 或服务端生成 sequence。
- pull response 返回每类实体的 next cursor。
- 添加同时间戳、大批量分页、跨实体分页测试。

### P1：Google Play entitlement 仍未形成生产闭环

已有 purchase token 验证和 entitlement 保存，但仍缺：

- RTDN Pub/Sub push JWT 验证。
- RTDN message 解码与订阅状态更新。
- purchase token 和 user 绑定策略。
- 恢复购买、过期、退款、取消、宽限期、暂停、重新订阅等状态测试。
- Google Play service account 权限与部署 secret 文档。
- 防止一个 purchase token 被多个账号重复绑定的约束。

建议：

- 增加 `purchase_token_hash` 唯一约束或按产品策略设计转移流程。
- 完成 RTDN 处理后，entitlements 以服务端状态为准。
- Android 端本地 entitlement cache 只能作为短期缓存。

### P1：导出功能是 stub

`/exports` 当前只返回 `PENDING`，查询会返回未实现。

对 Pro tier 来说，Export 是承诺功能之一。若 MVP 宣传 Pro 包含导出，则发布前必须补齐；否则需要在产品文案中暂不承诺。

建议：

- 明确 MVP 是否包含导出。
- 如包含：实现 CSV/JSON 导出、GCS signed URL、过期时间、权限校验、异步任务状态。
- 如不包含：Paywall/设置页不要展示“Export”作为已可用功能。

### P1：账号删除只是后端软删除，Firebase Auth 与外部数据清理未闭环

当前后端会软删除默认家庭数据并匿名化用户 email/display_name，但仍需补齐：

- Firebase Auth 用户删除或 token revoke 的服务端/客户端责任边界。
- Firestore 数据如果仍被使用，删除路径需要同步清理 Firestore。
- GCS export 文件、support messages、analytics user deletion request 的处理。
- 删除请求审计日志或状态表。
- 非默认家庭、多家庭、未来 caregiver 共享场景的删除策略。

### P1：Firestore rules 仍偏宽，需要发布前加固

当前 rules 已有基础 family member 校验，但存在加固空间：

- `families/{familyId}` update 未限制字段，成员可能修改 ownerUserId 等关键字段。
- `members/{memberId}` write 由 owner 执行，但 memberId 与 userId 关系未强约束。
- sync record schema 只检查基础 metadata，不检查 `familyId == familyId` 路径参数。
- `ownerUserId == request.auth.uid` 会影响未来 caregiver 代录入场景，需要明确 ownerUserId 与 lastModifiedBy 的语义。
- delete 允许 family member 直接 hard delete 记录，和 local-first soft delete 设计不一致。

如果 Firebase 只保留 Auth，不继续作为主数据库，这些规则优先级可下降；如果 Firebase-first，则这些是发布前必修。

### P1：缺少后端部署与运行闭环验证

后端 README 有 Cloud Run 部署示例，但还缺生产运行所需清单：

- Cloud SQL / Postgres 建库、连接池、迁移执行方式。
- Secret Manager 条目完整列表。
- Firebase service account 最小权限。
- Google Play service account 最小权限。
- CORS 在生产环境不能使用 `"*"`。
- 日志结构、错误告警、健康检查、数据库连接检查。
- 备份恢复、迁移回滚、schema 兼容策略。

### P1：后端测试环境未隔离生产配置

本次执行 `uv run pytest` 时，测试在 collection 阶段失败。原因是 `app.core.config.Settings` 在导入 `app.main` 时强制要求以下环境变量：

- `database_url`
- `database_sync_url`
- `firebase_project_id`
- `firebase_service_account_path`

这说明当前测试环境还没有独立的默认配置或 fixture。即使纯路由/health 测试，也会被生产配置阻断。

建议：

- 增加 `.env.test` 或 pytest fixture，为测试提供 dummy settings。
- 将 Firebase Admin 初始化延迟到真正验证 token 时，避免 import app 时强依赖真实 service account。
- 为无需数据库的单元测试避免导入完整 FastAPI app。
- 增加 CI 命令文档，明确本地和 CI 运行测试需要的环境变量。

### P2：远程配置职责未落地

PRD/架构要求 Remote Config 只用于非安全 UI、paywall、rollout 设置。当前代码已有 Firebase 依赖，但未看到完整 remote config domain 边界。

建议：

- 明确 remote config 允许字段，例如 paywall copy、价格展示、feature flag、rollout percentage。
- 明确禁止字段，例如瓶子 timer duration、规则版本静默覆盖、安全措辞。
- 增加默认值和离线降级策略。

### P2：数据模型与隐私约束需要进一步收紧

当前 records 有 note 字段，并且 sync 会上传 note。隐私规则要求不要上传不必要的个人数据，不要将 baby notes 用于 analytics。

建议：

- 在备份/同步开启时明确告知会上传哪些字段。
- note 字段不进入 analytics。
- 提供同步开关、导出、删除路径。
- Baby name UI 引导使用昵称。

## 4. 后端完成度评估

| 模块 | 当前状态 | 完成度 | 发布前要求 |
|---|---:|---:|---|
| Firebase Auth 登录 | Android 与 FastAPI 均已接入 | 中 | 真机验证登录失败/离线缓存 |
| 用户初始化 | `/me/init` 已有默认 family 创建 | 中 | 幂等与并发测试 |
| 本地优先 | Android Room + 队列已具备 | 中高 | 验证后端失败不影响所有核心操作 |
| FastAPI 同步 push | 已实现 | 中 | 补 family/baby/bottle 归属校验 |
| FastAPI 同步 pull | 已实现基础版 | 中 | 稳定分页、客户端 merge、删除同步 |
| Firestore 同步 | 已实现基础版 | 中 | 若保留，需 rules 加固和冲突测试 |
| 同步路径一致性 | 未完成 | 低 | 必须收敛为单一主路径 |
| Entitlement 校验 | 已有基础框架 | 中 | RTDN、恢复、退款/过期测试 |
| 导出 | stub | 低 | 实现或移出 MVP 承诺 |
| 账号删除 | 后端软删除 | 中 | Firebase/Firestore/GCS/analytics 清理闭环 |
| Remote Config | 依赖存在，边界未落地 | 低 | 定义允许/禁止配置 |
| 安全规则/RLS | Firestore rules 有基础版；Postgres 靠 API 校验 | 中 | 补跨 family 写入测试 |
| 测试运行环境 | 当前缺测试配置，pytest collection 会失败 | 低中 | 增加 `.env.test`/fixture/CI 配置 |
| 部署运维 | README 有示例 | 低中 | Secret、迁移、监控、备份恢复 |

## 5. 推荐后续路线

### 阶段 1：架构收敛，先做 3-5 天

目标：消除双同步通道。

任务：

1. 决定 MVP 主后端：Firebase-first 或 FastAPI/Postgres-first。
2. 画出最终数据流：
   - 本地 Room 写入
   - sync queue
   - push
   - pull
   - conflict handling
   - entitlement restore
   - account deletion
3. 更新 DI 绑定和设置页/登录页调用，确保手动同步和后台同步走同一通道。
4. 删除或标注冻结非主路径代码，避免误用。
5. 添加架构决策记录：`docs/adr-backend-sync-source-of-truth.md`。

建议默认选择：

- 短期上线优先：Firebase-first。
- 长期数据可控和后端产品化优先：FastAPI/Postgres-first。

### 阶段 2：FastAPI/Postgres-first 需要补齐的任务

如果选择 FastAPI/Postgres-first：

1. 已新增 `ApiSyncRepository`，并替换 `FirebaseSyncRepository` 绑定。
2. 已通过 `ApiSyncRepository.syncAll()` 支持：
   - push 队列
   - pull changes
   - 本地 merge
   - 删除 tombstone merge
   - cursor 持久化
3. 已为 FastAPI upsert 增加 family ownership 校验：
   - existing record family 校验
   - baby belongs to family
   - optional bottle belongs to family
   - payload family_id 与 envelope family_id 一致
4. 修改 pull cursor：
   - 每实体独立 cursor，或统一 change log。
   - 使用 `(updated_at, id)` tie-breaker。
5. 增加数据库唯一约束与索引：
   - entitlements.purchase_token_hash 唯一或策略性唯一。
   - sync record 可考虑 `(family_id, id)` 访问模式。
6. 完成 Google Play RTDN。
7. 完成导出或从 MVP Pro 承诺里移除。
8. 编写后端集成测试：
   - A 用户不能写 B family。
   - bottle 的 baby_id 不属于 family 时拒绝。
   - stale remote update 被拒绝。
   - 同 timestamp 状态优先级正确。
   - 删除同步能被 pull 到。
   - 后端不可用时 Android 本地创建仍成功。

### 阶段 3：Firebase-first 需要补齐的任务

如果选择 Firebase-first：

1. 冻结 FastAPI 同步队列，避免业务写入双写。
2. 保留 FastAPI 仅用于 entitlement 或暂时移除 API client。
3. 加固 Firestore rules：
   - familyId 必须等于路径 familyId。
   - 禁止客户端修改 ownerUserId、createdAt 等关键字段。
   - 将 hard delete 改为 soft delete 策略，或明确 delete 权限只用于本地清理。
   - 为 family members 增加字段校验。
4. 完成 Functions：
   - Google Play entitlement 校验。
   - RTDN。
   - account deletion cleanup。
5. 补 Firestore emulator 测试：
   - 跨用户读取拒绝。
   - 跨 family 写入拒绝。
   - stale update 拒绝。
   - 非 owner 不能改 membership。
6. 明确 Remote Config 只控制非安全 UI/paywall/rollout。

### 阶段 4：发布前验收

发布前至少完成：

- Android `flutter` 不适用；此项目应使用 Gradle/Kotlin 测试和 assemble。
- `./gradlew :frontend:app:testDebugUnitTest` 通过。
- `./gradlew :frontend:app:assembleDebug` 或 release 变体构建通过。
- 后端 `pytest` 通过。
- 后端测试不能依赖生产 secret；CI 应使用测试配置或 mock。
- Alembic migration 在空库可执行。
- Android 真机/模拟器验证：
  - 无网络创建瓶子。
  - 有网络后同步。
  - 后端 500/超时不影响计时器。
  - 通知权限拒绝后 app 可用。
  - 账号登录失败后本地基础功能可用。
  - 删除账号后本地和远端行为符合隐私说明。
- 文案检查：
  - 不出现安全保证、医疗背书或医学判断。
  - 定时器相关文案保持 “timer expired / based on selected guideline / reminder tool / not medical advice” 语义。

## 6. 建议优先级清单

### P0：必须先处理

- 选定唯一主同步后端。
- 移除 Android 端 Firestore sync 与 FastAPI sync 的并行产品路径。
- FastAPI upsert 增加跨 family / baby / bottle 归属校验。
- 手动同步、后台同步、启动同步统一到同一实现。
- 为同步安全边界增加测试。

### P1：MVP 发布前处理

- entitlement restore 与 RTDN 闭环。
- 导出功能实现，或从 Pro/MVP 文案中移除。
- 账号删除补齐 Firebase/Firestore/GCS/analytics 边界。
- pull cursor 稳定化。
- 生产 CORS、secrets、Cloud Run/Cloud SQL 部署清单。
- Firestore rules 加固，除非 Firebase 不再作为业务数据库。

### P2：发布后早期迭代

- 多家庭/多 caregiver 共享模型。
- 更完整的 conflict audit trail。
- 后端 admin/support 工具。
- 数据导出更多格式。
- 远程配置管理面板。
- 备份恢复 UX 完善。

## 7. 建议的下一份技术文档

建议新增两份文档：

1. `docs/adr-backend-sync-source-of-truth.md`
   - 明确选择 Firebase-first 或 FastAPI/Postgres-first。
   - 写清楚为什么选择、放弃什么、迁移成本、回滚方案。

2. `docs/sync-contract.md`
   - 定义所有实体 payload。
   - 定义 server accepted/rejected/conflict 语义。
   - 定义 cursor。
   - 定义 soft delete。
   - 定义客户端 merge 规则。
   - 定义后端安全校验。

## 8. 审核判断

当前代码适合继续推进 MVP，但不建议在现状下直接发布带云备份/同步/Pro 权益承诺的版本。原因不是功能完全缺失，而是后端路径并存导致可信源不清晰。

最务实的下一步是先做一次小范围架构收敛：选定主后端，改 DI 和同步入口，补安全校验和同步测试。完成后，项目会从“功能堆叠中的中期状态”进入“可验证的 MVP 后端闭环”。
