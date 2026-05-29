# Nurtlina 第 17 点技术方案：FastAPI 后端生产级性价比架构

> 产品：Nurtlina: Baby Feeding Timer  
> 平台：Android-only  
> 技术倾向：Android Kotlin + Python FastAPI + PostgreSQL + Firebase 移动端基础设施  
> 阶段：MVP 到 12 个月生产演进  
> 目标：生产可用、成本可控、一人可维护、后端接口完整、核心计时离线可靠  
> 文档版本：v2.1  
> 日期：2026-05-29

---

### 当前项目实施状态（2026-05-29）

#### 已完成

**Android 工程脚手架（Phase 1 部分）**

- 项目结构：`app/build.gradle.kts`、`settings.gradle.kts`、`build.gradle.kts`、`gradle/libs.versions.toml`
- Application ID：`com.nurtlina.app`（注意：与下文 17.7.5 Billing 示例中的旧值不同，已以此为准）
- `minSdk 26`，`targetSdk 35`，`compileSdk 35`，JDK 17，Kotlin 2.0.21
- 全部依赖已在 `libs.versions.toml` 声明：Compose + Material 3、Hilt、Room、DataStore、WorkManager、Firebase BOM（Auth/Analytics/Crashlytics/Firestore/Functions）、Google Play Billing 7.1.1、AdMob
- `AndroidManifest.xml`：声明了 `POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED`、`SCHEDULE_EXACT_ALARM`、`USE_EXACT_ALARM`、`VIBRATE`、`INTERNET`、`ACCESS_NETWORK_STATE`；注册了 `BootReceiver`、`NotificationReceiver`
- 多语言字符串资源：EN / ES / DE / FR / ZH-CN
- 主题、颜色、通知图标、备份规则

**Firebase 后端（已实施为 Cloud Functions + Firestore，非 FastAPI）**

> **重要说明**：当前后端实际采用的是 Firebase-first 路线，而非本文档主体所描述的 FastAPI + PostgreSQL 路线。Firestore 担任云端业务数据存储，Cloud Functions（TypeScript）担任服务端逻辑。FastAPI 后端目前尚未创建。

- Firestore Security Rules（`firebase/firestore.rules`）：已覆盖 `users`、`families`、`families/members`、`families/babies`、`families/bottles`、`families/feedLogs`、`families/diaperLogs`、`families/sleepLogs`、`families/settings`、`entitlements`、`supportMessages`；含 `isNotStale()` 防止旧远端状态覆盖本地
- Cloud Functions（`firebase/functions/src/index.ts`，TypeScript）：
  - `playBillingWebhook`：Google Play RTDN Pub/Sub 订阅，更新 `entitlements/{userId}`（原计划 V1.1，已提前实现）
  - `registerPurchaseToken`：HTTPS Callable，Android 购买后注册 purchaseToken → userId 映射
  - `getEntitlement`：HTTPS Callable，返回当前 Pro 权益状态
  - `provisionFamily`：HTTPS Callable，首次登录创建 family + owner member（对应 FastAPI `/me/init`）
  - `deleteUserData`：HTTPS Callable，删除用户家庭数据、软删除记录、撤销 Firebase Auth token
  - `cleanupDeletedRecords`：Pub/Sub 定时任务（每 24 小时），清除 `deletedAt` 超过 90 天的记录
- Firebase 项目配置：`.firebaserc`、`firebase.json`、`firestore.indexes.json`

**CI/CD（Phase 1）**

- GitHub Actions（`.github/workflows/ci.yml`）：unit test → build debug APK → build release AAB（main 分支）

#### 尚未开始

- **FastAPI 后端**：`backend/` 目录不存在，无 Python 代码
- **Android Kotlin 源代码**：无 `MainActivity`、`NurtlinaApp`、Room 数据库、Domain 模型、ViewModel、Compose UI、通知调度、同步逻辑等
- Widget、Pro 权益门控、AdMob 集成、本地导出、多语言完整化

#### 架构路线说明

当前实施选择了 Firebase Cloud Functions + Firestore 作为后端，与本文档 17.1—17.18 所描述的 FastAPI + PostgreSQL 方案存在分歧。

两条路线的主要差异：

| 能力 | 当前实施（Firebase-first） | 本文档方案（FastAPI） |
|---|---|---|
| 业务数据存储 | Firestore | PostgreSQL |
| 服务端逻辑 | Cloud Functions (TypeScript) | FastAPI (Python) |
| 订阅 RTDN | 已实现（Pub/Sub） | 计划 V1.1 |
| 家庭初始化 | `provisionFamily` Cloud Function | `/me/init` REST API |
| 账号删除 | `deleteUserData` Cloud Function | `/account/delete` REST API |
| SQL 查询/批量导出 | 不支持，需迁移 | 原生支持 |
| 后台管理/审计 | 受限 | 可扩展 |

如果决定继续 Firebase-first，17.3—17.11 中与 FastAPI/PostgreSQL 相关的章节可作为未来迁移或增量扩展的参考，不必立即实现。若坚持 FastAPI 路线，需在 Android 工程开始前先补齐后端工程，并将 Cloud Functions 中已有逻辑迁移或并行维护。

---

### 17.1 总体结论

本产品推荐采用：

```text
Android Kotlin/Compose 本地优先
+ Firebase 移动端基础设施
+ Python FastAPI 独立服务端
+ PostgreSQL 业务数据库
+ Cloud Run 容器化部署
+ Google Play Billing 服务端校验
```

核心思想：

> **Firebase 不作为唯一后端，而是作为移动端基础设施；真正的业务后端使用 FastAPI + PostgreSQL。**

这样可以兼顾：

- MVP 上线速度
- 一人维护成本
- 后端接口可控性
- 数据可迁移性
- 订阅校验可靠性
- 云备份/同步能力
- 未来家庭共享、Web Admin、客服后台、数据导出的扩展空间

---

### 17.2 设计原则

#### 17.2.1 本地优先，后端增强

奶瓶计时器是核心功能，不能依赖网络。

必须满足：

- 无网也能创建奶瓶计时器。
- 无网也能开始喂奶、标记冷藏、标记丢弃、标记喂完。
- 无网也能触发本地到期提醒。
- App 重启后计时器仍然正确。
- 设备重启后提醒可恢复。
- 网络恢复后再同步到后端。

后端负责：

- 账号身份
- 云备份
- 多设备同步
- Pro 权益验证
- 导出
- 删除
- 后台任务
- 未来家庭共享

#### 17.2.2 Firebase 做移动端基础设施，不承载全部业务

Firebase 用于：

- Firebase Auth
- Crashlytics
- Analytics
- Remote Config
- FCM，后期可用
- App Check，后期可用

不建议用 Firebase/Firestore 承载所有核心业务数据，因为本产品长期需要：

- SQL 查询
- 批量导出
- 订阅权益审计
- 数据删除和恢复
- 后台管理
- 多照护者权限
- 支持查询与客服处理
- 未来 Web Admin

这些更适合 PostgreSQL + API 服务。

#### 17.2.3 服务端接口从 MVP 起就存在

MVP 不是纯本地 App，也不是纯 Firebase App。

MVP 就要有：

- FastAPI 服务
- PostgreSQL
- Auth token 校验
- 用户初始化
- 基础同步接口
- 订阅校验接口
- 数据删除入口
- 健康检查
- 基础日志和监控

但 MVP 不做：

- 微服务
- Kubernetes
- 复杂后台管理系统
- 实时家庭协同
- 自建推送系统
- 高级数据仓库
- 复杂风控系统

---

### 17.3 推荐总体架构详细说明

### 17.3.1 架构总览

```text
┌─────────────────────────────────────────────────────────────┐
│ Android App                                                  │
│                                                             │
│ Kotlin + Compose                                            │
│ Room + DataStore                                            │
│ WorkManager + AlarmManager                                  │
│ Google Play Billing                                         │
│ AdMob                                                       │
│ Firebase SDK                                                │
└───────────────┬─────────────────────────────┬───────────────┘
                │                             │
                │ HTTPS REST API              │ Firebase SDK
                │                             │
                v                             v
┌───────────────────────────────┐   ┌─────────────────────────┐
│ FastAPI Backend                │   │ Firebase                 │
│                               │   │                         │
│ Auth verification              │   │ Auth                     │
│ Sync API                       │   │ Crashlytics              │
│ Billing verification           │   │ Analytics                │
│ Entitlement service            │   │ Remote Config            │
│ Export/delete API              │   │ FCM later                │
│ Admin/support API later        │   │ App Check later          │
└───────────────┬───────────────┘   └─────────────────────────┘
                │
                │ SQLAlchemy / SQLModel
                v
┌───────────────────────────────┐
│ PostgreSQL                     │
│                               │
│ users                          │
│ families                       │
│ babies                         │
│ bottles                        │
│ feed_logs                      │
│ diaper_logs                    │
│ sleep_logs                     │
│ entitlements                   │
│ sync metadata                  │
└───────────────────────────────┘

┌───────────────────────────────┐
│ External Services              │
│                               │
│ Google Play Developer API      │
│ Google Pub/Sub for RTDN later  │
│ Object Storage for exports     │
│ Sentry / Cloud Logging         │
└───────────────────────────────┘
```

---

### 17.3.2 Android App 层

Android App 是用户体验中心，负责所有核心交互和本地可靠性。

### 主要职责

1. UI 展示
   - Today
   - Bottle Timer
   - Logs
   - Insights
   - Settings
   - Paywall
   - Widget

2. 本地核心业务
   - 奶瓶计时状态机
   - 到期时间计算
   - 本地通知调度
   - 本地统计
   - 本地数据编辑

3. 本地数据
   - Room 保存核心记录
   - DataStore 保存设置
   - Sync Queue 保存待同步操作
   - Entitlement Cache 保存 Pro 状态

4. 网络同步
   - 登录后同步数据
   - 网络恢复后重试
   - 定时后台同步
   - 处理远端变更

5. 商业化
   - Google Play Billing
   - AdMob
   - Paywall
   - Pro gating

### 关键要求

- 任何奶瓶计时功能都不能等待 API 返回。
- 创建/修改记录采用 local write first。
- API 失败只影响云同步，不影响本地使用。
- Pro 权益本地缓存要有合理宽限期，避免临时网络问题锁死付费用户。

---

### 17.3.3 Firebase 层

Firebase 是移动端基础设施层，不是完整业务后端。

### Firebase Auth

用途：

- 匿名登录
- Google 登录
- 后端身份验证
- 用户跨设备身份
- 云备份身份基础

流程：

```text
Android 获取 Firebase ID Token
-> 请求 FastAPI
-> FastAPI 使用 Firebase Admin SDK 验证 token
-> FastAPI 提取 firebase uid
-> 映射到内部 users 表
```

### Crashlytics

用途：

- Android crash
- ANR
- 版本稳定性
- 发布灰度监控

### Analytics

用途：

- onboarding 漏斗
- bottle_created
- feed_logged
- paywall_viewed
- purchase_completed
- sync_failed

限制：

- 不记录宝宝真实姓名
- 不记录 note 内容
- 不记录医疗内容
- 不记录完整喂养明细作为事件参数

### Remote Config

可配置：

- Paywall 文案
- 广告开关
- 功能开关
- onboarding 文案
- A/B 测试分组
- 是否展示 lifetime

不可静默配置：

- 奶粉 2 小时规则
- 喂后 1 小时规则
- 冷藏 24 小时规则
- 医疗/安全判断
- 免责声明核心含义

### FCM

MVP 可不启用，V1.1/V1.2 用于：

- 多设备提醒
- 家庭共享变更提示
- 导出完成通知
- 订阅状态提醒，谨慎使用

---

### 17.3.4 FastAPI Backend 层

FastAPI 是业务后端核心。

### 为什么选 FastAPI

FastAPI 适合本产品的原因：

- Python 开发效率高
- 类型提示友好
- Pydantic 校验天然适合 API schema
- 自动生成 OpenAPI / Swagger 文档
- 与 AI 编程工具配合好
- 一人开发效率高
- 适合轻量业务 API
- 容器化部署简单
- 后期可接 Celery/RQ/Arq 做后台任务

### 后端主要职责

1. Auth
   - 校验 Firebase ID Token
   - 创建/维护内部 user
   - 处理匿名升级
   - 家庭空间权限校验

2. Sync
   - 接收客户端本地变更
   - 保存云端备份
   - 返回远端变更
   - 处理软删除
   - 处理冲突

3. Billing
   - 接收 purchase token
   - 调 Google Play Developer API 校验
   - 保存 entitlement
   - 提供权益查询
   - 后期处理 RTDN

4. Export
   - 创建导出任务
   - 生成 CSV/PDF
   - 返回下载链接

5. Deletion
   - 删除账号
   - 删除云端记录
   - 处理软删除和匿名化

6. Admin later
   - 查询用户
   - 查询订阅
   - 查看同步状态
   - 处理支持请求

### FastAPI 服务不做什么

- 不做倒计时 tick
- 不做本地通知
- 不做医疗判断
- 不做“奶是否安全”的判断
- 不直接控制广告展示
- 不保存无必要敏感信息
- 不在每次 UI 刷新时全量返回数据

---

### 17.3.5 PostgreSQL 层

PostgreSQL 是云端业务数据的长期存储。

### 为什么选 PostgreSQL

相比 Firestore，PostgreSQL 更适合：

- 导出 CSV/PDF
- 多表查询
- 权限关系
- 用户数据删除
- 订阅状态审计
- 后台管理
- 未来报表
- 数据迁移
- 成本可预测
- 避免文档读写放大

### 推荐托管

MVP 优先：

```text
Neon Postgres 或 Supabase Postgres
```

更稳生产：

```text
Cloud SQL for PostgreSQL
```

建议阶段：

- 0-5k MAU：Neon/Supabase 低价档
- 5k-50k MAU：Neon/Supabase Pro 或 Cloud SQL 小实例
- 50k+ MAU：Cloud SQL/Neon 高阶 + 更完善备份监控

---

### 17.3.6 Object Storage 层

MVP 可不强制使用。

V1.1 后用于：

- CSV 导出文件
- PDF 导出文件
- 临时下载链接
- 未来用户备份包

可选方案：

- Google Cloud Storage
- Supabase Storage
- Cloudflare R2

原则：

- 核心结构化记录不要存对象存储。
- 对象文件设置过期时间。
- 下载 URL 使用短期签名链接。
- 不公开暴露宝宝数据文件。

---

### 17.3.7 Google Play Billing 与订阅链路

### 客户端即时链路

```text
用户购买
-> Android Google Play Billing 返回 purchase
-> 客户端本地临时解锁 Pro
-> 客户端发送 purchaseToken 到 FastAPI
-> FastAPI 调 Google Play Developer API 校验
-> FastAPI 保存 entitlement
-> 客户端刷新最终 Pro 状态
```

### 服务端长期链路

```text
Google Play 订阅状态变化
-> RTDN/PubSub
-> FastAPI webhook 或 Cloud Function
-> FastAPI 再次查询 Google Play Developer API
-> 更新 entitlements 表
```

MVP 可以先做主动校验，RTDN 放 V1.1，但接口和表结构要预留。

---

### 17.3.8 同步链路

### 本地写入链路

```text
用户创建奶瓶
-> 写入 Room bottles
-> 写入 sync_queue
-> UI 立即更新
-> 本地通知立即调度
-> WorkManager 后台推送到 FastAPI
-> FastAPI 写入 PostgreSQL
-> 返回 sync success
-> 本地标记 synced
```

### 远端拉取链路

```text
App 启动/回前台/网络恢复
-> 调 GET /sync/changes?since=cursor
-> FastAPI 返回远端变化
-> 客户端合并到 Room
-> 更新 sync cursor
```

### 冲突处理

MVP 使用：

```text
client-generated id + updatedAt + deletedAt + last-write-wins
```

特殊规则：

- 活跃 bottle 的本地新状态不能被旧远端状态覆盖。
- 终态 Fed/Discarded/Canceled 优先级高于旧的 NotStarted/Refrigerated。
- 删除使用 deletedAt 软删除，避免旧设备复活数据。

---

### 17.4 技术栈明细

### 17.4.1 Android

```text
Language: Kotlin
UI: Jetpack Compose + Material 3
Architecture: MVVM + UseCase + Repository
DI: Hilt
Local DB: Room
Settings: DataStore
Async: Coroutines + Flow
Networking: Retrofit + OkHttp
Serialization: Kotlinx Serialization or Moshi
Background: WorkManager
Exact reminders: AlarmManager when justified
Widget: Glance or RemoteViews
Billing: Google Play Billing Library
Ads: AdMob
Analytics: Firebase Analytics
Crash: Firebase Crashlytics
Auth: Firebase Auth
Config: Firebase Remote Config
```

---

### 17.4.2 Backend

```text
Language: Python 3.12+
Framework: FastAPI
ASGI Server: Uvicorn
Validation: Pydantic v2
ORM: SQLAlchemy 2.0 or SQLModel
Migrations: Alembic
Database: PostgreSQL
Auth verification: Firebase Admin SDK
HTTP Client: httpx
Background jobs MVP: FastAPI BackgroundTasks / Cloud Tasks
Background jobs later: Celery / RQ / Arq
Testing: pytest
Lint/format: ruff
Type check: mypy or pyright
Dependency: uv or Poetry
Container: Docker
Deploy: Google Cloud Run
Monitoring: Sentry + Cloud Logging
```

### SQLAlchemy vs SQLModel

推荐：

```text
MVP 用 SQLAlchemy 2.0 + Pydantic schema
```

原因：

- 更成熟
- 资料多
- 迁移稳定
- 复杂查询可控

SQLModel 也可用，但 SQLAlchemy 原生更稳。

### uv vs Poetry

推荐：

```text
uv
```

原因：

- 快
- 现代 Python 项目体验好
- 适合 CI

如果你熟悉 Poetry，也可以用 Poetry。

---

### 17.4.3 Database

```text
PostgreSQL 15+
Migration: Alembic
Connection pooling: SQLAlchemy pool + Cloud Run careful tuning
IDs: UUID/ULID text
Timestamps: timestamptz
Soft delete: deleted_at
JSON fields: jsonb for provider raw status only
```

MVP 不需要 Redis。

什么时候加 Redis：

- API rate limit 要全局准确
- 后台任务队列变复杂
- 实时家庭共享增加
- 高频缓存需要

---

### 17.5 FastAPI 后端目录结构

推荐：

```text
backend/
  app/
    main.py
    core/
      config.py
      logging.py
      security.py
      errors.py
      clock.py
      ids.py
    db/
      session.py
      base.py
      migrations/
    models/
      user.py
      family.py
      baby.py
      bottle.py
      feed_log.py
      diaper_log.py
      sleep_log.py
      entitlement.py
      sync.py
    schemas/
      common.py
      user.py
      family.py
      baby.py
      bottle.py
      logs.py
      billing.py
      sync.py
      export.py
    api/
      deps.py
      router.py
      routes/
        health.py
        me.py
        sync.py
        billing.py
        export.py
        account.py
        admin.py
    services/
      auth_service.py
      user_service.py
      sync_service.py
      billing_service.py
      entitlement_service.py
      export_service.py
      deletion_service.py
    repositories/
      user_repository.py
      family_repository.py
      sync_repository.py
      entitlement_repository.py
    integrations/
      firebase_admin.py
      google_play.py
      storage.py
      email.py
    tests/
  alembic/
  Dockerfile
  pyproject.toml
  README.md
```

---

### 17.6 FastAPI 核心依赖设计

### 17.6.1 Auth Dependency

伪代码：

```python
async def get_current_user(
    authorization: str = Header(...),
    db: AsyncSession = Depends(get_db),
) -> CurrentUser:
    token = extract_bearer_token(authorization)
    decoded = firebase_admin.auth.verify_id_token(token)
    firebase_uid = decoded["uid"]

    user = await user_service.get_or_create_by_firebase_uid(
        db=db,
        firebase_uid=firebase_uid,
        email=decoded.get("email"),
    )

    return CurrentUser(
        user_id=user.id,
        firebase_uid=firebase_uid,
        default_family_id=user.default_family_id,
    )
```

### 要求

- 不信任客户端传来的 userId。
- 所有 userId 从 Firebase token 推导。
- 所有 familyId 都要服务端校验权限。
- token 校验失败返回 401。
- family 无权限返回 403。

---

### 17.6.2 Family Permission Dependency

伪代码：

```python
async def require_family_access(
    family_id: str,
    current_user: CurrentUser,
    db: AsyncSession,
) -> FamilyAccess:
    member = await family_repository.get_member(
        db=db,
        family_id=family_id,
        user_id=current_user.user_id,
    )
    if not member:
        raise ForbiddenError()
    return FamilyAccess(family_id=family_id, role=member.role)
```

MVP 只支持 OWNER，但代码结构预留 CAREGIVER/VIEWER。

---

### 17.7 API 设计

所有接口使用：

```text
/api/v1
```

统一响应错误：

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request.",
    "requestId": "req_abc"
  }
}
```

---

### 17.7.1 Health

```http
GET /health
```

返回：

```json
{
  "status": "ok",
  "version": "1.0.0",
  "time": "2026-05-29T12:00:00Z"
}
```

---

### 17.7.2 Me

```http
POST /api/v1/me/init
```

用途：

- 首次登录创建 user
- 创建 default family
- 返回后端内部 ID

返回：

```json
{
  "userId": "usr_01",
  "defaultFamilyId": "fam_01",
  "isNewUser": true
}
```

```http
GET /api/v1/me
```

返回：

```json
{
  "userId": "usr_01",
  "email": "user@example.com",
  "defaultFamilyId": "fam_01",
  "createdAt": "2026-05-29T12:00:00Z"
}
```

---

### 17.7.3 Sync Push

推荐 MVP 使用分实体同步接口，简单清晰。

```http
POST /api/v1/sync/babies
POST /api/v1/sync/bottles
POST /api/v1/sync/feed-logs
POST /api/v1/sync/diaper-logs
POST /api/v1/sync/sleep-logs
POST /api/v1/sync/settings
```

请求结构：

```json
{
  "familyId": "fam_01",
  "clientId": "android_abc",
  "changes": [
    {
      "id": "bottle_01",
      "updatedAt": "2026-05-29T12:00:00Z",
      "deletedAt": null
    }
  ]
}
```

返回结构：

```json
{
  "serverTime": "2026-05-29T12:00:02Z",
  "accepted": ["bottle_01"],
  "rejected": [],
  "conflicts": []
}
```

---

### 17.7.4 Sync Pull

```http
GET /api/v1/sync/changes?familyId=fam_01&since=2026-05-29T00:00:00Z
```

返回：

```json
{
  "serverTime": "2026-05-29T12:00:00Z",
  "babies": [],
  "bottles": [],
  "feedLogs": [],
  "diaperLogs": [],
  "sleepLogs": [],
  "settings": []
}
```

### MVP 注意

- 返回变更数量要限制，例如每类最多 500 条。
- 如果数据多，返回 nextCursor。
- 客户端循环拉取。
- 不要每次全量拉取。

---

### 17.7.5 Billing

提交购买：

```http
POST /api/v1/billing/google-play/purchases
```

请求：

```json
{
  "packageName": "com.nurtlina.app",
  "productId": "pro_yearly",
  "purchaseToken": "purchase_token"
}
```

返回：

```json
{
  "isPro": true,
  "plan": "YEARLY",
  "status": "ACTIVE",
  "expiresAt": "2027-05-29T00:00:00Z"
}
```

获取权益：

```http
GET /api/v1/entitlements/me
```

返回：

```json
{
  "isPro": true,
  "source": "GOOGLE_PLAY",
  "plan": "YEARLY",
  "status": "ACTIVE",
  "expiresAt": "2027-05-29T00:00:00Z",
  "gracePeriodUntil": null,
  "lastVerifiedAt": "2026-05-29T12:00:00Z"
}
```

RTDN webhook，V1.1：

```http
POST /api/v1/webhooks/google-play/rtdn
```

---

### 17.7.6 Export

MVP 可以先做客户端本地 CSV 导出。服务端导出建议 V1.1。

创建导出：

```http
POST /api/v1/exports
```

查询导出：

```http
GET /api/v1/exports/{exportId}
```

导出完成后返回短期签名 URL。

---

### 17.7.7 Account Deletion

```http
POST /api/v1/account/delete
```

行为：

- 验证当前用户
- 创建删除请求
- 软删除业务数据
- 异步清理云端数据
- 保留必要账务/反欺诈记录，注意最小化
- 返回删除状态

---

### 17.8 数据库设计

### 17.8.1 users

```sql
CREATE TABLE users (
  id TEXT PRIMARY KEY,
  firebase_uid TEXT UNIQUE NOT NULL,
  email TEXT,
  display_name TEXT,
  default_family_id TEXT,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  deleted_at TIMESTAMPTZ
);
```

---

### 17.8.2 families

```sql
CREATE TABLE families (
  id TEXT PRIMARY KEY,
  owner_user_id TEXT NOT NULL REFERENCES users(id),
  name TEXT,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  deleted_at TIMESTAMPTZ
);
```

---

### 17.8.3 family_members

```sql
CREATE TABLE family_members (
  id TEXT PRIMARY KEY,
  family_id TEXT NOT NULL REFERENCES families(id),
  user_id TEXT NOT NULL REFERENCES users(id),
  role TEXT NOT NULL,
  status TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  deleted_at TIMESTAMPTZ,
  UNIQUE(family_id, user_id)
);
```

---

### 17.8.4 babies

```sql
CREATE TABLE babies (
  id TEXT PRIMARY KEY,
  family_id TEXT NOT NULL REFERENCES families(id),
  name TEXT NOT NULL,
  birth_date DATE,
  avatar_color TEXT,
  client_id TEXT,
  schema_version INT NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_babies_family_updated
ON babies(family_id, updated_at);
```

---

### 17.8.5 bottles

```sql
CREATE TABLE bottles (
  id TEXT PRIMARY KEY,
  family_id TEXT NOT NULL REFERENCES families(id),
  baby_id TEXT NOT NULL REFERENCES babies(id),
  milk_type TEXT NOT NULL,
  amount_ml NUMERIC(8,2),
  prepared_at TIMESTAMPTZ NOT NULL,
  feeding_started_at TIMESTAMPTZ,
  refrigerated_at TIMESTAMPTZ,
  status TEXT NOT NULL,
  guideline_region TEXT NOT NULL,
  rule_version TEXT NOT NULL,
  expires_at TIMESTAMPTZ,
  discarded_at TIMESTAMPTZ,
  fed_at TIMESTAMPTZ,
  note TEXT,
  client_id TEXT,
  schema_version INT NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_bottles_family_updated
ON bottles(family_id, updated_at);

CREATE INDEX idx_bottles_baby_prepared
ON bottles(baby_id, prepared_at);
```

---

### 17.8.6 feed_logs

```sql
CREATE TABLE feed_logs (
  id TEXT PRIMARY KEY,
  family_id TEXT NOT NULL REFERENCES families(id),
  baby_id TEXT NOT NULL REFERENCES babies(id),
  bottle_id TEXT REFERENCES bottles(id),
  feed_type TEXT NOT NULL,
  amount_ml NUMERIC(8,2),
  started_at TIMESTAMPTZ NOT NULL,
  ended_at TIMESTAMPTZ,
  note TEXT,
  client_id TEXT,
  schema_version INT NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_feed_logs_baby_started
ON feed_logs(baby_id, started_at);
```

---

### 17.8.7 diaper_logs

```sql
CREATE TABLE diaper_logs (
  id TEXT PRIMARY KEY,
  family_id TEXT NOT NULL REFERENCES families(id),
  baby_id TEXT NOT NULL REFERENCES babies(id),
  diaper_type TEXT NOT NULL,
  changed_at TIMESTAMPTZ NOT NULL,
  note TEXT,
  client_id TEXT,
  schema_version INT NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_diaper_logs_baby_changed
ON diaper_logs(baby_id, changed_at);
```

---

### 17.8.8 sleep_logs

```sql
CREATE TABLE sleep_logs (
  id TEXT PRIMARY KEY,
  family_id TEXT NOT NULL REFERENCES families(id),
  baby_id TEXT NOT NULL REFERENCES babies(id),
  started_at TIMESTAMPTZ NOT NULL,
  ended_at TIMESTAMPTZ,
  note TEXT,
  client_id TEXT,
  schema_version INT NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_sleep_logs_baby_started
ON sleep_logs(baby_id, started_at);
```

---

### 17.8.9 entitlements

```sql
CREATE TABLE entitlements (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  source TEXT NOT NULL,
  product_id TEXT NOT NULL,
  purchase_token_hash TEXT,
  status TEXT NOT NULL,
  plan TEXT,
  expires_at TIMESTAMPTZ,
  grace_period_until TIMESTAMPTZ,
  last_verified_at TIMESTAMPTZ,
  raw_provider_status JSONB,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_entitlements_user
ON entitlements(user_id);
```

---

### 17.8.10 sync_cursors

```sql
CREATE TABLE sync_cursors (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  family_id TEXT NOT NULL REFERENCES families(id),
  client_id TEXT NOT NULL,
  last_pull_at TIMESTAMPTZ,
  last_push_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  UNIQUE(user_id, family_id, client_id)
);
```

---

### 17.9 同步策略

### 17.9.1 Client-generated ID

所有核心记录由客户端生成 ID。

推荐：

```text
ULID 或 UUIDv7
```

原因：

- 离线创建无需等服务端
- 排序友好
- 同步简单
- 避免本地临时 ID 到远端 ID 的映射复杂度

---

### 17.9.2 Local Write First

所有用户操作：

```text
先写 Room
再写 sync_queue
再异步调用 API
```

好处：

- UI 立即响应
- 离线可用
- 不丢操作
- 用户体验稳定

---

### 17.9.3 Sync Queue

Room 表：

```text
sync_queue
- id
- entity_type
- entity_id
- operation
- payload_json
- created_at
- retry_count
- next_retry_at
- last_error
```

重试策略：

- 1 分钟
- 5 分钟
- 15 分钟
- 60 分钟
- 每 6 小时

避免无限高频重试。

---

### 17.9.4 Pull Cursor

每个 client 保存：

```text
family_id
client_id
last_successful_pull_at
```

服务端返回：

```text
serverTime
nextCursor
hasMore
```

避免用客户端时间作为唯一同步依据。

---

### 17.9.5 冲突处理

MVP：

```text
last-write-wins
```

但 bottle 状态增加保护规则：

状态优先级：

```text
Canceled / Discarded / Fed
> Expired
> FeedingStarted
> Refrigerated
> NotStarted
```

如果远端状态更旧，不覆盖本地。

---

### 17.10 Google Play 订阅校验

### 17.10.1 为什么必须服务端校验

客户端购买只能用于即时体验，服务端校验用于：

- 防篡改
- 恢复购买
- 换设备
- 宽限期
- 取消/过期
- 退款
- 客服处理
- 未来 Web Admin

---

### 17.10.2 MVP 流程

```text
Android Billing 成功
-> 本地临时 Pro
-> POST /billing/google-play/purchases
-> FastAPI 校验 purchaseToken
-> 更新 entitlements
-> 返回最终 Pro 状态
-> Android 缓存 entitlement
```

### 临时 Pro

如果服务端短暂失败：

- 可以短暂保留本地购买状态
- 后台继续重试校验
- 不要立即让用户觉得购买失败
- 明确显示“purchase is being verified”一类温和文案

---

### 17.10.3 RTDN V1.1

> **当前状态**：RTDN 已提前实现（原计划 V1.1）。`playBillingWebhook` Cloud Function 已通过 Pub/Sub topic `play-billing-notifications` 接收 Google Play 实时通知，并写入 Firestore `entitlements/{userId}`。

```text
Google Play
-> Pub/Sub (topic: play-billing-notifications)
-> playBillingWebhook Cloud Function [已实现 ✅]
-> 更新 Firestore entitlements/{userId}
```

FastAPI 路线仍需在 V1.1 预留 webhook endpoint 和 entitlements 表字段。

---

### 17.11 部署方案

### 17.11.1 推荐部署

```text
FastAPI -> Docker -> Google Cloud Run
PostgreSQL -> Neon/Supabase/Cloud SQL
Secrets -> Google Secret Manager
Logs -> Cloud Logging
Errors -> Sentry
CI/CD -> GitHub Actions
```

### 为什么 Cloud Run

- 容器化
- 支持自动扩缩
- 小流量成本低
- 不需要管理服务器
- 与 Google/Firebase/Play 生态顺
- 适合 FastAPI
- 后期可设置 min instances 改善冷启动

---

### 17.11.2 Dockerfile 示例

```dockerfile
FROM python:3.12-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

COPY pyproject.toml uv.lock ./
RUN pip install uv && uv sync --frozen --no-dev

COPY app ./app
COPY alembic ./alembic
COPY alembic.ini ./alembic.ini

CMD ["uv", "run", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8080"]
```

---

### 17.11.3 Cloud Run 配置建议

MVP：

```text
CPU: 1
Memory: 512MiB
Min instances: 0
Max instances: 5
Concurrency: 40-80
Timeout: 30s
```

首发后：

```text
Min instances: 1
Max instances: 20
Memory: 512MiB-1GiB
```

注意：

- min instances = 0 更省钱，但有冷启动。
- 订阅收入起来后，设置 min instances = 1 改善体验。

---

### 17.12 成本方案

### 17.12.1 MVP：0-5,000 MAU

```text
Cloud Run: scale to zero
Database: Neon Free/低价 或 Supabase Free/Pro
Firebase: Spark/Blaze low usage
Sentry: Free
Object Storage: none or minimal
```

估算：

```text
$0-$50/月
```

---

### 17.12.2 5,000-50,000 MAU

```text
Cloud Run: min 0 或 1
Database: Neon/Supabase paid tier 或 Cloud SQL small
Object Storage: exports
Sentry: Free/Paid
Cloud Scheduler: small
```

估算：

```text
$35-$200/月
```

---

### 17.12.3 50,000-300,000 MAU

```text
Cloud Run: min 1+
Database: higher tier with backup
Cloud Tasks / PubSub
Admin dashboard
Sentry paid
```

估算：

```text
$200-$1,000+/月
```

成本控制关键：

- 不同步倒计时 tick。
- 不全量拉取所有历史。
- 不高频 realtime listener。
- 不把大文件放数据库。
- 不让广告/统计 SDK 采集过多无用事件。
- 导出任务限制频率。
- Sync 批量提交。

---

### 17.13 安全方案

### API

- HTTPS only
- Firebase ID Token 校验
- request id
- structured logging
- rate limit
- schema validation
- 权限校验 familyId
- 不信任客户端 userId
- 不记录 token/purchase token
- 生产和测试环境隔离

### Database

- 最小权限账号
- migration 管理
- 定期备份
- 软删除
- PII 字段清单
- 慢查询监控

### Privacy

- 不强制宝宝真实姓名
- 不请求位置/联系人/相机/麦克风
- Notes 不进 analytics
- Baby care records 不用于广告定向
- 支持删除账号/云端数据
- Data Safety 如实披露

---

### 17.14 监控与告警

Android：

- Crash-free users
- ANR
- Notification permission grant rate
- bottle_created
- bottle_expired
- sync_failed
- purchase_completed
- purchase_failed

Backend：

- API p50/p95/p99 latency
- 5xx rate
- DB connection count
- DB slow query
- sync error rate
- billing verification error rate
- export failure rate
- account deletion failure

告警：

- API 5xx 过高
- billing verification 连续失败
- DB connection exhausted
- Cloud Run deploy failed
- Sentry error spike
- 日成本异常

---

### 17.15 CI/CD

### Android

GitHub Actions：

- lint
- unit test
- assembleDebug
- assembleRelease
- Firebase App Distribution，可选
- Google Play internal testing，可后期加 Fastlane

### Backend

GitHub Actions：

- ruff
- mypy/pyright
- pytest
- alembic migration check
- docker build
- deploy staging
- manual approval
- deploy production

### DB Migration

使用 Alembic。

规则：

- migration 文件必须进 Git。
- 不手改生产库。
- 先 staging 后 production。
- 删除字段分阶段。
- 关键表加索引前评估锁表影响。

---

### 17.16 环境划分

```text
local
staging
production
```

### local

- Android debug
- FastAPI local
- Docker Postgres
- Firebase dev project
- AdMob test ads

### staging

- Cloud Run staging
- Staging Postgres
- Firebase staging
- Google Play internal testing
- 测试订阅商品

### production

- Cloud Run production
- Production Postgres
- Firebase production
- Real AdMob
- Real Billing

规则：

- debug 包不连 production。
- staging 和 production 数据库隔离。
- secrets 不进 Git。
- API base URL 通过 build flavor 区分。

---

### 17.17 阶段实施计划

## Phase 1：基础工程，1 周

Android：（尚未开始）

- Compose 工程
- Room
- DataStore
- Hilt
- Navigation
- Crashlytics
- Analytics

工程脚手架（已完成）：

- `build.gradle.kts`、`libs.versions.toml`、依赖声明
- `AndroidManifest.xml`、权限、Receiver 注册
- 多语言字符串资源（EN/ES/DE/FR/ZH-CN）
- GitHub Actions CI（unit test + build）

Backend（FastAPI 路线，尚未开始）：

- FastAPI 工程
- PostgreSQL
- Alembic
- Docker
- `/health`
- Cloud Run staging

Firebase 后端（已完成，当前实际采用）：

- Firestore Security Rules ✅
- Cloud Functions 工程 ✅
- `provisionFamily`（等价于 `/me/init`）✅
- `deleteUserData` ✅
- `cleanupDeletedRecords` 定时清理 ✅

---

## Phase 2：Auth + 用户初始化，1 周

Android：（尚未开始）

- Firebase Anonymous Auth
- Google Sign-In 预留
- Token interceptor
- clientId

Backend（FastAPI 路线，尚未开始）：

- Firebase Admin SDK
- users
- families
- family_members
- `/me/init`
- `/me`

Firebase 后端（已完成，当前实际采用）：

- `provisionFamily` Cloud Function ✅（创建 family + owner member）
- `getEntitlement` Cloud Function ✅
- `registerPurchaseToken` Cloud Function ✅
- `playBillingWebhook` RTDN Pub/Sub ✅

---

## Phase 3：Bottle Timer 本地核心，2 周

Android：

- Baby/Bottle Room
- 状态机
- Guideline rules
- Expiry calculation
- 通知调度
- 设备重启恢复
- Today active bottle

Backend：

- babies/bottles schema
- sync babies
- sync bottles
- pull changes

---

## Phase 4：Feed/Diaper/Sleep + Sync，1.5 周

Android：

- Feed logs
- Diaper logs
- Sleep logs
- Logs timeline
- Today summary
- Sync queue

Backend：

- feed/diaper/sleep schema
- sync endpoints
- conflict handling

---

## Phase 5：Billing + Entitlement，1.5 周

Android：（尚未开始）

- Google Play Billing
- Paywall
- Restore purchase
- Local entitlement cache

Backend（FastAPI 路线，尚未开始）：

- Google Play Developer API verification
- entitlements table
- `/billing/google-play/purchases`
- `/entitlements/me`

Firebase 后端（已完成，当前实际采用）：

- `registerPurchaseToken` ✅
- `getEntitlement` ✅
- `playBillingWebhook`（RTDN Pub/Sub）✅
- `entitlements/{userId}` Firestore 文档 ✅

---

## Phase 6：Ads + Pro Gating，1 周

Android：

- AdMob
- Pro no ads
- multi-baby gating
- export gating
- paywall triggers

Backend：

- entitlement hardening
- purchase retry handling

---

## Phase 7：Export/Delete/Privacy，1 周

Android：（尚未开始）

- CSV local export
- delete account entry
- privacy settings

Backend（FastAPI 路线，尚未开始）：

- account delete
- export endpoint optional
- deletion job

Firebase 后端（已完成，当前实际采用）：

- `deleteUserData` Cloud Function ✅（删除家庭全部子集合，撤销 Auth token）
- `cleanupDeletedRecords` 定时清理 ✅（每 24 小时，90 天软删除记录）

---

## Phase 8：Widget/Night Mode/Localization，2 周

Android：

- widget
- night mode
- accessibility
- EN/ES/ZH/DE/FR
- FAQ
- safety sources

Backend：

- no major change

---

## Phase 9：Testing + Launch，1-2 周

- sync tests
- billing tests
- notification tests
- backend integration tests
- security tests
- closed testing
- production rollout

---

### 17.18 最终推荐组合

长期推荐（本文档主体方案）：

```text
Android:
Kotlin + Compose + Room + DataStore + WorkManager + AlarmManager

Firebase:
Auth + Crashlytics + Analytics + Remote Config

Backend:
Python 3.12 + FastAPI + Pydantic v2 + SQLAlchemy 2.0 + Alembic

Database:
Neon Postgres or Supabase Postgres for MVP
Cloud SQL PostgreSQL when revenue and scale justify

Deploy:
Google Cloud Run + Docker + GitHub Actions

Billing:
Google Play Billing + FastAPI server-side verification

Monitoring:
Firebase Crashlytics for Android
Sentry + Cloud Logging for backend

Storage:
Object storage only for export files
```

**当前实际已采用（Firebase-first）：**

```text
Android:
工程脚手架 ✅（Application ID: com.nurtlina.app）
Kotlin 源代码：尚未开始

Firebase:
Auth（依赖已声明，未初始化）
Crashlytics（依赖已声明，未初始化）
Analytics（依赖已声明，未初始化）
Firestore（Security Rules ✅，作为业务数据存储）
Cloud Functions（TypeScript，已实现：RTDN、entitlement、family provisioning、data deletion、scheduled cleanup）✅

Backend:
FastAPI 后端：尚未创建

CI/CD:
GitHub Actions（unit test + build debug + build release AAB）✅
```

这套方案比纯 Firebase 更适合长期业务化，比一开始全自建更省钱，比 Ktor 方案对 AI 辅助开发和后端迭代更友好。

最终判断：

> 对 Nurtlina 这种 Android-only、一人公司、订阅工具型 App，**FastAPI + PostgreSQL + Firebase 移动基础设施 + Cloud Run** 是生产级性价比最高的后端路线之一。若继续 Firebase-first，现有 Cloud Functions 已覆盖核心后端逻辑，下一步重心应放在 Android Kotlin 源代码的实现上。
