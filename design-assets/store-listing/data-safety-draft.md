# Google Play Data Safety 填写草案

> 依据 2026-09-08 代码与官网内容整理。该文件不是法律意见，也不是最终提交答案。提交前必须结合发布 AAB、Firebase/AdMob/RevenueCat 当前 SDK 文档、用户同意流程和 Play Console 最新定义逐项复核。

## 应用级回答草案

| 问题 | 草案 | 核对说明 |
|---|---|---|
| 应用是否收集或共享用户数据 | 是 | Firebase、AdMob、RevenueCat、账号与可选云同步会处理设备外数据 |
| 传输中的数据是否加密 | 是 | 后端地址使用 HTTPS；仍需用发布环境实测 |
| 用户是否可以请求删除数据 | 是 | 应用内账号删除及外部删除页面均已存在 |
| 是否通过独立安全审核 | 否／待确认 | 没有证据时不要选择“是” |
| 是否面向儿童 | 否 | 产品面向成人父母与照护者；仍需在目标受众表单中准确选择 |

## 数据类别草案

| Play 数据类别 | 来源 | 收集条件 | 用途 | 共享判断 | 必需性 |
|---|---|---|---|---|---|
| 电子邮件地址 | Firebase Auth / Google 登录 | 用户登录时 | 账号管理、认证、同步 | 服务提供商处理；按 Play 定义复核 | 可选 |
| 用户 ID | Firebase Auth、后端、RevenueCat | 登录、购买或恢复权益时 | 账号、同步、权益 | 可能由处理服务接收 | 视功能而定 |
| 姓名／显示名 | Google 登录 | 账号返回显示名时 | 账号界面 | 按 Firebase/Google 定义复核 | 可选 |
| 应用互动 | Firebase Analytics | 使用应用时 | 分析、产品改进 | 通常作为服务提供商处理；需复核 | 当前 SDK 自动或事件触发 |
| 崩溃日志 | Firebase Crashlytics | 发生崩溃时 | 稳定性、诊断 | 服务提供商处理 | 当前实现默认启用 |
| 诊断信息 | Crashlytics、AdMob | 使用应用或广告时 | 诊断、广告测量、防欺诈 | 可能共享给服务提供商 | 视 SDK 行为而定 |
| 设备或其他标识符 | AdMob、Firebase、RevenueCat | Free 广告、分析、购买 | 广告、分析、权益、防欺诈 | AdMob 等可能接收 | 视场景而定 |
| 购买记录 | Google Play / RevenueCat / 后端 | 购买或恢复时 | 付款、权益验证、防欺诈 | Google Play 与 RevenueCat 处理 | 购买功能需要 |
| 健康与健身／其他用户内容 | 喂养、尿布、睡眠、备注 | 用户启用并使用云备份或同步时才离开设备 | 应用功能、备份、同步、导出 | 不用于广告；分类需按 Play 最新口径确认 | 云功能可选 |
| 宝宝昵称和可选出生日期 | 本地数据库；可选云同步 | 用户录入，启用同步时上传 | 应用功能、宝宝档案 | 不用于广告 | 可选 |
| 广告互动 | AdMob | Free 用户展示广告时 | 广告、测量、防欺诈 | AdMob 处理 | Free 广告模式需要 |

## 仅在设备本地处理的数据

- 通知权限状态。
- 提醒调度所需的设备时间。
- 用户未登录或未启用云功能时的本地照护记录。
- 本地 Room 数据库中的记录，除非进入已声明的备份／同步流程。

注意：Android 系统备份可能按设备设置处理应用数据，应继续核对 `backup_rules.xml` 与 `data_extraction_rules.xml` 的实际范围。

## SDK 与服务清单

- Firebase Analytics。
- Firebase Crashlytics。
- Firebase Authentication。
- Cloud Firestore。
- Firebase Functions。
- Google Sign-In。
- Google Mobile Ads SDK / AdMob。
- RevenueCat Purchases SDK。
- Google Play Billing（通过 RevenueCat 及后端权益验证）。
- 自有 API 与数据库托管服务。

## 提交前必须确认

- [ ] 使用 Play SDK Index 和各 SDK 最新 Data Safety 指引逐项核对。
- [ ] 确认 AdMob 是否启用个性化广告，以及同意管理平台是否已配置。
- [ ] 确认 Analytics 与 Crashlytics 的默认收集、延迟初始化和退出机制。
- [ ] 确认“备份与同步”究竟是用户主动开启还是登录即启用。
- [ ] 确认宝宝昵称、出生日期、备注和照护记录的 Play 数据类别映射。
- [ ] 确认生产后端的数据保留、删除队列和最长清理时间。
- [ ] 确认账号删除后 RevenueCat、Firebase 和后端残留数据的处理方式。
- [ ] 用最终签名 AAB 在 Play Console 中检查自动识别的 SDK。
- [ ] 保证表单答案与 `https://nurtlina.app/privacy` 内容一致。

