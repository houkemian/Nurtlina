# Nurtlina 项目评估、PRD 与后续任务规划

> 评估日期：2026-07-10
> 项目代号：Nurtlina — Baby Feeding Tracker
> 当前版本：v1.0.0-dev（内测前）
> **Phase 0 清理状态**：✅ 已完成 — Bottle 系统已从代码库中完全移除

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

---

## 1. 项目概要

### 1.1 产品定位

**Nurtlina** 是一款面向海外新手父母的 Android 婴儿喂养记录与护理追踪工具。核心功能是 **极简喂奶记录**——帮助用户在最少操作下快速记录每次喂奶的类型、奶量和时间。

> **v2.0 已实施**：Bottle 计时器/状态机系统已于 2026-07-10 完全移除。产品现在纯以 FeedLog 为中心：`NewFeedSheet`（原 NewBottleSheet）直接创建喂奶记录，`FeedingStatusCard` 展示上次喂奶状态与下次喂奶倒计时。

### 1.2 一句话定义

> 一个简单、可信、无压力的婴儿喂奶记录和护理追踪 App。

### 1.3 商业模式

| 层级 | 价格 | 权益 |
|---|---|---|
| Free（广告支持） | $0 | 1 个宝宝、喂奶/尿布/睡眠记录、今日统计、默认小组件、广告 |
| Pro Monthly | $2.99/月 | 无广告、多宝宝、全部历史、高级统计、导出、备份、小组件主题 |
| Pro Yearly | $19.99/年 | 同上 |
| Pro Lifetime | $29.99 | 同上，一次性买断 |

---

## 2. 当前实施状态评估

### 2.1 总体评估

**项目已进入 MVP 后端集成中期。Bottle 系统已完全移除，FeedLog 是唯一的喂养数据路径。**

| 维度 | 完成度 | 状态 |
|---|---|---|
| Android 数据层 | 95% | ✅ FeedLog/DiaperLog/SleepLog 完整；BottleEntity 已移除 |
| Android 领域层 | 90% | ✅ LogFeedUseCase + GetTodaySummaryUseCase；Bottle 用例已移除 |
| Android UI 层 | 90% | ✅ FeedingStatusCard + NewFeedSheet；ActiveBottleCard 已移除 |
| Android 通知系统 | 90% | ✅ NextFeedNotificationScheduler；BottleNotificationScheduler 已移除 |
| Android 同步系统 | 85% | ✅ Bottle 同步路径已移除 |
| Android 商业化 | 70% | ⚠️ RevenueCat 已集成，AdMob 测试 ID |
| Android 小组件 | 20% | ❌ 未开始 |
| FastAPI 后端 | 90% | ✅ 核心 API 完成；bottles 表已移除 |
| Firebase | 95% | ✅ Firestore bottles 规则已移除 |
| 测试覆盖 | 55% | ⚠️ Bottle 测试已移除，FeedLog 测试需补齐 |
| 上架素材 | 30% | ❌ 缺少图标、截图、Feature Graphic |

### 2.2 已完成的核心能力

#### 喂奶记录管理

- **NewFeedSheet**（`ui/feed/NewFeedSheet.kt`）：直接创建 FeedLog，支持奶类型选择、预设奶量、时间编辑、备注
- **FeedingStatusCard**（`TodayScreen.kt`）：上次喂奶时间/间隔/奶量、今日统计、下次喂奶倒计时
- **logFeed() / quickLogFeed()**：完整记录 + 一键预设奶量
- **NextFeedNotificationScheduler**：基于间隔的喂奶提醒
- **FeedLog CRUD**：完整，Logs 时间线 + 类型筛选 + 删除

#### Phase 0 清理成果

| 类别 | 清理量 |
|---|---|
| 删除文件 | 20 个 Kotlin + 2 个 Python |
| 编辑文件 | 39 个 Kotlin + 8 个 Python/Markdown + Firestore rules |
| 重命名 | `NewBottleSheet` → `NewFeedSheet`，包 `ui/bottle` → `ui/feed` |
| 数据库 | Room v2→v3（DROP bottles），PostgreSQL migration 0003 |
| 净代码变化 | -8,373 行，+992 行 |

---

## 3. PRD 概述

### 3.1 核心用户旅程

1. 打开 App → 创建宝宝 → 免责声明 → 通知权限
2. 首页点击 "+" → NewFeedSheet → 选奶类型、奶量 → 保存
3. FeedingStatusCard 显示"上次喂奶：X 分钟前 · 120 ml"
4. 下次喂奶时间到达时收到温和提醒

### 3.2 信息架构

```
Nurtlina App
├── Onboarding
├── Main (底部 4 Tab)
│   ├── Today — FeedingStatusCard + QuickLogRow + TodaySummary
│   ├── Logs — 时间线（Feed/Diaper/Sleep 筛选）
│   ├── Insights — 今日统计 + 趋势图表
│   └── Settings — 宝宝/单位/通知/Pro/导出/关于
├── NewFeed (→ NewFeedSheet)
├── Paywall
└── Sign In
```

### 3.3 数据模型（v2.0 — 当前实施）

```
Baby (id, familyId, name, birthDate, avatarColor)
FeedLog (id, babyId, feedType, amountMl, startedAt, endedAt, note) ← 唯一喂养实体
DiaperLog (id, babyId, diaperType, changedAt, note)
SleepLog (id, babyId, startedAt, endedAt, note)
```

---

## 4. 功能完成度矩阵

| 功能 | 状态 | 备注 |
|---|---|---|
| 喂奶记录 (CRUD) | ✅ 95% | NewFeedSheet + LogFeedUseCase 完整 |
| FeedingStatusCard | ✅ 90% | 上次喂奶+下次提醒 |
| 快捷奶量输入 | ✅ 85% | 预设按钮 + 手动输入 |
| 下次喂奶提醒 | ✅ 85% | NextFeedNotificationScheduler |
| 尿布记录 | ✅ 85% | 完整 |
| 睡眠记录 | ✅ 85% | 完整 + 活跃计时器 |
| Logs 时间线 | ✅ 80% | 筛选/删除有，编辑/日期选择器空回调 |
| 今日统计 | ✅ 85% | GetTodaySummaryUseCase |
| Onboarding | ✅ 90% | 5 步流程 |
| Pro Paywall | ✅ 80% | RevenueCat 完整 |
| 暗色模式 | ✅ 80% | Material 3 主题 |
| 评分弹窗 | ✅ 90% | RatingPromptEligibility（已更新为 Feed 指标） |
| 数据同步 | ✅ 80% | FastAPI push/pull |
| CSV 导出 | ⚠️ 70% | 后端可用，前端空回调 |
| 桌面小组件 | ❌ 0% | 未实现 |
| FAQ | ❌ 0% | 空回调 |
| Privacy/Terms | ❌ 0% | URL 页面未创建 |

---

## 5. 技术债务与风险

| 债务项 | 严重程度 | 状态 |
|---|---|---|
| ~~Bottle 遗留代码~~ | ~~P0~~ | ✅ **已清理**（Phase 0 完成） |
| ~~后端 Bottle 残留~~ | ~~P0~~ | ✅ **已清理**（migration 0003 + 7 个文件修复） |
| 硬编码英文字符串 | P1 | 待处理 |
| FeedLog/Summary 测试不足 | P1 | 待补齐 |
| 后端 CI/CD 未自动化 | P2 | GitHub Actions 待配置 |
| 前端空回调（Export/FAQ/Logs edit） | P1 | 待接通 |
| 小组件未实现 | P1 | P2 任务 |

---

## 6. 后续任务规划

### ✅ Phase 0：Bottle 清理 — 已完成

所有 15 项任务已执行，代码库中不再存在 Bottle 相关代码。详见 commit `0e42f39` + `ba00f03`。

### Phase 1：交互缺口补齐

| ID | 任务 | 估时 |
|---|---|---|
| FEAT-1 | Quick Feed 预设奶量按钮直接入口 | 3h |
| FEAT-2 | Logs 编辑日志详情 | 2h |
| FEAT-3 | Logs 日期选择器 | 1h |
| FEAT-4 | Onboarding 通知权限实现 | 1h |
| FEAT-5 | CSV 导出前端实现 | 3h |
| FEAT-6 | FAQ 页面内容 | 2h |

### Phase 2：体验提升

| ID | 任务 | 估时 |
|---|---|---|
| FEAT-7 | 夜间模式简化布局 | 4h |
| FEAT-8 | 桌面小组件（Glance API） | 6h |
| FEAT-9 | Insights Pro 数据源 | 3h |
| FEAT-10 | Insights oz 单位支持 | 0.5h |

### Phase 3：商业化 + 上架

| ID | 任务 | 估时 |
|---|---|---|
| FEAT-11 | AdMob 真实 ID | 0.5h |
| FEAT-12 | Privacy/Terms 页面 | 3h |
| FEAT-13 | App Icon + 截图 | 8h |
| FEAT-14 | Play Listing + Data Safety | 3h |
| FEAT-15 | Closed Testing | 2h |

---

## 7. 里程碑与时间线

| 里程碑 | 预计时间 | 交付物 |
|---|---|---|
| ~~M0: Bottle 清理~~ | ~~Week 1 Day 2~~ | ✅ 已完成 |
| M1: 核心体验可用 | Week 2 Day 5 | 交互缺口补齐 |
| M2: 商业化闭环 | Week 3 Day 5 | Free/Pro 完整 |
| M3: 小组件 | Week 4 Day 5 | 桌面小组件 |
| M4: 内测 | Week 5 Day 3 | Closed Testing |
| M5: 发布 | Week 6 Day 5 | 灰度上线 |

---

*文档版本：v4.0*
*生成日期：2026-07-10*
