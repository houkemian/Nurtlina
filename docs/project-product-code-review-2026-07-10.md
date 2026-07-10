# Nurtlina 当前项目产品与代码评审

> 评审日期：2026-07-10  
> 评审范围：Android Kotlin/Compose、Room、本地同步队列、FastAPI 后端、Firebase/Firestore 配置、测试与现有项目文档  
> 结论等级：MVP 主路径可继续推进，但后端 Bottle 残留与发布配置是当前最大阻塞

---

## 1. 执行摘要

Nurtlina 当前已经从“奶瓶计时器”转向“FeedLog 喂养记录”主路径。Android 端的 Today 首页、新增喂奶、今日统计、下次喂养提醒、尿布/睡眠快捷记录、夜间模式、广告/Pro 边界、评分弹窗等核心体验已经具备 MVP 雏形。

但项目尚未达到可发布状态，主要问题集中在四类：

1. **后端 Bottle 清理不完整，可能导致后端导入/测试失败。**
   Android Room 已删除 `bottles` 表，但 FastAPI 后端模型、测试、导出、删除服务和迁移仍引用 Bottle。
2. **发布前配置未完成。**
   Android SDK 本地配置缺失导致 Gradle 单测无法执行；AdMob、RevenueCat、Firebase、App Icon、Google Play 商品仍存在待替换项。
3. **产品文案仍混有 Bottle Timer 心智。**
   主路径是 feeding record，但 onboarding、评分弹窗、通知/资源字符串、部分包名/组件名仍出现 bottle timer。
4. **测试覆盖与验收链路不足。**
   现有 Android 单测覆盖了评分、同步队列、启动、鉴权拦截、权益缓存等，但缺少 FeedLog CRUD、Today summary、单位换算、跨日日志、UI/通知/同步端到端测试。

建议下一步优先级：

| 优先级 | 建议 |
|---|---|
| P0 | 修复后端 Bottle 残留导致的导入/测试失败风险 |
| P0 | 配置 Android SDK / CI，使 `testDebugUnitTest` 与 `assembleDebug` 可稳定运行 |
| P1 | 完成 Bottle 文案与命名清理，统一为 FeedLog/feeding record |
| P1 | 补齐 FeedLog、Today summary、同步冲突和删除测试 |
| P1 | 完成发布配置：真实 Firebase、AdMob、RevenueCat/Google Play 商品、图标和隐私/条款页面 |

---

## 2. 产品层面评审

### 2.1 当前产品定位

当前产品方向是正确的：Nurtlina 更适合定位为“宝宝喂养与护理记录工具”，而不是“奶瓶安全计时器”。FeedLog 主路径更简单、更符合疲惫照护者的真实场景，也更低合规风险。

推荐产品定位：

> Nurtlina 是一个本地优先、离线可用、低压力的宝宝喂养与护理记录工具，帮助照护者快速记录喂奶、尿布、睡眠，并提供温和的下次喂养提醒。

### 2.2 已具备的 MVP 产品能力

| 能力 | 当前状态 | 证据 |
|---|---|---|
| Today 首页主路径 | 已具备 | `TodayScreen` 展示喂养状态、快捷操作、今日摘要 |
| 新增喂奶记录 | 已具备 | `NewFeedRoute` 调用 `TodayViewModel.logFeed()` |
| 快捷尿布/睡眠 | 已具备 | `TodayViewModel.quickLogDiaper()`、`startSleep()`、`endSleep()` |
| 下次喂养提醒 | 已具备 | `NextFeedNotificationScheduler.schedule()` |
| 夜间模式广告边界 | 基本符合 | `showAds = !isPro && !nightModeEnabled` |
| Pro 入口 | 已具备 | 多宝宝/Insights/Settings 导向 Paywall |
| 本地优先 | 基本符合 | Room 写入 + SyncQueue 后台同步 |
| 后端账号/同步基础 | 部分完成 | FastAPI init、sync push/pull、family access 校验 |
| 隐私/删除/导出 | 后端部分完成 | 后端有 deletion/export service，但前端入口仍未接通 |

### 2.3 产品风险

#### P0：主心智仍被 Bottle Timer 文案污染

虽然主功能已经是 FeedLog，资源里仍有大量 bottle timer 文案：

- onboarding 通知文案仍说 “before a bottle timer expires”
- rating prompt 仍说 “tracking bottle timers”
- 通知 channel 仍有 Bottle Timer
- 空状态仍有 “No active bottle”“start a timer”
- `NewBottleSheet` 组件名、`ui/bottle` 包名、`domain/usecase/bottle/GetTodaySummaryUseCase.kt` 仍保留旧命名

影响：

- 用户会误解产品是“奶瓶新鲜度/安全计时器”。
- 与当前 AGENTS 指令中“Bottle freshness timer / state machine 已移除”的非目标冲突。
- 增加合规风险，因为 bottle timer 更容易被理解为安全判断。

建议：

- 用户可见文案统一为 “feeding record / feed / next feed reminder”。
- 内部命名逐步从 `NewBottleSheet` 迁移为 `NewFeedSheet`，`domain/usecase/bottle` 迁移为 `domain/usecase/summary` 或 `today`。
- 移除未使用的 bottle 状态、bottle detail、bottle notification 资源 key。

#### P1：Onboarding 仍强调 guideline/timer，和记录工具定位不完全一致

当前 onboarding 仍有 “guideline region”“timer durations” 的心智。对于 MVP，如果主路径是记录和提醒，建议弱化 guideline 选择，把它放入 Settings 或后续高级设置中。

推荐 onboarding 顺序：

1. 欢迎：快速记录喂奶、尿布、睡眠
2. 宝宝昵称
3. 单位与提醒偏好
4. 免责声明
5. 通知权限说明

#### P1：Pro 权益合理，但部分权益尚未接通

Pro 文案包含 no ads、multiple babies、full history、export、backup、custom reminders、widget themes。当前风险是前端部分权益还未真正可用：

- `onExportCsv = {}` 为空回调。
- 小组件未见实现。
- 自定义提醒仅有设置基础，不完整。
- Backup/sync 入口是 `requestFullSync()`，不是清晰的“启用/关闭备份”产品流。

发布前建议：

- 未实现权益不要在 Paywall 里作为已可用能力售卖，或标注为后续能力。
- Pro 用户 no ads 已有基础边界，但还需要确认所有广告位都受 `isPro` 控制。

#### P1：评分弹窗策略方向正确，但触发文案需更新

评分弹窗已有夜间模式、通知会话、负向行为、冷却等拦截逻辑，产品方向符合“不要打断喂养场景”。但文案仍提 bottle timers，应替换为 feeding/care tracking。

推荐文案：

> If Nurtlina has been helpful for tracking feeds and baby care, would you like to rate it?

---

## 3. 代码层面评审

### 3.1 Android 架构现状

Android 端整体采用了可维护的分层：

- `domain/model`：Baby、FeedLog、DiaperLog、SleepLog、UserSettings、SyncState 等
- `domain/usecase`：LogFeed、LogDiaper、Sleep、TodaySummary
- `data/local`：Room DAO/entity/db
- `data/repository`：Room repository、API sync、Firebase auth
- `data/sync`：SyncQueueWriter、SyncQueueProcessor、SyncWorker
- `ui`：Compose screen + ViewModel
- `core`：analytics、notification、time

优点：

- FeedLog 已成为 Android 主数据实体。
- 本地写入不依赖网络，符合 local-first。
- 同步队列有 retry/backoff。
- Settings、Paywall、Auth、Rating 等边界相对清晰。
- Hilt、Room、WorkManager、DataStore、Firebase、RevenueCat、AdMob 集成方向正确。

### 3.2 Android 主路径状态

| 路径 | 状态 | 备注 |
|---|---|---|
| 新增 Feed | 已实现 | `NewFeedRoute` 使用 `NewBottleSheet` UI，但保存为 FeedLog |
| 快捷 Feed | 部分实现 | `quickLogFeed(amountMl)` 存在，但 Today UI 当前主要是弹窗输入，不是预设奶量一键 |
| 下次提醒 | 已实现 | 基于固定 `FeedReminderConfig.nextFeedAttentionInterval` |
| 今日摘要 | 已实现 | `GetTodaySummaryUseCase` 仍位于 `domain/usecase/bottle` 包 |
| Logs 删除 | 已实现 | 删除本地记录并入队同步 |
| Logs 编辑 | UI 文件存在 | 当前导航中 `onEntryClick = {}`，编辑入口未接通 |
| Export | 后端有能力 | 前端 `onExportCsv = {}` 未接通 |
| FAQ | 未接通 | `onFaqClick = {}` |

### 3.3 后端严重不一致：Bottle 清理半完成

这是当前最重要的代码风险。

发现：

- `backend/app/models/__init__.py` 仍导入 `app.models.bottle.Bottle`。
- `backend/app/services/export_service.py` 仍导入并导出 `Bottle`。
- `backend/app/services/deletion_service.py` 仍软删除 `Bottle`。
- `backend/app/tests/test_sync_ownership.py` 仍导入 `Bottle` 和 `upsert_bottle`。
- `backend/app/tests/test_sync_conflict.py` 仍测试 Bottle 状态优先级。
- `backend/app/models/feed_log.py` 的 `bottle_id` 仍是 `ForeignKey("bottles.id")`。
- `backend/alembic/versions/0001_initial_schema.py` 仍创建 `bottles` 表和 `feed_logs.bottle_id` 外键。
- 但 `backend/app/repositories/sync_repository.py` 已没有 `upsert_bottle`，文件清单中也没有 `backend/app/models/bottle.py`。

影响：

- 后端 app 导入可能失败。
- 后端测试必然与当前代码不一致。
- 删除/导出接口可能因缺失 Bottle model 失败。
- Android 本地 DB v3 删除 `bottles`，但后端 DB 初始 schema 仍保留 `bottles`，跨端数据模型不一致。

建议修复方向：

1. 如果 Bottle 已确定移除：
   - 删除后端 Bottle model 引用。
   - 移除 `feed_logs.bottle_id` 外键，必要时保留普通 nullable legacy 字段或彻底移除。
   - 更新 Alembic 迁移，新增 migration 移除 `bottles` 表和外键。
   - 修改 export/deletion service，只处理 Baby/FeedLog/DiaperLog/SleepLog。
   - 删除或重写 Bottle 测试。
2. 如果后端仍需兼容历史 Bottle：
   - 恢复 `Bottle` model 并保留只读/legacy 迁移策略。
   - 但不要再暴露 `/sync/bottles` 或任何前端入口。

从产品方向看，推荐方案 1。

### 3.4 同步架构评审

当前实现方向正确：

- 本地 repository 写入 Room 后入 `sync_queue`。
- `SyncQueueProcessor` 按队列上推。
- `ApiSyncRepository.syncAll()` 先 push 再 pull。
- 后端按 family membership 校验访问权。
- 后端 upsert 用 `updated_at` 做 stale guard。

主要风险：

| 风险 | 说明 |
|---|---|
| pull 分页未实现 | `_PAGE_LIMIT = 500` 存在，但 `pull_changes()` 传 `None`，`has_more = False` 固定 |
| 客户端冲突不可见 | 本地较新时跳过远端，但没有显式冲突记录或用户/日志可观察状态 |
| 删除语义不完全一致 | 本地 delete 会先入队删除再物理删除本地记录；pull 到远端 delete 也物理删除本地 |
| notes 会同步 | 产品允许，但需在隐私说明中明确“备份会上传备注内容” |
| Sync enable/disable 产品开关不完整 | Settings 现在更像“手动 full sync”，不是明确备份同意流 |

建议：

- MVP 可以继续使用 last-write-wins，但需要文档化冲突策略。
- 增加 pull 分页与 next cursor。
- 在 Settings 中明确“备份开启后会上传宝宝资料、喂养/尿布/睡眠记录和备注”。
- 增加 sync failed 的非阻塞 UI 状态。

### 3.5 通知评审

优点：

- 下次喂养提醒只依赖本地时间和 AlarmManager，不依赖后端。
- exact alarm 权限不足时 fallback 到普通 `set()`。
- BootReceiver/Reschedule worker 文件存在，方向符合重启恢复。

风险：

- `TodayRoute` 对 `latestFeed` 变化会调用 `scheduleNextFeedReminder()`，`logFeed()` 内也会 schedule，存在重复调度调用，但 PendingIntent requestCode 相同，应以更新为主。
- 目前提醒间隔看起来是固定配置，尚未看到完整自定义提醒产品流。
- 旧 bottle notification 字符串和类型仍残留，容易产生维护误解。

### 3.6 商业化评审

优点：

- RevenueCat 接入方向正确。
- 产品 ID 与 TODO 中 Google Play 商品一致。
- 本地 entitlement cache 有宽限期。
- Today 广告受 `!isPro && !nightModeEnabled` 控制，符合夜间不打扰原则。

风险：

- `REVENUECAT_API_KEY` 为空时会降级为 Free；发布前必须配置。
- `TodayScreen` 默认 banner ad unit 是测试 ID。
- `admob_app_id` 仍是测试 ID。
- 后端也有 Google Play billing/entitlement 能力，Android 当前主要使用 RevenueCat，本地与后端权益源需要统一策略。
- Paywall 权益包含尚未交付的 Export/Widget themes/Backup，发布前需避免过度承诺。

### 3.7 本地化与合规评审

合规优点：

- 免责声明包含 “not medical advice”“cannot determine whether milk is safe”“follow formula label/local guidance/pediatrician”“when in doubt, discard the milk”。
- source disclaimer 明确没有 CDC/AAP/NHS endorsement。
- Paywall disclaimer 没有医疗承诺。

需要修正：

- “safe formula preparation guidance” 作为来源标题可以接受，但用户界面应避免强化 “safe” 心智。
- Bottle timer 相关文案需要清理。
- `TodayScreen.formatNextFeedCountdown()`、`formatDuration()` 返回 `"0s"`, `"5m"`, `"1h 20m"`，属于用户可见字符串但未本地化。
- SignIn previews 中有硬编码 `"Sign In"`、`"Sync and backup"` 等，预览不影响运行，但建议统一。

---

## 4. 测试与验证结果

本次执行：

| 命令 | 结果 | 说明 |
|---|---|---|
| `bash gradlew testDebugUnitTest` | 未执行成功 | Gradle 下载完成后失败：缺少 Android SDK 配置，需设置 `ANDROID_HOME` 或 `local.properties sdk.dir` |
| `python3 -m pytest app/tests` | 未执行成功 | 当前 Python 环境缺少 `pytest` |

本次静态检查发现：

- Android 单测目录包含 RatingPrompt、EntitlementCache、AppStartup、SyncQueueWriter、AuthTokenInterceptor。
- 未看到 FeedLog CRUD、TodaySummary、单位换算、跨日日志、同步 pull/merge 的 Android 单测。
- 后端测试仍引用已缺失的 Bottle model/upsert_bottle，与当前后端实现不一致。

发布前最低测试门槛：

1. `bash gradlew testDebugUnitTest` 通过。
2. `bash gradlew assembleDebug` 通过。
3. 后端 pytest 在有依赖环境下通过。
4. 手动验证：
   - 首次启动 + 创建宝宝
   - 离线记录 Feed/Diaper/Sleep
   - 网络恢复后同步
   - 通知权限拒绝/允许
   - 夜间模式无广告
   - Pro 恢复购买
   - 后端不可用时本地记录不受影响

---

## 5. 风险清单

| 严重级别 | 风险 | 影响 | 建议 |
|---|---|---|---|
| P0 | 后端 Bottle model 缺失但仍被导入 | 后端启动、导出、删除、测试可能失败 | 完成 Bottle 后端清理或恢复 legacy model |
| P0 | Android SDK 未配置 | 无法验证编译/单测 | 配置 `ANDROID_HOME` 或 `local.properties`，接入 CI |
| P1 | 产品文案仍强调 Bottle Timer | 用户误解与合规风险 | 全量替换为 feeding record/next feed reminder |
| P1 | 导出/FAQ 前端入口为空 | Pro 权益和设置入口不可用 | 接通或隐藏入口 |
| P1 | Pull 分页未实现 | 大数据量同步不完整或响应过大 | 实现 page limit、next cursor、hasMore |
| P1 | 测试覆盖不足 | FeedLog 主路径回归风险 | 补齐 FeedLog/summary/sync/notification 测试 |
| P1 | 发布配置未完成 | 无法上架或真实购买/广告不可用 | 完成 Firebase、AdMob、RevenueCat、Google Play 商品 |
| P2 | 部分用户可见字符串未本地化 | 多语言质量下降 | 抽取时间格式与硬编码文案 |
| P2 | 权益源策略不清 | RevenueCat 与后端 entitlement 可能分叉 | 明确 RevenueCat 为主或后端为主的 restore/verification 策略 |

---

## 6. 建议路线图

### Phase 0：发布阻塞修复

1. 修复后端 Bottle 残留：
   - 删除 `Bottle` 导入、测试、导出/删除引用。
   - 更新 `feed_logs.bottle_id` 策略。
   - 增加 Alembic migration。
2. 配置 Android SDK 与 CI：
   - `testDebugUnitTest`
   - `assembleDebug`
3. 让后端测试在本地/CI 可运行：
   - 使用 uv 或虚拟环境安装 dev dependencies。
   - 移除 Bottle 旧测试。

### Phase 1：产品一致性

1. `NewBottleSheet` 重命名为 `NewFeedSheet`。
2. `ui/bottle` 包迁移为 `ui/feed`。
3. 清理 strings.xml 中 bottle timer 旧文案。
4. Onboarding 弱化 guideline timer，强调 tracking/reminders。
5. 评分弹窗文案更新为 feeding/care tracking。

### Phase 2：MVP 完整度

1. 接通导出 CSV 前端入口。
2. 完成 Backup/sync opt-in 说明和开关。
3. 增加 sync failed 非阻塞状态。
4. 完成真实 AdMob/RevenueCat/Firebase 配置。
5. 增加隐私政策、服务条款、数据删除页面。

### Phase 3：质量与增长

1. 补齐 FeedLog CRUD、Today summary、单位换算、跨日日志测试。
2. 增加 Compose UI 测试：
   - 大字体
   - 深色/夜间模式
   - 无宝宝空状态
   - 无网络同步失败
3. 增加 widget MVP。
4. 增加 Pro 高级统计和历史限制逻辑。

---

## 7. 当前状态评分

| 维度 | 评分 | 说明 |
|---|---:|---|
| 产品方向 | 8/10 | FeedLog 方向正确，符合照护者低压力场景 |
| Android 主路径 | 7/10 | 核心记录可用，但命名/入口/测试仍需清理 |
| 后端一致性 | 4/10 | Bottle 残留导致高风险 |
| 本地优先 | 8/10 | Room + queue + WorkManager 方向正确 |
| 同步可靠性 | 6/10 | 基础 push/pull 有了，分页/冲突/开关仍弱 |
| 商业化 | 6/10 | Paywall/RevenueCat/Ads 有基础，发布配置和权益落地不足 |
| 合规与隐私 | 7/10 | 免责声明较好，Bottle timer 文案和备份说明需修正 |
| 测试与发布就绪 | 4/10 | 本地验证链路未跑通，后端测试不一致 |

综合判断：

> 当前项目适合继续作为 MVP 工程推进，但不建议直接进入 Google Play 内测。先完成后端 Bottle 清理、构建/测试环境修复、产品文案统一和发布配置，再进入封闭测试更稳妥。

