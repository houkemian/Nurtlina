# Nurtlina 产品功能任务规划 & 付费墙设计分析

> 分析日期：2026-07-09
> 分析范围：全部 Kotlin 源代码（131 个文件）、FastAPI 后端代码、Firebase Cloud Functions
> 关联文档：[[Nurtlina_PRD_Evaluation_and_Task_Plan]]

---

## 目录

1. [当前功能实现全景](#1-当前功能实现全景)
2. [代码审查发现的严重问题](#2-代码审查发现的严重问题)
3. [产品功能优先级任务规划](#3-产品功能优先级任务规划)
4. [付费墙设计分析](#4-付费墙设计分析)
5. [付费墙改进优先级汇总](#5-付费墙改进优先级汇总)
6. [改进后的 Paywall 推荐布局](#6-改进后的-paywall-推荐布局)

---

## 1. 当前功能实现全景

图例：✅ 已实现  ⚠️ 部分实现（有缺口）  ❌ 未实现  🔶 技术债务

### 1.1 Today 首页（核心页面）

| 子功能 | 状态 | 代码位置 | 缺口 |
|---|---|---|---|
| Baby Switcher（宝宝切换） | ✅ | `TodayScreen.kt:538-592` | — |
| FeedingStatus 卡片 | ✅ | `TodayScreen.kt:598-700` | 上一次喂奶数据、下次喂奶倒计时已有 |
| ActiveBottle 卡片 + 倒计时 | ✅ | `TodayScreen.kt:839-939` | — |
| 奶瓶操作按钮（按状态） | ✅ | `TodayScreen.kt:961-1099` | 4 种状态各有不同按钮布局 |
| 活跃睡眠卡片 + 计时器 | ✅ | `TodayScreen.kt:736-833` | 每秒刷新的实时计时器 |
| Quick Log 快捷记录 | ✅ | `TodayScreen.kt:1168-1247` | Feed/Diaper/Sleep 三按钮 |
| Today Summary 统计卡片 | ✅ | `TodayScreen.kt:1253-1341` | — |
| AdMob Banner | ✅ | `TodayScreen.kt:1347-1362` | 测试 ID，需替换真实 ID |
| 操作反馈动画 | ✅ | `TodayScreen.kt:406-520` | 俏皮的滑入/滑出反馈条 |
| 评分弹窗 | ✅ | `TodayScreen.kt:365-404` | 完整的三选项弹窗 |
| 空状态引导 | ⚠️ | `TodayScreen.kt:1106-1162` | 有 EmptyBottleCard，但未被调用（route 层 `activeBottle=null` 时未使用） |
| 夜间模式大按钮 | ⚠️ | 内联在 TodayScreen 中 | `nightModeEnabled` 参数控制按钮高度，但没有专用简化版布局 |
| Pro 广告隐藏 | ✅ | `NurtlinaNavHost.kt:358` | `showAds = !isPro && !nightModeEnabled` |

### 1.2 奶瓶系统

| 子功能 | 状态 | 代码位置 | 缺口 |
|---|---|---|---|
| 创建奶瓶（NewBottleSheet） | ✅ | `NewBottleSheet.kt` + route | — |
| 奶瓶状态机 + 转换 | ✅ | `BottleStateMachine.kt`, `TransitionBottleUseCase.kt` | — |
| 到期时间计算 | ✅ | `ExpiryCalculator.kt` | — |
| Bottle Detail 页面 | ✅ | `BottleDetailScreen.kt` | — |
| 编辑 prepared_at | ❌ | `BottleDetailRoute:452` | `onEditPreparedTime = {}` 是空回调 |
| 到期前/到期通知 | ✅ | `BottleNotificationScheduler.kt` | — |
| 下次喂奶提醒 | ✅ | `NextFeedNotificationScheduler.kt` | — |
| 设备重启恢复提醒 | ✅ | `BootReceiver.kt`, `RescheduleNotificationsWorker.kt` | — |
| 通知点击跳转 | ✅ | `NotificationReceiver.kt` | — |
| **关键 Bug**：NewBottleSheet 实际调用的是 `logFeed` 而非创建 Bottle | 🔴 | `NurtlinaNavHost.kt:415-429` | `NewBottleRoute` 内 `onCreate` 调用的是 `todayViewModel.logFeed()`，不是 `CreateBottleUseCase`。这意味着新奶瓶没有进入 Bottle 状态机 |

### 1.3 Logs 记录页

| 子功能 | 状态 | 代码位置 | 缺口 |
|---|---|---|---|
| 时间线展示 | ✅ | `LogsScreen.kt` | — |
| 类型筛选（All/Feed/Diaper/Sleep） | ✅ | `LogsViewModel.kt` | — |
| 日期前后切换 | ✅ | `LogsViewModel.kt` | — |
| 编辑日志 | ⚠️ | `LogEditSheet.kt` | 存在但 route 层 `onEntryClick = {}` 空回调 |
| 删除日志 | ✅ | `NurtlinaNavHost.kt:473-479` | — |
| 日期选择器（跳转） | ❌ | `NurtlinaNavHost.kt:471` | `onPickDate = {}` 空回调 |
| 日期快速跳转（今天） | ❌ | — | 未实现 |

### 1.4 Insights 统计页

| 子功能 | 状态 | 代码位置 | 缺口 |
|---|---|---|---|
| 今日摘要卡片 | ✅ | `InsightsScreen.kt:153-212` | 4 卡片布局 |
| 免责声明 | ✅ | `InsightsScreen.kt:260-288` | — |
| 7/14/30 天切换 | ✅ | `InsightsScreen.kt:294-320` | FilterChip 三选一 |
| 趋势柱状图 | ✅ | `InsightsScreen.kt:326-423` | 手绘 Canvas 柱状图 |
| 平均奶量卡片 | ✅ | `InsightsScreen.kt:429-458` | — |
| Pro 升级横幅 | ✅ | `InsightsScreen.kt:464-515` | — |
| **关键缺口**：Pro 数据源 | 🔴 | `NurtlinaNavHost.kt:494` | `proData = null` 硬编码为 null！Pro 趋势图表永远不会渲染实际数据 |
| 单位 oz 支持 | ⚠️ | `NurtlinaNavHost.kt:493` | `useOz = false` 硬编码为 ml |

### 1.5 Settings 设置页

| 子功能 | 状态 | 代码位置 | 缺口 |
|---|---|---|---|
| 账号状态 + Sign In/Out | ✅ | `SettingsScreen.kt:286-317` | — |
| 宝宝资料编辑 | ✅ | `SettingsScreen.kt:323-399` | 只支持改名，不支持改生日/颜色 |
| Pro 状态显示 | ✅ | `SettingsScreen.kt:150-173` | Free/Pro 不同样式 |
| 单位选择（ml/oz） | ✅ | `SettingsScreen.kt:406-434` | — |
| 指南地区选择 | ✅ | `SettingsScreen.kt:440-474` | US/UK/Custom |
| 语言选择（5 种） | ✅ | `SettingsScreen.kt:480-519` | — |
| 通知开关 + 提醒时间 | ✅ | `SettingsScreen.kt:208-239` | — |
| 夜间模式开关 | ✅ | `SettingsScreen.kt:233-239` | — |
| CSV 导出 | ❌ | `NurtlinaNavHost.kt:559` | `onExportCsv = {}` 空回调 |
| 备份/同步 | ⚠️ | `NurtlinaNavHost.kt:560` | `onBackupClick` 触发全量同步，但无备份配置 UI |
| FAQ 页 | ❌ | `NurtlinaNavHost.kt:563` | `onFaqClick = {}` 空回调 |
| 安全来源页 | ✅ | `SettingsScreen.kt:780-909` | 完整，含 CDC/AAP/NHS 链接和免责声明 |
| Privacy Policy / Terms | ⚠️ | 链接已配置 | URL `nurtlina.app` 的页面尚未创建 |
| 联系支持 | ✅ | 打开邮件客户端 | `support@nurtlina.app` |

### 1.6 Onboarding 引导

| 子功能 | 状态 | 代码位置 | 缺口 |
|---|---|---|---|
| Welcome → CreateBaby → Guideline → Disclaimer → Notification | ✅ | `NurtlinaNavHost.kt:207-226` | 5 步流程完整 |
| 通知权限请求 | ⚠️ | 在 Settings 中处理 | Onboarding 阶段不阻塞权限请求，但 route 中 `onRequestNotificationPermission` 是空回调 |

### 1.7 商业化

| 子功能 | 状态 | 代码位置 | 缺口 |
|---|---|---|---|
| RevenueCat 集成 | ✅ | `EntitlementManager.kt` | 完整：configure/identify/purchase/restore |
| Paywall 页面 | ✅ | `PaywallScreen.kt` | 3 个定价选项，7 个 Pro 功能列表 |
| Pro 权益缓存（3 天宽限期） | ✅ | `EntitlementManager.kt:61,191` | — |
| AdMob Banner | ✅ | `TodayScreen.kt:1347-1362` | 测试 ID |
| Pro 去广告 | ✅ | `NurtlinaNavHost.kt:358` | — |
| 多宝宝 Pro 门控 | ⚠️ | `NurtlinaNavHost.kt:364-370` | 非 Pro 点"Add Baby"直接跳 Paywall，但 Pro 用户的 baby creation screen 未实现 |
| 导出 Pro 门控 | ❌ | — | 无门控，export 回调本身为空 |
| 高级统计 Pro 门控 | ⚠️ | `InsightsScreen` | UI 层面已区分 Free/Pro，但数据源为空 |
| 自定义提醒 Pro 门控 | ❌ | — | FeedReminderConfig 有数据结构，设置 UI 未做 Pro 限制 |

### 1.8 小组件 (Widget)

| 子功能 | 状态 | 代码位置 | 缺口 |
|---|---|---|---|
| 小组件（全部） | ❌ | — | 0 行代码，AndroidManifest 中也无声明 |

### 1.9 测试

| 子功能 | 状态 | 缺口 |
|---|---|---|
| `ExpiryCalculatorTest` | ✅ | Formula/BreastMilk 规则测试 |
| `BottleStateMachineTest` | ✅ | 状态转换测试 |
| `SyncQueueWriterTest` | ✅ | — |
| `EntitlementCacheRepositoryTest` | ✅ | — |
| `RatingPromptEligibilityTest` | ✅ | — |
| `AuthTokenInterceptorTest` | ✅ | — |
| `AppStartupCoordinatorTest` | ✅ | — |
| UI 测试 | ❌ | 0 个 Compose UI 测试 |
| 集成测试 | ❌ | 0 个端到端测试 |
| 后端测试 | ⚠️ | 仅健康检查和少量规则测试 |

---

## 2. 代码审查发现的严重问题

| # | 严重度 | 问题 | 位置 |
|---|---|---|---|
| **1** | 🔴 P0 | **"创建奶瓶"实际创建的是 FeedLog，不是 Bottle**。`NewBottleRoute.onCreate` 调用 `todayViewModel.logFeed()`，绕过了整个 Bottle 状态机。这意味着用户从首页点"+ New Bottle"后，创建的是一个喂奶记录，而不是一个带计时器的 Bottle。核心差异化功能断裂。 | `NurtlinaNavHost.kt:415-429` |
| **2** | 🔴 P0 | **Insights Pro 趋势数据硬编码为 null**。即使你是 Pro 用户，也看不到任何趋势图表数据。 | `NurtlinaNavHost.kt:494` |
| **3** | 🟠 P1 | **Quick Feed 快捷记录喂 120ml** 但代码中 `quickLogFeed` 没有被 UI 绑定到具体奶量按钮上。TodayScreen 的 Quick Log "Feed" 按钮实际跳转到了 `onNewBottle`（即 NewBottleSheet），而不是调用 `quickLogFeed`。 | `TodayScreen.kt:302` |
| **4** | 🟠 P1 | **EmptyBottleCard 未被调用**。当 `activeBottle == null` 时，TodayScreen 不会渲染空状态引导卡片。 | `TodayScreen.kt:255-277`，`if (state.activeBottle != null)` 包裹了卡片但没有 else 分支 |
| **5** | 🟡 P2 | **BottleDetail 编辑时间功能为空**。`onEditPreparedTime = {}` | `NurtlinaNavHost.kt:452` |
| **6** | 🟡 P2 | **Logs 页面无法点击查看/编辑日志详情**。`onEntryClick = {}` | `NurtlinaNavHost.kt:472` |
| **7** | 🟡 P2 | **Onboarding 通知权限请求是空回调**。`onRequestNotificationPermission` 未绑定实际权限请求。 | `NurtlinaNavHost.kt:296-298` |

---

## 3. 产品功能优先级任务规划

### 🔴 P0 — 修复核心功能断裂（必须在任何新功能前完成）

| ID | 任务 | 估时 | 依赖 |
|---|---|---|---|
| **FIX-1** | 修复 NewBottleSheet：应调用 `CreateBottleUseCase` 创建 Bottle 进入状态机，而非 `logFeed`。需要保留现有的"喂奶即记录"快速路径，同时让 Bottle 先进入 NotStarted 状态。 | 2h | 无 |
| **FIX-2** | 实现 Insights Pro 数据源：从 Repository 拉取 7/14/30 天聚合数据，填充 `DailyDataPoint` 列表。 | 3h | 无 |
| **FIX-3** | TodayScreen 添加空状态分支：`activeBottle == null` 时渲染 `EmptyBottleCard`。 | 0.5h | 无 |

### 🟠 P1 — 补齐关键交互缺口

| ID | 任务 | 估时 | 依赖 |
|---|---|---|---|
| **FEAT-1** | 实现 Quick Feed 一键记录预设奶量功能（60/90/120/150 ml），不进入 NewBottleSheet。 | 3h | 无 |
| **FEAT-2** | 实现 BottleDetail 编辑 prepared_at 时间功能。 | 2h | 无 |
| **FEAT-3** | 实现 Logs 点击查看/编辑日志详情（唤起 LogEditSheet）。 | 2h | 无 |
| **FEAT-4** | 实现 Logs 日期选择器（DatePickerDialog 跳转任意日期）。 | 1h | 无 |
| **FEAT-5** | Onboarding 流程中正确请求通知权限（非阻塞式）。 | 1h | 无 |
| **FEAT-6** | 实现 FAQ 页面内容。 | 2h | 无 |
| **FEAT-7** | 实现 CSV 导出（前端调用后端 API，生成本地文件并触发分享 Sheet）。 | 3h | 无 |

### 🟡 P2 — 体验提升

| ID | 任务 | 估时 | 依赖 |
|---|---|---|---|
| **FEAT-8** | 夜间模式专用简化布局：更大的按钮、更大字号倒计时、隐藏非核心信息。 | 4h | 无 |
| **FEAT-9** | 实现桌面小组件（Glance API）：显示活跃奶瓶倒计时 + 状态。 | 6h | 无 |
| **FEAT-10** | 小组件点击跳转对应奶瓶（Deep Link）。 | 1h | FEAT-9 |
| **FEAT-11** | 实现多尺寸小组件（2x1, 3x1, 4x1）。 | 2h | FEAT-9 |
| **FEAT-12** | Pro 用户多宝宝创建页面（OnboardingBabyOnly 简化版）。 | 2h | 无 |
| **FEAT-13** | Insights 页面支持 oz 单位。 | 0.5h | 无 |
| **FEAT-14** | 宝宝资料编辑支持修改出生日期和颜色。 | 1h | 无 |

### 🔵 P3 — 打磨与边缘场景

| ID | 任务 | 估时 | 依赖 |
|---|---|---|---|
| **FEAT-15** | 全局 Toast/Snackbar 错误提示统一管理。 | 2h | 无 |
| **FEAT-16** | 时区变化、手动改时间场景测试与保护。 | 2h | 无 |
| **FEAT-17** | TalkBack 无障碍完整适配。 | 3h | 无 |
| **FEAT-18** | App 内 Feedback 入口（非邮件，轻量级表单）。 | 2h | 无 |
| **FEAT-19** | 后端同步 Pull Cursor 完整实现（分页拉取，避免全量）。 | 3h | 无 |
| **FEAT-20** | 旧 Firestore 同步代码清理。 | 2h | 无 |

---

## 4. 付费墙设计分析

### 4.1 当前 Paywall 实现架构

```
触发源 (多个入口)                展示层                  购买层                 权益层
────────────────────────────────────────────────────────────────────────────────
Today: Add Baby ───┐
Settings: Upgrade ──┼──→ PaywallScreen ──→ PaywallViewModel ──→ EntitlementManager
Insights: Upgrade ──┤         │                    │                    │
Logs: (未来) ──────┘         │                    │              RevenueCat SDK
                              │                    │                    │
                        3 个定价选项           subscribe(plan)     awaitPurchase()
                        + Restore             restorePurchases()  awaitRestore()
                        + Privacy/Terms                          identify(userId)
```

### 4.2 当前 Paywall UI 结构

文件：`PaywallScreen.kt`（258 行）

```
┌──────────────────────────────┐
│  [X] 关闭                     │
│                              │
│        ⭐ (56dp 图标)         │
│   "Make baby care tracking   │
│         easier"              │
│                              │
│  ┌────────────────────────┐  │
│  │ 🚫 No ads              ✓│  │
│  │ 👨‍👩‍👧 Multiple babies     ✓│  │
│  │ 📜 Full history         ✓│  │
│  │ 📥 Export data          ✓│  │
│  │ ☁️ Cloud backup         ✓│  │
│  │ 🔔 Custom reminders     ✓│  │
│  │ 🧩 Widget themes        ✓│  │
│  └────────────────────────┘  │
│                              │
│  ┌─ Monthly — $2.99 ──────┐  │
│  ┌─ Yearly — $19.99 🏷 BEST VALUE ─┐ ← 默认选中
│  ┌─ Lifetime — $29.99 ────┐  │
│                              │
│  [   Subscribe / Purchase  ]  │  ← 单一 CTA，文案不区分方案
│                              │
│    Restore Purchases         │
│                              │
│  Disclaimer text             │
│  Privacy Policy · Terms      │
└──────────────────────────────┘
```

### 4.3 当前 Paywall 的优点

1. **结构清晰**：功能列表 → 定价选项 → CTA → 恢复 → 合规链接，信息层次合理
2. **RevenueCat 集成扎实**：`EntitlementManager` 实现了 configure / identify / purchase / restore / 宽限期缓存（3 天 offline grace period）
3. **合规**：底部有免责声明、Privacy Policy、Terms 链接
4. **Yearly 默认选中 + "BEST VALUE" 徽章**：鼓励年订阅，提高 LTV
5. **Lifetime 选项**：低门槛一次性买断，适合育儿场景（用户知道需求时间有限）
6. **ProStatus 枚举完整**：UNKNOWN / FREE / MONTHLY / YEARLY / LIFETIME

### 4.4 当前 Paywall 的不足与改进方向

#### 🔴 严重不足

**1. 没有免费试用 (Free Trial) 选项**

竞品标配 3-7 天免费试用，当前 Paywall 完全没有。

**改进**：添加试用标签，如 "3-day free trial, then $2.99/month"。RevenueCat 支持在 Offering 中配置试用的 Package。

**2. 没有价格对比锚定**

当前三个价格并排展示，用户缺少 "为什么要选 Yearly 而不是 Monthly" 的说服力。

**改进**：在每个选项下方显示等效月价格 / 节省百分比：

```
Monthly  — $2.99/mo
Yearly   — $1.67/mo  (Save 44%)  ← 🏷 BEST VALUE
Lifetime — $29.99 one-time (≈ 15 months of yearly)
```

**3. CTA 按钮文案不区分方案**

当前 `subscribe_cta` 对三个方案都是同一个文案。选中 Monthly 时按钮应该写 "Start Free Trial" 或 "Subscribe Monthly"，选中 Lifetime 时应写 "Buy Once — $29.99"。

**改进**：按钮文案动态变化：

- Monthly → `Start 3-Day Free Trial`
- Yearly → `Try 7 Days Free, Then $19.99/year`
- Lifetime → `Unlock Forever — $29.99`

**4. 购买成功/失败后无反馈，页面不自动关闭**

`PaywallRoute` 没有监听 `proStatus` 变化来自动 dismiss。用户购买成功后需要手动点 X 关闭。

**改进**：在 `PaywallRoute` 中添加 `LaunchedEffect(isPro)` —— 当 Pro 状态变为 true 时自动 popBackStack 并显示购买成功提示。

**5. 购买中无加载状态**

点击 Subscribe 按钮后无任何 loading 指示，用户可能多次点击。

**改进**：添加 `isPurchasing` 状态，按钮显示 CircularProgressIndicator，禁用点击。

#### 🟠 中等不足

**6. 缺少社会证明 (Social Proof)**

没有评分、用户数、评价等信任信号。

**改进**：

- "Join 10,000+ parents"
- "⭐ 4.8 · Trusted by parents worldwide"
- 或在功能列表上方添加一句简短的用户推荐语

**7. Paywall 触发点过少**

当前只有 3 个触发点（Add Baby、Settings Upgrade、Insights Upgrade）。对比 PRD 设计的 8 个触发点：

| PRD 触发点 | 当前实现 |
|---|---|
| 添加第二个宝宝 | ✅ 已实现 |
| 查看 30 天以上历史 | ❌ Logs 页面无限制/无触发 |
| 导出 CSV/PDF | ❌ 回调为空 |
| 开启云备份 | ❌ 无触发 |
| 使用小组件主题 | ❌ 小组件未实现 |
| 自定义保存规则 | ❌ 未实现 |
| 去广告入口 | ❌ 无独立入口 |
| 查看高级统计 | ✅ Insights 有升级横幅 |

**改进**：每个 Pro 功能入口都应有明确的 paywall 触发 + 上下文说明（"Why go Pro for this feature?"）

**8. 无 AB 测试框架**

价格、文案、布局都是硬编码的。无法测试 Lifetime 放前面 vs 放后面的转化差异。

**改进**：接入 Firebase Remote Config 控制 paywall 布局变体：

- Variant A: Lifetime 突出（育儿场景用户偏好一次买断）
- Variant B: Yearly 突出（提高 LTV）
- Variant C: Monthly + Free Trial 突出（降低首次付费门槛）

**9. 无退出挽留机制**

用户点 X 关闭 paywall 时没有任何挽留。

**改进**：在关闭前弹出二次确认：

> "Wait! You can try Pro free for 3 days. No charges until the trial ends."
>
> [Start Free Trial] [No Thanks]

**10. 功能列表缺乏差异化和场景化**

当前 7 个功能项是对称列表（图标 + 文字 + 勾），没有突出"这是你当前缺失的"。

**改进**：根据触发上下文高亮相关功能。例如：

- 从 "Add Baby" 进入 → 高亮 "Multiple Babies" 那一行
- 从 Insights 进入 → 高亮 "Advanced 30-day charts"

#### 🟡 轻度不足

**11. Pro 功能列表与 PRD 不一致**

| PRD 定义的 Pro 功能 | Paywall 展示 |
|---|---|
| No Ads | ✅ |
| Multiple Babies | ✅ |
| Full History | ✅ |
| CSV/PDF Export | ✅ |
| Cloud Backup | ✅ |
| Custom Reminders | ✅ |
| Widget Themes | ✅ |
| **Custom Timer Rules** | ❌ 未展示 |
| **Priority Support** | ❌ 未展示 |

**12. 缺少 Lifetime 专属卖点**

Lifetime 的独特价值（"pay once, use forever — no subscription to manage"）没有在 UI 中体现。

**改进**：在 Lifetime 选项旁添加一行小字 "One payment. All features. Forever."

**13. 底部没有 App 图标/品牌标识**

纯文字 paywall 缺乏品牌感。

**改进**：顶部添加 App 图标（与启动图标一致）+ "Nurtlina Pro" 标题。

---

## 5. 付费墙改进优先级汇总

| 优先级 | 改进项 | 影响 | 难度 | 估时 |
|---|---|---|---|---|
| **P0** | 购买成功后自动关闭 + 反馈 | 用户体验断裂 | 低 | 10 min |
| **P0** | 购买中 Loading 状态 | 防止重复购买 | 低 | 20 min |
| **P0** | CTA 按钮文案动态变化 | 转化率 | 低 | 30 min |
| **P0** | 价格对比锚定（月等效价/节省%） | 转化率 | 低 | 30 min |
| **P1** | 免费试用选项 | 转化率 | 中（需 RevenueCat 配置） | 1h |
| **P1** | 退出挽留弹窗 | 转化率 | 低 | 1h |
| **P1** | 扩展 Paywall 触发点（3→8 个） | 曝光率 | 中 | 3h |
| **P1** | 根据触发上下文高亮相关功能 | 转化率 | 中 | 2h |
| **P2** | 社会证明（评分/用户数） | 信任感 | 低 | 1h |
| **P2** | Firebase Remote Config AB 测试 | 数据驱动优化 | 中 | 3h |
| **P2** | 功能列表补全（Custom Rules + Priority Support） | 与 PRD 一致 | 低 | 10 min |
| **P3** | Lifetime 专属卖点文案 | 转化率 | 低 | 10 min |
| **P3** | 品牌标识（App 图标） | 品牌感 | 低 | 10 min |

---

## 6. 改进后的 Paywall 推荐布局

```
┌──────────────────────────────────┐
│  [X]                      ↗ Restore │
│                                    │
│         🍼 (App Icon)              │
│      Nurtlina Pro                  │
│                                    │
│   "Track with less stress"         │
│   ⭐ 4.8 · 10,000+ parents         │
│                                    │
│  ┌──────────────────────────────┐  │
│  │  🚫 No ads                   │  │
│  │  👨‍👩‍👧 Multiple babies  ← 📍 高亮 │  │  ← 根据触发上下文高亮
│  │  📜 Full history             │  │
│  │  📥 Export data              │  │
│  │  ☁️ Cloud backup             │  │
│  │  🔔 Custom reminders & rules │  │
│  │  🧩 Widget themes            │  │
│  │  💬 Priority support         │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌─ Free Trial ─────────────────┐  │
│  │  3 days free, then $2.99/mo  │  │
│  └──────────────────────────────┘  │
│  ┌─ Yearly ─────── 🏷 SAVE 44% ──┐  │  ← 默认选中
│  │  $1.67/mo · billed $19.99/yr  │  │  ← 等效月价
│  │  7-day free trial             │  │
│  └──────────────────────────────┘  │
│  ┌─ Lifetime ───────────────────┐  │
│  │  $29.99 one-time             │  │
│  │  Pay once. All features.      │  │  ← 专属卖点
│  │  Forever.                     │  │
│  └──────────────────────────────┘  │
│                                    │
│  [  Try 7 Days Free — Then $19.99  ]  │  ← 动态文案
│                                    │
│    Restore Purchases               │
│                                    │
│  This app provides reminders and   │
│  tracking only, not medical advice.│
│  Privacy Policy · Terms            │
└──────────────────────────────────┘
```

### 6.1 布局设计决策说明

| 设计决策 | 理由 |
|---|---|
| Yearly 默认选中 | 提高 LTV，年订阅留存率远高于月订阅 |
| 显示等效月价 | 降低价格感知，让 Yearly 选项看起来更划算 |
| Lifetime 单列 | 育儿场景用户知道需求时间有限（0-12 月），一次性买断有吸引力 |
| Free Trial 独立选项 | 降低首次付费门槛，3 天试用后很多用户会忘记取消 |
| 高亮触发上下文对应的功能 | "你来到这里是因为想添加第二个宝宝——Pro 正好支持这个" |
| 顶部品牌标识 | 增加专业感，区别于诈骗/低质量 App |
| 社会证明（评分/用户数） | 育儿类购买决策高度依赖信任 |

---

*文档版本：v1.0*
*分析日期：2026-07-09*
*关联文档：[[Nurtlina_PRD_Evaluation_and_Task_Plan]]*
