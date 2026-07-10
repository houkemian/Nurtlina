# Nurtlina 项目评估、PRD 与后续任务规划

> 评估日期：2026-07-10
> 项目代号：Nurtlina — Baby Feeding Tracker
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

**Nurtlina** 是一款面向海外新手父母的 Android 婴儿喂养记录与护理追踪工具。核心功能是 **极简喂奶记录**——帮助用户在最少操作下快速记录每次喂奶的类型、奶量和时间。

> **当前实施状态**：喂奶记录管理功能已实现。`NewBottleSheet` 入口已改为直接创建 FeedLog（喂奶记录），`FeedingStatusCard` 展示上次喂奶状态和下次喂奶倒计时。原有的 Bottle 计时器/状态机基础设施（BottleEntity、BottleStateMachine、BottleNotificationScheduler、ActiveBottleCard 等）仍在代码库中但已不再作为主路径使用——Bottle 完成时自动转为 FeedLog。

### 1.2 一句话定义

> 一个简单、可信、无压力的婴儿喂奶记录和护理追踪 App。

### 1.3 商业模式

| 层级 | 价格 | 权益 |
|---|---|---|
| Free（广告支持） | $0 | 1 个宝宝、基础喂奶/尿布/睡眠记录、今日统计、默认小组件、广告 |
| Pro Monthly | $2.99/月 | 无广告、多宝宝、全部历史、高级统计、导出、备份、小组件主题 |
| Pro Yearly | $19.99/年 | 同上 |
| Pro Lifetime | $29.99 | 同上，一次性买断 |

### 1.4 目标市场

- 首发地区：美国、英国、加拿大、澳大利亚、新西兰
- 首发语言：English、Spanish、Simplified Chinese、German、French
- 平台：Android-only（minSdk 26, targetSdk 35）
- 分发渠道：Google Play

### 1.5 核心差异化

1. **极简喂奶记录**：1-2 步完成，预设奶量一键记录
2. **夜间友好**：暗色模式、大按钮、单手操作、夜间不弹广告
3. **本地优先**：核心记录离线可用，网络恢复后异步同步
4. **多语言首发**：5 种语言同步上线
5. **权威参考引用**：基于 CDC/AAP/NHS 公开指南的参考信息

---

## 2. 当前实施状态评估

### 2.1 总体评估

**项目已进入 MVP 后端集成中期。喂奶记录管理功能已实现并投入使用。Bottle 系统作为遗留基础设施仍存在于代码库中，完成 Bottle→FeedLog 自动转换。**

| 维度 | 完成度 | 状态 |
|---|---|---|
| Android 数据层 (Room/Repository) | 90% | ✅ FeedLog/ DiaperLog/ SleepLog 完成；BottleEntity 仍存在 |
| Android 领域层 (UseCase) | 85% | ✅ LogFeedUseCase 为主路径；BottleStateMachine 遗留 |
| Android UI 层 (Compose Screens) | 80% | ✅ FeedingStatusCard 为主；ActiveBottleCard 遗留 |
| Android 通知系统 | 85% | ✅ NextFeedNotificationScheduler 为主；BottleNotificationScheduler 遗留 |
| Android 同步系统 | 80% | ⚠️ 需清理旧 Bottle 同步路径 |
| Android 商业化 (Billing/Ads) | 70% | ⚠️ RevenueCat 已集成，AdMob 测试 ID |
| Android 小组件 | 20% | ❌ 未开始 |
| FastAPI 后端 | 80% | ✅ 核心 API 完成；bottles 表遗留 |
| Firebase Cloud Functions | 90% | ✅ 已完成 |
| 测试覆盖 | 50% | ⚠️ Bottle 相关测试遗留 |
| 多语言资源 | 60% | ⚠️ 含 bottle 相关字符串遗留 |
| CI/CD | 70% | ⚠️ Android CI 完成，后端部署未自动化 |
| 上架素材 | 30% | ❌ 缺少图标、截图、Feature Graphic |

### 2.2 已完成的核心能力

#### 喂奶记录管理（✅ 已实现）

**NewBottleSheet → FeedLog 直接创建**
- `NurtlinaNavHost.kt:417-423`：`NewBottleRoute.onCreate` 调用 `todayViewModel.logFeed()`
- 使用 feed 导向的字符串资源：`R.string.new_feed_title`、`R.string.action_save_feed`
- 支持奶类型选择（Formula/Breast Milk/Custom）、奶量输入、备注

**FeedingStatusCard（上次喂奶状态卡片）**
- `TodayScreen.kt:598-700`：展示上次喂奶时间、间隔、奶量
- 今日喂奶次数和总奶量
- 下次喂奶倒计时/到时提醒

**TodayViewModel 喂奶记录方法**
- `logFeed()` — 完整喂奶记录（类型+奶量+时间+备注）
- `quickLogFeed(amountMl)` — 一键预设奶量记录
- `scheduleNextFeedReminder()` — 下次喂奶提醒

**NextFeedNotificationScheduler**
- `NextFeedNotificationScheduler.kt` — 基于喂奶间隔的提醒

#### Bottle 遗留系统（⚠️ 仍存在，已降级为辅助）

以下组件仍存在于代码库中，但已不再是主用户路径：

- `BottleEntity.kt` / `BottleDao.kt` — Room 层
- `Bottle.kt` / `BottleStatus` / `BottleTransition` — Domain 模型
- `BottleStateMachine.kt` / `ExpiryCalculator.kt` / `GuidelineRule.kt` — 业务规则
- `CreateBottleUseCase.kt` / `TransitionBottleUseCase.kt` / `ObserveBottlesUseCase.kt` / `CheckAndExpireBottlesUseCase.kt` — 用例
- `BottleNotificationScheduler.kt` — 到期通知
- `ActiveBottleCard` / `BottleActionButtons` — TodayScreen 中的活跃奶瓶 UI
- `BottleDetailScreen.kt` / `BottleViewModel.kt` — 奶瓶详情页
- 后端 `bottles` 表 + `/sync/bottles` 端点
- `BottleStateMachineTest.kt` / `ExpiryCalculatorTest.kt` — 遗留测试

**Bottle→FeedLog 桥接**：当用户通过 Bottle 路径标记 "Fed" 时，`TodayViewModel.logFeedFromBottle()` 自动创建对应的 FeedLog 记录。

---

## 3. PRD 概述

### 3.1 核心用户旅程

#### 旅程 A：首次使用 → 记录第一次喂奶
1. 打开 App → 欢迎页 → 创建宝宝 → 免责声明 → 通知权限
2. 进入首页 → 点击 "+ New Feed" 按钮
3. 选择奶类型（Formula/Breast Milk）、输入奶量 → 保存
4. 首页 FeedingStatusCard 显示"上次喂奶：刚才 · 120 ml"

#### 旅程 B：夜间喂奶
1. 打开 App（自动暗色模式）
2. 看到上次喂奶信息
3. 点击 Quick Feed → 一键记录预设奶量

#### 旅程 C：快捷记录（1 步完成）
1. 首页点击 Quick Log Row 中的 "Feed" → NewBottleSheet
2. 输入奶量 → 保存（无需选择类型，默认 Formula）

### 3.2 信息架构

```
Nurtlina App
├── Onboarding (首次)
├── Main (底部 4 Tab)
│   ├── Today (首页)
│   │   ├── Baby Switcher
│   │   ├── FeedingStatusCard (上次喂奶 + 下次提醒)
│   │   ├── ActiveBottleCard (遗留 — 有活跃奶瓶时显示)
│   │   ├── + New Feed (→ NewBottleSheet → logFeed)
│   │   ├── Quick Actions: Feed / Diaper / Sleep
│   │   └── Today Summary
│   ├── Logs (时间线)
│   ├── Insights (统计)
│   └── Settings (设置)
├── BottleDetail (遗留 — 奶瓶详情)
├── Paywall
└── Sign In
```

### 3.3 数据模型（当前实施）

```
Baby (id, familyId, name, birthDate, avatarColor)
Bottle (id, babyId, milkType, amountMl, preparedAt, status, expiresAt, ...)  ← 遗留
FeedLog (id, babyId, bottleId?, feedType, amountMl, startedAt, endedAt, note) ← 主实体
DiaperLog (id, babyId, diaperType, changedAt, note)
SleepLog (id, babyId, startedAt, endedAt, note)
```

---

## 4. 功能完成度矩阵

### 4.1 喂奶记录管理（主路径）

| 功能 | 状态 | 代码位置 | 备注 |
|---|---|---|---|
| NewBottleSheet → logFeed | ✅ | `NurtlinaNavHost.kt:415-429` | 直接创建 FeedLog |
| FeedingStatusCard | ✅ | `TodayScreen.kt:598-700` | 上次喂奶+下次提醒 |
| logFeed() | ✅ | `TodayViewModel.kt:196-218` | 完整记录+提醒 |
| quickLogFeed() | ✅ | `TodayViewModel.kt:221-245` | 一键预设奶量 |
| 喂奶记录 CRUD | ✅ | `LogFeedUseCase.kt` + `FeedLogDao` | 完整 |
| 下次喂奶提醒 | ✅ | `NextFeedNotificationScheduler.kt` | 间隔可配置 |
| Logs 时间线 | ✅ | `LogsScreen.kt` | 类型筛选 |
| 今日统计 | ✅ | `GetTodaySummaryUseCase` | 奶量+次数 |

### 4.2 Bottle 遗留系统（辅助路径）

| 组件 | 状态 | 说明 |
|---|---|---|
| BottleEntity/BottleDao | ⚠️ 遗留 | 仍在 Room 中 |
| BottleStateMachine | ⚠️ 遗留 | 状态机规则完整 |
| ActiveBottleCard | ⚠️ 遗留 | 有活跃 Bottle 时仍渲染 |
| Bottle→FeedLog 桥接 | ✅ | MarkFed 时自动创建 FeedLog |
| BottleNotificationScheduler | ⚠️ 遗留 | 到期通知仍会触发 |
| BottleDetailScreen | ⚠️ 遗留 | 详情页仍可访问 |

### 4.3 其他功能

| 功能 | 状态 | 备注 |
|---|---|---|
| 尿布记录 (CRUD) | ✅ 85% | DiaperLog 完整 |
| 睡眠记录 (CRUD) | ✅ 85% | SleepLog 完整 + 活跃计时器 |
| Onboarding | ✅ 90% | 5 步流程完整 |
| Pro Paywall | ✅ 80% | RevenueCat 集成 |
| AdMob Banner | ✅ | 测试 ID |
| 暗色模式 | ✅ 80% | Material 3 主题 |
| 评分弹窗 | ✅ 90% | RatingPromptEligibility 完整 |
| 数据同步 | ✅ 75% | FastAPI push/pull |
| 桌面小组件 | ❌ 0% | 未实现 |
| CSV 导出 | ⚠️ 70% | 后端可用，前端入口空回调 |
| FAQ 页面 | ❌ 0% | 空回调 |
| Privacy Policy / Terms | ❌ 0% | URL 页面未创建 |

---

## 5. 技术债务与风险

| 债务项 | 严重程度 | 描述 | 建议 |
|---|---|---|---|
| **Bottle 遗留代码** | P1 | BottleEntity, BottleStateMachine, BottleNotificationScheduler, ActiveBottleCard, BottleDetailScreen 等不再需要的代码仍存在 | 逐步清理：先移除 UI 渲染，再移除业务逻辑，最后移除数据层 |
| **Bottle 相关测试** | P1 | BottleStateMachineTest, ExpiryCalculatorTest 测试不再需要的代码 | 随 Bottle 代码一起清理 |
| **后端 bottles 表** | P1 | PostgreSQL bottles 表 + /sync/bottles 端点 | 随前端清理后移除 |
| 硬编码英文 | P1 | 部分 Compose UI 中仍有英文字符串 | 全量扫描替换 |
| 测试覆盖不足 | P1 | 集成和 UI 测试不足 | 补齐关键路径测试 |
| 后端 CI/CD 未自动化 | P2 | GitHub Actions 未配置部署 | 配置 Cloud Run 自动部署 |

---

## 6. 后续任务规划

### Phase 0：Bottle 遗留代码清理（1-2 天）

| ID | 任务 | 估时 | 依赖 |
|---|---|---|---|
| CLN-1 | TodayScreen: 移除 ActiveBottleCard + BottleActionButtons 渲染 | 1h | 无 |
| CLN-2 | TodayViewModel: 移除 activeBottles 观察 + transitionBottle + logFeedFromBottle | 1h | CLN-1 |
| CLN-3 | 移除 BottleDetailScreen + BottleViewModel | 0.5h | CLN-1 |
| CLN-4 | NavRoutes: 移除 BottleDetail route | 0.5h | CLN-3 |
| CLN-5 | 移除 BottleNotificationScheduler | 0.5h | CLN-2 |
| CLN-6 | 移除 BottleStateMachine + ExpiryCalculator + GuidelineRule | 0.5h | CLN-2 |
| CLN-7 | 移除 CreateBottleUseCase + TransitionBottleUseCase + ObserveBottlesUseCase + CheckAndExpireBottlesUseCase | 1h | CLN-2 |
| CLN-8 | 移除 RoomBottleRepository + BottleRepository 接口 | 0.5h | CLN-7 |
| CLN-9 | 移除 BottleEntity + BottleDao + Room migration | 1h | CLN-8 |
| CLN-10 | 移除 Bottle.kt + BottleStatus + BottleTransition 领域模型 | 0.5h | CLN-7 |
| CLN-11 | 移除 RemoteBottleDto | 0.5h | CLN-8 |
| CLN-12 | 移除 BottleStateMachineTest + ExpiryCalculatorTest | 0.5h | CLN-6 |
| CLN-13 | 后端: 移除 bottles 表 + /sync/bottles + migration | 1h | CLN-9 |
| CLN-14 | NewBottleSheet 重命名为 QuickFeedSheet（可选—低优先级） | 1h | CLN-7 |
| CLN-15 | 全量编译验证 + 冒烟测试 | 1h | 全部 |

**估时总计：约 11h（约 1.5 个工作日）**

### Phase 1：交互缺口补齐

| ID | 任务 | 估时 |
|---|---|---|
| FEAT-1 | 实现 Quick Feed 预设奶量按钮（60/90/120/150 ml）的 TodayScreen 直接入口 | 3h |
| FEAT-2 | Logs 点击查看/编辑日志详情（LogEditSheet） | 2h |
| FEAT-3 | Logs 日期选择器 | 1h |
| FEAT-4 | Onboarding 通知权限请求实现 | 1h |
| FEAT-5 | CSV 导出前端实现 | 3h |
| FEAT-6 | FAQ 页面内容 | 2h |

### Phase 2：体验提升

| ID | 任务 | 估时 |
|---|---|---|
| FEAT-7 | 夜间模式专用简化布局 | 4h |
| FEAT-8 | 桌面小组件（Glance API） | 6h |
| FEAT-9 | Insights Pro 数据源实现 | 3h |
| FEAT-10 | Insights oz 单位支持 | 0.5h |

### Phase 3：商业化 + 上架

| ID | 任务 | 估时 |
|---|---|---|
| FEAT-11 | AdMob 真实 ID 替换 | 0.5h |
| FEAT-12 | Privacy Policy / Terms 页面创建 | 3h |
| FEAT-13 | App Icon + Feature Graphic + 截图制作 | 8h |
| FEAT-14 | Google Play Listing + Data Safety 表单 | 3h |
| FEAT-15 | Closed Testing 发布 | 2h |

---

## 7. 里程碑与时间线

| 里程碑 | 预计时间 | 交付物 |
|---|---|---|
| M0: Bottle 遗留代码清理 | Week 1 Day 2 | 编译通过，无 Bottle 渲染 |
| M1: 核心体验可用 | Week 2 Day 5 | 喂奶记录路径流畅，交互缺口补齐 |
| M2: 商业化闭环 | Week 3 Day 5 | Free/Pro 体系完成，真实 AdMob |
| M3: 小组件可用 | Week 4 Day 5 | 桌面小组件工作正常 |
| M4: 内测发布 | Week 5 Day 3 | Closed Testing 发布 |
| M5: 生产发布 | Week 6 Day 5 | 灰度上线 |

---

## 8. 附录：文件清单

### 8.1 喂奶记录主路径文件（✅ 活跃使用）

- `ui/navigation/NurtlinaNavHost.kt:415-429` — NewBottleRoute → logFeed
- `ui/today/TodayScreen.kt:598-700` — FeedingStatusCard
- `ui/today/TodayScreen.kt:1168-1247` — QuickLogRow
- `ui/today/TodayViewModel.kt:196-245` — logFeed() + quickLogFeed()
- `ui/bottle/NewBottleSheet.kt` — Feed 创建 UI（仍命名为 NewBottle）
- `domain/usecase/feed/LogFeedUseCase.kt` — 喂奶记录用例
- `data/local/entity/FeedLogEntity.kt` — Room 实体
- `data/local/dao/FeedLogDao.kt` — DAO
- `core/notification/NextFeedNotificationScheduler.kt` — 下次喂奶提醒

### 8.2 Bottle 遗留文件（⚠️ 待清理）

- `domain/model/Bottle.kt` + `Enums.kt` (BottleStatus, BottleTransition)
- `domain/guideline/BottleStateMachine.kt` + `ExpiryCalculator.kt` + `GuidelineRule.kt`
- `domain/usecase/bottle/CreateBottleUseCase.kt` + `TransitionBottleUseCase.kt` + `ObserveBottlesUseCase.kt` + `CheckAndExpireBottlesUseCase.kt`
- `domain/repository/BottleRepository.kt`
- `data/local/entity/BottleEntity.kt` + `data/local/dao/BottleDao.kt`
- `data/repository/RoomBottleRepository.kt`
- `data/remote/dto/RemoteBottleDto.kt`
- `core/notification/BottleNotificationScheduler.kt`
- `ui/bottle/BottleDetailScreen.kt` + `BottleViewModel.kt`
- `ui/today/TodayScreen.kt:839-1100` — ActiveBottleCard + BottleActionButtons
- `ui/today/TodayViewModel.kt:99-112` — activeBottles
- `ui/today/TodayViewModel.kt:164-194` — transitionBottle + logFeedFromBottle

---

*文档版本：v3.0*
*生成日期：2026-07-10*
