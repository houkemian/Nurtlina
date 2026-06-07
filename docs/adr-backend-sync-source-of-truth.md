# ADR: Backend Sync Source Of Truth

Date: 2026-06-07

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
