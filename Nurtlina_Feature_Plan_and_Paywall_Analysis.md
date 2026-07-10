# Nurtlina 产品功能任务规划 & 付费墙设计分析

> 分析日期：2026-07-10
> 分析范围：全部 Kotlin 源代码（约 120 个文件）、FastAPI 后端代码、Firebase Cloud Functions
> 关联文档：[[Nurtlina_PRD_Evaluation_and_Task_Plan]]

---

## 目录

1. [当前功能实现全景](#1-当前功能实现全景)
2. [Bottle 遗留代码清理任务](#2-bottle-遗留代码清理任务)
3. [产品功能优先级任务规划](#3-产品功能优先级任务规划)
4. [付费墙设计分析](#4-付费墙设计分析)
5. [付费墙改进优先级汇总](#5-付费墙改进优先级汇总)

---

## 1. 当前功能实现全景

图例：✅ 已实现  ⚠️ 部分实现  ❌ 未实现  🔶 Bottle 遗留（待清理）

### 1.1 Today 首页（核心页面）

| 子功能 | 状态 | 代码位置 | 说明 |
|---|---|---|---|
| Baby Switcher | ✅ | `TodayScreen.kt:538-592` | 多宝宝切换 |
| **FeedingStatusCard** | ✅ | `TodayScreen.kt:598-700` | 上次喂奶时间、间隔、奶量、下次喂奶倒计时 |
| **ActiveBottleCard + 操作按钮** | 🔶 | `TodayScreen.kt:839-1099` | **遗留**：有活跃 Bottle 时渲染，否则不显示 |
| 活跃睡眠卡片 + 计时器 | ✅ | `TodayScreen.kt:736-833` | 每秒刷新的实时计时器 |
| Quick Log 快捷记录 | ✅ | `TodayScreen.kt:1168-1247` | Feed→NewBottleSheet(即 logFeed) / Diaper / Sleep |
| Today Summary 统计卡片 | ✅ | `TodayScreen.kt:1253-1341` | 今日喂奶/尿布/睡眠 |
| AdMob Banner | ✅ | `TodayScreen.kt:1347-1362` | 测试 ID |
| 评分弹窗 | ✅ | `TodayScreen.kt:365-404` | 三选项弹窗 |
| Pro 广告隐藏 | ✅ | `NurtlinaNavHost.kt:358` | `showAds = !isPro && !nightMode` |

### 1.2 喂奶记录系统（✅ 主路径已实现）

| 子功能 | 状态 | 代码位置 | 说明 |
|---|---|---|---|
| **NewBottleSheet → logFeed** | ✅ | `NurtlinaNavHost.kt:415-429` | 直接创建 FeedLog，不再创建 Bottle |
| **logFeed()** | ✅ | `TodayViewModel.kt:196-218` | 完整记录 + 下次提醒调度 |
| **quickLogFeed(amountMl)** | ✅ | `TodayViewModel.kt:221-245` | 一键预设奶量；有活跃 Bottle 时自动 MarkFed |
| **FeedingStatusCard** | ✅ | `TodayScreen.kt:598-700` | 上次喂奶+下次提醒 UI |
| 下次喂奶提醒 | ✅ | `NextFeedNotificationScheduler.kt` | 基于间隔调度 |
| FeedLog CRUD | ✅ | `LogFeedUseCase.kt` + DAO | 完整 |

### 1.3 Bottle 遗留系统（🔶 待清理）

| 组件 | 状态 | 说明 |
|---|---|---|
| BottleEntity / BottleDao | 🔶 | Room 层遗留 |
| Bottle.kt / BottleStatus / BottleTransition | 🔶 | Domain 模型遗留 |
| BottleStateMachine / ExpiryCalculator / GuidelineRule | 🔶 | 业务规则遗留 |
| CreateBottleUseCase / TransitionBottleUseCase / etc. | 🔶 | 用例遗留 |
| BottleNotificationScheduler | 🔶 | 到期通知遗留 |
| ActiveBottleCard (TodayScreen) | 🔶 | UI 遗留 |
| BottleDetailScreen / BottleViewModel | 🔶 | 详情页遗留 |
| BottleRepository / RoomBottleRepository | 🔶 | 仓库遗留 |
| RemoteBottleDto | 🔶 | DTO 遗留 |
| BottleStateMachineTest / ExpiryCalculatorTest | 🔶 | 测试遗留 |
| 后端 bottles 表 + /sync/bottles | 🔶 | 后端遗留 |
| Bottle→FeedLog 桥接 (logFeedFromBottle) | 🔶 | MarkFed 时自动创建 FeedLog |

### 1.4 Logs 记录页

| 子功能 | 状态 | 缺口 |
|---|---|---|
| 时间线展示 | ✅ | — |
| 类型筛选（All/Feed/Diaper/Sleep） | ✅ | — |
| 日期前后切换 | ✅ | — |
| 编辑日志 | ⚠️ | `onEntryClick = {}` 空回调 |
| 删除日志 | ✅ | — |
| 日期选择器（跳转） | ❌ | `onPickDate = {}` 空回调 |

### 1.5 Insights 统计页

| 子功能 | 状态 | 缺口 |
|---|---|---|
| 今日摘要卡片 | ✅ | 4 卡片布局 |
| 7/14/30 天切换 | ✅ | FilterChip |
| 趋势柱状图 | ✅ | Canvas 手绘 |
| Pro 数据源 | 🔴 | `proData = null` 硬编码 |

### 1.6 商业化

| 子功能 | 状态 | 缺口 |
|---|---|---|
| RevenueCat 集成 | ✅ | 完整 |
| Paywall 页面 | ✅ | 3 个定价选项 |
| Pro 权益缓存（3 天宽限期） | ✅ | — |
| AdMob Banner | ✅ | 测试 ID |
| Pro 去广告 | ✅ | — |
| 多宝宝 Pro 门控 | ⚠️ | baby creation screen 未实现 |
| 导出 Pro 门控 | ❌ | 回调为空 |
| 高级统计 Pro 门控 | ⚠️ | 数据源为空 |
| 小组件 | ❌ | 0 行代码 |

---

## 2. Bottle 遗留代码清理任务

### 🔴 P0 — 清理（约 11h / 1.5 天）

| ID | 任务 | 估时 |
|---|---|---|
| **CLN-1** | TodayScreen: 移除 ActiveBottleCard + BottleActionButtons 渲染 | 1h |
| **CLN-2** | TodayViewModel: 移除 activeBottles 观察 + transitionBottle + logFeedFromBottle | 1h |
| **CLN-3** | 移除 BottleDetailScreen + BottleViewModel | 0.5h |
| **CLN-4** | NavRoutes: 移除 BottleDetail route + 导航入口 | 0.5h |
| **CLN-5** | 移除 BottleNotificationScheduler | 0.5h |
| **CLN-6** | 移除 BottleStateMachine + ExpiryCalculator + GuidelineRule | 0.5h |
| **CLN-7** | 移除 CreateBottleUseCase + TransitionBottleUseCase + ObserveBottlesUseCase + CheckAndExpireBottlesUseCase | 1h |
| **CLN-8** | 移除 RoomBottleRepository + BottleRepository 接口 | 0.5h |
| **CLN-9** | 移除 BottleEntity + BottleDao + Room DB migration | 1h |
| **CLN-10** | 移除 Bottle.kt + BottleStatus + BottleTransition 领域模型 | 0.5h |
| **CLN-11** | 移除 RemoteBottleDto | 0.5h |
| **CLN-12** | 移除 BottleStateMachineTest + ExpiryCalculatorTest | 0.5h |
| **CLN-13** | 后端: 移除 bottles 表 + /sync/bottles + Alembic migration | 1h |
| **CLN-14** | Firestore rules: 移除 bottles 子集合 | 0.5h |
| **CLN-15** | 全量编译验证 + 基础冒烟测试 | 1h |

---

## 3. 产品功能优先级任务规划

### 🟠 P1 — 补齐关键交互缺口

| ID | 任务 | 估时 |
|---|---|---|
| **FEAT-1** | Quick Feed 预设奶量按钮（60/90/120/150 ml）直接入口 | 3h |
| **FEAT-2** | Logs 点击查看/编辑日志详情（LogEditSheet 回调实现） | 2h |
| **FEAT-3** | Logs 日期选择器（DatePickerDialog） | 1h |
| **FEAT-4** | Onboarding 通知权限请求实现 | 1h |
| **FEAT-5** | CSV 导出前端实现（调用后端 API + 分享 Sheet） | 3h |
| **FEAT-6** | FAQ 页面内容 | 2h |

### 🟡 P2 — 体验提升

| ID | 任务 | 估时 |
|---|---|---|
| **FEAT-7** | 夜间模式专用简化布局（大按钮、大字） | 4h |
| **FEAT-8** | 桌面小组件（Glance API）— 上次喂奶+下次提醒 | 6h |
| **FEAT-9** | Insights Pro 数据源实现 | 3h |
| **FEAT-10** | Insights oz 单位支持 | 0.5h |
| **FEAT-11** | 宝宝资料编辑支持生日和颜色 | 1h |
| **FEAT-12** | Pro 用户多宝宝创建页面 | 2h |

---

## 4. 付费墙设计分析

（当前 Paywall 实现与之前分析一致，无重大变更）

### Pro 功能列表

- No ads ✅
- Multiple babies ✅
- Full history ✅
- CSV/PDF Export ✅
- Cloud backup ✅
- Custom reminders ✅
- Widget themes ✅
- Priority support ✅

---

*文档版本：v2.1*
*分析日期：2026-07-10*
