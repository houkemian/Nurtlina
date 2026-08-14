# Nurtlina 技术方案：FastAPI 后端生产级性价比架构

> 产品：Nurtlina: Baby Feeding Tracker  
> 平台：Android-only  
> 文档版本：v3.1  
> 日期：2026-07-10

---

### 当前项目实施状态

#### 喂奶记录管理 — ✅ 已实现

- `NewFeedSheet` 入口已改为直接创建 FeedLog（`NurtlinaNavHost.kt:415-429`）
- `FeedingStatusCard` 展示上次喂奶状态和下次喂奶倒计时（`TodayScreen.kt:598-700`）
- `TodayViewModel.logFeed()` / `quickLogFeed()` — 喂奶记录主路径
- `NextFeedNotificationScheduler` — 基于间隔的喂奶提醒
- FeedLog CRUD（Room + Repository + UseCase）完整

#### Bottle 系统 — ✅ 已移除（Phase 0 完成）

Bottle 相关组件（BottleEntity、BottleStateMachine、BottleNotificationScheduler、ActiveBottleCard 等）已从代码库中完全移除。唯一残留是后端 `feed_logs` 表的可空 legacy `bottle_id` 字段（同步透传）和 Room 的 `DROP TABLE IF EXISTS bottles` 迁移。

详见 [[Nurtlina_PRD_Evaluation_and_Task_Plan]] Phase 0（commit `0e42f39` + `ba00f03`）。

---

### 架构总览

```text
┌─────────────────────────────────────────────────────────────┐
│ Android App (Kotlin + Compose)                              │
│                                                             │
│ UI: TodayScreen (FeedingStatusCard + QuickLogRow)           │
│ Domain: LogFeedUseCase + SleepUseCase + LogDiaperUseCase    │
│ Data: Room (FeedLog + DiaperLog + SleepLog)                 │
│     + SyncQueue + Retrofit + DataStore                      │
│                                                             │
│ Local-first: 所有写入先写 Room，异步同步到后端               │
└───────────────┬─────────────────────────────┬───────────────┘
                │ REST API (Retrofit)         │ Firebase SDK
                v                             v
┌───────────────────────────────┐   ┌─────────────────────────┐
│ FastAPI Backend (Python 3.12) │   │ Firebase                 │
│                               │   │ Auth / Crashlytics       │
│ PostgreSQL (SQLAlchemy 2.0)   │   │ Analytics / Remote Config│
│ Alembic Migrations            │   │ Cloud Functions          │
└───────────────────────────────┘   └─────────────────────────┘
```

### 数据库表

| 表 | 状态 | 说明 |
|---|---|---|
| users, families, family_members | ✅ | 用户和家庭 |
| babies | ✅ | 宝宝信息 |
| feed_logs | ✅ 主实体 | 喂奶记录 |
| diaper_logs | ✅ | 尿布记录 |
| sleep_logs | ✅ | 睡眠记录 |
| bottles | ✅ 已移除 | Phase 0 已删除（仅 feed_logs.bottle_id 可空 legacy 字段保留） |
| entitlements | ✅ | Pro 权益 |
| sync_cursors | ✅ | 同步游标 |

### API 端点

| 端点 | 状态 |
|---|---|
| `/api/v1/sync/feed-logs` / `diaper-logs` / `sleep-logs` / `babies` / `settings` | ✅ |
| `/api/v1/sync/bottles` | ✅ 已移除 |
| `/api/v1/sync/changes` | ✅ |
| `/api/v1/me/init` / `/api/v1/me` | ✅ |
| `/api/v1/billing/google-play/purchases` | ✅ |
| `/api/v1/entitlements/me` | ✅ |
| `/api/v1/exports` | ✅ |
| `/api/v1/account/delete` | ✅ |

### 同步链路

```text
用户记录喂奶
-> 写入 Room feed_logs
-> 写入 sync_queue
-> UI 立即更新（FeedingStatusCard 刷新）
-> WorkManager 后台推送到 FastAPI
-> FastAPI 写入 PostgreSQL feed_logs
-> 返回 sync success
-> 本地标记 synced
```

---

*文档版本：v3.1*
*日期：2026-07-10*
