"""Low-level data access for sync operations (upsert with conflict guard)."""

import datetime
from typing import TypeVar

from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.baby import Baby
from app.models.bottle import Bottle
from app.models.diaper_log import DiaperLog
from app.models.feed_log import FeedLog
from app.models.sleep_log import SleepLog
from app.models.sync import SyncCursor

# Bottle status priority for conflict resolution (higher = wins)
BOTTLE_STATUS_PRIORITY: dict[str, int] = {
    "CANCELED": 6,
    "DISCARDED": 6,
    "FED": 6,
    "EXPIRED": 4,
    "FEEDING_STARTED": 3,
    "REFRIGERATED": 2,
    "NOT_STARTED": 1,
}

T = TypeVar("T", Baby, Bottle, FeedLog, DiaperLog, SleepLog)


async def upsert_baby(
    db: AsyncSession, data: dict, family_id: str
) -> tuple[str, str]:
    """Returns (record_id, outcome) where outcome is 'accepted'|'rejected'|'conflict'."""
    existing = await db.get(Baby, data["id"])
    if existing is None:
        db.add(Baby(**data, family_id=family_id))
        await db.flush()
        return data["id"], "accepted"
    if data["updated_at"] >= existing.updated_at:
        for k, v in data.items():
            setattr(existing, k, v)
        await db.flush()
        return data["id"], "accepted"
    return data["id"], "rejected"


async def upsert_bottle(
    db: AsyncSession, data: dict, family_id: str
) -> tuple[str, str]:
    existing = await db.get(Bottle, data["id"])
    if existing is None:
        db.add(Bottle(**data, family_id=family_id))
        await db.flush()
        return data["id"], "accepted"

    incoming_ts: datetime.datetime = data["updated_at"]
    if incoming_ts > existing.updated_at:
        for k, v in data.items():
            setattr(existing, k, v)
        await db.flush()
        return data["id"], "accepted"

    if incoming_ts == existing.updated_at:
        # Same timestamp: apply bottle status priority rule
        incoming_priority = BOTTLE_STATUS_PRIORITY.get(data.get("status", ""), 0)
        existing_priority = BOTTLE_STATUS_PRIORITY.get(existing.status or "", 0)
        if incoming_priority > existing_priority:
            for k, v in data.items():
                setattr(existing, k, v)
            await db.flush()
            return data["id"], "conflict"

    return data["id"], "rejected"


async def upsert_feed_log(
    db: AsyncSession, data: dict, family_id: str
) -> tuple[str, str]:
    existing = await db.get(FeedLog, data["id"])
    if existing is None:
        db.add(FeedLog(**data, family_id=family_id))
        await db.flush()
        return data["id"], "accepted"
    if data["updated_at"] >= existing.updated_at:
        for k, v in data.items():
            setattr(existing, k, v)
        await db.flush()
        return data["id"], "accepted"
    return data["id"], "rejected"


async def upsert_diaper_log(
    db: AsyncSession, data: dict, family_id: str
) -> tuple[str, str]:
    existing = await db.get(DiaperLog, data["id"])
    if existing is None:
        db.add(DiaperLog(**data, family_id=family_id))
        await db.flush()
        return data["id"], "accepted"
    if data["updated_at"] >= existing.updated_at:
        for k, v in data.items():
            setattr(existing, k, v)
        await db.flush()
        return data["id"], "accepted"
    return data["id"], "rejected"


async def upsert_sleep_log(
    db: AsyncSession, data: dict, family_id: str
) -> tuple[str, str]:
    existing = await db.get(SleepLog, data["id"])
    if existing is None:
        db.add(SleepLog(**data, family_id=family_id))
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
    db: AsyncSession, family_id: str, since: datetime.datetime, limit: int = 500
) -> list[Baby]:
    result = await db.execute(
        select(Baby)
        .where(Baby.family_id == family_id, Baby.updated_at > since)
        .order_by(Baby.updated_at)
        .limit(limit)
    )
    return list(result.scalars().all())


async def pull_bottles(
    db: AsyncSession, family_id: str, since: datetime.datetime, limit: int = 500
) -> list[Bottle]:
    result = await db.execute(
        select(Bottle)
        .where(Bottle.family_id == family_id, Bottle.updated_at > since)
        .order_by(Bottle.updated_at)
        .limit(limit)
    )
    return list(result.scalars().all())


async def pull_feed_logs(
    db: AsyncSession, family_id: str, since: datetime.datetime, limit: int = 500
) -> list[FeedLog]:
    result = await db.execute(
        select(FeedLog)
        .where(FeedLog.family_id == family_id, FeedLog.updated_at > since)
        .order_by(FeedLog.updated_at)
        .limit(limit)
    )
    return list(result.scalars().all())


async def pull_diaper_logs(
    db: AsyncSession, family_id: str, since: datetime.datetime, limit: int = 500
) -> list[DiaperLog]:
    result = await db.execute(
        select(DiaperLog)
        .where(DiaperLog.family_id == family_id, DiaperLog.updated_at > since)
        .order_by(DiaperLog.updated_at)
        .limit(limit)
    )
    return list(result.scalars().all())


async def pull_sleep_logs(
    db: AsyncSession, family_id: str, since: datetime.datetime, limit: int = 500
) -> list[SleepLog]:
    result = await db.execute(
        select(SleepLog)
        .where(SleepLog.family_id == family_id, SleepLog.updated_at > since)
        .order_by(SleepLog.updated_at)
        .limit(limit)
    )
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
