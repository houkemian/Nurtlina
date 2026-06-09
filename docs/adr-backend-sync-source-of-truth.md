# ADR: Backend Sync Source Of Truth

Date: 2026-06-07

## English

## Status

Accepted.

## Decision

Nurtlina MVP uses **FastAPI/Postgres-first** for baby care backup/sync and entitlement-backed account data.

Firebase remains in scope for:

- Firebase Auth on Android.
- Crashlytics and Analytics, without baby notes or sensitive baby care records.
- Remote Config for non-safety UI, paywall, and rollout settings only.
- Optional Cloud Functions support jobs where they do not become the baby care record source of truth.

Firestore is not the product sync source of truth for Baby, Bottle, FeedLog, DiaperLog, or SleepLog records.

## Rationale

The repository already has a FastAPI/Postgres backend with:

- authenticated `/me/init` provisioning,
- family-scoped sync push/pull,
- server-side entitlement verification,
- account deletion,
- export generation,
- Alembic migrations.

Keeping Firestore direct sync in parallel creates data-splitting and conflict-policy risk. The Android `SyncRepository` binding now points to `ApiSyncRepository`, and WorkManager sync delegates to that same path.

## Consequences

- Android local Room remains the source of truth for active timers.
- FastAPI/Postgres is the cloud backup/sync target.
- Backend failures must not block bottle timer creation, state changes, or notifications.
- Remote Config must not silently change safety timer rules.
- Firestore business-sync code should be removed or kept only as clearly deprecated reference code.

## Follow-Up

- Keep `docs/project-backend-status-audit.md` aligned with this ADR.
- Add integration tests around FastAPI sync, export, entitlement, and deletion behavior before broad launch.


## 中文

## 状态

已接受。

## 决策

Nurtlina MVP 对 baby care 备份/同步和基于权益的账号数据采用 **FastAPI/Postgres-first**。

Firebase 仍保留以下职责：

- Android 端 Firebase Auth。
- Crashlytics 和 Analytics，但不得包含 baby notes 或敏感 baby care 记录。
- Remote Config 仅用于非安全相关 UI、paywall 和 rollout 设置。
- 可选 Cloud Functions 后台辅助任务，前提是这些任务不成为 baby care 记录的可信源。

Firestore 不是 Baby、Bottle、FeedLog、DiaperLog 或 SleepLog 记录的产品同步可信源。

## 理由

当前仓库已经具备 FastAPI/Postgres 后端，包括：

- 已认证的 `/me/init` 初始化；
- 按 family 范围隔离的 sync push/pull；
- 服务端 entitlement 校验；
- 账号删除；
- 导出生成；
- Alembic migrations。

如果同时保留 Firestore 直连同步，会带来数据分裂和冲突策略风险。Android `SyncRepository` 绑定现在指向 `ApiSyncRepository`，WorkManager 同步也委托到同一条路径。

## 影响

- Android 本地 Room 仍是 active timers 的 source of truth。
- FastAPI/Postgres 是云端备份/同步目标。
- 后端失败不得阻塞 bottle timer 创建、状态变更或通知。
- Remote Config 不得静默改变安全相关 timer rules。
- Firestore 业务同步代码应删除，或仅作为明确 deprecated 的参考代码保留。

## 后续事项

- 保持 `docs/project-backend-status-audit.md` 与本 ADR 一致。
- 在大范围发布前补充 FastAPI sync、export、entitlement 和 deletion 行为的集成测试。
