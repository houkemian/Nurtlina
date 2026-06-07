"""Low-level data access for sync operations (upsert with conflict guard)."""

import datetime

from sqlalchemy import select
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

def _payload_family_matches(data: dict, family_id: str) -> bool:
    return data.get("family_id") == family_id


def _belongs_to_family(record: object | None, family_id: str) -> bool:
    return record is not None and getattr(record, "family_id", None) == family_id


def _existing_can_be_updated(existing: object | None, family_id: str) -> bool:
    return existing is None or _belongs_to_family(existing, family_id)


async def _baby_belongs_to_family(db: AsyncSession, baby_id: str, family_id: str) -> bool:
    baby = await db.get(Baby, baby_id)
    return _belongs_to_family(baby, family_id)


async def _bottle_belongs_to_family(db: AsyncSession, bottle_id: str, family_id: str) -> bool:
    bottle = await db.get(Bottle, bottle_id)
    return _belongs_to_family(bottle, family_id)


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
    bottle_id = data.get("bottle_id")
    if bottle_id is None:
        return True
    return await _bottle_belongs_to_family(db, bottle_id, family_id)


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


async def upsert_bottle(db: AsyncSession, data: dict, family_id: str) -> tuple[str, str]:
    existing = await db.get(Bottle, data["id"])
    if not await _child_record_is_owned(db, data, family_id, existing):
        return data["id"], "rejected"

    if existing is None:
        db.add(Bottle(**data))
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
