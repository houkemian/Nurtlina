# Baby Feeding Tracker 生产级 PRD 与阶段执行方案

> 文档版本：v2.0  
> 面向平台：Android-only  
> 目标市场：海外市场优先，首发建议美国、英国、加拿大、澳大利亚、新西兰  
> 产品类型：ToC 工具型订阅 App  
> 商业模式：广告 + Pro 订阅 + Lifetime  
> 适合团队：一人开发、轻运营、低后端依赖  
> 风险等级：中低；核心风险在医疗/育儿安全表述与提醒误导，需要严格边界  
> 文档日期：2026-07-10
> **v2.0 重大变更**：已移除奶瓶计时器/状态机功能，产品核心从"奶瓶新鲜度管理"调整为"喂奶记录管理"。

---

## 1. 产品一句话定义

Baby Feeding Tracker 是一款面向新手父母和照护者的婴儿喂养记录与护理追踪工具，帮助用户快速记录每次喂奶（配方奶/母乳/混合）、尿布、睡眠，并通过清晰提醒降低"忘记上一次什么时候喂的"的焦虑。

---

## 2. 产品定位

### 2.1 英文定位

A simple and trustworthy feeding tracker for baby formula, breast milk, diapers, and sleep.

### 2.2 中文定位

一个简单、可信、无压力的婴儿喂奶记录和护理追踪 App。

### 2.3 核心价值主张

用户不需要复杂的育儿社区，也不需要 AI 聊天医生。用户真正需要的是：

- 一键记录宝宝什么时候喝了多少。
- 夜里半睡半醒时也能快速操作。
- 多个照护者看到同一份喂奶记录。
- 有权威来源支撑的喂养参考信息，而不是模糊经验。

### 2.4 品牌气质

- Simple：少步骤、少配置。
- Calm：降低焦虑，不制造恐慌。
- Trustworthy：所有建议都标注来源。
- Non-medical：不诊断、不承诺、不替代医生。
- Parent-first：面向疲惫父母，夜间场景友好。

---

## 3. 为什么这个方向适合一人公司

### 3.1 一人可做

首版不需要复杂社交关系链，不需要内容审核，不需要 UGC，不需要医生认证体系。核心是本地记录、统计、提醒、备份。

### 3.2 Android-only 足够

Google Play 面向海外安卓用户，育儿工具类 App 在 Android 上仍有足够空间。可以先用 Android 原生能力做好：

- 通知提醒
- 桌面小组件
- 本地数据库
- Material You 设计
- 深色模式
- Google Drive / Firebase 备份
- RevenueCat / Google Play Billing 订阅

### 3.3 不依赖复杂后端

MVP 可完全本地运行。后续云备份、跨设备同步、多照护者同步再逐步引入 Firebase。

### 3.4 能用 AI 提升生产力

AI 可以辅助完成：

- 多语言文案
- FAQ
- ASO 标题、副标题、描述
- Google Play 素材文案
- 应用内插画 Prompt
- 客服模板
- 用户反馈归类
- 合规免责声明草稿
- 本地化 A/B 文案

但产品本身不必强调 AI，这样可降低监管、信任和实现复杂度。

---

## 4. 目标用户

### 4.1 核心用户

#### Persona A：新手妈妈 / 新手爸爸

- 宝宝年龄：0-12 个月
- 痛点：夜里喂奶频繁，记不清上一次什么时候喂的、宝宝喝了多少、尿布换了没有。
- 需求：快速记录、下次喂奶提醒、日统计、夜间模式。
- 付费可能性：中高。对省心和减少焦虑愿意付费。

#### Persona B：混合喂养家庭

- 使用母乳 + 配方奶
- 痛点：不同奶源需要区分记录，容易混淆。
- 需求：区分 formula / breast milk / mixed / nursing。
- 付费可能性：高。需要更细统计。

#### Persona C：多照护者家庭

- 父母、祖父母、保姆共同照护
- 痛点：谁喂过？什么时候喂的？喂了多少？
- 需求：多宝宝、多照护者同步、导出记录。
- 付费可能性：高。云同步与多成员可作为 Pro 高价值点。

#### Persona D：海外移民 / 多语言家庭

- 家里可能使用中英双语或西语等
- 痛点：英文育儿资料看不顺，家人语言不同
- 需求：多语言界面
- 付费可能性：中。多语言可提升转化。

---

## 5. 问题定义

### 5.1 用户当前怎么解决

- 用手机闹钟
- 用备忘录
- 用纸笔
- 用通用 Baby Tracker
- 靠记忆
- 靠 WhatsApp / iMessage 给伴侣发消息

### 5.2 当前方案的问题

- 纸笔和备忘录夜里操作麻烦。
- 通用 Baby Tracker 功能太多，打开慢，路径深。
- 很多 App 没有明确区分"喝过"和"没喝过"的记录。
- 很多 App 没有引用权威参考信息。
- 很多 App 的统计复杂但核心记录不好用。
- 多宝宝、多照护者、导出功能常常要高价订阅。

### 5.3 本产品要解决的核心问题

> 在最少操作下，让照护者快速记录每次喂奶，知道上一次什么时候喂的、喂了多少，并顺手沉淀喂养记录。

---

## 6. 产品范围

### 6.1 第一版必须做

- 喂奶记录（formula / breast milk / mixed / nursing）
- 快捷奶量输入（预设 + 自定义）
- 下次喂奶提醒
- 尿布记录
- 睡眠记录
- 夜间模式
- 桌面小组件
- 多语言：英文、西语、中文、德语、法语
- 基础 FAQ 与安全来源说明
- 本地数据存储
- Google Play Billing
- 广告接入
- Pro 解锁

### 6.2 第一版不做

- ~~奶瓶计时器/状态机~~（v2.0 已移除——新手父母没有时间操作复杂计时器）
- 医疗建议
- 诊断宝宝健康状态
- AI 聊天医生
- 社区
- 内容流
- 在线问诊
- 复杂成长曲线
- 复杂疫苗提醒
- 硬件连接
- 与奶粉品牌合作推荐
- 处方、营养补充剂、医疗级建议
- 上传宝宝照片做 AI 分析

### 6.3 第二阶段考虑

- 云备份
- 多设备同步
- 多照护者共享
- PDF/CSV 导出
- 更丰富的小组件主题
- Apple Watch 不考虑；Android Wear 可后置
- 喂奶趋势洞察
- 成长曲线集成 WHO/CDC 数据，但需谨慎

### 6.4 第三阶段考虑

- Baby Care Suite：喂奶、尿布、睡眠、用药、体温、成长记录
- 家庭共享
- 保姆模式
- 儿科医生导出报告
- 品牌化小组件主题
- 区域化指南切换

---

## 7. 权威参考信息与合规边界

### 7.1 重要原则

App 不应写：

- "保证安全"
- "这个奶一定可以喝"
- "医生推荐"
- "医学级判断"
- "自动判断宝宝健康"
- "替代医生建议"

App 应写：

- "Based on public guidelines"
- "Use as a reference, not medical advice"
- "When in doubt, discard"
- "Follow your formula label, local guidance, and pediatrician advice"
- "Different regions may publish different guidance"

### 7.2 喂养参考信息（仅供参考，非医疗建议）

#### 配好的婴儿配方奶

- 室温：配好后一般建议 2 小时内使用。
- 如果 2 小时内不会使用，应立即冷藏。
- 冷藏：一般建议 24 小时内使用。

#### 已开始喂的配方奶

- 从开始喂奶起一般建议 1 小时内使用。
- 喝过后剩余奶不应冷藏留到下次。
- 原因说明：宝宝唾液可能带入细菌。

#### 母乳

- 新鲜挤出的母乳：室温一般建议 4 小时内，冷藏 4 天内，冷冻最佳 6 个月内，最长可到 12 个月。

### 7.3 来源

以下来源用于 App 内 FAQ、设置页说明、免责声明与参考信息依据。具体应以用户所在地区、奶粉包装说明和医生建议为准。

- CDC：Infant Formula Preparation and Storage  
  https://www.cdc.gov/infant-toddler-nutrition/formula-feeding/preparation-and-storage.html

- CDC：Breast Milk Storage and Preparation  
  https://www.cdc.gov/breastfeeding/breast-milk-preparation-and-storage/handling-breastmilk.html

- NHS：Formula milk: common questions  
  https://www.nhs.uk/baby/breastfeeding-and-bottle-feeding/bottle-feeding/formula-milk-questions/

- NHS：How to make up baby formula  
  https://www.nhs.uk/baby/breastfeeding-and-bottle-feeding/bottle-feeding/making-up-baby-formula/

- AAP / HealthyChildren：How to Safely Prepare Baby Formula With Water  
  https://www.healthychildren.org/English/ages-stages/baby/formula-feeding/Pages/how-to-safely-prepare-formula-with-water.aspx

### 7.4 App 内免责声明文案

#### 英文

This app provides tracking tools based on public guidelines. It does not provide medical advice and cannot determine whether milk is safe. Always follow your formula label, local health guidance, and your pediatrician's advice. When in doubt, discard the milk.

#### 中文

本 App 仅基于公开指南提供记录和提醒工具，不提供医疗建议，也不能判断奶液是否绝对安全。请始终遵循奶粉包装说明、当地卫生机构指南以及儿科医生建议。如有疑问，请丢弃。

---

## 8. 产品目标

### 8.1 MVP 目标

在 8-10 周内上线 Google Play，验证以下问题：

- 用户是否愿意每天使用喂奶记录？
- 用户是否愿意为了无广告、小组件、多宝宝、统计而付费？
- "极简喂奶记录"是否能成为差异化入口？
- 多语言是否能降低获客成本？
- ASO 是否能带来自然下载？

### 8.2 6 个月目标

- 累计下载：20,000+
- 日活：1,000+
- D1 留存：35%+
- D7 留存：18%+
- D30 留存：8%+
- 免费到 Pro 转化：1.5%-3.5%
- 年订阅占付费比例：35%+
- Lifetime 占付费比例：20%-40%
- 广告 ARPDAU：视地区 $0.01-$0.05
- Google Play 评分：4.5+

### 8.3 12 个月目标

- 累计下载：100,000+
- MRR：$3,000-$10,000
- 自然搜索下载占比：60%+
- 形成 Baby Care Suite 的第二产品或大版本基础

---

## 9. 成功指标

### 9.1 激活指标

- 首次打开到创建宝宝完成率
- 首次记录喂奶完成率
- 首次授权通知完成率
- 首日创建 2 个以上记录的比例

### 9.2 留存指标

- D1 / D3 / D7 / D30 留存
- 每日喂奶记录次数
- 每周尿布记录次数
- 夜间使用比例
- 小组件启用比例

### 9.3 变现指标

- 订阅页曝光率
- 免费到 Pro 转化率
- 月订阅 / 年订阅 / Lifetime 分布
- 试用转化率
- 取消率
- 广告展示次数
- 广告 eCPM
- Pro 用户留存

### 9.4 产品健康指标

- Crash-free users > 99.5%
- ANR rate < 0.5%
- 冷启动 < 1.5 秒
- 喂奶记录路径 < 2 步
- 夜间记录单次操作 < 5 秒
- 通知送达准确率 > 95%

---

## 10. 核心用户旅程

### 10.1 首次使用

1. 用户打开 App。
2. 看到简单欢迎页：Track feeds, diapers, and sleep.
3. 选择语言或使用系统语言。
4. 创建宝宝：昵称、出生日期、单位偏好。
5. 看到免责声明。
6. 请求通知权限，解释用途。
7. 进入首页。
8. 首页中央是快捷喂奶记录按钮。
9. 用户记录第一次喂奶：选择类型、输入奶量。
10. App 显示"上次喂奶：X 分钟前"，以及下次喂奶预计时间。

### 10.2 夜间喂奶

1. 用户打开 App 或点击小组件。
2. 夜间模式自动降低亮度/使用深色。
3. 一键选择预设奶量（60/90/120/150 ml）。
4. 自动记录喂奶时间。
5. 用户可继续睡觉。

### 10.3 日常喂奶记录

1. 用户打开 App。
2. 看到"上次喂奶：45 分钟前"。
3. 点击快捷奶量按钮或自定义输入。
4. 选择奶类型（Formula/Breast Milk/Mixed/Nursing）。
5. 一键记录。
6. 自动更新今日统计。

### 10.4 多宝宝家庭

1. 首页顶部切换宝宝。
2. 免费版只能创建 1 个宝宝。
3. 点击添加第二个宝宝时触发 Pro paywall。
4. Pro 用户可创建多个宝宝，分别统计。

### 10.5 小组件使用

1. 用户添加桌面小组件。
2. 小组件显示上次喂奶时间和奶量。
3. 显示宝宝昵称。
4. 可一键打开 App 记录喂奶。
5. Pro 用户可使用主题、多个小组件。

---

## 11. 信息架构

### 11.1 底部导航

建议 4 个 Tab：

1. Today
2. Logs
3. Insights
4. Settings

### 11.2 Today 首页

模块顺序：

- 当前宝宝切换器
- 上次喂奶状态卡片 + 快捷记录按钮
- 快捷记录：Feed / Diaper / Sleep
- 今日摘要
- 最近记录

### 11.3 Logs 记录页

- 时间线
- 类型筛选：Feed / Diaper / Sleep
- 日期选择
- 编辑/删除记录
- 空状态引导

### 11.4 Insights 统计页

免费版：

- 今日奶量
- 今日喂奶次数
- 今日尿布次数
- 今日睡眠时长

Pro 版：

- 7/14/30 天趋势
- 按宝宝统计
- 导出
- 图表
- 自定义日期范围

### 11.5 Settings 设置页

- Baby profile
- Units
- Notifications
- Night mode
- Widgets
- Language
- Backup
- Export
- Pro
- FAQ & safety sources
- Privacy policy
- Terms
- Contact support

---

## 12. 功能需求

## 12.1 喂奶记录

### 12.1.1 功能说明

记录宝宝每次喂奶时间、奶量、类型和备注。极简设计，1-2 步完成记录。

### 12.1.2 喂奶类型

MVP 支持：

- Formula（配方奶）
- Breast milk（母乳）
- Mixed（混合）
- Nursing（亲喂）
- Other

### 12.1.3 记录字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| baby_id | UUID | 是 | 所属宝宝 |
| feed_type | enum | 是 | formula / breast_milk / mixed / nursing / other |
| amount_ml | number | 否 | 奶量 |
| started_at | datetime | 是 | 开始时间，默认当前 |
| ended_at | datetime | 否 | 结束时间 |
| side | enum | 否 | breastfeeding: left / right / both |
| note | text | 否 | 备注 |

### 12.1.4 快捷输入

预设奶量按钮：

- 60 ml
- 90 ml
- 120 ml
- 150 ml
- 180 ml
- Last amount
- Custom

可根据地区切换 oz：

- 2 oz
- 3 oz
- 4 oz
- 5 oz
- 6 oz

### 12.1.5 操作

- Quick log（预设奶量一键记录）
- Custom log（自定义奶量和类型）
- Edit amount / time
- Delete record

### 12.1.6 下次喂奶提醒

默认提醒：

- 用户可设置喂奶间隔提醒（如每 3 小时）
- 上次喂奶后按间隔计算下次提醒时间
- 用户可配置静音时段和夜间免打扰

### 12.1.7 验收标准

- 可一键记录"刚刚喝完 X ml"。
- 支持编辑时间和奶量。
- 今日总奶量自动更新。
- 删除喂奶记录后统计自动回滚。
- 喂奶记录创建 ≤ 2 步。

---

## 12.2 尿布记录

### 12.2.1 功能说明

记录尿布更换类型，用于父母回看宝宝排便排尿情况。

### 12.2.2 类型

- Wet
- Dirty
- Mixed
- Dry

### 12.2.3 字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| baby_id | UUID | 是 | 宝宝 |
| diaper_type | enum | 是 | wet / dirty / mixed / dry |
| changed_at | datetime | 是 | 更换时间 |
| note | text | 否 | 备注 |

### 12.2.4 验收标准

- 首页可一键记录 Wet / Dirty / Mixed。
- 今日摘要显示尿布次数。
- 记录页可筛选尿布类型。
- 不提供"是否正常"的医学判断。

---

## 12.3 睡眠记录

### 12.3.1 功能说明

记录宝宝睡眠开始和结束时间，支持正在进行中的睡眠计时器。

### 12.3.2 字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| baby_id | UUID | 是 | 宝宝 |
| started_at | datetime | 是 | 入睡时间 |
| ended_at | datetime | 否 | 醒来时间 |
| note | text | 否 | 备注 |

### 12.3.3 操作

- Start sleep
- End sleep
- Add past sleep
- Edit
- Delete

### 12.3.4 验收标准

- 只能有一个进行中的睡眠记录。
- 今日睡眠总时长正确。
- 跨天睡眠能正确归属统计。
- 不输出睡眠医学建议。

---

## 12.4 统计

### 12.4.1 免费版统计

- 今日喂奶次数
- 今日总奶量
- 今日尿布次数
- 今日睡眠时长
- 最近 24 小时记录

### 12.4.2 Pro 统计

- 7 天趋势
- 14 天趋势
- 30 天趋势
- 按宝宝比较
- 平均喂奶间隔
- 平均单次奶量
- 夜间喂奶次数
- CSV/PDF 导出

### 12.4.3 注意边界

统计页不要自动判断宝宝摄入是否正常，不要给出"偏低/偏高/异常"的医疗判断。可以写：

- "This is a tracking summary, not a medical assessment."
- "Talk to your pediatrician if you have concerns."

---

## 12.5 夜间模式

### 12.5.1 功能说明

夜间打开 App 时自动使用低亮度、深色、高对比、少文字界面，减少刺激。

### 12.5.2 功能

- 跟随系统深色模式
- 手动夜间模式
- 夜间快速记录页面
- 大按钮
- 低亮度提示色
- 可选防误触确认

### 12.5.3 验收标准

- 夜间模式下所有关键操作可单手完成。
- 按钮字号和点击区域足够大。
- 不使用刺眼颜色。
- 不弹出全屏广告打断夜间记录。

---

## 12.6 桌面小组件

### 12.6.1 免费版

- 显示上次喂奶时间和奶量
- 点击打开 App

### 12.6.2 Pro 版

- 多宝宝小组件
- 多尺寸小组件
- 主题颜色
- 透明背景
- 一键记录喂奶

### 12.6.3 验收标准

- 小组件信息准确或至少分钟级更新。
- 手机重启后可恢复。
- 多宝宝不会显示错宝宝。
- Pro 主题在订阅失效后回落到默认主题。

---

## 12.7 多语言

### 12.7.1 首发语言

- English
- Spanish
- Simplified Chinese
- German
- French

### 12.7.2 本地化范围

- App UI
- Google Play listing
- FAQ
- 通知文案
- Paywall
- 错误提示
- 免责声明
- 引导页

### 12.7.3 优先级

第一优先级：

- English
- Spanish

第二优先级：

- German
- French
- Chinese

### 12.7.4 注意事项

- 医疗/安全相关文案不要自由发挥。
- 多语言版本都应保留"not medical advice"。

---

## 12.8 备份与导出

### 12.8.1 MVP

- 本地数据
- 手动 CSV 导出：Pro
- Android 系统分享

### 12.8.2 V1.1

- Google Drive 备份
- Firebase Anonymous + Google 登录
- 自动备份：Pro
- 恢复数据

### 12.8.3 V1.2

- 多设备同步
- 家庭共享

---

## 12.9 Pro 订阅

### 12.9.1 免费版

- 1 个宝宝
- 基础喂奶记录
- 基础尿布记录
- 基础睡眠记录
- 今日统计
- 有广告
- 默认小组件
- 最近 30 天记录可查看

### 12.9.2 Pro 版

- 无广告
- 多宝宝
- 全部历史记录
- 7/14/30 天统计
- CSV/PDF 导出
- 云备份
- 小组件主题
- 自定义提醒
- 优先支持

### 12.9.3 价格

建议：

- Monthly：$2.99 / month
- Yearly：$19.99 / year
- Lifetime：$29.99 one-time

### 12.9.4 Paywall 触发点

- 添加第二个宝宝
- 查看 30 天以上历史
- 导出 CSV/PDF
- 开启云备份
- 使用小组件主题
- 去广告入口
- 查看高级统计

### 12.9.5 Paywall 文案

#### 标题

Make baby care tracking easier

#### 卖点

- No ads
- Track multiple babies
- Export feeding history
- Backup your data
- Customize reminders and widgets

#### 免责声明

Subscription terms apply. This app provides tracking and reminders only, not medical advice.

---

## 13. 广告策略

### 13.1 广告形式

MVP 建议只使用：

- Banner：Today 页底部
- Native：Logs 空白区域或设置页
- 不建议首版使用 Interstitial
- 不建议夜间弹广告
- 不建议 Rewarded，和育儿场景不匹配

### 13.2 广告限制

以下场景不展示广告：

- 夜间模式
- 喂奶记录流程中
- Paywall 展示前后 10 秒

### 13.3 广告合规

- 避免展示与婴儿健康、奶粉、药品、减肥、成人内容相关的敏感广告类别。
- 尽量使用 Google AdMob 的敏感类别过滤。
- 对儿童相关应用需谨慎设置目标受众，App 不是给儿童使用，而是给成人父母使用。

---

## 14. 数据模型

### 14.1 Baby

```text
Baby
- id: UUID
- name: String
- birth_date: LocalDate?
- avatar_color: String?
- created_at: Instant
- updated_at: Instant
- archived_at: Instant?
```

### 14.2 FeedLog

```text
FeedLog
- id: UUID
- baby_id: UUID
- feed_type: FeedType
- amount_ml: Double?
- started_at: Instant
- ended_at: Instant?
- side: Side?
- note: String?
- created_at: Instant
- updated_at: Instant
```

### 14.3 DiaperLog

```text
DiaperLog
- id: UUID
- baby_id: UUID
- diaper_type: DiaperType
- changed_at: Instant
- note: String?
- created_at: Instant
- updated_at: Instant
```

### 14.4 SleepLog

```text
SleepLog
- id: UUID
- baby_id: UUID
- started_at: Instant
- ended_at: Instant?
- note: String?
- created_at: Instant
- updated_at: Instant
```

### 14.5 UserSettings

```text
UserSettings
- id: String
- language: String
- unit: UnitType
- notification_enabled: Boolean
- feed_reminder_interval_minutes: Int
- night_mode_enabled: Boolean
- theme: ThemeType
- subscription_status: SubscriptionStatus
- created_at: Instant
- updated_at: Instant
```

---

## 15. UI/UX 设计

### 15.1 首页布局

```text
[Baby Switcher]                         [Settings]

Good evening, Emma's feeding tracker

[Last Feeding Card]
Formula • 120 ml
25 min ago
Next feeding in ~2h 35m

[Quick Log]
[ 60ml ] [ 90ml ] [ 120ml ] [ 150ml ] [ Custom ]

[Feed] [Diaper] [Sleep]

Today:
Formula: 480 ml
Feeds: 5
Diapers: 6
Sleep: 3h 20m
```

### 15.2 夜间页面

```text
Emma

Last feed: 42 min ago

[Log 120 ml fed]
[Diaper]
[Sleep]
```

### 15.3 空状态文案

#### Today 空状态

No feeding records yet  
Tap to log your first feeding.

#### Logs 空状态

No logs for this day  
Quickly add a feed, diaper, or sleep record.

#### Insights 空状态

Track for a few days to see trends.

---

## 16. 文案系统

### 16.1 语气原则

- 不恐吓
- 不做安全保证
- 不制造父母内疚
- 用"tracking / reminder / based on public guidelines"
- 多用 "when in doubt, discard"
- 对错误操作给出温和提醒

### 16.2 核心英文文案

#### Log feeding

Record a feeding

#### Quick feed

Quick log

#### Last feeding

Last feeding: 25 min ago  
Formula · 120 ml

#### Safety note

This app provides tracking and reminders only, not medical advice.

#### Paywall

Track with less stress  
Unlock multiple babies, deeper history, exports, backup, and ad-free tracking.

### 16.3 中文文案

#### 记录喂奶

记录一次喂奶

#### 快捷记录

快捷记录

#### 上次喂奶

上次喂奶：25 分钟前  
配方奶 · 120 ml

#### 安全提示

本 App 仅提供记录和提醒工具，不提供医疗建议。

---

## 17. Google Play 上架策略

### 17.1 App 名称候选

- Baby Feeding Tracker
- Baby Feed & Care Tracker
- Simple Baby Feeding Tracker

建议首发名称：

Baby Feeding Tracker — Nurtlina

原因：

- 覆盖 baby feeding tracker 搜索
- 覆盖 baby tracker 搜索
- 不局限于 formula
- 后续可扩展尿布、睡眠

### 17.2 短描述

Track baby feeds, diapers, and sleep with simple one-tap logging.

### 17.3 长描述结构

1. 解决什么问题
2. 核心功能
3. 喂奶记录
4. 尿布/睡眠记录
5. 统计与导出
6. 多宝宝与小组件
7. 安全来源与免责声明
8. Pro 说明

### 17.4 关键词

核心关键词：

- baby feeding tracker
- baby feed log
- newborn feeding tracker
- baby care tracker
- diaper tracker
- sleep tracker
- formula feeding log
- breastfeeding tracker
- bottle feeding app

### 17.5 截图卖点

建议 8 张：

1. Log feeds in one tap
2. Track formula and breast milk
3. Night mode for sleepy parents
4. Log diapers and sleep
5. See daily feeding summaries
6. Add a home screen widget
7. Multiple babies (Pro)
8. Unlock Pro: exports, backup, no ads

### 17.6 图标方向

- 奶瓶 + 心形
- 柔和圆角
- 不使用红十字
- 不使用医疗符号
- 避免奶粉品牌联想
- 深浅模式适配

---

## 18. 隐私与合规

### 18.1 收集数据

MVP 尽量少收集：

- 宝宝昵称
- 出生日期，可选
- 喂奶记录
- 尿布记录
- 睡眠记录
- App 使用分析事件
- 崩溃日志
- 购买状态

### 18.2 不收集

- 宝宝真实姓名，建议提示使用昵称
- 照片
- 精确地址
- 医疗诊断
- 联系人
- 麦克风
- 相机
- 位置

### 18.3 隐私策略重点

- 数据主要存储在用户设备上。
- 若开启云备份，将数据同步到指定服务。
- 广告和分析 SDK 可能收集设备标识符。
- 用户可导出或删除本地数据。
- App 面向成人父母/照护者，不面向儿童直接使用。

### 18.4 Google Play Data Safety

需要声明：

- App activity：analytics
- App info and performance：crash logs
- Personal info：如果使用邮箱登录，则声明 email
- Health and fitness：谨慎。婴儿喂养/睡眠记录可能被视作健康相关数据，应按 Google Play 最新分类如实填写。
- Data encryption in transit：云备份时必须有
- User deletion request：提供邮箱或 App 内删除

### 18.5 儿童政策

App 的目标用户不是儿童，而是成人父母。Google Play 内容评级和目标受众应避免选择"主要面向儿童"。但由于内容涉及婴儿护理，广告策略要保守。

---

## 19. 竞品分析方向

### 19.1 竞品类别

- 通用 Baby Tracker
- Breastfeeding tracker
- Formula feeding tracker
- Diaper tracker
- Sleep tracker
- Pumping tracker

### 19.2 常见竞品问题

- 功能太重
- UI 复杂
- 广告打断
- 订阅过贵
- 没有清晰引用安全参考
- 小组件不好用
- 夜间模式体验差
- 多语言粗糙

### 19.3 差异化机会

- 极简喂奶记录（1-2 步完成）
- 夜间体验优秀
- 小组件好用
- 价格更友好
- 多语言首发
- 轻量而可信

---

## 20. MVP 开发范围

### 20.1 Must Have

- Onboarding
- 创建宝宝
- 选择单位
- 免责声明
- Today 首页
- 喂奶记录（快捷 + 自定义）
- 下次喂奶提醒
- 尿布记录
- 睡眠记录
- Logs 时间线
- 今日统计
- 设置页
- 多语言基础
- Pro Paywall
- Google Play Billing
- AdMob Banner
- Crashlytics
- Analytics
- Privacy Policy / Terms 链接
- Google Play Listing 素材

### 20.2 Should Have

- 小组件
- 夜间模式
- CSV 导出
- 30 天统计
- 自定义提醒
- RevenueCat

### 20.3 Could Have

- PDF 导出
- Google Drive 备份
- 多主题
- 多照护者

### 20.4 Won't Have in MVP

- ~~奶瓶计时器/状态机~~（v2.0 已移除）
- 社区
- AI 医生
- 成长曲线判断
- 医疗建议
- 照片识别
- 在线同步
- 家庭共享

---

## 21. 阶段执行方案

## Phase 0：调研与产品定稿，3-5 天

### 目标

把需求冻结到 MVP，避免功能膨胀。

### 任务

- 搜索 Google Play 竞品 20 个。
- 记录竞品名称、评分、下载量、订阅价格、差评关键词。
- 梳理 30 条用户差评。
- 确认首发国家。
- 确认首发语言。
- 确认产品名。
- 确认安全参考和来源。
- 完成 PRD v2.0。
- 完成线框图。

### 产出

- 竞品表
- 差评洞察
- MVP 范围清单
- 线框图
- 安全文案库

### 不做

- 不开始复杂后端。
- 不设计 Baby Care Suite 全量功能。
- 不做社区和 AI 功能。

---

## Phase 1：技术骨架与基础 UI，1 周

### 目标

搭建可持续迭代的 Android 工程骨架。

### 任务

- 新建 Kotlin + Compose 项目。
- 接入 Material 3。
- 建立导航结构。
- 建立 Room 数据库。
- 建立 DataStore 设置。
- 建立 Repository。
- 建立本地化资源结构。
- 建立基础设计系统。
- 接入 Crashlytics。
- 接入 Analytics。
- 建立 CI/CD，至少能生成 release APK/AAB。
- 建立隐私政策网页。

### 产出

- 可运行工程
- 基础页面
- 数据库 schema v2
- 设计系统初版
- Analytics 事件规范

### 验收

- App 能打开，能在 Today/Logs/Insights/Settings 切换。
- 能创建宝宝并持久化。
- 重启 App 数据仍在。
- 崩溃日志可上报。

---

## Phase 2：喂奶记录核心，1.5-2 周

### 目标

完成产品核心功能——喂奶记录。

### 任务

- FeedLog CRUD。
- 快捷奶量输入。
- 奶类型选择。
- 下次喂奶提醒。
- 今日奶量统计。
- 编辑/删除喂奶记录。
- 免责声明弹窗。

### 产出

- 完整喂奶记录系统
- 本地通知
- 安全来源页面
- 单元测试

### 验收

- 记录喂奶 2 步内完成。
- 今日统计实时更新。
- 下次喂奶提醒正常。
- 不出现安全保证文案。

---

## Phase 3：尿布、睡眠记录，1-1.5 周

### 目标

让产品具备日常 Baby Tracker 基础能力。

### 任务

- DiaperLog CRUD。
- SleepLog CRUD。
- 首页快捷记录。
- Logs 时间线。
- 日期筛选。
- 编辑/删除。
- 今日摘要。
- 单位 ml / oz 转换。

### 产出

- 基础记录系统
- 今日统计
- 时间线

### 验收

- 记录能正确新增、编辑、删除。
- 今日统计实时更新。
- 跨天记录归属正确。

---

## Phase 4：夜间模式、小组件、体验打磨，1-1.5 周

### 目标

提升日常使用粘性。

### 任务

- 夜间模式。
- 大按钮快速记录。
- 小组件基础版。
- 小组件 Pro 主题预留。
- 空状态。
- 错误状态。
- 动画和触感反馈。
- 无障碍支持。
- 字体大小适配。
- TalkBack 标签。

### 产出

- 夜间体验
- 桌面小组件
- 可用性优化清单

### 验收

- 夜里单手可操作。
- 小组件显示上次喂奶信息。
- 大字号下布局不崩。
- TalkBack 可读关键按钮。

---

## Phase 5：订阅、广告、导出，1 周

### 目标

完成商业闭环。

### 任务

- Google Play Billing 或 RevenueCat。
- Pro 权益判断。
- Paywall。
- Restore purchase。
- 多宝宝限制。
- 历史记录限制。
- CSV 导出。
- AdMob Banner。
- 广告频率控制。
- 夜间不展示打断式广告。
- 购买事件埋点。

### 产出

- 可付费版本
- 免费/Pro 权限体系
- 广告接入
- 导出功能

### 验收

- 订阅购买、恢复、过期状态正确。
- 免费版限制清晰。
- Pro 用户无广告。
- CSV 可导出并分享。
- 广告不影响核心记录路径。

---

## Phase 6：多语言与上架素材，1 周

### 目标

准备 Google Play 首发。

### 任务

- 英文 UI 文案精修。
- 西语、中文、德语、法语本地化。
- Google Play listing。
- 截图制作。
- 图标制作。
- Feature graphic。
- Privacy Policy。
- Terms。
- FAQ。
- App 内反馈入口。
- 内容评级问卷。
- Data Safety 表单。
- 内测发布。

### 产出

- 多语言 App
- Google Play 素材
- 法务/隐私页面
- 内测版本

### 验收

- 所有核心页面无硬编码英文。
- 安全文案多语言一致。
- Google Play Console 检查通过。
- 20 台以内测试设备无严重崩溃。

---

## Phase 7：封闭测试与首发，1-2 周

### 目标

用小流量验证真实用户行为。

### 任务

- Google Play Closed testing。
- 邀请 20-50 名测试用户。
- 收集崩溃和反馈。
- 修复关键问题。
- 调整 Paywall。
- 调整截图和 ASO。
- Production rollout 5% -> 20% -> 50% -> 100%。

### 产出

- v1.0 production
- 反馈列表
- 修复列表
- 首发指标看板

### 验收

- Crash-free users > 99%。
- 喂奶记录路径无严重 bug。
- 订阅购买无阻断 bug。
- 通知权限引导清晰。
- 用户能理解免责声明。

---

## Phase 8：上线后 30 天迭代

### 目标

围绕留存和转化做快速迭代。

### 每周关注

第 1 周：

- Crash / ANR
- 通知是否准确
- 记录漏斗
- D1 留存
- 差评修复

第 2 周：

- Paywall 转化
- 广告展示体验
- 小组件使用率
- 语言市场表现

第 3 周：

- ASO 调整
- 截图 A/B
- 年订阅折扣文案
- 用户反馈最高频功能

第 4 周：

- 云备份评估
- 多宝宝转化评估
- 导出使用率
- V1.1 范围冻结

---

## 22. 详细排期建议

### 10 周版本

| 周 | 重点 | 产出 |
|---:|---|---|
| Week 1 | 调研、PRD、线框图、项目骨架 | PRD、竞品表、可运行工程 |
| Week 2 | 数据库、宝宝、设置、首页 | 基础 App |
| Week 3 | 喂奶记录核心 | 记录系统 |
| Week 4 | 通知、统计、测试 | 可用记录 |
| Week 5 | 尿布、睡眠 | 完整记录系统 |
| Week 6 | Logs、统计、夜间模式 | 完整日常使用 |
| Week 7 | 小组件、体验打磨 | 粘性功能 |
| Week 8 | 订阅、广告、导出 | 商业化 |
| Week 9 | 多语言、上架素材 | 内测包 |
| Week 10 | 测试、修复、发布 | v1.0 上线 |

### 极简 6 周版本

如果要压缩，建议砍掉：

- 小组件主题
- Sleep
- CSV 导出
- 德语/法语
- 高级统计

保留：

- Feed log
- Diaper log
- Notification
- English / Spanish / Chinese
- Billing
- AdMob

---

## 23. 开发任务拆解

### Epic 1：Onboarding

- 语言选择
- 创建宝宝
- 单位选择
- 免责声明
- 通知权限说明

### Epic 2：Feed Log

- FeedLog 数据表
- 快捷记录 UI
- 自定义记录
- 奶类型选择
- 下次喂奶提醒
- 单元测试

### Epic 3：Tracking Logs

- DiaperLog
- SleepLog
- Logs 页面
- 今日摘要
- 编辑/删除
- 时间选择器

### Epic 4：Insights

- 今日统计
- 7 天统计
- 30 天统计 Pro
- 图表
- 空状态

### Epic 5：Monetization

- Billing
- Entitlement
- Paywall
- Restore purchase
- AdMob
- Pro gating

### Epic 6：Widget

- 基础小组件
- Pro 主题
- 更新策略
- 点击行为

### Epic 7：Localization

- i18n 资源
- 多语言 QA
- Play listing 多语言

### Epic 8：Compliance

- Privacy Policy
- Terms
- Data Safety
- FAQ
- 来源引用
- 免责声明

---

## 24. 测试计划

### 24.1 单元测试

- FeedLog CRUD 操作
- 单位转换
- 今日统计
- 跨天统计
- 喂奶间隔计算

### 24.2 集成测试

- 记录喂奶 → 统计更新
- 删除记录 → 统计回滚
- App 重启 → 数据恢复
- 设备重启 → 提醒恢复

### 24.3 UI 测试

- 首次引导
- 首页快捷记录
- 夜间模式
- 大字号
- 小组件点击
- Paywall
- 多语言布局

### 24.4 人工测试清单

- 时区变化
- 夏令时变化
- 手机时间手动修改
- 无网络
- 通知权限拒绝
- 通知权限后续开启
- 订阅取消
- 订阅恢复
- 广告加载失败
- 低端安卓设备
- Android 12/13/14/15/16 权限差异

---

## 25. 风险与应对

### 25.1 医疗安全风险

风险：用户误以为 App 判断奶一定安全。

应对：

- 全局免责声明。
- 文案不写 safe。
- FAQ 引用权威来源。
- 鼓励遵循包装和医生建议。
- 使用 "When in doubt, discard."

### 25.2 留存风险

风险：用户只用几天就卸载。

应对：

- 小组件。
- 夜间模式。
- 多宝宝。
- 历史统计。
- 温和提醒。
- 让记录路径极短。

### 25.3 竞品风险

风险：Baby Tracker 类 App 竞争多。

应对：

- 不做泛 Baby Tracker 起手。
- 主打极简喂奶记录。
- 截图和 ASO 聚焦这个细分痛点。
- 用更好夜间体验差异化。

### 25.4 变现风险

风险：用户不愿订阅。

应对：

- Lifetime 低门槛。
- 年订阅明显折扣。
- 免费版可用但有明确限制。
- Pro 功能绑定真实高价值：多宝宝、导出、无广告、备份、小组件。

### 25.5 技术风险

风险：提醒不准、后台限制。

应对：

- 本地持久化。
- App 打开时刷新状态。
- 设备重启恢复。
- 通知提前量容错。
- 不承诺"精确判断"。

---

## 26. 上线后迭代路线图

### V1.0

- Feed log
- Diaper / sleep logs
- Basic insights
- Notifications
- Night mode
- Widget
- Ads
- Pro
- Multi-language

### V1.1

- Google Drive backup
- Better export
- More widget sizes
- ASO improvements

### V1.2

- Multi-caregiver sharing
- Firebase sync
- Caregiver roles
- Doctor export report

### V2.0

- Baby Care Suite
- Medication reminder
- Temperature log
- Growth log
- Smart summaries

---

## 27. 最小可行技术实现建议

### 27.1 一人开发优先级

第一优先级：

- 让喂奶记录路径极短。
- 让通知可靠。
- 让 Google Play 上架合规。

第二优先级：

- 统计漂亮。
- 小组件好看。
- 多语言完善。
- Paywall 优化。

第三优先级：

- 云同步。
- 多照护者。
- PDF 导出。
- 更复杂 Baby Care Suite。

### 27.2 不要过早复杂化

不要一开始就做：

- 账号系统
- 家庭邀请
- 实时同步
- AI 分析
- 医疗建议
- 社区
- 高级图表
- 复杂自定义规则

### 27.3 推荐首版开发顺序

1. Baby model
2. FeedLog model
3. Feed log UI
4. Quick log buttons
5. Notifications
6. Diaper log
7. Sleep log
8. Logs
9. Today summary
10. Paywall
11. Ads
12. Widget
13. Localization
14. Play listing

---

## 28. 一人公司执行建议

### 28.1 每天固定节奏

- 上午：核心开发
- 下午：测试与修 bug
- 晚上：ASO、文案、多语言、素材
- 每周末：打包内测、看数据、写更新日志

### 28.2 版本纪律

每个版本只解决一个核心目标：

- v0.1：喂奶记录跑通
- v0.2：尿布+睡眠跑通
- v0.3：提醒可靠
- v0.4：商业化跑通
- v0.5：多语言和素材
- v1.0：上架

### 28.3 先上线再扩展

不要等到 Baby Care Suite 完整再发。这个产品的验证点是：

> 用户会不会因为"极简喂奶记录"下载、留存、付费。

如果这个点成立，再扩展尿布、睡眠、备份、家庭共享。

---

## 29. 首发版本验收清单

### 产品

- [ ] 可创建宝宝
- [ ] 可记录喂奶（快捷 + 自定义）
- [ ] 可记录尿布
- [ ] 可记录睡眠
- [ ] 首页有今日摘要
- [ ] Logs 可查看历史
- [ ] 设置可查看参考来源
- [ ] 有免责声明

### 商业化

- [ ] 免费版限制生效
- [ ] Pro 权益生效
- [ ] Restore purchase 可用
- [ ] 订阅过期处理正确
- [ ] 广告展示位置合理
- [ ] Pro 无广告

### 技术

- [ ] Crashlytics 可用
- [ ] Analytics 可用
- [ ] Room migration 策略存在
- [ ] 通知权限处理完整
- [ ] 设备重启后提醒恢复
- [ ] 大字体适配
- [ ] 深色模式适配
- [ ] 多语言无明显溢出

### 合规

- [ ] Privacy Policy
- [ ] Terms
- [ ] Data Safety
- [ ] 内容评级
- [ ] 不写安全保证
- [ ] 不替代医生建议
- [ ] 来源链接可访问
- [ ] 广告类别过滤

### 上架

- [ ] App icon
- [ ] Feature graphic
- [ ] Phone screenshots
- [ ] Tablet screenshots，可选
- [ ] Short description
- [ ] Long description
- [ ] 多语言 listing
- [ ] Closed testing
- [ ] Production rollout plan

---

## 30. 附录：首版页面清单

### Onboarding

- Welcome
- Create baby
- Unit preference
- Disclaimer
- Notification permission

### Main

- Today
- Quick Feed
- Quick Diaper
- Quick Sleep
- Logs
- Insights
- Settings

### Monetization

- Paywall
- Manage subscription
- Restore purchase

### Support

- FAQ
- Safety Sources
- Contact
- Privacy Policy
- Terms

---

## 31. 附录：FAQ 草稿

### Is this app medical advice?

No. This app provides tracking tools based on public guidelines. It does not provide medical advice and cannot determine whether milk is safe.

### What should I do if I am unsure?

When in doubt, discard the milk and follow your pediatrician's advice.

### Does the app work offline?

Yes. The core tracking and logs work offline. Backup and sync features may require internet access.

### Can I track more than one baby?

Yes, multi-baby tracking is available in Pro.

### Can I export my data?

CSV export is available in Pro.

---

## 32. 附录：首版数据埋点表

| Event | 触发时机 | 关键参数 |
|---|---|---|
| onboarding_started | 首次打开 | locale, country |
| baby_created | 创建宝宝 | baby_age_days |
| feed_logged | 记录喂奶 | feed_type, amount |
| diaper_logged | 记录尿布 | diaper_type |
| sleep_started | 开始睡眠 | baby_age_days |
| sleep_ended | 结束睡眠 | duration |
| widget_added | 添加小组件 | size |
| paywall_viewed | 展示付费页 | trigger |
| purchase_completed | 购买成功 | product_id |
| ad_impression | 广告展示 | placement |
| export_completed | 导出成功 | format |

---

## 33. 附录：首版安全文案库

### General

This app is a tracking and reminder tool, not medical advice.

### Guideline note

Information is based on public guidelines. Always follow your formula label, local guidance, and your pediatrician's advice.

### Region difference

Guidance may vary by country or organization. You can review the sources in Settings.

---

## 34. 附录：MVP 决策记录

| 决策 | 选择 | 原因 |
|---|---|---|
| 平台 | Android-only | 一人开发聚焦，已有 Google Play 企业账号 |
| 后端 | MVP 无后端 | 降低复杂度 |
| 核心入口 | 喂奶记录 | 极简、父母最需要的功能 |
| 商业模式 | 广告 + Pro | 工具 App 常见模式 |
| Pro 价格 | $2.99 / $19.99 / $29.99 | 低门槛，适合轻工具 |
| 首发语言 | EN/ES/ZH/DE/FR | 覆盖主要海外市场 |
| 医疗边界 | 不提供医疗建议 | 降低合规风险 |
| v2.0 移除奶瓶计时器 | 喂奶记录替代 | 新手父母无时间操作复杂计时器，极简记录更实用 |

---

# End of Document
