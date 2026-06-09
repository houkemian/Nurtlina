# Nurtlina 项目状态与后端审核

审核日期：2026-06-07  
审核范围：当前仓库代码、后端实现、Android 端后端接入、Firebase 配置、测试与后续工作。  
重点：后端是否满足 MVP 的账号、备份/同步基础、权益校验、导出/删除、远程配置与未来家庭共享基础。

## 1. 总体结论

当前项目已经超过纯原型阶段，具备一个可继续推进的 Android 本地优先应用骨架，并且后端不是空壳：

- Android 端已经有 Room 本地数据、瓶子/喂养/尿布/睡眠记录、通知、WorkManager 同步队列、Firebase Auth、FastAPI API 客户端，以及 FastAPI/Postgres 主同步仓库。
- 后端已经有 FastAPI + PostgreSQL + Alembic 初始 schema、Firebase ID token 验证、用户/默认家庭初始化、同步 push/pull、Google Play entitlement 基础校验、账号软删除、导出接口占位。
- Firebase 侧已有 Firestore rules、Functions scaffold、Firebase 配置文件。

但当前状态仍应判断为 **MVP 后端集成中期，而不是可发布的后端闭环**。原先最大的架构问题是后端通道并存；按当前代码复评，主同步路径已收敛到 **FastAPI/Postgres-first**，剩余问题主要是旧 Firestore 同步实现仍未清理、生产闭环和测试覆盖不足：

- Android 的后台队列 `SyncQueueProcessor` 会通过 FastAPI 推送本地变更。
- Android 的 `SyncRepository` 绑定已切换为 `ApiSyncRepository`，设置页/登录页手动同步会 flush FastAPI 队列并通过 `/sync/changes` 拉取后端变更。
- FastAPI 后端也有自己的 PostgreSQL 数据模型与同步接口。

这意味着项目已经实质选择 FastAPI/Postgres-first。旧 Firestore 同步实现现在更像遗留维护风险，而不是当前产品主路径风险；后续应清理或冻结旧实现，避免误绑定和团队认知分叉。

推荐方向：**继续按 FastAPI/Postgres-first 推进 MVP 后端闭环**。Firebase 侧建议保留 Auth、Crashlytics、Analytics、Remote Config 等职责；Firestore 业务同步路径应删除、冻结或明确标注 deprecated。

基于当前代码投入，后续首要任务已经从“选择主后端”转为“清理旧同步路径、补齐 pull cursor、entitlement、导出、删除和测试/部署闭环”。

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
- `/api/v1/exports` 已支持即时 CSV/JSON 导出，返回短期 data URL。

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

### P0 复评：后端同步路径不统一（已关闭）

原判断：此前存在两条同步路径：

- Firestore 直连：`FirebaseSyncRepository`
- FastAPI/Postgres：`SyncQueueProcessor` + `BackendApiService`

复评结论：**主同步路径已收敛到 FastAPI/Postgres-first，原 P0 风险已关闭。**

代码证据：

- `SyncRepository` 绑定已由 `FirebaseSyncRepository` 替换为 `ApiSyncRepository`。
- `ApiSyncRepository.syncAll()` 会先 flush 本地 sync queue，再调用 FastAPI `/api/v1/sync/changes` 拉取远端变更并合并到 Room。
- `SyncWorker` 与 `WorkManagerSyncManager.syncNow()` 已改为委托 `SyncRepository`，后台同步、手动同步和登录后同步使用同一主通道。
- `FirebaseSyncRepository` 旧实现已删除，产品同步路径只保留 `ApiSyncRepository` 绑定。

剩余风险：

- Firestore DTO/rules 仍存在，但不再承担 baby care 业务同步。
- 后续如果重新启用 Firestore 业务数据，需要重新评估安全规则和 emulator tests。

后续建议：

- ADR 已记录 FastAPI/Postgres 为同步 source of truth。
- Firebase 保留 Auth/Analytics/Crashlytics/Remote Config 等非 baby care 业务同步职责。

### P0 复评：FastAPI 同步入库缺少部分归属校验（已关闭）

原判断：FastAPI push 入口只校验当前用户是 `family_id` 的成员，入库前缺少记录级归属校验。

复评结论：**已修复，当前不再列为 P0。**

代码证据：

- incoming record 的 `family_id` 必须等于 request envelope 的 `family_id`，不再由 service 层覆盖 payload family。
- bottle/feed/diaper/sleep 的 `baby_id` 必须属于同一个 family。
- feed_log 的 `bottle_id` 如果存在，也必须属于同一个 family。
- 已存在记录如果 ID 属于其他 family，会返回 `rejected`，不会按主键取出后更新。
- 删除型变更通过 `deleted_at` tombstone 软删除合并，不会硬删或覆盖其他 family 数据。

新增 `test_sync_ownership.py` 覆盖 payload family mismatch、跨 family existing record、child baby/bottle 归属校验和软删除合并。

剩余建议：

- 增加真实数据库集成测试，覆盖外键约束、并发更新和事务回滚。
- 在 API 层对 `rejected` 结果增加可观测性，避免客户端静默积压不可恢复的同步项。
- 将 family ownership 规则写入 `docs/sync-contract.md`，作为客户端和后端共同契约。

### P0 复评：Android 手动同步和后台同步语义不一致（已关闭）

原判断：设置页/登录页走 `SyncRepository.syncAll()`，业务写入后走 `SyncManager.requestSyncSoon()`，两者可能落到不同后端通道。

复评结论：**已修复，当前不再列为 P0。**

设置页/登录页和 WorkManager 后台同步已统一到 `ApiSyncRepository.syncAll()`。该入口会 flush 本地 FastAPI sync queue，并执行 `/api/v1/sync/changes` pull。

代码证据：

- `SyncRepository` 与 `SyncManager` 已收敛到 FastAPI/Postgres 主通道。
- 手动同步会 flush 同步队列并执行 pull。
- UI 展示的 sync state 来自 `ApiSyncRepository.observeSyncState()`。

剩余建议：

- 为 `ApiSyncRepository.syncAll()` 增加 Android 单元测试，覆盖 push 失败时不推进 pull cursor、pull 成功时写入 Room、删除 tombstone 合并。
- `WorkManagerSyncManager.syncNow()` 当前只返回成功/失败计数，后续可透传实际 push/pull 数量和错误类型，便于设置页展示。

### P1 复评：FastAPI pull 分页游标不够稳（已解决）

原问题：`/sync/changes` 使用 timestamp-only 分页，超过 500 条时可能因同一 `updated_at` 时间戳漏数据。

当前处理：

- 后端 pull repository 查询已统一按 `(updated_at, id)` 稳定排序。
- `/sync/changes` 已取消 timestamp-only 分页，MVP 阶段返回请求 cursor 后的完整变更集。
- `has_more` 固定为 `false`，`next_cursor` 固定为 `null`，避免客户端用不完整 timestamp cursor 继续分页造成漏记录。

后续如数据量增长，应引入 per-entity cursor 或统一 change log，而不是恢复 timestamp-only 分页。

### P1 复评：Google Play entitlement 生产闭环（已补齐 MVP 闭环）

当前处理：

- `purchase_token_hash` 已增加唯一约束和 Alembic 迁移，防止同一 purchase token 绑定多个账号。
- `verify_and_save_purchase()` 会检测 token hash 是否已属于其他 user，冲突时拒绝绑定。
- Google Play RTDN endpoint 已解码 Pub/Sub push message，并按既有 purchase token hash 更新 entitlement。
- RTDN 仅更新已通过客户端 purchase submit 链接过的 token，未知 token 会忽略，避免无 user 归属的远端消息创建错误 entitlement。
- Android 本地 entitlement cache 仍作为缓存，服务端 entitlement 状态是恢复和校验依据。

剩余发布前操作项：在部署入口或 Pub/Sub 配置层限制 RTDN push 来源和 audience；该要求已写入 `docs/backend-deployment-runbook.md`。

### P1 复评：导出功能是 stub（已解决）

当前处理：

- `/api/v1/exports` 已从 stub 改为即时生成导出。
- 支持 `CSV` 和 `JSON`。
- 导出会校验当前用户是请求 family 的成员。
- 导出数据按 family 范围读取 Baby、Bottle、FeedLog、DiaperLog、SleepLog，排除 soft-deleted 记录。
- MVP 不持久化导出文件，接口返回短期 `data:` URL 和 `DONE` 状态。

后续如需要大文件或邮件链接，再扩展为异步任务 + GCS signed URL。

### P1 复评：账号删除只是后端软删除（已补齐 MVP 闭环）

当前处理：

- 删除范围从默认 family 扩展为用户拥有的所有 family。
- owner 删除会 soft-delete family 下 baby care records 和 memberships。
- 非 owner membership 会标记为 `REMOVED`。
- 用户 PII 字段会匿名化，`default_family_id` 清空。
- sync cursors 会清理，entitlements 会标记为 `CANCELED`。
- Firebase Auth refresh tokens revoke 和 Firebase user delete 已作为 best-effort cleanup 集成；失败会记录日志但不阻断 Postgres 数据删除。

Firestore 业务同步路径已不再作为 source of truth；若未来仍保留 Firestore 业务数据，需要另加 Firestore cleanup job。

### P1 复评：Firestore rules 仍偏宽（已降级）

当前处理：

- MVP 已采用 FastAPI/Postgres-first，Firestore 不再作为 Baby、Bottle、FeedLog、DiaperLog、SleepLog 的业务同步 source of truth。
- 旧 `FirebaseSyncRepository` 已删除，降低误绑定 Firestore 业务同步的风险。
- `docs/adr-backend-sync-source-of-truth.md` 已明确 Firebase 仅保留 Auth/Analytics/Crashlytics/Remote Config 等职责。

因此 Firestore rules 加固不再是当前 P1 发布阻塞项。若未来重新启用 Firestore 业务数据，必须重新评估 rules 并添加 emulator security tests。

### P1 复评：缺少后端部署与运行闭环验证（已解决为文档闭环）

当前处理：

- 已新增 `docs/backend-deployment-runbook.md`。
- Runbook 覆盖 runtime services、required secrets、service account 权限、migration、health check、rollback、backup/restore、observability、local verification。
- 明确生产 `ALLOWED_ORIGINS` 不应使用 `*`。
- 明确 RTDN push 来源/audience 应在部署入口或 Pub/Sub 配置层限制。

### P1 复评：后端测试环境未隔离生产配置（已解决）

当前处理：

- 已新增 `backend/app/tests/conftest.py`，为测试注入 dummy DB/Firebase/Google Play 配置。
- `uv run pytest` 不再需要真实生产 secret 即可 import FastAPI app 并运行测试。

验证结果：

- `uv run pytest`：33 passed。
- `uv run ruff check app`：passed。
- `uv run ruff format --check app`：passed。

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
| FastAPI 同步 push | 已实现并补齐记录级归属校验 | 中高 | 增加真实 DB 集成测试与 rejected 可观测性 |
| FastAPI 同步 pull | 已取消不稳定分页并稳定排序 | 中高 | 后续大规模数据再引入 per-entity cursor/change log |
| Firestore 同步 | 旧业务同步 repository 已删除 | 中高 | 保留 Firebase 非业务同步职责 |
| 同步路径一致性 | 主路径已收敛到 FastAPI/Postgres | 中高 | 清理旧 Firestore 同步实现并补 ADR |
| Entitlement 校验 | 已补 purchase token 绑定与 RTDN 更新 | 中高 | 部署层限制 RTDN push 来源/audience |
| 导出 | 已支持即时 CSV/JSON 导出 | 中高 | 大文件场景再接异步任务/GCS |
| 账号删除 | 已扩展多 family soft delete 与 Firebase Auth best-effort cleanup | 中高 | 若恢复 Firestore 业务数据需补 cleanup job |
| Remote Config | 依赖存在，边界未落地 | 低 | 定义允许/禁止配置 |
| 安全规则/RLS | Firestore rules 有基础版；Postgres 靠 API 校验 | 中 | 补跨 family 写入测试 |
| 测试运行环境 | 已加测试 dummy env，pytest 可运行 | 高 | CI 恢复时使用同一测试隔离策略 |
| 部署运维 | 已新增 backend deployment runbook | 中高 | 按目标平台实操演练 |

## 5. 推荐后续路线

### 阶段 1：架构收敛（已完成主路径选择）

目标：消除双同步通道。当前已选择 FastAPI/Postgres-first，并已删除旧 Firestore 业务同步 repository。

后续任务：

1. 维护最终数据流：
   - 本地 Room 写入
   - sync queue
   - push
   - pull
   - conflict handling
   - entitlement restore
   - account deletion
2. 保持 DI 绑定、设置页、登录页、WorkManager 都走 `ApiSyncRepository`。
3. 按 `docs/adr-backend-sync-source-of-truth.md` 维护后端职责边界。

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

### P0：当前复评结果

当前未发现仍处于打开状态的 P0。原三项 P0 中：

- 主同步路径已收敛到 FastAPI/Postgres-first，旧 Firestore 业务同步 repository 已删除。
- FastAPI upsert 跨 family / baby / bottle 归属校验已补齐，状态关闭。
- 手动同步、后台同步、启动同步已统一到 `ApiSyncRepository.syncAll()`，状态关闭。

下一步不应再按 P0 阻塞处理这三项，但应在 P1 中继续补旧 Firestore 同步清理、ADR、真实数据库集成测试和 pull cursor 稳定化。

### P1：当前复评结果

`3. 主要风险与缺口` 下原 P1 项已完成 MVP 级修复或降级：

- pull cursor 不再使用不稳定 timestamp-only 分页。
- entitlement 已补 purchase token 唯一绑定和 RTDN 更新入口。
- 导出已支持即时 CSV/JSON。
- 账号删除已扩展多 family soft delete 和 Firebase Auth best-effort cleanup。
- Firestore 业务同步路径已删除旧 repository，并由 ADR 明确降级为非主路径。
- 后端部署 runbook 已补齐。
- 后端测试配置已隔离生产 secret，`uv run pytest` 通过。

剩余工作不再按 P1 阻塞，但发布前仍建议做真实环境演练：RTDN push audience 限制、Cloud/服务器部署实操、Google Play sandbox purchase restore、账号删除真机验证。

### P2：发布后早期迭代

- 多家庭/多 caregiver 共享模型。
- 更完整的 conflict audit trail。
- 后端 admin/support 工具。
- 数据导出更多格式。
- 远程配置管理面板。
- 备份恢复 UX 完善。

## 7. 建议的下一份技术文档

已新增：

1. `docs/adr-backend-sync-source-of-truth.md`
   - 明确选择 FastAPI/Postgres-first。
   - 写清楚 Firebase 保留职责和 Firestore 业务同步降级。

仍建议新增：

2. `docs/sync-contract.md`
   - 定义所有实体 payload。
   - 定义 server accepted/rejected/conflict 语义。
   - 定义 cursor。
   - 定义 soft delete。
   - 定义客户端 merge 规则。
   - 定义后端安全校验。

## 8. 审核判断

当前代码适合继续推进 MVP，但不建议在现状下直接发布带云备份/同步/Pro 权益承诺的版本。原因不是主后端路径不清晰；主路径已经收敛到 FastAPI/Postgres-first。当前发布风险主要来自真实环境演练、集成测试、Remote Config 边界、隐私说明和部署闭环仍需补齐。

最务实的下一步是围绕已选定的 FastAPI/Postgres-first 路径做发布前硬化：补同步契约文档、增加真实数据库集成测试、完成部署演练、验证 Google Play sandbox purchase restore，并确保所有后端失败都不会影响本地瓶子 timer flow。

## 9. 接下来任务规划

### Sprint 1：同步契约与测试硬化

目标：把 FastAPI/Postgres-first 从“代码已收敛”推进到“行为可验证”。

任务：

1. 新增 `docs/sync-contract.md`。
   - 定义 Baby、Bottle、FeedLog、DiaperLog、SleepLog payload。
   - 定义 `accepted`、`rejected`、`conflict` 语义。
   - 定义 soft delete/tombstone 合并规则。
   - 定义客户端 pull merge 和 cursor 持久化规则。
   - 定义 family ownership、baby ownership、bottle ownership 校验。
2. 增加 FastAPI 真实数据库集成测试。
   - A 用户不能写 B family。
   - `baby_id` 不属于 family 时拒绝。
   - `feed_log.bottle_id` 不属于 family 时拒绝。
   - stale update 不覆盖较新记录。
   - soft delete 能被 pull 到 Android。
3. 增加 Android `ApiSyncRepository.syncAll()` 单元测试。
   - push 失败时不推进 pull cursor。
   - pull 成功时写入 Room。
   - tombstone merge 后本地记录进入删除状态。
   - 后端不可用时本地创建 bottle 仍成功。

验收：

- 后端 `uv run pytest` 通过。
- Android 相关单元测试通过。
- 文档中的 sync contract 与 API/Room 行为一致。

### Sprint 2：部署与权益闭环

目标：确认云端部署、购买恢复和账号删除在真实环境中可操作。

任务：

1. 按 `docs/backend-deployment-runbook.md` 完成 staging 部署演练。
2. 验证 Alembic 在空库和已有 staging 库均可执行。
3. 配置 RTDN push 来源/audience 限制。
4. 使用 Google Play sandbox 验证：
   - purchase submit；
   - entitlement restore；
   - token hash 唯一绑定；
   - RTDN 更新已绑定 token。
5. 真机验证账号删除。
   - Postgres 记录 soft delete/匿名化。
   - Firebase Auth cleanup best-effort 行为符合日志预期。
   - Android 本地状态与隐私说明一致。

验收：

- staging `/health`、`/api/v1/me/init`、sync push/pull、export、delete 均通过。
- 日志不包含 purchase token、baby notes 或原始个人记录。
- 后端失败不影响本地 timer 创建、状态变更和通知。

### Sprint 3：Remote Config 与隐私发布准备

目标：补齐发布前合规边界和用户说明。

任务：

1. 定义 Remote Config allowlist。
   - 允许：paywall copy、价格展示、feature flags、rollout percentage。
   - 禁止：timer duration、规则版本静默覆盖、安全判断文案。
2. 增加 Remote Config 默认值和离线降级策略。
3. 检查 user-facing strings。
   - 不出现 “safe to drink”、“guaranteed safe”、“doctor recommended” 等禁用措辞。
   - 保持 “timer expired”、“based on your selected guideline”、“not medical advice” 语义。
4. 完善 backup/sync 说明。
   - 明确开启备份/同步时会上传哪些字段。
   - 明确 notes 不进入 analytics。
   - 引导 baby name 使用昵称。
5. 完成 Google Play Data Safety 初稿。

验收：

- Remote Config 不控制 safety timer rules。
- 隐私说明、导出、删除路径与实际实现一致。
- 中英文核心合规文案含义一致。
