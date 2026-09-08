# Google Play App Access 审核说明模板

> 提交时请替换所有方括号内容。不要把真实密码提交到仓库；仅在 Play Console 的受保护审核字段中提供凭据。

## 基本信息

- Package name：`com.nurtlina.app`
- Version：`1.0.1` (`versionCode 4`)
- 联系邮箱：`support@muyestudio.net`
- 核心记录是否必须登录：否。

## 无账号审核路径

1. 启动应用。
2. 完成引导并使用虚构昵称创建一个宝宝档案。
3. 在 Today 页面选择 Feed、Diaper 或 Sleep。
4. 新建的核心记录会先保存在设备本地，网络不可用时仍可创建。
5. 使用底部导航查看 Logs、Insights 和 Settings。
6. 如果拒绝通知权限，核心功能仍可正常使用。

## 需要审核账号的功能

以下功能可能需要登录或服务端连接：

- 云备份与同步。
- 账号和数据删除。
- Pro 权益恢复。
- Google 登录相关流程。

### 审核账号

- 登录方式：Google / [补充其他已启用方式]
- 审核账号：`[仅填写到 Play Console，不提交到 Git]`
- 密码：`[仅填写到 Play Console，不提交到 Git]`
- 其他步骤：`[例如验证码、测试许可或预置账号说明]`

## Pro 审核路径

1. 打开 Settings 或受限功能入口。
2. 进入 Nurtlina Pro 页面。
3. 使用 Google Play 测试购买账号完成测试购买，或使用已配置的审核权益账号。
4. 可检查无广告、多宝宝、扩展历史／趋势、CSV 导出、备份同步和其他已上线权益。
5. Restore purchases 位于 `[填写当前准确路径]`。

## 删除账号路径

- 应用内：Settings → Account → Delete account & data。
- 外部页面：`https://nurtlina.app/data-deletion`。
- 删除云端账号不会自动取消 Google Play 订阅；订阅需在 Google Play 中管理。

## 审核注意事项

- 通知为可选权限。
- 应用不请求位置、联系人、相机或麦克风权限。
- Free 用户可能在 Today 页面看到横幅广告；Pro 和夜间模式不展示广告。
- 本应用是记录和提醒工具，不提供医疗建议。

