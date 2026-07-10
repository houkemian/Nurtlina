# Nurtlina 产品功能任务规划 & 付费墙设计分析

> 分析日期：2026-07-10
> 分析范围：全部 Kotlin 源代码（约 113 个文件）、FastAPI 后端代码、Firebase Cloud Functions
> 关联文档：[[Nurtlina_PRD_Evaluation_and_Task_Plan]]
> **状态**：Phase 0 Bottle 清理已完成 ✅

---

## 目录

1. [当前功能实现全景](#1-当前功能实现全景)
2. [产品功能优先级任务规划](#2-产品功能优先级任务规划)
3. [付费墙设计分析](#3-付费墙设计分析)
4. [付费墙改进优先级汇总](#4-付费墙改进优先级汇总)

---

## 1. 当前功能实现全景

图例：✅ 已实现  ⚠️ 部分实现  ❌ 未实现

### 1.1 Today 首页（核心页面）

| 子功能 | 状态 | 代码位置 | 说明 |
|---|---|---|---|
| Baby Switcher | ✅ | `TodayScreen.kt:538-592` | 多宝宝切换 |
| **FeedingStatusCard** | ✅ | `TodayScreen.kt:598-700` | 上次喂奶时间、间隔、奶量、下次喂奶倒计时 |
| 活跃睡眠卡片 + 计时器 | ✅ | `TodayScreen.kt:736-833` | 每秒刷新的实时计时器 |
| Quick Log 快捷记录 | ✅ | `TodayScreen.kt:1168-1247` | Feed→NewFeedSheet / Diaper / Sleep |
| Today Summary 统计卡片 | ✅ | `TodayScreen.kt:1253-1341` | 今日喂奶/尿布/睡眠 |
| AdMob Banner | ✅ | `TodayScreen.kt:1347-1362` | 测试 ID |
| 评分弹窗 | ✅ | `TodayScreen.kt:365-404` | 三选项弹窗 |
| Pro 广告隐藏 | ✅ | `NurtlinaNavHost.kt:358` | `showAds = !isPro && !nightMode` |
| 夜间模式大按钮 | ⚠️ | `nightModeEnabled` 参数 | 按钮高度适配有，缺专用简化布局 |

### 1.2 喂奶记录系统（✅ 主路径完整）

| 子功能 | 状态 | 代码位置 | 说明 |
|---|---|---|---|
| **NewFeedSheet → logFeed** | ✅ | `NurtlinaNavHost.kt:415-429` | NewFeedRoute 直接创建 FeedLog |
| **logFeed()** | ✅ | `TodayViewModel.kt` | 完整记录 + 下次提醒调度 |
| **quickLogFeed(amountMl)** | ✅ | `TodayViewModel.kt` | 一键预设奶量 |
| **FeedingStatusCard** | ✅ | `TodayScreen.kt:598-700` | 上次喂奶+下次提醒 UI |
| 下次喂奶提醒 | ✅ | `NextFeedNotificationScheduler.kt` | 基于间隔调度 |
| FeedLog CRUD | ✅ | `LogFeedUseCase.kt` + DAO | 完整 |

### 1.3 Logs 记录页

| 子功能 | 状态 | 缺口 |
|---|---|---|
| 时间线展示 | ✅ | — |
| 类型筛选 | ✅ | — |
| 日期前后切换 | ✅ | — |
| 编辑日志 | ⚠️ | `onEntryClick = {}` 空回调 |
| 删除日志 | ✅ | — |
| 日期选择器 | ❌ | `onPickDate = {}` 空回调 |

### 1.4 Insights 统计页

| 子功能 | 状态 | 缺口 |
|---|---|---|
| 今日摘要卡片 | ✅ | 4 卡片布局 |
| 7/14/30 天切换 | ✅ | FilterChip |
| 趋势柱状图 | ✅ | Canvas 手绘 |
| Pro 数据源 | 🔴 | `proData = null` 硬编码 |

### 1.5 商业化

| 子功能 | 状态 | 缺口 |
|---|---|---|
| RevenueCat 集成 | ✅ | 完整 |
| Paywall 页面 | ✅ | 3 个定价选项 |
| Pro 权益缓存 | ✅ | 3 天宽限期 |
| AdMob Banner | ✅ | 测试 ID |
| Pro 去广告 | ✅ | — |
| 多宝宝 Pro 门控 | ⚠️ | baby creation screen 未实现 |
| 导出 Pro 门控 | ❌ | 回调为空 |
| 高级统计 Pro 门控 | ⚠️ | 数据源为空 |
| 小组件 | ❌ | 未实现 |

### 1.6 Phase 0 清理完成清单 ✅

以下组件已从代码库中完全移除（详见 commit `0e42f39` + `ba00f03`）：

| 已删除 | 类别 |
|---|---|
| Bottle.kt, BottleStatus, BottleTransition, BottleTransitionResult | Domain 模型 |
| BottleStateMachine, ExpiryCalculator, GuidelineRule | 业务规则 |
| CreateBottleUseCase, TransitionBottleUseCase, ObserveBottlesUseCase, CheckAndExpireBottlesUseCase | 用例 |
| BottleEntity, BottleDao, RoomBottleRepository, BottleRepository, RemoteBottleDto | Data 层 |
| BottleDetailScreen, BottleViewModel, ActiveBottleCard, BottleActionButtons | UI 层 |
| BottleNotificationScheduler | 通知 |
| BottleStateMachineTest, ExpiryCalculatorTest, test_sync_conflict.py | 测试 |
| 后端 bottles 表, /sync/bottles, Bottle model, test_bottle_timer_rules.py | 后端 |
| Firestore bottles 子集合规则 | Firebase |
| `ui/bottle/` `domain/guideline/` `domain/usecase/bottle/` 目录 | 包结构 |

| 已重命名 | 旧 → 新 |
|---|---|
| NewBottleSheet | → `NewFeedSheet` (`ui/bottle/` → `ui/feed/`) |
| NewBottleUiState | → `NewFeedUiState` |
| GetTodaySummaryUseCase | → `domain/usecase/summary/` |
| NavRoutes.BottleDetail | → `NavRoutes.NewFeed` |
| NurtlinaDatabase | v2 → v3（新增 MIGRATION_2_3 删除 bottles 表） |

---

## 2. 产品功能优先级任务规划

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

### 🔵 P3 — 打磨

| ID | 任务 | 估时 |
|---|---|---|
| **FEAT-13** | 全局 Toast/Snackbar 错误提示统一 | 2h |
| **FEAT-14** | 时区变化 / 手动改时间场景测试 | 2h |
| **FEAT-15** | TalkBack 无障碍适配 | 3h |
| **FEAT-16** | App 内 Feedback 入口 | 2h |
| **FEAT-17** | 后端同步 Pull 分页（cursor/hasMore） | 3h |

---

## 3. 付费墙设计分析

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

*文档版本：v3.0*
*分析日期：2026-07-10*
