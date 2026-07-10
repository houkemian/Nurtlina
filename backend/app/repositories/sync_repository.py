"""Low-level data access for sync operations (upsert with conflict guard)."""

import datetime

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.baby import Baby
from app.models.diaper_log import DiaperLog
from app.models.feed_log import FeedLog
from app.models.sleep_log import SleepLog
from app.models.sync import SyncCursor


def _payload_family_matches(data: dict, family_id: str) -> bool:
    return data.get("family_id") == family_id


def _belongs_to_family(record: object | None, family_id: str) -> bool:
    return record is not None and getattr(record, "family_id", None) == family_id


def _existing_can_be_updated(existing: object | None, family_id: str) -> bool:
    return existing is None or _belongs_to_family(existing, family_id)


async def _baby_belongs_to_family(db: AsyncSession, baby_id: str, family_id: str) -> bool:
    baby = await db.get(Baby, baby_id)
    return _belongs_to_family(baby, family_id)


async def _child_record_is_owned(
    db: AsyncSession,
    data: dict,
    family_id: str,
    existing: object | None,
) -> bool:
    if not _payload_family_matches(data, family_id):
        return False
    if not _existing_can_be_updated(existing, family_id):
        return False
    if not await _baby_belongs_to_family(db, data["baby_id"], family_id):
        return False
    return True


async def upsert_baby(db: AsyncSession, data: dict, family_id: str) -> tuple[str, str]:
    """Returns (record_id, outcome) where outcome is 'accepted'|'rejected'|'conflict'."""
    if not _payload_family_matches(data, family_id):
        return data["id"], "rejected"

    existing = await db.get(Baby, data["id"])
    if not _existing_can_be_updated(existing, family_id):
        return data["id"], "rejected"

    if existing is None:
        db.add(Baby(**data))
        await db.flush()
        return data["id"], "accepted"
    if data["updated_at"] >= existing.updated_at:
        for k, v in data.items():
            setattr(existing, k, v)
        await db.flush()
        return data["id"], "accepted"
    return data["id"], "rejected"


async def upsert_feed_log(db: AsyncSession, data: dict, family_id: str) -> tuple[str, str]:
    existing = await db.get(FeedLog, data["id"])
    if not await _child_record_is_owned(db, data, family_id, existing):
        return data["id"], "rejected"

    if existing is None:
        db.add(FeedLog(**data))
        await db.flush()
        return data["id"], "accepted"
    if data["updated_at"] >= existing.updated_at:
        for k, v in data.items():
            setattr(existing, k, v)
        await db.flush()
        return data["id"], "accepted"
    return data["id"], "rejected"


async def upsert_diaper_log(db: AsyncSession, data: dict, family_id: str) -> tuple[str, str]:
    existing = await db.get(DiaperLog, data["id"])
    if not await _child_record_is_owned(db, data, family_id, existing):
        return data["id"], "rejected"

    if existing is None:
        db.add(DiaperLog(**data))
        await db.flush()
        return data["id"], "accepted"
    if data["updated_at"] >= existing.updated_at:
        for k, v in data.items():
            setattr(existing, k, v)
        await db.flush()
        return data["id"], "accepted"
    return data["id"], "rejected"


async def upsert_sleep_log(db: AsyncSession, data: dict, family_id: str) -> tuple[str, str]:
    existing = await db.get(SleepLog, data["id"])
    if not await _child_record_is_owned(db, data, family_id, existing):
        return data["id"], "rejected"

    if existing is None:
        db.add(SleepLog(**data))
        await db.flush()
        return data["id"], "accepted"
    if data["updated_at"] >= existing.updated_at:
        for k, v in data.items():
            setattr(existing, k, v)
        await db.flush()
        return data["id"], "accepted"
    return data["id"], "rejected"


# ─── Pull helpers ─────────────────────────────────────────────────────────────


async def pull_babies(
    db: AsyncSession, family_id: str, since: datetime.datetime, limit: int | None = 500
) -> list[Baby]:
    query = (
        select(Baby)
        .where(Baby.family_id == family_id, Baby.updated_at > since)
        .order_by(Baby.updated_at, Baby.id)
    )
    if limit is not None:
        query = query.limit(limit)
    result = await db.execute(query)
    return list(result.scalars().all())


async def pull_feed_logs(
    db: AsyncSession, family_id: str, since: datetime.datetime, limit: int | None = 500
) -> list[FeedLog]:
    query = (
        select(FeedLog)
        .where(FeedLog.family_id == family_id, FeedLog.updated_at > since)
        .order_by(FeedLog.updated_at, FeedLog.id)
    )
    if limit is not None:
        query = query.limit(limit)
    result = await db.execute(query)
    return list(result.scalars().all())


async def pull_diaper_logs(
    db: AsyncSession, family_id: str, since: datetime.datetime, limit: int | None = 500
) -> list[DiaperLog]:
    query = (
        select(DiaperLog)
        .where(DiaperLog.family_id == family_id, DiaperLog.updated_at > since)
        .order_by(DiaperLog.updated_at, DiaperLog.id)
    )
    if limit is not None:
        query = query.limit(limit)
    result = await db.execute(query)
    return list(result.scalars().all())


async def pull_sleep_logs(
    db: AsyncSession, family_id: str, since: datetime.datetime, limit: int | None = 500
) -> list[SleepLog]:
    query = (
        select(SleepLog)
        .where(SleepLog.family_id == family_id, SleepLog.updated_at > since)
        .order_by(SleepLog.updated_at, SleepLog.id)
    )
    if limit is not None:
        query = query.limit(limit)
    result = await db.execute(query)
    return list(result.scalars().all())


# ─── Cursor helpers ───────────────────────────────────────────────────────────


async def get_or_create_cursor(
    db: AsyncSession,
    user_id: str,
    family_id: str,
    client_id: str,
    now: datetime.datetime,
) -> SyncCursor:
    result = await db.execute(
        select(SyncCursor).where(
            SyncCursor.user_id == user_id,
            SyncCursor.family_id == family_id,
            SyncCursor.client_id == client_id,
        )
    )
    cursor = result.scalar_one_or_none()
    if cursor is None:
        from app.core.ids import new_id

        cursor = SyncCursor(
            id=new_id(),
            user_id=user_id,
            family_id=family_id,
            client_id=client_id,
            created_at=now,
            updated_at=now,
        )
        db.add(cursor)
        await db.flush()
    return cursor
