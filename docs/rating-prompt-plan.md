# Nurtlina 评分弹窗产品方案

本文档记录 Nurtlina Android App 的评分弹窗策略，重点是避免打扰照护场景，不在焦虑、失败、过期或夜间流程中出现，并确保文案不产生医疗或安全保证。

## 目标

- 在用户已经感受到产品价值后，温和请求 Google Play 评分。
- 避免打断奶瓶计时、喂养记录、提醒处理等核心路径。
- 避免在用户焦虑、失败、负反馈或夜间使用时触发。
- 遵守 Nurtlina 合规语言：评分弹窗不得暗示医疗建议、安全判断或权威背书。

## 产品原则

评分弹窗应该像一个安静的请求，而不是增长弹窗。

必须遵守：

- 不在瓶奶计时过期时弹出。
- 不在用户点击过期提醒通知后弹出。
- 不在夜间模式中弹出。
- 不在正在喂养计时中弹出。
- 不在用户刚丢弃、取消、删除记录后弹出。
- 不在通知权限被拒绝、广告加载失败、同步失败、购买失败后弹出。
- 不在首次打开 App 或刚完成 onboarding 后弹出。
- 不使用医疗、安全保证或权威背书文案。

## 推荐触发条件

MVP 建议使用保守触发规则：

| 条件 | 规则 |
|---|---|
| 安装时间 | 首次启动后至少第 3 天 |
| 核心使用深度 | 至少创建过 5 个瓶奶计时 |
| 正向完成行为 | 至少完成过 3 次正向结束操作，例如标记 `Fed` 或完整记录一次喂养 |
| 当前风险状态 | 当前没有 active expired bottle |
| 夜间状态 | 当前不在夜间模式 |
| 展示冷却 | 距离上次提示至少 30 天 |
| 永久拒绝 | 用户未点过“不再提示” |
| 页面时机 | Today 页面稳定展示后延迟 5-10 秒，且没有其他弹窗 |

优先触发点：

1. 用户把一个 `FeedingStarted` 瓶奶标记为 `Fed`。
2. 页面回到 Today。
3. 当前没有过期卡片、没有正在处理的提醒、没有其他弹窗。
4. 延迟 1-2 秒显示应用内轻量询问。

## 不应触发的场景

- App 启动后立刻弹出。
- Onboarding 刚结束。
- 用户正在创建、编辑或启动瓶奶计时。
- 用户正在处理过期计时。
- 用户刚点击“Mark as discarded”、“Cancel”或删除记录。
- 用户刚看到权限、购买、同步、广告或网络错误。
- 用户从提醒通知进入 App 后的同一会话。
- 夜间模式或用户本地夜间时段。

## 弹窗流程

推荐采用两步式：

1. 先显示应用内自定义轻量弹窗，询问用户是否愿意评分。
2. 用户点击“去评分”后，再调用 Google Play In-App Review API 或跳转 Google Play 评分入口。

不要直接在满足条件时立即调用 Google Play 评分框。应用内前置询问可以让用户明确选择，避免在照护场景中突然出现系统评分 UI。

## 推荐文案

### 英文

标题：

> Enjoying Nurtlina?

正文：

> If Nurtlina has been helpful for tracking bottle timers and baby care, would you like to rate it?

按钮：

- Rate Nurtlina
- Maybe later
- No thanks

### 中文

标题：

> 喜欢使用 Nurtlina 吗？

正文：

> 如果 Nurtlina 帮你更轻松地记录奶瓶计时和宝宝照护，愿意给我们一个评分吗？

按钮：

- 去评分
- 稍后再说
- 不再提示

## 禁用文案

评分弹窗不得使用以下表达或近义表达：

- 让喂养更安全
- 保障宝宝健康
- 医学级提醒
- 专业建议
- 安全喝奶
- 医生推荐
- CDC/AAP/NHS approved
- safe to drink
- medically approved

允许使用的方向：

- 记录奶瓶计时
- 记录宝宝照护
- 更轻松地记录
- 如果 Nurtlina 对你有帮助

## 频率与状态字段

建议持久化字段：

```text
ratingPromptShownCount
ratingPromptLastShownAt
ratingPromptDismissedPermanently
ratingPromptRatedAt
eligiblePositiveActionCount
firstLaunchAt
lastNotificationOpenAt
```

频率规则：

- 用户点击 `Maybe later`：至少 30 天后才可再次出现。
- 用户点击 `No thanks`：永久不再主动弹出。
- 用户点击 `Rate Nurtlina`：记录后不再主动弹出。
- 主动展示最多 2 次。
- 同一 App 会话内最多展示 1 次。

## 负反馈处理

MVP 不建议在评分弹窗里做二次追问，例如“你不喜欢哪里？”。这会增加照护场景中的认知负担。

如果需要反馈入口，应放在 Settings：

- English: `Send feedback`
- Chinese: `发送反馈`

评分弹窗中的 `No thanks` 应直接关闭并尊重选择。

## Analytics

评分相关埋点只能记录非敏感产品事件，不得包含宝宝姓名、备注、喂养详情、健康担忧或任何个人照护内容。

建议事件：

```text
rating_prompt_eligible
rating_prompt_shown
rating_prompt_rate_clicked
rating_prompt_maybe_later_clicked
rating_prompt_no_thanks_clicked
rating_prompt_blocked_night_mode
rating_prompt_blocked_active_expired_bottle
rating_prompt_blocked_notification_session
```

可用属性：

```text
trigger_source
days_since_first_launch_bucket
shown_count
blocked_reason
```

不要记录：

- 宝宝真实姓名
- 备注内容
- 具体喂养量或护理详情
- 医疗或健康担忧

## 验收标准

- 评分弹窗不会在过期、提醒点击、夜间、失败、购买失败、同步失败场景出现。
- 弹窗不阻塞瓶奶计时创建、状态变更或提醒处理。
- 用户可以选择稍后、不再提示或去评分。
- `No thanks` 后不再主动弹出。
- 所有用户可见文案进入本地化资源。
- 文案不包含医疗、安全保证或权威背书。
- Analytics 不包含个人照护内容。
- Google Play 评分入口失败时静默降级，不影响核心功能。
