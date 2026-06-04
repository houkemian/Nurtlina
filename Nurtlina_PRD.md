# Baby Formula Timer & Feeding Tracker 生产级 PRD 与阶段执行方案

> 文档版本：v1.0  
> 面向平台：Android-only  
> 目标市场：海外市场优先，首发建议美国、英国、加拿大、澳大利亚、新西兰  
> 产品类型：ToC 工具型订阅 App  
> 商业模式：广告 + Pro 订阅 + Lifetime  
> 适合团队：一人开发、轻运营、低后端依赖  
> 风险等级：中低；核心风险在医疗/育儿安全表述与提醒误导，需要严格边界  
> 文档日期：2026-05-29

---

## 1. 产品一句话定义

Baby Formula Timer & Feeding Tracker 是一款面向新手父母和照护者的婴儿喂养计时与护理记录工具，帮助用户记录奶瓶新鲜度、喂奶、尿布、睡眠，并通过清晰提醒降低“忘记这瓶奶什么时候冲的/喝过没有”的焦虑。

---

## 2. 产品定位

### 2.1 英文定位

A simple and trustworthy timer and tracker for baby formula feeding, bottle freshness, diapers, and sleep.

### 2.2 中文定位

一个简单、可信、无压力的婴儿配方奶计时器和护理记录 App。

### 2.3 核心价值主张

用户不需要复杂的育儿社区，也不需要 AI 聊天医生。用户真正需要的是：

- 一眼知道这瓶奶还能不能喝。
- 一键记录宝宝什么时候喝了多少。
- 夜里半睡半醒时也能快速操作。
- 多个照护者遵循同一套提醒规则。
- 有权威来源支撑的保存时间提示，而不是模糊经验。

### 2.4 品牌气质

- Simple：少步骤、少配置。
- Calm：降低焦虑，不制造恐慌。
- Trustworthy：所有安全建议都标注来源。
- Non-medical：不诊断、不承诺、不替代医生。
- Parent-first：面向疲惫父母，夜间场景友好。

---

## 3. 为什么这个方向适合一人公司

### 3.1 一人可做

首版不需要复杂社交关系链，不需要内容审核，不需要 UGC，不需要医生认证体系。核心是本地计时、记录、统计、提醒、备份。

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
- 痛点：夜里喂奶频繁，记不清上一瓶什么时候冲的、宝宝喝了多少、尿布换了没有。
- 需求：快速记录、到期提醒、日统计、夜间模式。
- 付费可能性：中高。对省心和减少焦虑愿意付费。

#### Persona B：混合喂养家庭

- 使用母乳 + 配方奶
- 痛点：不同奶源保存规则不同，容易混淆。
- 需求：区分 formula / breast milk / ready-to-feed / expressed milk。
- 付费可能性：高。需要更细统计。

#### Persona C：多照护者家庭

- 父母、祖父母、保姆共同照护
- 痛点：谁喂过？什么时候喂的？这瓶奶有没有喝过？
- 需求：多宝宝、多照护者同步、导出记录。
- 付费可能性：高。云同步与多成员可作为 Pro 高价值点。

#### Persona D：海外移民 / 多语言家庭

- 家里可能使用中英双语或西语等
- 痛点：英文育儿资料看不顺，家人语言不同
- 需求：多语言界面、保存规则解释清楚
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

- 闹钟不知道奶瓶状态。
- 纸笔和备忘录夜里操作麻烦。
- 通用 Baby Tracker 功能太多，打开慢，路径深。
- 很多 App 没有明确区分“喝过”和“没喝过”。
- 很多 App 没有引用权威保存规则。
- 很多 App 的统计复杂但核心计时不好用。
- 多宝宝、多照护者、导出功能常常要高价订阅。

### 5.3 本产品要解决的核心问题

> 在最少操作下，让照护者知道每一瓶奶的状态、到期时间、是否喝过，并顺手沉淀喂养记录。

---

## 6. 产品范围

### 6.1 第一版必须做

- 奶瓶计时器
- 喝过 / 没喝过状态
- 到期提醒
- 喂奶记录
- 奶量统计
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
- 自定义保存规则模板
- 成长曲线集成 WHO/CDC 数据，但需谨慎

### 6.4 第三阶段考虑

- Baby Care Suite：喂奶、尿布、睡眠、用药、体温、成长记录
- 家庭共享
- 保姆模式
- 儿科医生导出报告
- 品牌化小组件主题
- 区域化安全指南切换

---

## 7. 权威规则与合规边界

### 7.1 重要原则

App 不应写：

- “保证安全”
- “这个奶一定可以喝”
- “医生推荐”
- “医学级判断”
- “自动判断宝宝健康”
- “替代医生建议”

App 应写：

- “Based on selected guideline”
- “Use as a reminder, not medical advice”
- “When in doubt, discard”
- “Follow your formula label, local guidance, and pediatrician advice”
- “Different regions may publish different guidance”

### 7.2 默认保存规则建议

默认规则建议以美国 CDC / AAP 口径为主，同时在设置中提供地区说明。

#### 配好的婴儿配方奶，未开始喂

- 室温：配好后 2 小时内使用。
- 如果 2 小时内不会使用，应立即冷藏。
- 冷藏：24 小时内使用。

#### 已开始喂的配方奶

- 从开始喂奶起 1 小时内使用。
- 喝过后剩余奶不应冷藏留到下次。
- 原因说明：宝宝唾液可能带入细菌。

#### 母乳

- 新鲜挤出的母乳：室温 4 小时内，冷藏 4 天内，冷冻最佳 6 个月内，最长可到 12 个月。
- 解冻后的母乳规则需单独做，因为不同来源有细节差异。

### 7.3 来源

以下来源用于 App 内 FAQ、设置页说明、免责声明与保存规则依据。具体文案应以用户所在地区、奶粉包装说明和医生建议为准。

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

This app provides reminders and tracking tools based on selected public guidelines. It does not provide medical advice and cannot determine whether milk is safe. Always follow your formula label, local health guidance, and your pediatrician’s advice. When in doubt, discard the milk.

#### 中文

本 App 仅基于所选公开指南提供计时提醒和记录工具，不提供医疗建议，也不能判断奶液是否绝对安全。请始终遵循奶粉包装说明、当地卫生机构指南以及儿科医生建议。如有疑问，请丢弃。

---

## 8. 产品目标

### 8.1 MVP 目标

在 8-10 周内上线 Google Play，验证以下问题：

- 用户是否愿意每天使用奶瓶计时器？
- 用户是否愿意为了无广告、小组件、多宝宝、统计而付费？
- “奶瓶新鲜度计时”是否能成为差异化入口？
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
- 首次创建奶瓶计时器完成率
- 首次记录喂奶完成率
- 首次授权通知完成率
- 首日创建 2 个以上事件的比例

### 9.2 留存指标

- D1 / D3 / D7 / D30 留存
- 每日创建奶瓶计时器次数
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
- 奶瓶计时器创建路径 < 2 步
- 夜间记录单次操作 < 5 秒
- 通知送达准确率 > 95%

---

## 10. 核心用户旅程

### 10.1 首次使用

1. 用户打开 App。
2. 看到简单欢迎页：Track bottles, feeds, diapers, and sleep.
3. 选择语言或使用系统语言。
4. 创建宝宝：昵称、出生日期、单位偏好。
5. 选择默认指南地区：US / UK / Custom。
6. 看到免责声明。
7. 请求通知权限，解释用途。
8. 进入首页。
9. 首页中央是“Start Bottle Timer”。
10. 用户创建第一瓶奶。
11. App 显示倒计时、状态、提醒时间。

### 10.2 夜间喂奶

1. 用户打开 App 或点击小组件。
2. 夜间模式自动降低亮度/使用深色。
3. 一键选择“Start feeding”。
4. 输入奶量或选择预设奶量。
5. 如果宝宝喝完，点击 Done。
6. 如果剩余，App 提示“Used formula should be discarded after 1 hour from feeding start based on selected guideline.”
7. 自动生成喂奶记录。

### 10.3 未喝过的奶瓶

1. 用户冲好奶。
2. 点击“New bottle”。
3. 选择 formula / breast milk。
4. 输入容量。
5. 状态默认为“Not started”。
6. 倒计时 2 小时。
7. 到期前 15 分钟通知。
8. 到期时通知。
9. 用户可以选择“Fed”, “Refrigerated”, “Discarded”。

### 10.4 多宝宝家庭

1. 首页顶部切换宝宝。
2. 免费版只能创建 1 个宝宝。
3. 点击添加第二个宝宝时触发 Pro paywall。
4. Pro 用户可创建多个宝宝，分别统计。

### 10.5 小组件使用

1. 用户添加桌面小组件。
2. 小组件显示最近一瓶奶状态。
3. 显示剩余时间、宝宝昵称、状态颜色。
4. 可一键打开 App。
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
- 主操作按钮：New Bottle
- 活跃奶瓶卡片
- 快捷记录：Feed / Diaper / Sleep
- 今日摘要
- 最近记录

### 11.3 Logs 记录页

- 时间线
- 类型筛选：Bottle / Feed / Diaper / Sleep
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
- Guidelines region
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

## 12.1 奶瓶计时器

### 12.1.1 功能说明

用户创建奶瓶后，App 根据奶瓶类型、是否已开始喝、保存状态，自动计算到期时间并发送提醒。

### 12.1.2 奶瓶类型

MVP 支持：

- Powder formula / Prepared formula
- Ready-to-feed formula
- Expressed breast milk
- Custom

V1 可以先显示为：

- Formula
- Breast milk
- Custom

Formula 内部默认使用配方奶规则。

### 12.1.3 奶瓶状态

- Not started：未开始喝
- Feeding started：已开始喝
- Refrigerated：已冷藏
- Expired：已过期
- Fed：已完成喂奶
- Discarded：已丢弃
- Canceled：取消记录

### 12.1.4 创建奶瓶字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| baby_id | UUID | 是 | 所属宝宝 |
| milk_type | enum | 是 | formula / breast_milk / custom |
| amount_ml | number | 否 | 奶量 |
| prepared_at | datetime | 是 | 配好时间，默认当前 |
| status | enum | 是 | 默认 not_started |
| guideline_region | enum | 是 | US / UK / Custom |
| note | text | 否 | 备注 |

### 12.1.5 操作

- New bottle
- Start feeding
- Mark as refrigerated
- Mark as fed
- Mark as discarded
- Edit amount
- Edit time
- Cancel timer
- Duplicate last bottle

### 12.1.6 计时规则

#### Formula 默认规则

| 状态 | 默认到期规则 |
|---|---|
| Not started at room temperature | prepared_at + 2 hours |
| Feeding started | feeding_started_at + 1 hour |
| Refrigerated before use | prepared_at + 24 hours |
| Fed | 停止计时 |
| Discarded | 停止计时 |

#### Breast milk 默认规则

| 状态 | 默认到期规则 |
|---|---|
| Fresh expressed at room temperature | expressed_at + 4 hours |
| Refrigerated | expressed_at + 4 days |
| Frozen | MVP 可不做计时，仅记录 |
| Previously frozen / thawed | V1.1 增加 |

#### Custom 规则

Pro 版提供：

- 室温可保存时间
- 喂后可保存时间
- 冷藏可保存时间
- 提醒提前量

### 12.1.7 提醒

默认提醒：

- 到期前 15 分钟
- 到期时
- 喂奶开始后 45 分钟
- 喂奶开始后 60 分钟

用户可配置：

- 关闭提醒
- 到期前 5 / 10 / 15 / 30 分钟提醒
- 静音时段
- 夜间震动
- 通知音

### 12.1.8 通知文案

#### 到期前

Bottle expires in 15 minutes  
Use it soon or discard it based on your selected guideline.

#### 到期时

Bottle timer expired  
When in doubt, discard the milk. This app is a reminder, not medical advice.

#### 已开始喂后

Feeding started 45 minutes ago  
Used formula should be discarded after the selected timer ends.

### 12.1.9 验收标准

- 用户可在 2 步内创建奶瓶计时器。
- 计时器在后台保持准确。
- 修改 prepared_at 后到期时间实时重算。
- 状态变化后旧通知取消，新通知创建。
- App 重启后计时器状态正确恢复。
- 手机重启后提醒可恢复。
- 到期规则页面可查看来源与免责声明。
- 不出现“safe to drink”之类绝对安全文案。

---

## 12.2 喂奶记录

### 12.2.1 功能说明

记录宝宝每次喂奶时间、奶量、类型和备注，支持从奶瓶计时器自动生成。

### 12.2.2 字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| baby_id | UUID | 是 | 宝宝 |
| feed_type | enum | 是 | formula / breast_milk / mixed / nursing / other |
| amount_ml | number | 否 | 奶量 |
| started_at | datetime | 是 | 开始时间 |
| ended_at | datetime | 否 | 结束时间 |
| bottle_id | UUID | 否 | 关联奶瓶 |
| side | enum | 否 | breastfeeding: left / right / both |
| note | text | 否 | 备注 |

### 12.2.3 快捷输入

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

### 12.2.4 验收标准

- 可一键记录“刚刚喝完 X ml”。
- 支持编辑时间和奶量。
- 今日总奶量自动更新。
- 删除喂奶记录后统计自动回滚。
- 与奶瓶计时器关联时，可显示“from bottle created at 02:15”。

---

## 12.3 尿布记录

### 12.3.1 功能说明

记录尿布更换类型，用于父母回看宝宝排便排尿情况。

### 12.3.2 类型

- Wet
- Dirty
- Mixed
- Dry

### 12.3.3 字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| baby_id | UUID | 是 | 宝宝 |
| diaper_type | enum | 是 | wet / dirty / mixed / dry |
| changed_at | datetime | 是 | 更换时间 |
| note | text | 否 | 备注 |

### 12.3.4 验收标准

- 首页可一键记录 Wet / Dirty / Mixed。
- 今日摘要显示尿布次数。
- 记录页可筛选尿布类型。
- 不提供“是否正常”的医学判断。

---

## 12.4 睡眠记录

### 12.4.1 功能说明

记录宝宝睡眠开始和结束时间，支持正在进行中的睡眠计时器。

### 12.4.2 字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| baby_id | UUID | 是 | 宝宝 |
| started_at | datetime | 是 | 入睡时间 |
| ended_at | datetime | 否 | 醒来时间 |
| note | text | 否 | 备注 |

### 12.4.3 操作

- Start sleep
- End sleep
- Add past sleep
- Edit
- Delete

### 12.4.4 验收标准

- 只能有一个进行中的睡眠记录。
- 今日睡眠总时长正确。
- 跨天睡眠能正确归属统计。
- 不输出睡眠医学建议。

---

## 12.5 统计

### 12.5.1 免费版统计

- 今日喂奶次数
- 今日总奶量
- 今日尿布次数
- 今日睡眠时长
- 最近 24 小时记录

### 12.5.2 Pro 统计

- 7 天趋势
- 14 天趋势
- 30 天趋势
- 按宝宝比较
- 平均喂奶间隔
- 平均单次奶量
- 夜间喂奶次数
- CSV/PDF 导出

### 12.5.3 注意边界

统计页不要自动判断宝宝摄入是否正常，不要给出“偏低/偏高/异常”的医疗判断。可以写：

- “This is a tracking summary, not a medical assessment.”
- “Talk to your pediatrician if you have concerns.”

---

## 12.6 夜间模式

### 12.6.1 功能说明

夜间打开 App 时自动使用低亮度、深色、高对比、少文字界面，减少刺激。

### 12.6.2 功能

- 跟随系统深色模式
- 手动夜间模式
- 夜间快速记录页面
- 大按钮
- 低亮度提示色
- 可选防误触确认

### 12.6.3 验收标准

- 夜间模式下所有关键操作可单手完成。
- 按钮字号和点击区域足够大。
- 不使用刺眼颜色。
- 不弹出全屏广告打断夜间记录。

---

## 12.7 桌面小组件

### 12.7.1 免费版

- 显示最近一个活跃奶瓶
- 显示剩余时间
- 点击打开 App

### 12.7.2 Pro 版

- 多宝宝小组件
- 多尺寸小组件
- 主题颜色
- 透明背景
- 一键 New Bottle
- 一键 Start Feeding

### 12.7.3 验收标准

- 小组件倒计时准确或至少分钟级更新。
- 手机重启后可恢复。
- 多宝宝不会显示错宝宝。
- Pro 主题在订阅失效后回落到默认主题。

---

## 12.8 多语言

### 12.8.1 首发语言

- English
- Spanish
- Simplified Chinese
- German
- French

### 12.8.2 本地化范围

- App UI
- Google Play listing
- FAQ
- 通知文案
- Paywall
- 错误提示
- 免责声明
- 引导页

### 12.8.3 优先级

第一优先级：

- English
- Spanish

第二优先级：

- German
- French
- Chinese

### 12.8.4 注意事项

- 医疗/安全相关文案不要自由发挥。
- 多语言版本都应保留“not medical advice”。
- 地区指南不要因语言自动切换，必须让用户知道当前选择的是哪套指南。

---

## 12.9 备份与导出

### 12.9.1 MVP

- 本地数据
- 手动 CSV 导出：Pro
- Android 系统分享

### 12.9.2 V1.1

- Google Drive 备份
- Firebase Anonymous + Google 登录
- 自动备份：Pro
- 恢复数据

### 12.9.3 V1.2

- 多设备同步
- 家庭共享

---

## 12.10 Pro 订阅

### 12.10.1 免费版

- 1 个宝宝
- 基础奶瓶计时
- 基础喂奶记录
- 基础尿布记录
- 基础睡眠记录
- 今日统计
- 有广告
- 默认小组件
- 最近 30 天记录可查看

### 12.10.2 Pro 版

- 无广告
- 多宝宝
- 全部历史记录
- 7/14/30 天统计
- CSV/PDF 导出
- 云备份
- 小组件主题
- 自定义提醒
- 自定义保存规则
- 优先支持

### 12.10.3 价格

建议：

- Monthly：$2.99 / month
- Yearly：$19.99 / year
- Lifetime：$29.99 one-time

### 12.10.4 Paywall 触发点

- 添加第二个宝宝
- 查看 30 天以上历史
- 导出 CSV/PDF
- 开启云备份
- 使用小组件主题
- 自定义保存规则
- 去广告入口
- 查看高级统计

### 12.10.5 Paywall 文案

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
- 不建议在提醒后立刻弹广告
- 不建议 Rewarded，和育儿场景不匹配

### 13.2 广告限制

以下场景不展示广告：

- 夜间模式
- 创建奶瓶流程中
- 到期提醒后打开的落地页
- 喂奶进行中
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

### 14.2 Bottle

```text
Bottle
- id: UUID
- baby_id: UUID
- milk_type: MilkType
- amount_ml: Double?
- prepared_at: Instant
- feeding_started_at: Instant?
- refrigerated_at: Instant?
- status: BottleStatus
- guideline_region: GuidelineRegion
- expires_at: Instant?
- discarded_at: Instant?
- fed_at: Instant?
- note: String?
- created_at: Instant
- updated_at: Instant
```

### 14.3 FeedLog

```text
FeedLog
- id: UUID
- baby_id: UUID
- bottle_id: UUID?
- feed_type: FeedType
- amount_ml: Double?
- started_at: Instant
- ended_at: Instant?
- note: String?
- created_at: Instant
- updated_at: Instant
```

### 14.4 DiaperLog

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

### 14.5 SleepLog

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

### 14.6 UserSettings

```text
UserSettings
- id: String
- language: String
- unit: UnitType
- guideline_region: GuidelineRegion
- notification_enabled: Boolean
- reminder_before_expiry_minutes: Int
- night_mode_enabled: Boolean
- theme: ThemeType
- subscription_status: SubscriptionStatus
- created_at: Instant
- updated_at: Instant
```

### 14.7 GuidelineRule

```text
GuidelineRule
- id: String
- region: GuidelineRegion
- milk_type: MilkType
- room_temp_minutes: Int?
- feeding_started_minutes: Int?
- refrigerated_minutes: Int?
- source_name: String
- source_url: String
- source_updated_at: LocalDate?
```

---

## 15. 核心状态机

### 15.1 Bottle 状态流转

```text
NotStarted
  -> FeedingStarted
  -> Fed
  -> Discarded
  -> Expired

NotStarted
  -> Refrigerated
  -> FeedingStarted
  -> Fed
  -> Discarded
  -> Expired

NotStarted
  -> Discarded
  -> Canceled

Refrigerated
  -> Discarded
  -> Expired

Expired
  -> Discarded
```

### 15.2 关键规则

- FeedingStarted 后不能回到 NotStarted，除非用户编辑并确认。
- Fed 后停止所有提醒。
- Discarded 后停止所有提醒。
- Expired 后可以标记 Discarded，但不能标记 Fed，除非用户确认“record anyway”。
- 用户可编辑历史，但必须保留 updated_at。
- 所有到期计算应可追溯到 guideline_region 和 rule_id。

---

## 16. 计时计算逻辑

### 16.1 Formula

伪代码：

```kotlin
fun calculateFormulaExpiry(bottle: Bottle, rule: GuidelineRule): Instant? {
    return when (bottle.status) {
        NotStarted -> bottle.preparedAt + rule.roomTempMinutes.minutes
        Refrigerated -> bottle.preparedAt + rule.refrigeratedMinutes.minutes
        FeedingStarted -> bottle.feedingStartedAt + rule.feedingStartedMinutes.minutes
        Fed, Discarded, Canceled -> null
        Expired -> bottle.expiresAt
    }
}
```

### 16.2 提醒调度

```kotlin
fun scheduleBottleReminders(bottle: Bottle) {
    cancelExistingBottleReminders(bottle.id)

    val expiresAt = bottle.expiresAt ?: return
    val before = expiresAt - settings.reminderBeforeExpiryMinutes.minutes

    if (before > now()) {
        scheduleNotification(
            id = "bottle_${bottle.id}_before",
            time = before
        )
    }

    if (expiresAt > now()) {
        scheduleNotification(
            id = "bottle_${bottle.id}_expired",
            time = expiresAt
        )
    }
}
```

### 16.3 Android 技术注意

- 使用 WorkManager 处理可延迟任务。
- 对精确到分钟的到期提醒，可考虑 AlarmManager。
- Android 12+ exact alarm 权限需谨慎，尽量避免强依赖。
- App 重启、设备重启后重新扫描未完成 bottle 并恢复提醒。
- 使用 Room 持久化计时状态，不依赖内存倒计时。
- UI 倒计时由 expires_at - now 动态计算。

---

---

## 18. UI/UX 设计

### 18.1 首页布局

```text
[Baby Switcher]                         [Settings]

Good evening, Emma's bottle tracker

[Active Bottle Card]
Formula • 120 ml
Not started
Expires in 1h 24m
Prepared at 8:12 PM

[Start feeding] [Refrigerate] [Discard]

[+ New Bottle]

Quick log:
[Feed] [Diaper] [Sleep]

Today:
Formula: 480 ml
Feeds: 5
Diapers: 6
Sleep: 3h 20m
```

### 18.2 活跃奶瓶卡片状态颜色

建议：

- 正常：绿色/中性
- 临近到期：橙色
- 已过期：红色
- 已冷藏：蓝色
- 已完成：灰色

注意不要只靠颜色表达状态，需要文字。

### 18.3 夜间页面

```text
Emma

Bottle expires in
42 min

[Start feeding]

[Log 120 ml fed]
[Diaper]
[Sleep]
```

### 18.4 空状态文案

#### Today 空状态

No active bottle yet  
Start a bottle timer to track freshness and reminders.

#### Logs 空状态

No logs for this day  
Quickly add a feed, diaper, or sleep record.

#### Insights 空状态

Track for a few days to see trends.

---

## 19. 文案系统

### 19.1 语气原则

- 不恐吓
- 不做安全保证
- 不制造父母内疚
- 用“reminder / guideline / based on selected source”
- 多用 “when in doubt, discard”
- 对错误操作给出温和提醒

### 19.2 核心英文文案

#### New bottle

Start a bottle timer

#### Status

Not started  
Feeding started  
Refrigerated  
Expired  
Fed  
Discarded

#### Safety note

Timers are based on your selected guideline. This app cannot determine whether milk is safe.

#### Expired

This bottle timer has expired. When in doubt, discard the milk.

#### Paywall

Track with less stress  
Unlock multiple babies, deeper history, exports, backup, and ad-free tracking.

### 19.3 中文文案

#### 新建奶瓶

开始奶瓶计时

#### 状态

未开始喝  
已开始喝  
已冷藏  
已到期  
已喂完  
已丢弃

#### 安全提示

计时基于你选择的公开指南。本 App 不能判断奶液是否绝对安全。

#### 到期

这瓶奶的计时已到期。如有疑问，请丢弃。

---

## 20. Google Play 上架策略

### 20.1 App 名称候选

- Bottle Timer & Baby Tracker
- Baby Formula Timer
- Formula Feeding Tracker
- Baby Bottle Timer
- Feed & Bottle Timer

建议首发名称：

Bottle Timer & Baby Tracker

原因：

- 覆盖 bottle timer 搜索
- 覆盖 baby tracker 搜索
- 不局限于 formula
- 后续可扩展尿布、睡眠

### 20.2 短描述

Track bottle freshness, formula feeds, diapers, and sleep with simple reminders.

### 20.3 长描述结构

1. 解决什么问题
2. 核心功能
3. 奶瓶计时器
4. 喂奶/尿布/睡眠记录
5. 统计与导出
6. 多宝宝与小组件
7. 安全来源与免责声明
8. Pro 说明

### 20.4 关键词

核心关键词：

- baby bottle timer
- formula timer
- baby feeding tracker
- formula feeding tracker
- newborn tracker
- baby care tracker
- diaper tracker
- sleep tracker
- breast milk storage timer
- bottle feeding app

### 20.5 截图卖点

建议 8 张：

1. Never forget when a bottle expires
2. Track formula feeds in seconds
3. Know if a bottle was started or not
4. Night mode for sleepy parents
5. Log diapers and sleep
6. See daily feeding summaries
7. Add a home screen widget
8. Unlock Pro: multi-baby, exports, backup

### 20.6 图标方向

- 奶瓶 + 小时钟
- 柔和圆角
- 不使用红十字
- 不使用医疗符号
- 避免奶粉品牌联想
- 深浅模式适配

---

## 21. 隐私与合规

### 21.1 收集数据

MVP 尽量少收集：

- 宝宝昵称
- 出生日期，可选
- 喂奶记录
- 尿布记录
- 睡眠记录
- App 使用分析事件
- 崩溃日志
- 购买状态

### 21.2 不收集

- 宝宝真实姓名，建议提示使用昵称
- 照片
- 精确地址
- 医疗诊断
- 联系人
- 麦克风
- 相机
- 位置

### 21.3 隐私策略重点

- 数据主要存储在用户设备上。
- 若开启云备份，将数据同步到指定服务。
- 广告和分析 SDK 可能收集设备标识符。
- 用户可导出或删除本地数据。
- App 面向成人父母/照护者，不面向儿童直接使用。

### 21.4 Google Play Data Safety

需要声明：

- App activity：analytics
- App info and performance：crash logs
- Personal info：如果使用邮箱登录，则声明 email
- Health and fitness：谨慎。婴儿喂养/睡眠记录可能被视作健康相关数据，应按 Google Play 最新分类如实填写。
- Data encryption in transit：云备份时必须有
- User deletion request：提供邮箱或 App 内删除

### 21.5 儿童政策

App 的目标用户不是儿童，而是成人父母。Google Play 内容评级和目标受众应避免选择“主要面向儿童”。但由于内容涉及婴儿护理，广告策略要保守。

---

## 22. 竞品分析方向

### 22.1 竞品类别

- 通用 Baby Tracker
- Breastfeeding tracker
- Formula feeding tracker
- Diaper tracker
- Sleep tracker
- Pumping tracker

### 22.2 常见竞品问题

- 功能太重
- UI 复杂
- 广告打断
- 订阅过贵
- 没有奶瓶新鲜度状态机
- 没有清晰引用安全规则
- 小组件不好用
- 夜间模式体验差
- 多语言粗糙

### 22.3 差异化机会

- 专注 Bottle Freshness Timer
- 喝过/没喝过状态明确
- 保存规则可查看来源
- 夜间体验优秀
- 小组件好用
- 价格更友好
- 多语言首发
- 轻量而可信

---

## 23. MVP 开发范围

### 23.1 Must Have

- Onboarding
- 创建宝宝
- 选择单位
- 选择默认指南地区
- 免责声明
- Today 首页
- 奶瓶计时器
- 奶瓶状态流转
- 本地通知
- 喂奶记录
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

### 23.2 Should Have

- 小组件
- 夜间模式
- CSV 导出
- 30 天统计
- 自定义提醒
- RevenueCat

### 23.3 Could Have

- PDF 导出
- Google Drive 备份
- 多主题
- 多照护者
- Ready-to-feed 细分
- 母乳解冻规则

### 23.4 Won't Have in MVP

- 社区
- AI 医生
- 成长曲线判断
- 医疗建议
- 照片识别
- 在线同步
- 家庭共享

---

## 24. 阶段执行方案

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
- 确认安全规则和来源。
- 完成 PRD v1.0。
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
- 数据库 schema v1
- 设计系统初版
- Analytics 事件规范

### 验收

- App 能打开，能在 Today/Logs/Insights/Settings 切换。
- 能创建宝宝并持久化。
- 重启 App 数据仍在。
- 崩溃日志可上报。

---

## Phase 2：奶瓶计时器核心，1.5-2 周

### 目标

完成产品核心差异化功能。

### 任务

- 创建奶瓶流程。
- 奶瓶状态机。
- Formula 默认规则。
- Breast milk 默认规则。
- 到期时间计算。
- 活跃奶瓶卡片。
- Start feeding / Refrigerate / Fed / Discard 操作。
- 到期提醒。
- 通知点击回到对应奶瓶。
- 手机重启后恢复提醒。
- 规则来源页。
- 免责声明弹窗。

### 产出

- 完整奶瓶计时器
- 本地通知
- 安全来源页面
- 单元测试

### 验收

- 创建奶瓶 2 步内完成。
- Not started / Feeding started / Refrigerated 状态到期时间正确。
- 状态变化后通知重新调度。
- 不出现安全保证文案。
- 时间规则有测试覆盖。

---

## Phase 3：喂奶、尿布、睡眠记录，1-1.5 周

### 目标

让产品具备日常 Baby Tracker 基础能力。

### 任务

- FeedLog CRUD。
- DiaperLog CRUD。
- SleepLog CRUD。
- 首页快捷记录。
- Logs 时间线。
- 日期筛选。
- 编辑/删除。
- 今日摘要。
- 关联 Bottle -> FeedLog。
- 单位 ml / oz 转换。

### 产出

- 基础记录系统
- 今日统计
- 时间线

### 验收

- 记录能正确新增、编辑、删除。
- 今日统计实时更新。
- 跨天记录归属正确。
- 奶瓶完成后可自动生成喂奶记录。

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
- 小组件显示剩余时间。
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
- 奶瓶计时器无严重 bug。
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
- 创建奶瓶漏斗
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

## 25. 详细排期建议

### 10 周版本

| 周 | 重点 | 产出 |
|---:|---|---|
| Week 1 | 调研、PRD、线框图、项目骨架 | PRD、竞品表、可运行工程 |
| Week 2 | 数据库、宝宝、设置、首页 | 基础 App |
| Week 3 | 奶瓶计时器状态机 | 核心计时器 |
| Week 4 | 通知、规则来源、测试 | 可用计时器 |
| Week 5 | 喂奶、尿布、睡眠 | 记录系统 |
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
- Breast milk 复杂规则
- 高级统计

保留：

- Bottle timer
- Feed log
- Diaper log
- Notification
- English / Spanish / Chinese
- Billing
- AdMob

---

## 26. 开发任务拆解

### Epic 1：Onboarding

- 语言选择
- 创建宝宝
- 单位选择
- 指南地区选择
- 免责声明
- 通知权限说明

### Epic 2：Bottle Timer

- Bottle 数据表
- GuidelineRule
- 创建流程
- 状态机
- 倒计时 UI
- 状态操作
- 通知调度
- 来源页
- 单元测试

### Epic 3：Tracking Logs

- FeedLog
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

## 27. 测试计划

### 27.1 单元测试

- Formula 2 小时规则
- Formula 喂后 1 小时规则
- Formula 冷藏 24 小时规则
- Breast milk 4 小时规则
- 状态流转
- 单位转换
- 今日统计
- 跨天统计

### 27.2 集成测试

- 创建奶瓶 -> 通知调度
- Start feeding -> 到期重算
- Refrigerate -> 到期重算
- Fed -> 取消通知
- Discard -> 取消通知
- App 重启 -> 计时恢复
- 设备重启 -> 通知恢复

### 27.3 UI 测试

- 首次引导
- 首页创建奶瓶
- 夜间模式
- 大字号
- 小组件点击
- Paywall
- 多语言布局

### 27.4 人工测试清单

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

## 28. 风险与应对

### 28.1 医疗安全风险

风险：用户误以为 App 判断奶一定安全。

应对：

- 全局免责声明。
- 状态文案不写 safe。
- 到期只写 timer expired。
- FAQ 引用权威来源。
- 鼓励遵循包装和医生建议。
- 使用 “When in doubt, discard.”

### 28.2 留存风险

风险：用户只用几天就卸载。

应对：

- 小组件。
- 夜间模式。
- 多宝宝。
- 历史统计。
- 温和提醒。
- 让记录路径极短。

### 28.3 竞品风险

风险：Baby Tracker 类 App 竞争多。

应对：

- 不做泛 Baby Tracker 起手。
- 主打 Bottle Timer + Freshness。
- 截图和 ASO 聚焦这个细分痛点。
- 用可信来源和更好夜间体验差异化。

### 28.4 变现风险

风险：用户不愿订阅。

应对：

- Lifetime 低门槛。
- 年订阅明显折扣。
- 免费版可用但有明确限制。
- Pro 功能绑定真实高价值：多宝宝、导出、无广告、备份、小组件。

### 28.5 技术风险

风险：提醒不准、后台限制。

应对：

- expires_at 持久化。
- App 打开时重算。
- 设备重启恢复。
- 通知提前量容错。
- 不承诺“精确安全判断”。

---

## 29. 上线后迭代路线图

### V1.0

- Bottle timer
- Feed / diaper / sleep logs
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
- Ready-to-feed formula type
- Thawed breast milk tracking
- More widget sizes
- ASO improvements

### V1.2

- Multi-caregiver sharing
- Firebase sync
- Caregiver roles
- Doctor export report
- Custom guideline profiles

### V2.0

- Baby Care Suite
- Medication reminder
- Temperature log
- Growth log
- Smart summaries
- Regional guideline packs

---

## 30. 最小可行技术实现建议

### 30.1 一人开发优先级

第一优先级：

- 让奶瓶计时器极其稳定。
- 让通知可靠。
- 让记录路径极短。
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

### 30.2 不要过早复杂化

不要一开始就做：

- 账号系统
- 家庭邀请
- 实时同步
- AI 分析
- 医疗建议
- 社区
- 高级图表
- 复杂自定义规则

### 30.3 推荐首版开发顺序

1. Baby model
2. Bottle model
3. Bottle timer UI
4. Expiry rules
5. Notifications
6. Feed log
7. Diaper log
8. Logs
9. Today summary
10. Paywall
11. Ads
12. Widget
13. Localization
14. Play listing

---

## 31. Claude / ChatGPT / Codex 辅助开发 Prompt 模板

### 31.1 生成 Kotlin 状态机

```text
You are a senior Android Kotlin engineer. Design a sealed-state model and transition functions for a baby bottle timer app.

Requirements:
- Bottle statuses: NotStarted, FeedingStarted, Refrigerated, Expired, Fed, Discarded, Canceled.
- Formula rules: room temperature 2 hours, feeding started 1 hour, refrigerated 24 hours.
- No medical claims.
- Use immutable data classes.
- Include unit tests for transitions.
```

### 31.2 生成多语言文案

```text
Translate the following baby care tracking app strings into Spanish, German, French, and Simplified Chinese.

Rules:
- Keep a calm and non-medical tone.
- Do not translate "safe" as a guarantee.
- Preserve the meaning of "not medical advice".
- Use parent-friendly wording.
```

### 31.3 生成 Google Play 描述

```text
Write a Google Play long description for an Android app named Bottle Timer & Baby Tracker.

Positioning:
- Baby bottle freshness timer
- Formula feeding tracker
- Diaper and sleep logs
- Home screen widget
- Night mode
- Not medical advice
- Cite CDC/AAP/NHS style public guidelines without claiming endorsement
```

### 31.4 生成测试用例

```text
Create test cases for a baby formula bottle timer.

Rules:
- Prepared formula at room temperature expires after 2 hours.
- Once feeding starts, the bottle timer expires after 1 hour.
- If refrigerated before feeding starts, use within 24 hours.
- Fed and discarded statuses cancel reminders.
- Include edge cases for time zone changes, app restart, and edited preparation time.
```

---

## 32. 一人公司执行建议

### 32.1 每天固定节奏

- 上午：核心开发
- 下午：测试与修 bug
- 晚上：ASO、文案、多语言、素材
- 每周末：打包内测、看数据、写更新日志

### 32.2 版本纪律

每个版本只解决一个核心目标：

- v0.1：计时器跑通
- v0.2：记录跑通
- v0.3：提醒可靠
- v0.4：商业化跑通
- v0.5：多语言和素材
- v1.0：上架

### 32.3 先上线再扩展

不要等到 Baby Care Suite 完整再发。这个产品的验证点是：

> 用户会不会因为“奶瓶新鲜度提醒”下载、留存、付费。

如果这个点成立，再扩展尿布、睡眠、备份、家庭共享。

---

## 33. 首发版本验收清单

### 产品

- [ ] 可创建宝宝
- [ ] 可创建奶瓶计时器
- [ ] 区分未喝/已开始喝/冷藏/过期/丢弃
- [ ] 到期时间正确
- [ ] 通知正确
- [ ] 可记录喂奶
- [ ] 可记录尿布
- [ ] 可记录睡眠
- [ ] 首页有今日摘要
- [ ] Logs 可查看历史
- [ ] 设置可查看指南来源
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

## 34. 推荐首版功能裁剪

如果开发过程中时间不够，优先保留：

1. Formula bottle timer
2. Drinking state
3. Notifications
4. Feed log
5. Today summary
6. Disclaimer and sources
7. Pro no ads
8. Multi-baby Pro

可以延后：

1. Sleep
2. Widget themes
3. PDF export
4. Cloud backup
5. Advanced breast milk rules
6. German/French
7. Custom rules

不要延后：

1. 通知可靠性
2. 计时规则清晰
3. 免责声明
4. 数据持久化
5. 基础记录编辑

---

## 35. 最终建议

这个产品不应该做成“又一个大而全 Baby Tracker”。首发要把一个细分痛点打穿：

> I made a bottle. Is it still within the timer? Has my baby started drinking it? What should I do now?

只要这个体验足够快、足够可信、足够适合夜间使用，就有机会在海外 Android 市场靠 ASO 和自然搜索拿到第一批用户。

最适合你的执行路径是：

1. 先做 Bottle Timer 极简 MVP。
2. 同步加入 Feed / Diaper / Sleep，构成完整但不臃肿的 Baby Tracker。
3. 用“可信来源 + 夜间体验 + 小组件”做差异化。
4. 免费版可用，Pro 卖多宝宝、无广告、导出、备份、小组件主题。
5. 先上线验证，再扩展 Baby Care Suite。

---

## 36. 附录：首版页面清单

### Onboarding

- Welcome
- Create baby
- Unit preference
- Guideline region
- Disclaimer
- Notification permission

### Main

- Today
- New Bottle
- Bottle Detail
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

## 37. 附录：FAQ 草稿

### Is this app medical advice?

No. This app provides timers and tracking tools based on selected public guidelines. It does not provide medical advice and cannot determine whether milk is safe.

### What should I do if I am unsure?

When in doubt, discard the milk and follow your pediatrician’s advice.

### Why does the formula timer change after feeding starts?

Public guidelines commonly recommend a shorter timer once feeding starts because the bottle may come into contact with the baby’s saliva.

### Can I change the guideline?

Yes. You can select a default guideline region or use custom timers in Pro. Always follow your formula label and local health guidance.

### Does the app work offline?

Yes. The core timer and logs work offline. Backup and sync features may require internet access.

### Can I track more than one baby?

Yes, multi-baby tracking is available in Pro.

### Can I export my data?

CSV export is available in Pro.

---

## 38. 附录：首版数据埋点表

| Event | 触发时机 | 关键参数 |
|---|---|---|
| onboarding_started | 首次打开 | locale, country |
| baby_created | 创建宝宝 | baby_age_days |
| guideline_selected | 选择指南 | region |
| bottle_created | 创建奶瓶 | milk_type, amount, guideline |
| bottle_started_feeding | 开始喂 | milk_type |
| bottle_refrigerated | 标记冷藏 | milk_type |
| bottle_expired | 到期 | milk_type, status_before |
| bottle_discarded | 丢弃 | milk_type |
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

## 39. 附录：首版安全文案库

### General

This app is a tracking and reminder tool, not medical advice.

### Bottle expired

This bottle timer has expired. When in doubt, discard the milk.

### Guideline note

Timers are based on your selected guideline. Always follow your formula label, local guidance, and your pediatrician’s advice.

### Region difference

Guidance may vary by country or organization. You can review the sources in Settings.

### Custom timer warning

Custom timers are your own settings and are not medical recommendations from this app.

---

## 40. 附录：MVP 决策记录

| 决策 | 选择 | 原因 |
|---|---|---|
| 平台 | Android-only | 一人开发聚焦，已有 Google Play 企业账号 |
| 后端 | MVP 无后端 | 降低复杂度 |
| 核心入口 | Bottle Timer | 差异化更强 |
| 商业模式 | 广告 + Pro | 工具 App 常见模式 |
| Pro 价格 | $2.99 / $19.99 / $29.99 | 低门槛，适合轻工具 |
| 首发语言 | EN/ES/ZH/DE/FR | 覆盖主要海外市场 |
| 医疗边界 | 不提供医疗建议 | 降低合规风险 |
| 来源 | CDC/AAP/NHS | 增强可信度 |
| 首发范围 | Timer + logs + basic stats | 足够验证需求 |

---

# End of Document
