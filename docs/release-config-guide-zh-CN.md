# 发布配置指南：AdMob 与 Google Play

本操作手册说明当前剩余的两项发布阻塞配置。在将 Nurtlina 提交到 Google Play
管理中心进行封闭式测试之前，请先完成以下操作。

> 相关文档：[`docs/revenuecat-setup.md`](revenuecat-setup.md) 介绍了同一组商品在
> RevenueCat 后台的配置。本指南仅介绍 **AdMob** 和 **Google Play 管理中心**
> 侧的配置。

---

## 1. 将 AdMob 测试 ID 替换为正式 ID

应用当前使用的是 Google 官方测试标识符。发布前必须替换以下两个 ID，否则
应用无法展示真实广告，并且可能无法通过 AdMob 政策审核。

### ID 所在位置

| 位置 | 当前值 | 用途 |
|---|---|---|
| `frontend/app/src/main/res/values/strings.xml` → `admob_app_id` | `ca-app-pub-2777583989705415~1390589355` | AdMob **应用 ID（App ID）** |
| `frontend/app/src/main/java/com/nurtlina/app/ui/today/TodayScreen.kt` → `TodayScreen(adUnitId = ...)` 默认值 | `ca-app-pub-2777583989705415/2546195256` | **横幅广告单元 ID（Banner ad unit ID）** |

### 操作步骤

1. 打开 [apps.admob.com](https://apps.admob.com)，使用拥有 Google Play 应用条目的
   Google 账号登录。
2. 创建或选择 Nurtlina 应用。
3. **创建应用条目**
   - 进入 `Apps` → `Add app` → 选择“Android” → 选择“No”（应用尚未在商店上架；
     如果 Google Play 应用已存在，也可以直接关联）。
   - 软件包名称：`com.nurtlina.app`。
4. **复制应用 ID**
   - 进入 `App settings` → `App ID`，其格式为
     `ca-app-pub-<publisher>~<app>`。
   - 使用该值替换 `values/strings.xml` 中的 `admob_app_id`。
5. **创建横幅广告单元**
   - 进入 `Ad units` → `Add ad unit` → `Banner`。
   - 将其命名为 `Nurtlina Today Banner` 或其他易于识别的名称。
   - 复制**广告单元 ID**，其格式为 `ca-app-pub-<publisher>/<unit>`。
   - 使用该值替换 `TodayScreen.kt` 中 `adUnitId` 的默认值。
6. **可选但建议：**针对婴儿和家庭内容启用敏感类别过滤：
   - 进入 `Blocking controls` → `Ad content rating`，将内容分级保持为
     “General audiences”，并根据需要屏蔽婴儿、健康相关的敏感类别。

请勿展示插页式广告或激励广告（这是 MVP 阶段的产品决定）。应用当前仅使用 Today
页横幅广告，并已通过 `showAds = !isPro && !nightModeEnabled` 确保 Pro 用户及夜间
模式下不展示广告。

---

## 2. 创建三个 Google Play 商品

应用和 RevenueCat 均使用以下商品 ID：

| 商品 ID | 类型 | 建议价格 |
|---|---|---|
| `nurtlina_pro_monthly` | 订阅 | 每月 2.99 美元 |
| `nurtlina_pro_yearly` | 订阅 | 每年 19.99 美元 |
| `nurtlina_pro_lifetime` | 一次性购买（应用内商品） | 29.99 美元 |

### 操作步骤

1. 打开 [Google Play 管理中心](https://play.google.com/console)，选择 Nurtlina 应用
   （软件包名称：`com.nurtlina.app`）。
2. **终身版（一次性商品）**
   - 进入 `Monetize` → `Products` → `In-app products` → `Create product`。
   - 商品 ID：`nurtlina_pro_lifetime`；名称示例：
     “Nurtlina Pro (Lifetime)”；然后设置价格。
   - 激活该商品。
3. **月度订阅**
   - 进入 `Monetize` → `Products` → `Subscriptions` →
     `Create subscription`。
   - 商品 ID：`nurtlina_pro_monthly`。
   - 添加一个自动续订的**基础方案（base plan）**，价格为每月 2.99 美元，结算周期
     设置为 1 个月。
4. **年度订阅**
   - 商品 ID：`nurtlina_pro_yearly`。
   - 添加价格为每年 19.99 美元的基础方案，结算周期设置为 1 年。
5. **许可测试人员（进行任何沙盒购买前）**
   - 进入 `Setup` → `License testing` → `Add testers`，添加用于测试的
     Google 账号。
6. **确认 RevenueCat 可以读取商品**
   - 在 RevenueCat 中进入 `Product Catalog` → `Products` →
     `Import from Google Play`；也可以使用上述完全一致的 ID 手动创建商品。
   - 将三个商品关联到 `pro` 权益，并关联到默认 Offering 中对应的软件包：
     `$rc_monthly`、`$rc_annual`、`$rc_lifetime`。
   - 完整后台操作参见 [`docs/revenuecat-setup.md`](revenuecat-setup.md)。

> 文案规则：商品和付费墙文案不得包含医疗或安全保证，也不得暗示获得
> CDC、AAP、NHS 或其他机构的认可。具体要求参见 `AGENTS.md` 中的合规规则。

---

## 3. 配置 Firebase Android 证书指纹

Firebase Auth 的 Microsoft OAuth 登录需要 Firebase 项目登记安装包所使用的 Android
签名证书。否则登录会返回 `ERROR_INVALID_CERT_HASH`。

在 Firebase Console → Project settings → General → Your apps 中分别配置：

- `com.nurtlina.app.debug`
  - SHA-1：`2B:82:5E:97:18:7B:3E:77:3A:34:6C:17:67:F0:AD:C5:B1:43:18:E3`
  - SHA-256：`60:BE:D2:35:70:14:84:8E:48:91:8F:9E:4F:16:76:E1:DC:DA:B5:14:FC:C5:98:F8:03:44:CA:33:EE:4E:92:3B`
- `com.nurtlina.app`（本地 release/upload key）
  - SHA-1：`04:AA:2E:53:14:83:74:9A:0F:AE:C4:0A:0E:5B:6F:45:AB:2F:6A:92`
  - SHA-256：`8F:AB:60:DA:CC:E7:09:0F:EE:03:01:1C:CF:D4:3C:EC:D6:06:D5:F4:FA:A8:BD:FE:14:C7:9F:E3:4D:29:39:84`

如果应用由 Google Play 安装，还必须把 Play Console → App integrity 中的
**App signing key certificate** SHA-1 和 SHA-256 添加到 `com.nurtlina.app`。保存后重新下载
`google-services.json` 并替换 `frontend/app/google-services.json`，然后重新构建应用。

---

## 4. 提交前检查清单

- [x] 已在正式应用配置中写入 AdMob 应用 ID 和横幅广告单元 ID。
- [ ] 三个 Google Play 商品均已创建并处于**已激活**状态。
- [ ] RevenueCat 的 `pro` 权益已关联全部三个商品，Offering 中包含三个对应软件包。
- [ ] 已通过 `~/.gradle/gradle.properties` 配置 `REVENUECAT_API_KEY`，且未提交到
      版本库。
- [ ] `frontend/app/google-services.json` 指向正式 Firebase 项目。
- [ ] Firebase 中已登记 debug、upload/release 和 Google Play App Signing 证书指纹。
- [ ] 隐私政策（`https://nurtlina.app/privacy`）和服务条款
      （`https://nurtlina.app/terms`）页面已创建并可正常访问。
- [ ] 已上传应用图标、置顶大图和应用截图。
- [ ] 已填写“数据安全”表单：应用会收集分析数据、崩溃日志、登录时的账号邮箱，
      以及用户选择同步到云端的宝宝记录；应用内提供账号与数据删除功能，参见
      Settings → Delete account & data。
