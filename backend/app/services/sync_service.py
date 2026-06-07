"""Sync push/pull service with conflict resolution.

Push (local → server):
  - Accept change if incoming.updated_at >= existing.updated_at
  - For Bottle: apply status priority rules when timestamps tie
  - Return accepted / rejected / conflicts per record ID

Pull (server → local):
  - Return all records updated after `since` cursor, per entity type
  - Limit each entity to 500 records; return hasMore + nextCursor
"""

import datetime

from sqlalchemy.ext.asyncio import AsyncSession

from app import repositories as repo
from app.core.clock import ensure_utc, utcnow
from app.repositories.sync_repository import (
    get_or_create_cursor,
    pull_babies,
    pull_bottles,
    pull_diaper_logs,
    pull_feed_logs,
    pull_sleep_logs,
    upsert_baby,
    upsert_bottle,
    upsert_diaper_log,
    upsert_feed_log,
    upsert_sleep_log,
)
from app.schemas.sync import (
    BabyChange,
    BabySyncRequest,
    BottleChange,
    BottleSyncRequest,
    DiaperLogChange,
    DiaperLogSyncRequest,
    FeedLogChange,
    FeedLogSyncRequest,
    SleepLogChange,
    SleepLogSyncRequest,
    SyncPullResponse,
    SyncPushResponse,
)

_PAGE_LIMIT = 500


async def push_babies(db: AsyncSession, req: BabySyncRequest, user_id: str) -> SyncPushResponse:
    await _assert_family_access(db, req.family_id, user_id)
    now = utcnow()
    accepted, rejected, conflicts = [], [], []
    for change in req.changes:
        row = change.model_dump()
        _normalise_timestamps(row)
        record_id, outcome = await upsert_baby(db, row, req.family_id)
        _bucket(record_id, outcome, accepted, rejected, conflicts)
    await _update_cursor_push(db, user_id, req.family_id, req.client_id, now)
    return SyncPushResponse(
        server_time=now, accepted=accepted, rejected=rejected, conflicts=conflicts
    )


async def push_bottles(db: AsyncSession, req: BottleSyncRequest, user_id: str) -> SyncPushResponse:
    await _assert_family_access(db, req.family_id, user_id)
    now = utcnow()
    accepted, rejected, conflicts = [], [], []
    for change in req.changes:
        row = change.model_dump()
        _normalise_timestamps(row)
        record_id, outcome = await upsert_bottle(db, row, req.family_id)
        _bucket(record_id, outcome, accepted, rejected, conflicts)
    await _update_cursor_push(db, user_id, req.family_id, req.client_id, now)
    return SyncPushResponse(
        server_time=now, accepted=accepted, rejected=rejected, conflicts=conflicts
    )


async def push_feed_logs(
    db: AsyncSession, req: FeedLogSyncRequest, user_id: str
) -> SyncPushResponse:
    await _assert_family_access(db, req.family_id, user_id)
    now = utcnow()
    accepted, rejected, conflicts = [], [], []
    for change in req.changes:
        row = change.model_dump()
        _normalise_timestamps(row)
        record_id, outcome = await upsert_feed_log(db, row, req.family_id)
        _bucket(record_id, outcome, accepted, rejected, conflicts)
    await _update_cursor_push(db, user_id, req.family_id, req.client_id, now)
    return SyncPushResponse(
        server_time=now, accepted=accepted, rejected=rejected, conflicts=conflicts
    )


async def push_diaper_logs(
    db: AsyncSession, req: DiaperLogSyncRequest, user_id: str
) -> SyncPushResponse:
    await _assert_family_access(db, req.family_id, user_id)
    now = utcnow()
    accepted, rejected, conflicts = [], [], []
    for change in req.changes:
        row = change.model_dump()
        _normalise_timestamps(row)
        record_id, outcome = await upsert_diaper_log(db, row, req.family_id)
        _bucket(record_id, outcome, accepted, rejected, conflicts)
    await _update_cursor_push(db, user_id, req.family_id, req.client_id, now)
    return SyncPushResponse(
        server_time=now, accepted=accepted, rejected=rejected, conflicts=conflicts
    )


async def push_sleep_logs(
    db: AsyncSession, req: SleepLogSyncRequest, user_id: str
) -> SyncPushResponse:
    await _assert_family_access(db, req.family_id, user_id)
    now = utcnow()
    accepted, rejected, conflicts = [], [], []
    for change in req.changes:
        row = change.model_dump()
        _normalise_timestamps(row)
        record_id, outcome = await upsert_sleep_log(db, row, req.family_id)
        _bucket(record_id, outcome, accepted, rejected, conflicts)
    await _update_cursor_push(db, user_id, req.family_id, req.client_id, now)
    return SyncPushResponse(
        server_time=now, accepted=accepted, rejected=rejected, conflicts=conflicts
    )


async def pull_changes(
    db: AsyncSession,
    family_id: str,
    user_id: str,
    client_id: str,
    since: datetime.datetime,
) -> SyncPullResponse:
    await _assert_family_access(db, family_id, user_id)
    now = utcnow()
    since_utc = ensure_utc(since)

    babies = await pull_babies(db, family_id, since_utc, _PAGE_LIMIT + 1)
    bottles = await pull_bottles(db, family_id, since_utc, _PAGE_LIMIT + 1)
    feed_logs = await pull_feed_logs(db, family_id, since_utc, _PAGE_LIMIT + 1)
    diaper_logs = await pull_diaper_logs(db, family_id, since_utc, _PAGE_LIMIT + 1)
    sleep_logs = await pull_sleep_logs(db, family_id, since_utc, _PAGE_LIMIT + 1)

    has_more = (
        len(babies) > _PAGE_LIMIT
        or len(bottles) > _PAGE_LIMIT
        or len(feed_logs) > _PAGE_LIMIT
        or len(diaper_logs) > _PAGE_LIMIT
        or len(sleep_logs) > _PAGE_LIMIT
    )
    babies = babies[:_PAGE_LIMIT]
    bottles = bottles[:_PAGE_LIMIT]
    feed_logs = feed_logs[:_PAGE_LIMIT]
    diaper_logs = diaper_logs[:_PAGE_LIMIT]
    sleep_logs = sleep_logs[:_PAGE_LIMIT]

    next_cursor: datetime.datetime | None = None
    if has_more:
        all_updated = (
            [b.updated_at for b in babies]
            + [b.updated_at for b in bottles]
            + [f.updated_at for f in feed_logs]
            + [d.updated_at for d in diaper_logs]
            + [s.updated_at for s in sleep_logs]
        )
        next_cursor = max(all_updated) if all_updated else None

    await _update_cursor_pull(db, user_id, family_id, client_id, now)

    return SyncPullResponse(
        server_time=now,
        next_cursor=next_cursor,
        has_more=has_more,
        babies=[_baby_to_change(b) for b in babies],
        bottles=[_bottle_to_change(b) for b in bottles],
        feed_logs=[_feed_log_to_change(f) for f in feed_logs],
        diaper_logs=[_diaper_log_to_change(d) for d in diaper_logs],
        sleep_logs=[_sleep_log_to_change(s) for s in sleep_logs],
    )


# ─── Helpers ──────────────────────────────────────────────────────────────────


async def _assert_family_access(db: AsyncSession, family_id: str, user_id: str) -> None:
    from app.core.errors import ForbiddenError

    member = await repo.family_repository.get_member(db, family_id, user_id)
    if not member:
        raise ForbiddenError("Not a member of this family.")


def _bucket(
    record_id: str,
    outcome: str,
    accepted: list[str],
    rejected: list[str],
    conflicts: list[str],
) -> None:
    if outcome == "accepted":
        accepted.append(record_id)
    elif outcome == "conflict":
        conflicts.append(record_id)
    else:
        rejected.append(record_id)


def _normalise_timestamps(row: dict) -> None:
    """Ensure all datetime values are UTC-aware before writing to DB."""
    for k, v in row.items():
        if isinstance(v, datetime.datetime):
            row[k] = ensure_utc(v)


async def _update_cursor_push(
    db: AsyncSession,
    user_id: str,
    family_id: str,
    client_id: str,
    now: datetime.datetime,
) -> None:
    cursor = await get_or_create_cursor(db, user_id, family_id, client_id, now)
    cursor.last_push_at = now
    cursor.updated_at = now


async def _update_cursor_pull(
    db: AsyncSession,
    user_id: str,
    family_id: str,
    client_id: str,
    now: datetime.datetime,
) -> None:
    cursor = await get_or_create_cursor(db, user_id, family_id, client_id, now)
    cursor.last_pull_at = now
    cursor.updated_at = now


# ─── ORM → Schema converters ─────────────────────────────────────────────────


def _baby_to_change(b: object) -> BabyChange:
    from app.models.baby import Baby as BabyModel

    assert isinstance(b, BabyModel)
    return BabyChange(
        id=b.id,
        family_id=b.family_id,
        name=b.name,
        birth_date=b.birth_date,
        avatar_color=b.avatar_color,
        client_id=b.client_id,
        schema_version=b.schema_version,
        created_at=b.created_at,
        updated_at=b.updated_at,
        deleted_at=b.deleted_at,
    )


def _bottle_to_change(b: object) -> BottleChange:
    from app.models.bottle import Bottle as BottleModel

    assert isinstance(b, BottleModel)
    return BottleChange(
        id=b.id,
        family_id=b.family_id,
        baby_id=b.baby_id,
        milk_type=b.milk_type,
        amount_ml=b.amount_ml,
        prepared_at=b.prepared_at,
        feeding_started_at=b.feeding_started_at,
        refrigerated_at=b.refrigerated_at,
        status=b.status,
        guideline_region=b.guideline_region,
        rule_version=b.rule_version,
        expires_at=b.expires_at,
        discarded_at=b.discarded_at,
        fed_at=b.fed_at,
        note=b.note,
        client_id=b.client_id,
        schema_version=b.schema_version,
        created_at=b.created_at,
        updated_at=b.updated_at,
        deleted_at=b.deleted_at,
    )


def _feed_log_to_change(f: object) -> FeedLogChange:
    from app.models.feed_log import FeedLog as FeedLogModel

    assert isinstance(f, FeedLogModel)
    return FeedLogChange(
        id=f.id,
        family_id=f.family_id,
        baby_id=f.baby_id,
        bottle_id=f.bottle_id,
        feed_type=f.feed_type,
        amount_ml=f.amount_ml,
        started_at=f.started_at,
        ended_at=f.ended_at,
        note=f.note,
        client_id=f.client_id,
        schema_version=f.schema_version,
        created_at=f.created_at,
        updated_at=f.updated_at,
        deleted_at=f.deleted_at,
    )


def _diaper_log_to_change(d: object) -> DiaperLogChange:
    from app.models.diaper_log import DiaperLog as DiaperLogModel

    assert isinstance(d, DiaperLogModel)
    return DiaperLogChange(
        id=d.id,
        family_id=d.family_id,
        baby_id=d.baby_id,
        diaper_type=d.diaper_type,
        changed_at=d.changed_at,
        note=d.note,
        client_id=d.client_id,
        schema_version=d.schema_version,
        created_at=d.created_at,
        updated_at=d.updated_at,
        deleted_at=d.deleted_at,
    )


def _sleep_log_to_change(s: object) -> SleepLogChange:
    from app.models.sleep_log import SleepLog as SleepLogModel

    assert isinstance(s, SleepLogModel)
    return SleepLogChange(
        id=s.id,
        family_id=s.family_id,
        baby_id=s.baby_id,
        started_at=s.started_at,
        ended_at=s.ended_at,
        note=s.note,
        client_id=s.client_id,
        schema_version=s.schema_version,
        created_at=s.created_at,
        updated_at=s.updated_at,
        deleted_at=s.deleted_at,
    )
