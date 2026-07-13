# Nurtlina 项目评估、PRD 与后续任务规划

> 评估日期：2026-07-10
> 项目代号：Nurtlina — Baby Feeding Tracker
> 当前版本：v1.0.0-dev（内测前）
> **Phase 0 清理状态**：✅ 已完成 — Bottle 系统已从代码库中完全移除

---

## 目录

1. [项目概要](#1-项目概要)
2. [当前实施状态评估](#2-当前实施状态评估)
3. [PRD 概述](#3-prd-概述)
4. [账号体系](#4-账号体系)
5. [功能完成度矩阵](#5-功能完成度矩阵)
6. [技术债务与风险](#6-技术债务与风险)
7. [后续任务规划](#7-后续任务规划)
8. [里程碑与时间线](#8-里程碑与时间线)

---

## 1. 项目概要

### 1.1 产品定位

**Nurtlina** 是一款面向海外新手父母的 Android 婴儿喂养记录与护理追踪工具。核心功能是 **极简喂奶记录**——帮助用户在最少操作下快速记录每次喂奶的类型、奶量和时间。

> **v2.0 已实施**：Bottle 计时器/状态机系统已于 2026-07-10 完全移除。产品现在纯以 FeedLog 为中心：`NewFeedSheet`（原 NewBottleSheet）直接创建喂奶记录，`FeedingStatusCard` 展示上次喂奶状态与下次喂奶倒计时。

### 1.2 一句话定义

> 一个简单、可信、无压力的婴儿喂奶记录和护理追踪 App。

### 1.3 商业模式

| 层级 | 价格 | 权益 |
|---|---|---|
| Free（广告支持） | $0 | 1 个宝宝、喂奶/尿布/睡眠记录、今日统计、默认小组件、广告 |
| Pro Monthly | $2.99/月 | 无广告、多宝宝、全部历史、高级统计、导出、备份、小组件主题 |
| Pro Yearly | $19.99/年 | 同上 |
| Pro Lifetime | $29.99 | 同上，一次性买断 |

---

## 2. 当前实施状态评估

### 2.1 总体评估

**项目已进入 MVP 后端集成中期。Bottle 系统已完全移除，FeedLog 是唯一的喂养数据路径。**

| 维度 | 完成度 | 状态 |
|---|---|---|
| Android 数据层 | 95% | ✅ FeedLog/DiaperLog/SleepLog 完整；BottleEntity 已移除 |
| Android 领域层 | 90% | ✅ LogFeedUseCase + GetTodaySummaryUseCase；Bottle 用例已移除 |
| Android UI 层 | 90% | ✅ FeedingStatusCard + NewFeedSheet；ActiveBottleCard 已移除 |
| Android 通知系统 | 90% | ✅ NextFeedNotificationScheduler；BottleNotificationScheduler 已移除 |
| Android 同步系统 | 85% | ✅ Bottle 同步路径已移除 |
| Android 商业化 | 70% | ⚠️ RevenueCat 已集成，AdMob 测试 ID |
| Android 小组件 | 20% | ❌ 未开始 |
| FastAPI 后端 | 90% | ✅ 核心 API 完成；bottles 表已移除 |
| Firebase | 95% | ✅ Firestore bottles 规则已移除 |
| 测试覆盖 | 55% | ⚠️ Bottle 测试已移除，FeedLog 测试需补齐 |
| 上架素材 | 30% | ❌ 缺少图标、截图、Feature Graphic |

### 2.2 已完成的核心能力

#### 喂奶记录管理

- **NewFeedSheet**（`ui/feed/NewFeedSheet.kt`）：直接创建 FeedLog，支持奶类型选择、预设奶量、时间编辑、备注
- **FeedingStatusCard**（`TodayScreen.kt`）：上次喂奶时间/间隔/奶量、今日统计、下次喂奶倒计时
- **logFeed() / quickLogFeed()**：完整记录 + 一键预设奶量
- **NextFeedNotificationScheduler**：基于间隔的喂奶提醒
- **FeedLog CRUD**：完整，Logs 时间线 + 类型筛选 + 删除

#### Phase 0 清理成果

| 类别 | 清理量 |
|---|---|
| 删除文件 | 20 个 Kotlin + 2 个 Python |
| 编辑文件 | 39 个 Kotlin + 8 个 Python/Markdown + Firestore rules |
| 重命名 | `NewBottleSheet` → `NewFeedSheet`，包 `ui/bottle` → `ui/feed` |
| 数据库 | Room v2→v3（DROP bottles），PostgreSQL migration 0003 |
| 净代码变化 | -8,373 行，+992 行 |

---

## 3. PRD 概述

### 3.1 核心用户旅程

1. 打开 App → 创建宝宝 → 免责声明 → 通知权限
2. 首页点击 "+" → NewFeedSheet → 选奶类型、奶量 → 保存
3. FeedingStatusCard 显示"上次喂奶：X 分钟前 · 120 ml"
4. 下次喂奶时间到达时收到温和提醒

### 3.2 信息架构

```
Nurtlina App
├── Onboarding
├── Main (底部 4 Tab)
│   ├── Today — FeedingStatusCard + QuickLogRow + TodaySummary
│   ├── Logs — 时间线（Feed/Diaper/Sleep 筛选）
│   ├── Insights — 今日统计 + 趋势图表
│   └── Settings — 宝宝/单位/通知/Pro/导出/关于
├── NewFeed (→ NewFeedSheet)
├── Paywall
└── Sign In
```

### 3.3 数据模型（v2.0 — 当前实施）

```
Baby (id, familyId, name, birthDate, avatarColor)
FeedLog (id, babyId, feedType, amountMl, startedAt, endedAt, note) ← 唯一喂养实体
DiaperLog (id, babyId, diaperType, changedAt, note)
SleepLog (id, babyId, startedAt, endedAt, note)
```

---

## 4. 账号体系

### 4.1 架构概览

Nurtlina 的账号体系采用 **Firebase Auth + 自建 FastAPI 后端** 的混合架构：

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Android 客户端                                │
│                                                                     │
│  ┌─────────────────────┐    ┌──────────────────┐                   │
│  │ FirebaseAuthSource   │    │ AuthTokenInter-  │                   │
│  │ (Firebase Auth SDK)  │    │ ceptor (OkHttp)  │                   │
│  └─────────┬───────────┘    └────────┬─────────┘                   │
│            │                         │                              │
│  ┌─────────▼───────────┐    ┌───────▼──────────┐                   │
│  │ FirebaseAuthRepo    │    │ BackendApiService │                   │
│  │ (AuthRepository)    │    │ (Retrofit)       │                   │
│  └─────────┬───────────┘    └───────┬──────────┘                   │
│            │                        │                               │
│  ┌─────────▼───────────┐   ┌───────▼──────────┐                    │
│  │ FirestoreSource      │   │ DataStoreSession │                    │
│  │ (familyId, Pro flag) │   │ Repository       │                    │
│  └─────────────────────┘   └──────────────────┘                    │
└─────────────────────────────────────────────────────────────────────┘
              │                           │
              ▼                           ▼
┌─────────────────────┐    ┌──────────────────────────┐
│   Firebase Auth      │    │   FastAPI 后端             │
│   (身份提供商)        │    │   /api/v1/*                │
│                      │    │                           │
│   - 匿名登录          │    │   security.py:            │
│   - Google OAuth      │    │     verify_firebase_token │
│   - Microsoft OAuth   │    │                           │
│   - Email/Password    │    │   deps.py:                │
│                      │    │     get_current_user       │
│   Cloud Functions:   │    │     → Firebase UID         │
│   - provisionFamily   │    │     → 内部 User ID         │
└─────────────────────┘    └──────────┬───────────────┘
                                      │
                            ┌─────────▼───────────────┐
                            │   PostgreSQL              │
                            │   users / families /      │
                            │   entitlements            │
                            └───────────────────────────┘
```

**核心设计原则：**

1. **设备唯一标识**：每个安装实例分配 `android_<UUID>` 作为 `clientId`，与用户身份解耦
2. **静默匿名登录**：用户首次启动即自动创建 Firebase 匿名账户，确保同步/备份链路始终可用
3. **渐进式账号升级**：匿名 → 第三方登录（Google/Microsoft）或邮箱注册，通过 Firebase Auth `linkWithCredential` 保留数据关联
4. **服务端不信任客户端**：后端永远从 Firebase ID Token（`verify_id_token`）解析用户身份，不接受客户端传入的 userId
5. **离线优先**：Auth 链路失败不影响本地核心功能（记录喂奶/尿布/睡眠），同步在恢复网络后自动重试

---

### 4.2 身份认证流程

#### 4.2.1 冷启动认证链路

```
App 启动
  │
  ├─► AppStartupCoordinator.start()
  │     │
  │     ├─► AuthRepository.ensureSignedIn()
  │     │     │
  │     │     ├─ 已有 Firebase User? ──► 复用当前用户
  │     │     └─ 无用户? ──► signInAnonymously() 创建匿名用户
  │     │
  │     ├─► EntitlementManager.identify(firebaseUid)
  │     │     └─► RevenueCat 关联 Firebase UID
  │     │
  │     ├─► POST /api/v1/me/init
  │     │     └─► 后端: upsert User + 默认 Family → 返回 userId, familyId
  │     │
  │     └─► SessionRepository.saveBackendSession(userId, familyId)
  │
  └─► SyncManager.requestSyncSoon()
```

**关键文件：**
- `AppStartupCoordinator.kt` — 启动协调器
- `FirebaseAuthRepository.ensureSignedIn()` — 匿名登录 + 家庭预配置
- `user_service.init_me()` — 后端幂等用户/家庭创建

#### 4.2.2 Onboarding 中的认证

```
Onboarding (5步骤)
  │
  ├─► Welcome → Create Baby → Guideline → Disclaimer → Notification
  │
  └─► OnboardingViewModel.completeOnboarding()
        │
        ├─► ManageBabyUseCase.create(name, birthDate, avatarColor)
        ├─► SettingsRepository: onboardingCompleted = true
        │
        └─► 认证:
              ├─ 未登录? → signInAnonymously() + provisionFamily()
              └─ 已登录? → provisionFamily() 确保家庭文件存在
```

**关键文件：** `OnboardingViewModel.kt:54-89`

---

### 4.3 登录方式

#### 4.3.1 支持的认证提供商

| 提供商 | 实现方式 | Android 类 | 备注 |
|---|---|---|---|
| **匿名 (Anonymous)** | Firebase `signInAnonymously()` | `FirebaseAuthSource` | 默认，启动时自动创建 |
| **Google** | `GoogleSignInClient` + `CredentialManager` | `SignInScreen.kt:77-101` | 需配置 `firebase_web_client_id` |
| **Microsoft** | Firebase `OAuthProvider("microsoft.com")` | `SignInScreen.kt:104-111` | Chrome Custom Tab OAuth 流程，90s 超时 |
| **Email/Password** | Firebase `signInWithEmailAndPassword` / `createUserWithEmailAndPassword` | `SignInScreen.kt:204-222` | 支持注册 + 登录 + 密码找回 |

#### 4.3.2 匿名升级策略

所有第三方登录均优先尝试 **关联 (link)** 当前匿名账户，失败时回退到**独立登录**：

```kotlin
// FirebaseAuthRepository.kt:58-66 (Google 示例)
override suspend fun signInWithGoogle(idToken: String): Result<UserAccount> = runCatching {
    val account = if (authSource.isSignedIn()) {
        runCatching { authSource.linkAnonymousWithGoogle(idToken) }  // 尝试关联
            .getOrElse { authSource.signInWithGoogle(idToken) }      // 失败则独立登录
    } else {
        authSource.signInWithGoogle(idToken)
    }
    enrichWithFamilyAndEntitlement(account)
}
```

**登录成功后统一处理：**
```kotlin
// SignInViewModel.kt:152-162
.onSuccess { user ->
    if (user.familyId == null) authRepository.provisionFamily()  // 补充家庭配置
    syncRepository.requestFullSync()                              // 触发全量同步
    SyncWorker.enqueue(workManager)                               // 排队同步任务
    _navigationEvents.send(Unit)                                   // 返回上一页
}
```

#### 4.3.3 SignInScreen UI

- **Google 按钮**: OutlinedButton + Google Logo 图标
- **Microsoft 按钮**: 填充蓝色 Button (#0078D4) + Microsoft Logo 图标
- **Email/Password 表单**: OutlinedTextField（Email + Password），支持注册/登录模式切换
- **密码找回**: 登录模式下发送重置密码邮件

**关键文件：**
- `SignInScreen.kt` — 完整登录 UI
- `SignInViewModel.kt` — 登录逻辑 + 错误码映射

---

### 4.4 API 认证机制

#### 4.4.1 Token 传递 (Android 端)

```
每个 HTTP 请求
  │
  ├─► AuthTokenInterceptor (OkHttp Interceptor)
  │     │
  │     ├─► Authorization: Bearer <Firebase_ID_Token>
  │     │
  │     └─► 如果收到 401:
  │           ├─► getIdToken(forceRefresh = true)  强制刷新 Token
  │           └─► 重试请求 (仅一次)
```

**Token 生命周期：**
- Firebase ID Token 有效期 1 小时
- 自动刷新：`currentUser.getIdToken(forceRefresh)`
- 401 触发强制刷新 + 重试

**关键文件：** `AuthTokenInterceptor.kt`

#### 4.4.2 Token 验证 (后端)

```python
# security.py
async def verify_firebase_token(
    authorization: str = Header(...),
) -> dict:
    token = _extract_bearer(authorization)  # "Bearer <token>" → "<token>"
    decoded = firebase_admin.verify_id_token(token)  # Firebase Admin SDK
    return decoded  # {uid, email, ...}

# deps.py
async def get_current_user(
    decoded = Depends(verify_firebase_token),
    db = Depends(get_db),
) -> CurrentUser:
    firebase_uid = decoded["uid"]
    user = await repo.user_repository.get_by_firebase_uid(db, firebase_uid)
    if user is None:
        # 首次请求自动创建用户（幂等）
        user, _ = await get_or_create(db, firebase_uid, decoded.get("email"))
    return CurrentUser(
        user_id=user.id,        # 内部 ID
        firebase_uid=firebase_uid,
        email=decoded.get("email"),
        default_family_id=user.default_family_id,
    )
```

**安全原则：** 后端永远不信任客户端传入的 userId，始终从 Firebase ID Token 解析 `firebase_uid` → 映射到内部 `user.id`。

**关键文件：**
- `security.py` — Token 验证
- `deps.py` — 用户解析 + 自动预配置

---

### 4.5 数据模型

#### 4.5.1 后端 (PostgreSQL)

```
users                          families
┌─────────────────────────┐    ┌──────────────────────────┐
│ id (PK)                 │    │ id (PK)                  │
│ firebase_uid (UNIQUE)   │◄───│ owner_user_id (FK→users) │
│ email                   │    │ name                     │
│ display_name            │    │ created_at / updated_at  │
│ default_family_id (FK)  │───►│ deleted_at               │
│ created_at / updated_at │    └──────────────────────────┘
│ deleted_at (软删除)      │
└─────────────────────────┘    family_members
                               ┌──────────────────────────┐
entitlements                   │ id (PK)                  │
┌─────────────────────────┐    │ family_id (FK→families)  │
│ id (PK)                 │    │ user_id (FK→users)       │
│ user_id (FK→users)      │    │ role: OWNER/CAREGIVER/   │
│ source: GOOGLE_PLAY     │    │        VIEWER            │
│ product_id              │    │ status: ACTIVE/INVITED/  │
│ purchase_token_hash     │    │         REMOVED          │
│ status / plan           │    │ created_at / updated_at  │
│ expires_at              │    │ deleted_at               │
│ last_verified_at        │    └──────────────────────────┘
└─────────────────────────┘
```

| 实体 | 说明 |
|---|---|
| **User** | 每个 Firebase Auth 用户对应一个内部 User 记录。`firebase_uid` 唯一索引。支持软删除 (`deleted_at`) |
| **Family** | 数据隔离容器。每个用户自动拥有一个默认 Family。`owner_user_id` 指向创建者 |
| **FamilyMember** | 多对多关联。当前 MVP 仅 OWNER 角色；预留 CAREGIVER（照顾者）和 VIEWER（只读）用于未来多家长协作 |
| **Entitlement** | Pro 订阅/买断状态。`purchase_token_hash` 为 SHA-256 哈希值，不存储原始 token。支持 Google Play RTDN 回调验单 |

#### 4.5.2 Android (Room + DataStore)

```
Room (本地数据库)                    DataStore (键值存储)
┌────────────────────┐              ┌──────────────────────────┐
│ FeedLogEntity      │              │ backend_user_id           │
│ DiaperLogEntity    │              │ default_family_id         │
│ SleepLogEntity     │              │ client_id (android_<UUID>)│
│ BabyEntity         │              │ last_init_at              │
└────────────────────┘              └──────────────────────────┘

Domain Models:
  AuthUser / UserAccount  — Firebase 用户信息
  SessionInfo             — 后端会话信息
  BackendInitResult       — /me/init 响应
```

#### 4.5.3 Firestore (Cloud)

```
users/{userId}
  └─ familyId: string               ← Cloud Function provisionFamily 写入

families/{familyId}
  ├─ babies/{babyId}
  ├─ feedLogs/{feedLogId}
  ├─ diaperLogs/{diaperLogId}
  └─ sleepLogs/{sleepLogId}

entitlements/{userId}
  └─ isProActive: boolean           ← Cloud Function 根据 purchaseToken 设置
```

**Firestore 读取路径：**
- `FirestoreSource.fetchFamilyId(userId)` → 读取 `users/{userId}.familyId`
- `FirestoreSource.fetchEntitlement(userId)` → 读取 `entitlements/{userId}.isProActive`

---

### 4.6 会话管理

```
┌────────────────────────────────────────────────────────┐
│                  DataStore (持久化)                      │
│                                                        │
│  backend_user_id:  "usr_xxx"    ← POST /me/init 返回   │
│  default_family_id: "fam_yyy"   ← POST /me/init 返回   │
│  client_id:         "android_<random-UUID>"            │
│  last_init_at:      1703456789000                      │
│                                                        │
│  hasBackendSession = backendUserId != null             │
│                      && defaultFamilyId != null        │
└────────────────────────────────────────────────────────┘
```

| 字段 | 用途 |
|---|---|
| `backend_user_id` | 后端内部 User ID，同步/导出时标识数据归属 |
| `default_family_id` | 用户默认 Family ID，同步时确定 Firestore 写入路径 |
| `client_id` | 设备唯一标识（`android_` + UUID），用于后端去重/分析 |
| `last_init_at` | 上次成功调用 `/me/init` 的时间戳 |

**离线容错策略：**
```kotlin
// AppStartupCoordinator.kt:37-43
.onFailure { throwable ->
    if (authRepository.isSignedIn() && cachedSession?.hasBackendSession == true) {
        Log.i(TAG, "Starting with cached session; backend init will retry later.")
    } else {
        Log.w(TAG, "Startup auth/session init failed.", throwable)
    }
}
```
- 缓存会话有效 → 使用缓存，后台重试
- 无缓存且 Auth 失败 → 仅限本地使用，不阻塞核心功能

---

### 4.7 账号管理与注销

#### 4.7.1 设置页账号区域

```
Settings Screen → Account Section (AccountRow)
  │
  ├─ 未登录 (匿名用户):
  │    └─ "Sign In — Sync and restore your data on any device"
  │       → 点击进入 SignInScreen
  │
  └─ 已登录 (已关联第三方账号):
       ├─ 显示: email / displayName
       └─ "Sign Out" (红色) → authRepository.signOut()
            └─ 取消 SyncWorker，恢复为匿名状态
```

**关键文件：** `SettingsScreen.kt:282-317` (`AccountRow`)

#### 4.7.2 账号注销 (GDPR / Google Play Data Safety)

```
POST /api/v1/account/delete (需 Firebase Auth Token)
  │
  ├─► 软删除用户所有数据:
  │     ├─ 拥有 Family 的全部数据 (Baby/FeedLog/DiaperLog/SleepLog)
  │     ├─ 非拥有 Family 的 Membership (status → REMOVED)
  │     ├─ User.deleted_at = now, email/display_name → null
  │     ├─ SyncCursor 记录删除
  │     └─ Entitlement status → CANCELED
  │
  └─► Firebase Auth 清理 (best-effort):
        ├─ revoke_refresh_tokens(firebase_uid)
        └─ delete_user(firebase_uid)
```

**关键文件：**
- `deletion_service.py` — 后端注销逻辑
- `account.py` — `POST /account/delete` 路由

---

### 4.8 商业化身份关联

```
Firebase UID (稳定身份)
  │
  ├─► RevenueCat: EntitlementManager.identify(firebaseUid)
  │     └─ 跨设备恢复 Pro 订阅
  │
  ├─► Firestore: entitlements/{userId}.isProActive
  │     └─ Cloud Functions 根据 Google Play purchaseToken 设置
  │
  └─► 后端: Entitlement 表
        └─ purchase_token_hash (SHA-256), status, plan, expires_at
```

**Pro 状态检查链路：**
1. `FirebaseAuthRepository.enrichWithFamilyAndEntitlement()` → 从 Firestore 读取 `isProActive`
2. `EntitlementManager.proStatus` → RevenueCat SDK 本地缓存
3. `AppViewModel.isPro` → 驱动 UI (广告展示、功能解锁)

---

### 4.9 安全规则 (Firestore)

```
// firestore.rules 关键规则
match /users/{userId} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}

match /entitlements/{userId} {
  allow read: if request.auth != null && request.auth.uid == userId;
  allow write: if false;  // 仅 Cloud Functions 可写
}

match /families/{familyId} {
  allow read, write: if isFamilyMember(familyId);
  match /{subcollection}/{docId} {
    allow read, write: if isFamilyMember(familyId)
                       && isValidSyncRecord(request.resource.data)
                       && isNotStale(request.resource.data);
  }
}
```

**关键文件：** `firebase/firestore.rules`

---

### 4.10 完成度评估

| 组件 | 完成度 | 说明 |
|---|---|---|
| Firebase Auth 匿名登录 | ✅ 100% | 启动自动创建 + Onboarding 补充 |
| Google 登录 | ✅ 90% | UI + 后端完整；需配置 Web Client ID |
| Microsoft 登录 | ✅ 90% | OAuth 流程完整；需配置 Firebase Microsoft Provider |
| Email/Password 登录 | ✅ 90% | 注册/登录/密码重置完整；UI 完成 |
| Firebase ID Token 自动刷新 | ✅ 100% | AuthTokenInterceptor + 401 重试 |
| 后端 Token 验证 | ✅ 100% | Firebase Admin SDK + FastAPI Depends |
| `/me/init` 用户预配置 | ✅ 100% | 幂等 User + Family 创建 |
| Family 数据模型 | ✅ 85% | 预留 CAREGIVER/VIEWER 角色；MVP 仅 OWNER |
| Session 持久化 | ✅ 100% | DataStore 存储 + 离线容错 |
| 账号信息展示 | ✅ 90% | Settings AccountRow；匿名/已登录双态 |
| Sign Out | ✅ 100% | 取消 SyncWorker + Firebase signOut |
| Account Deletion | ✅ 90% | 后端完整；Android 端调用待接入 |
| RevenueCat 关联 | ✅ 100% | `identify(firebaseUid)` 在启动时执行 |
| Firestore 安全规则 | ✅ 90% | 规则完整；需随功能迭代更新 |
| Cloud Functions | ✅ 85% | `provisionFamily` 完整；其他函数部分就绪 |

---

## 5. 功能完成度矩阵

| 功能 | 状态 | 备注 |
|---|---|---|
| 喂奶记录 (CRUD) | ✅ 95% | NewFeedSheet + LogFeedUseCase 完整 |
| FeedingStatusCard | ✅ 90% | 上次喂奶+下次提醒 |
| 快捷奶量输入 | ✅ 85% | 预设按钮 + 手动输入 |
| 下次喂奶提醒 | ✅ 85% | NextFeedNotificationScheduler |
| 尿布记录 | ✅ 85% | 完整 |
| 睡眠记录 | ✅ 85% | 完整 + 活跃计时器 |
| Logs 时间线 | ✅ 80% | 筛选/删除有，编辑/日期选择器空回调 |
| 今日统计 | ✅ 85% | GetTodaySummaryUseCase |
| Onboarding | ✅ 90% | 5 步流程 |
| Pro Paywall | ✅ 80% | RevenueCat 完整 |
| 暗色模式 | ✅ 80% | Material 3 主题 |
| 评分弹窗 | ✅ 90% | RatingPromptEligibility（已更新为 Feed 指标） |
| 数据同步 | ✅ 80% | FastAPI push/pull |
| CSV 导出 | ⚠️ 70% | 后端可用，前端空回调 |
| 桌面小组件 | ❌ 0% | 未实现 |
| FAQ | ❌ 0% | 空回调 |
| Privacy/Terms | ❌ 0% | URL 页面未创建 |

---

## 6. 技术债务与风险

| 债务项 | 严重程度 | 状态 |
|---|---|---|
| ~~Bottle 遗留代码~~ | ~~P0~~ | ✅ **已清理**（Phase 0 完成） |
| ~~后端 Bottle 残留~~ | ~~P0~~ | ✅ **已清理**（migration 0003 + 7 个文件修复） |
| 硬编码英文字符串 | P1 | 待处理 |
| FeedLog/Summary 测试不足 | P1 | 待补齐 |
| 后端 CI/CD 未自动化 | P2 | GitHub Actions 待配置 |
| 前端空回调（Export/FAQ/Logs edit） | P1 | 待接通 |
| 小组件未实现 | P1 | P2 任务 |

---

## 7. 后续任务规划

### ✅ Phase 0：Bottle 清理 — 已完成

所有 15 项任务已执行，代码库中不再存在 Bottle 相关代码。详见 commit `0e42f39` + `ba00f03`。

### Phase 1：交互缺口补齐

| ID | 任务 | 估时 |
|---|---|---|
| FEAT-1 | Quick Feed 预设奶量按钮直接入口 | 3h |
| FEAT-2 | Logs 编辑日志详情 | 2h |
| FEAT-3 | Logs 日期选择器 | 1h |
| FEAT-4 | Onboarding 通知权限实现 | 1h |
| FEAT-5 | CSV 导出前端实现 | 3h |
| FEAT-6 | FAQ 页面内容 | 2h |

### Phase 2：体验提升

| ID | 任务 | 估时 |
|---|---|---|
| FEAT-7 | 夜间模式简化布局 | 4h |
| FEAT-8 | 桌面小组件（Glance API） | 6h |
| FEAT-9 | Insights Pro 数据源 | 3h |
| FEAT-10 | Insights oz 单位支持 | 0.5h |

### Phase 3：商业化 + 上架

| ID | 任务 | 估时 |
|---|---|---|
| FEAT-11 | AdMob 真实 ID | 0.5h |
| FEAT-12 | Privacy/Terms 页面 | 3h |
| FEAT-13 | App Icon + 截图 | 8h |
| FEAT-14 | Play Listing + Data Safety | 3h |
| FEAT-15 | Closed Testing | 2h |

---

## 8. 里程碑与时间线

| 里程碑 | 预计时间 | 交付物 |
|---|---|---|
| ~~M0: Bottle 清理~~ | ~~Week 1 Day 2~~ | ✅ 已完成 |
| M1: 核心体验可用 | Week 2 Day 5 | 交互缺口补齐 |
| M2: 商业化闭环 | Week 3 Day 5 | Free/Pro 完整 |
| M3: 小组件 | Week 4 Day 5 | 桌面小组件 |
| M4: 内测 | Week 5 Day 3 | Closed Testing |
| M5: 发布 | Week 6 Day 5 | 灰度上线 |

---

*文档版本：v5.0*
*更新日期：2026-07-13（新增 §4 账号体系）*
*生成日期：2026-07-10*
