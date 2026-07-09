# Nurtlina 项目评估、PRD 与后续任务规划

> 评估日期：2026-07-09
> 项目代号：Nurtlina — Baby Feeding Timer
> 当前版本：v1.0.0-dev（内测前）
> 评估范围：全仓库代码、前后端架构、业务逻辑、测试覆盖、部署就绪度

---

## 目录

1. [项目概要](#1-项目概要)
2. [当前实施状态评估](#2-当前实施状态评估)
3. [PRD 概述](#3-prd-概述)
4. [架构现状](#4-架构现状)
5. [功能完成度矩阵](#5-功能完成度矩阵)
6. [技术债务与风险](#6-技术债务与风险)
7. [后续任务规划](#7-后续任务规划)
8. [里程碑与时间线](#8-里程碑与时间线)
9. [附录：文件清单](#9-附录文件清单)

---

## 1. 项目概要

### 1.1 产品定位

**Nurtlina** 是一款面向海外新手父母的 Android 婴儿喂养计时与护理记录工具。核心差异化在于 **奶瓶新鲜度计时器（Bottle Freshness Timer）**——帮助用户追踪每一瓶配方奶/母乳的冲调时间、是否已开始喂、剩余有效期，并通过清晰提醒降低"忘记这瓶奶什么时候冲的"的焦虑。

### 1.2 一句话定义

> 一个简单、可信、无压力的婴儿配方奶计时器和护理记录 App。

### 1.3 商业模式

| 层级 | 价格 | 权益 |
|---|---|---|
| Free（广告支持） | $0 | 1 个宝宝、基础计时/记录/统计、默认小组件、广告 |
| Pro Monthly | $2.99/月 | 无广告、多宝宝、全部历史、高级统计、导出、备份、自定义规则、小组件主题 |
| Pro Yearly | $19.99/年 | 同上 |
| Pro Lifetime | $29.99 | 同上，一次性买断 |

### 1.4 目标市场

- 首发地区：美国、英国、加拿大、澳大利亚、新西兰
- 首发语言：English、Spanish、Simplified Chinese、German、French
- 平台：Android-only（minSdk 26, targetSdk 35）
- 分发渠道：Google Play

### 1.5 核心差异化

1. **Bottle Freshness Timer 状态机**：精确区分 NotStarted / FeedingStarted / Refrigerated / Expired / Fed / Discarded 状态
2. **权威指南引用**：基于 CDC/AAP/NHS 公开指南的保存时间规则，但不声称医学背书
3. **夜间友好**：暗色模式、大按钮、单手操作、夜间不弹广告
4. **本地优先**：核心计时离线可用，网络恢复后异步同步
5. **多语言首发**：5 种语言同步上线

---

## 2. 当前实施状态评估

### 2.1 总体评估

**项目已进入 MVP 后端集成中期，代码基础扎实，核心业务逻辑已基本实现，距离内测发布还需 4-6 周的闭环工作。**

| 维度 | 完成度 | 状态 |
|---|---|---|
| Android 数据层 (Room/Repository) | 90% | ✅ 基本完成 |
| Android 领域层 (UseCase/StateMachine) | 85% | ✅ 核心逻辑完成 |
| Android UI 层 (Compose Screens) | 75% | ⚠️ 需打磨 |
| Android 通知系统 | 85% | ✅ 基本完成 |
| Android 同步系统 | 80% | ⚠️ 需清理旧路径 |
| Android 商业化 (Billing/Ads) | 70% | ⚠️ RevenueCat 已集成，AdMob 未实现 |
| Android 小组件 | 20% | ❌ 未开始 |
| FastAPI 后端 | 80% | ✅ 核心 API 完成 |
| Firebase Cloud Functions | 90% | ✅ 已完成 |
| 测试覆盖 | 50% | ⚠️ 关键规则已测，闭环不足 |
| 多语言资源 | 60% | ⚠️ 字符串已定义，UI 文案未全覆盖 |
| CI/CD | 70% | ⚠️ Android CI 完成，后端部署未自动化 |
| 上架素材 | 30% | ❌ 缺少图标、截图、Feature Graphic |

### 2.2 已完成的核心能力

#### Android 端（131 个 Kotlin 文件）

**数据层 (data/)**
- Room 数据库：`NurtlinaDatabase` — Baby, Bottle, FeedLog, DiaperLog, SleepLog, SyncQueue 全部实体和 DAO
- 本地 Repository：`RoomBabyRepository`, `RoomBottleRepository`, `RoomFeedLogRepository`, `RoomDiaperLogRepository`, `RoomSleepLogRepository`
- DataStore：`DataStoreSettingsRepository`, `DataStoreSessionRepository`
- 同步队列：`SyncQueueWriter`, `SyncQueueProcessor`, `SyncWorker`, `WorkManagerSyncManager`
- 远程 API：`BackendApiService` (Retrofit), `AuthTokenInterceptor`, `FirebaseAuthSource`, `FirestoreSource`
- 后端同步：`ApiSyncRepository`, `ApiBackendRepository`
- 权益管理：`EntitlementManager`, `EntitlementCache`, `EntitlementCacheRepository`
- 启动协调：`AppStartupCoordinator`
- 评分提示：`DataStoreRatingPromptRepository`

**领域层 (domain/)**
- 数据模型：Baby, Bottle, FeedLog, DiaperLog, SleepLog, UserSettings, TodaySummary, SessionInfo, AuthState, SyncState, UserAccount
- 枚举：MilkType, BottleStatus, FeedType, DiaperType, GuidelineRegion, UnitType
- 指南规则：`GuidelineRule`, `ExpiryCalculator`, `BottleStateMachine`
- 用例：`CreateBottleUseCase`, `TransitionBottleUseCase`, `ObserveBottlesUseCase`, `CheckAndExpireBottlesUseCase`, `GetTodaySummaryUseCase`, `LogFeedUseCase`, `LogDiaperUseCase`, `SleepUseCase`, `ManageBabyUseCase`
- 评分系统：`RatingPromptEligibility`, `RatingPromptDecision`, `RatingPromptState`

**UI 层 (ui/)**
- 主题：Material 3 浅色/深色主题、自定义色彩、字体
- 导航：`NurtlinaNavHost` — Onboarding → Main(Today/Logs/Insights/Settings) + BottleDetail + Paywall + SignIn
- 页面：`OnboardingScreen`, `TodayScreen`, `BottleDetailScreen`, `NewBottleSheet`, `LogsScreen`, `LogEditSheet`, `InsightsScreen`, `SettingsScreen`, `PaywallScreen`, `SignInScreen`
- 通用组件：`NurtlinaDialog`

**通知系统 (core/notification/)**
- `BottleNotificationScheduler`：到期前提醒、到期提醒
- `NextFeedNotificationScheduler`：喂奶间隔提醒
- `NotificationReceiver`：通知点击处理
- `BootReceiver`：设备重启恢复提醒
- `RescheduleNotificationsWorker`：WorkManager 定期重调度

#### FastAPI 后端（约 30 个 Python 文件）

**API 路由**
- `GET /health` — 健康检查
- `POST /api/v1/me/init` — 用户初始化、创建默认家庭
- `GET /api/v1/me` — 当前用户信息
- `POST /api/v1/sync/babies|bottles|feed-logs|diaper-logs|sleep-logs` — 分实体同步推送
- `GET /api/v1/sync/changes?familyId=&since=` — 拉取远端变更
- `POST /api/v1/billing/google-play/purchases` — Google Play 购买验证
- `GET /api/v1/entitlements/me` — Pro 权益查询
- `POST /api/v1/exports` — CSV/JSON 导出
- `POST /api/v1/account/delete` — 账号删除

**数据模型**
- PostgreSQL + SQLAlchemy 2.0 + Alembic 迁移（2 个版本）
- 表：users, families, family_members, babies, bottles, feed_logs, diaper_logs, sleep_logs, entitlements, sync_cursors

**服务层**
- `user_service`：用户创建/查询
- `sync_service`：同步推送/拉取、冲突处理
- `billing_service`：Google Play Developer API 验证
- `export_service`：CSV/JSON 生成
- `deletion_service`：账号软删除

#### Firebase 层

- Firestore Security Rules：完整覆盖所有集合，含 `isNotStale()` 防旧数据覆盖
- Cloud Functions：RTDN webhook、entitlement 查询、family provisioning、data deletion、定时清理
- Firebase 项目配置文件齐全

### 2.3 尚未完成的工作

1. **小组件 (Widget)**：完全未实现
2. **AdMob 集成**：依赖已声明，代码未实现
3. **App Icon**：当前使用默认占位图标
4. **夜间模式专项**：深色主题基础存在，但夜间专用大按钮/简化界面未实现
5. **Firebase 配置替换**：需替换真实 `google-services.json`
6. **Google Play 内购商品**：需在 Play Console 创建
7. **AdMob ID 替换**：需替换真实广告单元 ID
8. **多语言完整覆盖**：字符串资源已创建，但代码中仍有硬编码英文
9. **后端 Pull Cursor**：同步拉取的 cursor 机制需完善
10. **旧 Firestore 同步路径清理**：存在双同步路径残留
11. **后端测试覆盖**：仅基础健康检查和少量单元测试
12. **Google Play 上架素材**：截图、Feature Graphic、图标
13. **后端 CI/CD 自动化部署**：Dockerfile 存在，GitHub Actions 部署未配置

---

## 3. PRD 概述

### 3.1 核心用户旅程

#### 旅程 A：首次使用 → 创建第一瓶奶
1. 打开 App → 欢迎页
2. 选择语言（默认跟随系统）
3. 创建宝宝：昵称、出生日期、单位偏好（ml/oz）
4. 选择指南地区（US/UK/Custom）
5. 查看免责声明
6. 授予通知权限
7. 进入首页 → 点击"+ New Bottle"
8. 选择奶类型（Formula/Breast Milk）、输入奶量 → 创建
9. 首页显示活跃奶瓶卡片：类型、奶量、状态、倒计时

#### 旅程 B：夜间喂奶
1. 打开 App（自动暗色模式）
2. 看到活跃奶瓶卡片 → 点击"Start Feeding"
3. 喂奶中倒计时（1 小时到期）
4. 喂完后点击"Mark as Fed" → 自动生成喂奶记录
5. 或选择快捷记录奶量

#### 旅程 C：多照护者场景（V1.2+）
1. 主账号创建家庭
2. 邀请配偶/祖父母/保姆
3. 所有人看到同一瓶奶的状态
4. 谁喂的、什么时候喂的——记录清晰

### 3.2 信息架构

```
Nurtlina App
├── Onboarding (首次)
│   ├── Welcome
│   ├── Create Baby
│   ├── Unit & Guideline Selection
│   ├── Disclaimer
│   └── Notification Permission
├── Main (底部 4 Tab)
│   ├── Today (首页)
│   │   ├── Baby Switcher
│   │   ├── Active Bottle Card
│   │   ├── + New Bottle
│   │   ├── Quick Actions: Feed / Diaper / Sleep
│   │   └── Today Summary
│   ├── Logs (记录)
│   │   ├── Timeline (可按类型筛选)
│   │   ├── Date Picker
│   │   └── Edit/Delete Entry
│   ├── Insights (统计)
│   │   ├── Today Stats (Free)
│   │   └── 7/14/30 Day Trends (Pro)
│   └── Settings (设置)
│       ├── Baby Profile
│       ├── Units & Guidelines
│       ├── Notifications
│       ├── Night Mode
│       ├── Language
│       ├── Backup & Export
│       ├── Pro Subscription
│       ├── FAQ & Safety Sources
│       ├── Privacy Policy & Terms
│       └── Contact Support
├── Bottle Detail
├── Paywall (多触发点)
└── Sign In
```

### 3.3 数据模型（核心实体）

```
Baby (id, familyId, name, birthDate, avatarColor)
Bottle (id, babyId, milkType, amountMl, preparedAt, feedingStartedAt,
        refrigeratedAt, status, guidelineRegion, expiresAt, discardedAt,
        fedAt, note, clientId, schemaVersion, createdAt, updatedAt, deletedAt)
FeedLog (id, babyId, bottleId?, feedType, amountMl, startedAt, endedAt, note)
DiaperLog (id, babyId, diaperType, changedAt, note)
SleepLog (id, babyId, startedAt, endedAt, note)
UserSettings (id, language, unit, guidelineRegion, notificationEnabled, ...)
Entitlement (userId, source, productId, status, plan, expiresAt, ...)
```

### 3.4 奶瓶状态机

```
NotStarted ──→ FeedingStarted ──→ Fed (terminal)
    │               │
    │               ├──→ Discarded (terminal)
    │               └──→ Expired ──→ Discarded (terminal)
    │
    ├──→ Refrigerated ──→ FeedingStarted ──→ ...
    │       │
    │       ├──→ Discarded (terminal)
    │       └──→ Expired ──→ Discarded (terminal)
    │
    ├──→ Discarded (terminal)
    ├──→ Canceled (terminal)
    └──→ Expired ──→ Discarded (terminal)
```

### 3.5 计时规则（默认）

| 奶类型 | 状态 | 到期规则 |
|---|---|---|
| Formula | NotStarted, Room Temp | prepared_at + 2 hours |
| Formula | Feeding Started | feeding_started_at + 1 hour |
| Formula | Refrigerated | prepared_at + 24 hours |
| Breast Milk | Fresh, Room Temp | expressed_at + 4 hours |
| Breast Milk | Refrigerated | expressed_at + 4 days |
| Custom (Pro) | 用户自定义 | 用户设定各阶段时间 |

---

## 4. 架构现状

### 4.1 总体架构

```
┌─────────────────────────────────────────────────────────────┐
│ Android App (Kotlin + Compose)                              │
│                                                             │
│ UI Layer: Compose Screens + ViewModels                      │
│ Domain Layer: UseCases + StateMachine + ExpiryCalculator    │
│ Data Layer: Room + DataStore + Retrofit + Sync Queue        │
│                                                             │
│ Local-first: 所有写入先写 Room，异步同步到后端               │
└───────────────┬─────────────────────────────┬───────────────┘
                │                             │
                │ REST API (Retrofit)         │ Firebase SDK
                │                             │
                v                             v
┌───────────────────────────────┐   ┌─────────────────────────┐
│ FastAPI Backend (Python 3.12) │   │ Firebase                 │
│                               │   │                         │
│ Auth (Firebase Admin SDK)     │   │ Auth                     │
│ Sync API (push/pull)          │   │ Crashlytics              │
│ Billing Verification          │   │ Analytics                │
│ Entitlement Service           │   │ Remote Config            │
│ Export Service                │   │ Cloud Functions          │
│ Deletion Service              │   │ Firestore (Rules)        │
│                               │   │                         │
│ PostgreSQL (SQLAlchemy 2.0)   │   │                         │
│ Alembic Migrations            │   │                         │
└───────────────────────────────┘   └─────────────────────────┘
```

### 4.2 同步策略

- **客户端生成 ID**：ULID/UUIDv7，离线创建无需等服务端
- **本地优先写入**：先 Room → 再 SyncQueue → 异步 API
- **拉取游标**：`GET /sync/changes?since=<cursor>`
- **冲突处理**：last-write-wins + 奶瓶状态优先级（Fed/Discarded > Expired > FeedingStarted > Refrigerated > NotStarted）
- **软删除**：deletedAt 标记，不物理删除

### 4.3 技术栈明细

| 层级 | 技术选型 |
|---|---|
| Android UI | Jetpack Compose + Material 3 |
| Android DI | Hilt |
| Android 本地 DB | Room |
| Android 设置 | DataStore |
| Android 后台 | WorkManager + AlarmManager |
| Android 网络 | Retrofit + OkHttp + Kotlinx Serialization |
| Android 推送 | Firebase Cloud Messaging (FCM) |
| Android 内购 | RevenueCat (已集成) |
| Android 广告 | Google AdMob (依赖已声明) |
| 后端框架 | FastAPI + Uvicorn |
| 后端 ORM | SQLAlchemy 2.0 (async) |
| 后端数据校验 | Pydantic v2 |
| 数据库 | PostgreSQL 15+ |
| 数据库迁移 | Alembic |
| 容器化 | Docker + Google Cloud Run |
| CI/CD | GitHub Actions |
| 崩溃上报 | Firebase Crashlytics |
| 分析 | Firebase Analytics |

---

## 5. 功能完成度矩阵

### 5.1 MVP 必须功能

| 功能 | 前端 | 后端 | 状态 | 备注 |
|---|---|---|---|---|
| Onboarding (创建宝宝) | ✅ | ✅ | 90% | 导航流程完整 |
| 奶瓶计时器 (创建) | ✅ | ✅ | 90% | NewBottleSheet + CreateBottleUseCase |
| 奶瓶状态流转 | ✅ | ✅ | 90% | BottleStateMachine + TransitionBottleUseCase |
| 到期时间计算 | ✅ | N/A | 95% | ExpiryCalculator，规则完整 |
| 活跃奶瓶卡片 | ✅ | N/A | 85% | TodayScreen 中展示 |
| 本地通知（到期提醒） | ✅ | N/A | 85% | BottleNotificationScheduler |
| 通知点击跳转 | ✅ | N/A | 80% | NotificationReceiver |
| 设备重启恢复提醒 | ✅ | N/A | 80% | BootReceiver + RescheduleNotificationsWorker |
| 喂奶记录 (CRUD) | ✅ | ✅ | 85% | FeedLog 完整 |
| 尿布记录 (CRUD) | ✅ | ✅ | 85% | DiaperLog 完整 |
| 睡眠记录 (CRUD) | ✅ | ✅ | 85% | SleepLog 完整 |
| Logs 时间线 | ✅ | N/A | 80% | LogsScreen + 类型筛选 |
| 今日统计摘要 | ✅ | N/A | 85% | GetTodaySummaryUseCase |
| 设置页 | ✅ | ✅ | 80% | SettingsScreen + ViewModel |
| 免责声明 | ✅ | N/A | 70% | 各页面有，需统一审核 |
| 指南来源页 | ⚠️ | N/A | 50% | 规则类有，UI 展示不完整 |
| Pro Paywall | ✅ | ✅ | 80% | PaywallScreen + RevenueCat |
| RevenueCat 内购 | ✅ | N/A | 85% | 已集成 |
| 权益管理 | ✅ | ✅ | 80% | EntitlementManager + 后端验证 |
| 多语言 (5 种) | ⚠️ | N/A | 60% | 字符串资源已定义，代码中有硬编码 |
| 暗色模式 | ✅ | N/A | 80% | Material 3 主题支持 |
| Firebase Auth | ✅ | ✅ | 80% | 匿名登录 + 后端 token 校验 |
| 数据同步 | ✅ | ✅ | 75% | 推送/拉取可用，cursor 需完善 |
| 账号删除 | ✅ | ✅ | 85% | 前端入口 + 后端软删除 |
| CSV 导出 | ⚠️ | ✅ | 70% | 后端可用，前端入口待完善 |
| Google Play Billing | ✅ | ✅ | 80% | RevenueCat + 后端验证 |

### 5.2 MVP Should Have

| 功能 | 前端 | 后端 | 状态 | 备注 |
|---|---|---|---|---|
| 桌面小组件 | ❌ | N/A | 0% | 完全未实现 |
| 夜间模式（大按钮简化） | ❌ | N/A | 0% | 深色主题有，简化界面无 |
| 30 天统计 | ⚠️ | N/A | 40% | InsightsScreen 有框架，数据不全 |
| 自定义提醒 | ⚠️ | N/A | 30% | FeedReminderConfig 存在，设置 UI 不全 |
| 评分提示 | ✅ | N/A | 90% | RatingPromptEligibility 完整 |

### 5.3 商业化

| 功能 | 状态 | 备注 |
|---|---|---|
| RevenueCat 集成 | ✅ 85% | SDK 已集成，Paywall 展示正常 |
| Google Play Billing | ✅ 80% | 通过 RevenueCat + 后端 RTDN |
| AdMob Banner | ❌ 0% | 依赖已声明，未实现 |
| Pro 权益门控 | ⚠️ 60% | 多宝宝限制有，各处门控不统一 |
| 去广告 (Pro) | ❌ 0% | 依赖 AdMob 集成 |
| Lifetime 购买 | ✅ 80% | RevenueCat 配置支持 |

### 5.4 合规与上架

| 功能 | 状态 | 备注 |
|---|---|---|
| Privacy Policy | ❌ 0% | 需要创建 |
| Terms of Service | ❌ 0% | 需要创建 |
| Data Safety 表单 | ❌ 0% | 需要填写 |
| App Icon | ❌ 0% | 当前为默认占位 |
| Feature Graphic | ❌ 0% | 未制作 |
| Screenshots (8 张) | ❌ 0% | 未制作 |
| Google Play Listing | ❌ 0% | 文案有 PRD 模板，未执行 |
| Firebase 配置 | ⚠️ 50% | 需要替换真实 google-services.json |
| AdMob ID 配置 | ❌ 0% | 需替换真实 ID |
| Google Play 内购商品 | ❌ 0% | 需在 Play Console 创建 |

---

## 6. 技术债务与风险

### 6.1 技术债务

| 债务项 | 严重程度 | 描述 | 建议 |
|---|---|---|---|
| 旧 Firestore 同步路径残留 | P1 | `FirestoreSource.kt` 和 `SyncOperations.kt` 仍存在，与 FastAPI 主线形成双路径 | 删除或标记 deprecated，统一到 `ApiSyncRepository` |
| 同步 Pull Cursor 不完善 | P1 | `GET /sync/changes` 的 cursor 机制未完整实现，可能拉取重复数据 | 完善后端 cursor，前端存储 lastPullAt |
| 代码中硬编码英文 | P1 | 部分 Compose UI 中仍有英文字符串，未使用 `stringResource` | 全量扫描替换 |
| 测试覆盖不足 | P1 | Android 端仅 ~8 个测试文件，后端无系统测试 | 补齐关键路径测试 |
| 后端 CI/CD 未自动化 | P2 | Dockerfile 存在，GitHub Actions 未配置部署 | 配置 Cloud Run 自动部署 |
| Notification Channel 管理 | P2 | Android 8.0+ 通知渠道未统一管理 | 建立 NotificationChannel 管理器 |
| 数据库 Migration 测试 | P2 | Alembic migration 未在 CI 中自动测试 | 添加 migration 测试步骤 |

### 6.2 风险矩阵

| 风险 | 可能性 | 影响 | 缓解措施 |
|---|---|---|---|
| 提醒不准（Android 厂商限制） | 高 | 高 | 使用 WorkManager + AlarmManager 双重保障；文档说明提醒依赖系统 |
| 医疗合规风险 | 中 | 极高 | 严格遵守文案规范，不出现"safe/medical/diagnosis"等词 |
| App 被 Google Play 拒审 | 中 | 高 | 提前审核 Data Safety、Privacy Policy、内容评级、权限声明 |
| 订阅收入不达预期 | 中 | 中 | Lifetime 低门槛、年订阅折扣、免费版清晰限制 |
| 竞品大厂跟进 | 低 | 中 | 聚焦 Bottle Timer 细分、夜间体验、小组件差异化 |
| 用户留存低 | 中 | 高 | 小组件、夜间模式、温和提醒、降低操作步数 |
| 数据库成本失控 | 低 | 中 | 不做倒计时 tick 同步、不全量拉取、批量提交 |

---

## 7. 后续任务规划

### 概述

基于当前状态，后续工作分为 **5 个 Phase**，预计 **4-6 周**可达到内测发布状态。

---

### Phase 0：基础闭合（第 1 周）

**目标**：让项目可以编译、测试通过、配置正确。

#### 任务清单

| ID | 任务 | 优先级 | 估时 | 依赖 |
|---|---|---|---|---|
| P0-1 | 替换 `google-services.json` 为真实 Firebase 项目配置 | P0 | 0.5h | 无 |
| P0-2 | 替换 `admob_app_id` 和广告单元 ID | P0 | 0.5h | 无 |
| P0-3 | 在 Google Play Console 创建内购商品（monthly/yearly/lifetime） | P0 | 1h | 无 |
| P0-4 | 设计并替换 App Icon（mipmap 各尺寸） | P0 | 2h | 无 |
| P0-5 | 运行 `./gradlew test` 确保全部测试通过 | P0 | 1h | P0-1 |
| P0-6 | 运行 `./gradlew assembleDebug` 确保编译通过 | P0 | 0.5h | P0-1 |
| P0-7 | 修复所有编译警告和 lint 问题 | P0 | 2h | P0-6 |
| P0-8 | 确认 `API_BASE_URL` 指向正确的后端实例 | P0 | 0.5h | 无 |
| P0-9 | 确认 RevenueCat API Key 正确配置 | P0 | 0.5h | 无 |
| P0-10 | 确认 Firebase Auth 已启用匿名登录 | P0 | 0.5h | P0-1 |

**验收标准**：
- `./gradlew assembleDebug` 编译成功
- `./gradlew test` 全部通过
- App 图标为非默认占位
- Firebase 项目连接正常

---

### Phase 1：核心体验打磨（第 1-2 周）

**目标**：奶瓶计时器核心路径稳定、流畅、可信。

#### 任务清单

| ID | 任务 | 优先级 | 估时 | 依赖 |
|---|---|---|---|---|
| P1-1 | 全量扫描并替换所有硬编码英文字符串为 `stringResource` | P0 | 4h | P0-6 |
| P1-2 | 审核全部用户可见文案，确保合规（无 safe/medical/diagnosis） | P0 | 2h | P1-1 |
| P1-3 | 完善免责声明展示——首次创建奶瓶时弹出 | P0 | 1h | 无 |
| P1-4 | 完善指南来源页面——展示 CDC/AAP/NHS 引用 | P0 | 2h | P1-2 |
| P1-5 | 夜间模式专用简化界面——Today 页大按钮模式 | P1 | 3h | 无 |
| P1-6 | 优化奶瓶创建路径——确保 2 步内完成 | P1 | 2h | 无 |
| P1-7 | 完善 Bottle Detail 页面——编辑时间、奶量、备注 | P1 | 2h | 无 |
| P1-8 | 空状态引导——无活跃奶瓶/无记录时的友好提示 | P1 | 1h | 无 |
| P1-9 | Toast/Snackbar 反馈——创建、删除、状态变更 | P1 | 1h | 无 |
| P1-10 | 修复 Notification Channel——统一管理、适配 Android 8+ | P1 | 2h | 无 |
| P1-11 | 验证设备重启后提醒恢复 | P1 | 1h | 无 |
| P1-12 | 验证时区变化和手动改时间场景 | P1 | 1h | 无 |
| P1-13 | 深色模式完整适配检查——所有页面 | P1 | 2h | 无 |
| P1-14 | 大字体适配检查——所有关键页面 | P1 | 1h | 无 |

**验收标准**：
- 全局搜索无硬编码用户可见英文字符串（不含日志/注释）
- 所有页面在深色模式下可用
- 免责声明在适当位置展示
- 奶瓶创建 ≤ 2 步
- 通知在设备重启后可恢复

---

### Phase 2：商业化闭环（第 2-3 周）

**目标**：完成免费/Pro 权限体系、广告接入、购买流程。

#### 任务清单

| ID | 任务 | 优先级 | 估时 | 依赖 |
|---|---|---|---|---|
| P2-1 | 实现 AdMob Banner——Today 页面底部 | P0 | 3h | P0-2 |
| P2-2 | 实现广告无展示逻辑——Pro 用户、夜间模式、喂奶流程中 | P0 | 2h | P2-1 |
| P2-3 | 完善 Pro 权益门控——统一所有 Pro 功能的权限检查 | P0 | 3h | 无 |
| P2-4 | 完善 Paywall 触发点——添加第二个宝宝、导出、高级统计、自定义规则 | P0 | 2h | P2-3 |
| P2-5 | 实现 Restore Purchase 完整流程——异常状态处理 | P1 | 2h | 无 |
| P2-6 | 实现订阅管理入口——跳转 Google Play 订阅页 | P1 | 1h | 无 |
| P2-7 | 优化 Paywall 设计——AB 测试准备（Lifetime 突出/年订阅突出） | P1 | 2h | 无 |
| P2-8 | 购买事件埋点——purchase_started/completed/failed | P1 | 1h | 无 |
| P2-9 | 后端 Entitlement 验证加固——宽限期、重试、日志 | P1 | 2h | P2-3 |
| P2-10 | 统一内测环境 vs 生产环境 Billing 配置 | P1 | 1h | P0-3 |

**验收标准**：
- Free 用户看到广告，Pro 用户无广告
- 添加第二个宝宝触发 Paywall
- Restore Purchase 可正常恢复
- 订阅过期后 Pro 权益正确收回
- 购买事件正常上报

---

### Phase 3：同步与后端闭环（第 2-3 周，与 Phase 2 并行）

**目标**：清理旧代码、加固同步、完善后端部署。

#### 任务清单

| ID | 任务 | 优先级 | 估时 | 依赖 |
|---|---|---|---|---|
| P3-1 | 删除/标记 deprecated 旧 Firestore 同步路径（`FirestoreSource`、`SyncOperations`） | P0 | 2h | 无 |
| P3-2 | 完善同步 Pull Cursor——后端返回 `hasMore`/`nextCursor`，前端分页拉取 | P0 | 3h | P3-1 |
| P3-3 | 统一冲突处理规则——奶瓶状态优先级 + last-write-wins | P0 | 2h | 无 |
| P3-4 | 添加同步失败重试策略——指数退避 | P1 | 2h | 无 |
| P3-5 | 实现同步状态 UI 指示——同步中/失败/成功 | P1 | 1h | 无 |
| P3-6 | 后端添加数据库连接池配置调优 | P1 | 1h | 无 |
| P3-7 | 后端添加 rate limiting | P1 | 1h | 无 |
| P3-8 | 配置 GitHub Actions 自动部署后端到 Cloud Run | P1 | 2h | 无 |
| P3-9 | 配置 Cloud Run 环境变量和 Secrets | P1 | 1h | P3-8 |
| P3-10 | 后端添加 Sentry 错误监控 | P2 | 1h | 无 |

**验收标准**：
- 旧 Firestore 同步代码已移除或明确标记 deprecated
- Sync Pull 支持分页
- 两个设备可同步数据且无重复
- 后端部署自动化可运行
- 同步失败有合理的重试和 UI 反馈

---

### Phase 4：小组件与体验提升（第 3-4 周）

**目标**：实现桌面小组件、完善统计、打磨体验。

#### 任务清单

| ID | 任务 | 优先级 | 估时 | 依赖 |
|---|---|---|---|---|
| P4-1 | 实现基础小组件——显示最近活跃奶瓶、剩余时间 | P0 | 6h | 无 |
| P4-2 | 小组件点击打开 App 并跳转到对应奶瓶 | P1 | 1h | P4-1 |
| P4-3 | 实现多尺寸小组件——2x1, 3x1, 4x1 | P1 | 3h | P4-1 |
| P4-4 | Pro 小组件主题预留（颜色/透明度配置） | P1 | 2h | P4-1 |
| P4-5 | 完善 Insights 页面——7/14/30 天趋势图表 | P1 | 4h | 无 |
| P4-6 | 完善 CSV 导出——前端触发、分享 Sheet | P0 | 2h | 无 |
| P4-7 | 完善 Logs 页面——搜索、日期快速跳转 | P1 | 2h | 无 |
| P4-8 | 添加触感反馈——奶瓶创建、状态变更、记录完成 | P1 | 1h | 无 |
| P4-9 | TalkBack 无障碍适配——关键按钮和状态朗读 | P1 | 2h | 无 |
| P4-10 | 添加加载状态和错误状态 UI——网络错误、数据为空 | P1 | 2h | 无 |

**验收标准**：
- 小组件可显示奶瓶倒计时
- 倒计时准确或分钟级更新
- Insights 有图表（Pro 用户）
- CSV 可导出并分享
- 小组件在手机重启后恢复

---

### Phase 5：上架准备与内测（第 4-6 周）

**目标**：完成 Google Play 上架全部必要素材和合规要求。

#### 任务清单

| ID | 任务 | 优先级 | 估时 | 依赖 |
|---|---|---|---|---|
| P5-1 | 编写 Privacy Policy 页面/URL | P0 | 2h | 无 |
| P5-2 | 编写 Terms of Service 页面/URL | P0 | 1h | 无 |
| P5-3 | 制作 App Icon（Play Store 512x512） | P0 | 2h | P0-4 |
| P5-4 | 制作 Feature Graphic（1024x500） | P0 | 2h | 无 |
| P5-5 | 制作 8 张 Google Play 截图（含多语言版本） | P0 | 4h | 无 |
| P5-6 | 编写 Google Play Short Description & Long Description | P0 | 2h | P1-2 |
| P5-7 | 多语言 Play Listing 翻译（ES/DE/FR/ZH-CN） | P0 | 3h | P5-6 |
| P5-8 | 填写 Google Play Data Safety 表单 | P0 | 1h | P5-1 |
| P5-9 | 完成内容评级问卷 | P0 | 0.5h | 无 |
| P5-10 | 设置 Google Play Closed Testing Track | P0 | 1h | 无 |
| P5-11 | 邀请 20-50 名内测用户 | P0 | 2h | P5-10 |
| P5-12 | 设置 Crashlytics + Analytics 看板 | P0 | 1h | 无 |
| P5-13 | 配置 ProGuard/R8 规则验证 release 包 | P0 | 1h | P0-6 |
| P5-14 | 构建 Release AAB 并上传到 Play Console | P0 | 1h | P5-13 |
| P5-15 | 内测反馈收集与修复迭代 | P0 | 1-2w | P5-11 |
| P5-16 | 配置 Google Play 内购商品测试 | P0 | 1h | P0-3 |
| P5-17 | 编写 App 内 FAQ 页面 | P1 | 2h | 无 |

**验收标准**：
- Google Play Console 所有必填项完整
- Closed Testing 可正常分发
- Crash-free users > 99%
- 通知权限引导清晰
- 用户能理解免责声明
- 内购测试可完成完整购买流程

---

## 8. 里程碑与时间线

### 8.1 甘特图概览

```
Week 1        Week 2        Week 3        Week 4        Week 5        Week 6
│             │             │             │             │             │
├─ Phase 0 ───┤             │             │             │             │
│ 基础闭合    │             │             │             │             │
│             │             │             │             │             │
├── Phase 1 ──┼──── Phase 1 ───┤          │             │             │
│  核心体验   │   打磨        │          │             │             │
│             │             │             │             │             │
│             ├── Phase 2 ───┼── Phase 2 ──┤             │             │
│             │  商业化      │  闭环       │             │             │
│             │             │             │             │             │
│             ├── Phase 3 ───┼── Phase 3 ──┤             │             │
│             │  同步/后端   │  闭环       │             │             │
│             │             │             │             │             │
│             │             ├── Phase 4 ──┼── Phase 4 ──┤             │
│             │             │  小组件/体验│             │             │
│             │             │             │             │             │
│             │             │             ├── Phase 5 ──┼── Phase 5 ──┤
│             │             │             │  上架/内测  │  内测迭代  │
```

### 8.2 关键里程碑

| 里程碑 | 预计时间 | 交付物 |
|---|---|---|
| M0: 编译通过 | Week 1 Day 3 | Debug APK 可运行，全部测试通过 |
| M1: 核心体验可用 | Week 2 Day 5 | 奶瓶计时器完整路径流畅，多语言覆盖 |
| M2: 商业化闭环 | Week 3 Day 5 | Free/Pro 体系完成，购买/恢复正常 |
| M3: 同步可用 | Week 3 Day 5 | 双设备同步正常，旧代码清理完毕 |
| M4: 小组件可用 | Week 4 Day 5 | 桌面小组件工作正常 |
| M5: 内测发布 | Week 5 Day 3 | Closed Testing 发布，邀请测试用户 |
| M6: 生产发布 | Week 6 Day 5 | 5% → 20% → 50% → 100% 灰度上线 |

---

## 9. 附录：文件清单

### 9.1 Android 核心文件（131 个 .kt 文件）

**数据层 (42 个文件)**
- `data/local/db/NurtlinaDatabase.kt`
- `data/local/entity/` — BabyEntity, BottleEntity, FeedLogEntity, DiaperLogEntity, SleepLogEntity, SyncQueueEntity, SyncStatus
- `data/local/dao/` — BabyDao, BottleDao, FeedLogDao, DiaperLogDao, SleepLogDao, SyncQueueDao
- `data/repository/` — RoomBabyRepository, RoomBottleRepository, RoomFeedLogRepository, RoomDiaperLogRepository, RoomSleepLogRepository, ApiBackendRepository, ApiSyncRepository, FirebaseAuthRepository
- `data/remote/` — FirebaseAuthSource, FirestoreSource, BackendApiService, AuthTokenInterceptor, Remote DTOs
- `data/sync/` — SyncQueueWriter, SyncQueueProcessor, SyncWorker, WorkManagerSyncManager, SyncOperations
- `data/billing/` — EntitlementManager, EntitlementCache, EntitlementCacheRepository
- `data/datastore/` — DataStoreSettingsRepository, DataStoreSessionRepository
- `data/rating/` — DataStoreRatingPromptRepository
- `data/startup/` — AppStartupCoordinator

**领域层 (28 个文件)**
- `domain/model/` — Baby, Bottle, FeedLog, DiaperLog, SleepLog, UserSettings, TodaySummary, Enums, SessionInfo, AuthState, SyncState, SyncResult, BackendInitResult, UserAccount
- `domain/guideline/` — BottleStateMachine, ExpiryCalculator, GuidelineRule
- `domain/usecase/bottle/` — CreateBottleUseCase, TransitionBottleUseCase, ObserveBottlesUseCase, CheckAndExpireBottlesUseCase, GetTodaySummaryUseCase
- `domain/usecase/feed/` — LogFeedUseCase
- `domain/usecase/diaper/` — LogDiaperUseCase
- `domain/usecase/sleep/` — SleepUseCase
- `domain/usecase/baby/` — ManageBabyUseCase
- `domain/rating/` — RatingPromptState, RatingPromptDecision, RatingPromptEligibility
- `domain/repository/` — 13 个接口 (Auth, Baby, Bottle, FeedLog, DiaperLog, SleepLog, Settings, Session, Sync, Backend, SyncManager, RatingPrompt)

**UI 层 (24 个文件)**
- `ui/theme/` — Color, Shapes, Theme, Type
- `ui/navigation/` — NavRoutes, NurtlinaNavHost
- `ui/onboarding/` — OnboardingScreen, OnboardingViewModel
- `ui/today/` — TodayScreen, TodayViewModel
- `ui/bottle/` — BottleDetailScreen, BottleViewModel, NewBottleSheet
- `ui/logs/` — LogsScreen, LogsViewModel, LogEditSheet
- `ui/insights/` — InsightsScreen
- `ui/settings/` — SettingsScreen, SettingsViewModel
- `ui/paywall/` — PaywallScreen, PaywallViewModel
- `ui/auth/` — SignInScreen, SignInViewModel
- `ui/NurtlinaDialog.kt`

**核心 (11 个文件)**
- `core/notification/` — BottleNotificationScheduler, NextFeedNotificationScheduler, NotificationReceiver, BootReceiver, RescheduleNotificationsWorker, NotificationIds, FeedReminderConfig
- `core/analytics/Analytics.kt`
- `core/time/TimeFormatter.kt`

**DI (5 个文件)**
- `di/` — AppModule, DatabaseModule, DataStoreModule, RemoteModule, RepositoryModule

**测试 (7 个文件)**
- `test/` — BottleStateMachineTest, ExpiryCalculatorTest, SyncQueueWriterTest, AuthTokenInterceptorTest, AppStartupCoordinatorTest, EntitlementCacheRepositoryTest, RatingPromptEligibilityTest

### 9.2 Backend 核心文件（~30 个 .py 文件）

- `app/main.py`
- `app/core/` — config, security, errors, ids, clock, logging
- `app/db/` — session, base
- `app/models/` — user, family, baby, bottle, feed_log, diaper_log, sleep_log, entitlement, sync
- `app/schemas/` — common, user, sync, billing, export
- `app/api/` — deps, router, routes (health, me, sync, billing, export, account, admin)
- `app/services/` — user_service, sync_service, billing_service, export_service, deletion_service
- `app/integrations/` — firebase_admin, google_play, storage, email
- `alembic/` — env.py, versions (0001_initial_schema, 0002_entitlement_token_unique)

### 9.3 Firebase 文件

- `firebase/firestore.rules`
- `firebase/functions/src/index.ts`
- `firebase/firebase.json`
- `firebase/.firebaserc`
- `firebase/firestore.indexes.json`

### 9.4 文档文件

- `Nurtlina_PRD.md` — 完整 PRD（40 章节，2500+ 行）
- `Tech_Architecture.md` — 技术架构方案（含当前实施状态）
- `AGENTS.md` — AI Agent 行为规范
- `TODO` — 待办清单（5 项）
- `README.md` — 项目说明
- `docs/project-backend-status-audit.md` — 后端状态审核
- `docs/backend-deployment-runbook.md` — 部署手册
- `docs/rating-prompt-plan.md` — 评分提示方案
- `docs/revenuecat-setup.md` — RevenueCat 配置指南
- `docs/adr-backend-sync-source-of-truth.md` — ADR: 同步源

---

## 10. 总结

### 10.1 项目健康度总评

| 指标 | 评分 | 说明 |
|---|---|---|
| 代码架构 | ⭐⭐⭐⭐ | 清晰的分层架构，DI 完善 |
| 核心功能 | ⭐⭐⭐⭐ | 奶瓶状态机、计时计算、通知系统扎实 |
| 数据完整性 | ⭐⭐⭐⭐ | Room + PostgreSQL 双存储，同步队列可靠 |
| UI 覆盖度 | ⭐⭐⭐ | 主要页面完成，细节和空状态待打磨 |
| 测试覆盖 | ⭐⭐ | 关键逻辑有测试，集成和 UI 测试不足 |
| DevOps | ⭐⭐⭐ | CI 可用，CD 和后端自动部署未完成 |
| 上架就绪 | ⭐ | Play Console 素材几乎为零 |
| 合规就绪 | ⭐⭐ | 文案规范清晰，但 Privacy/Terms 文档未创建 |

### 10.2 核心建议

1. **优先闭合 Phase 0**：让项目可以编译和运行，这是一切的前提
2. **不要扩大范围**：当前 MVP 范围已经足够，V1.2 的云备份、多照护者等功能先冻结
3. **睡前测试**：每个 Phase 结束前，在真机上完成一次完整的夜间喂奶流程
4. **文案审核**：每次新增/修改文案，必须通过合规检查（无 safe/medical/diagnosis）
5. **小组件不要拖延**：这是留存的关键之一，必须在内测前完成
6. **AdMob 测试广告**：确保广告加载、展示、Pro 隐藏全链路正确

---

*文档版本：v2.0*
*生成日期：2026-07-09*
*下一审核日期：Phase 1 结束时（约 1-2 周后）*
