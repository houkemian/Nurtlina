"""Account and data deletion service (GDPR / Google Play Data Safety)."""

import logging

from sqlalchemy import delete, select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.clock import utcnow
from app.integrations.firebase_admin import delete_user, revoke_refresh_tokens
from app.models.baby import Baby
from app.models.bottle import Bottle
from app.models.diaper_log import DiaperLog
from app.models.entitlement import Entitlement
from app.models.family import Family, FamilyMember
from app.models.feed_log import FeedLog
from app.models.sleep_log import SleepLog
from app.models.sync import SyncCursor
from app.models.user import User

logger = logging.getLogger("app")


async def delete_account(db: AsyncSession, user_id: str) -> None:
    """Soft-delete data owned by the user and revoke Firebase credentials.

    The local database cleanup is authoritative for this backend. Firebase Auth
    cleanup is best-effort so a provider outage does not leave personal records
    active in Postgres.
    """
    now = utcnow()

    user = await db.get(User, user_id)
    if not user:
        return

    owned_family_ids = await _owned_family_ids(db, user_id)
    member_family_ids = await _member_family_ids(db, user_id)

    for family_id in owned_family_ids:
        await _soft_delete_family(db, family_id, now)

    for family_id in set(member_family_ids) - set(owned_family_ids):
        await db.execute(
            update(FamilyMember)
            .where(FamilyMember.family_id == family_id, FamilyMember.user_id == user_id)
            .values(deleted_at=now, updated_at=now, status="REMOVED")
        )

    user.deleted_at = now
    user.updated_at = now
    user.email = None
    user.display_name = None
    user.default_family_id = None

    await db.execute(delete(SyncCursor).where(SyncCursor.user_id == user_id))
    await db.execute(
        update(Entitlement)
        .where(Entitlement.user_id == user_id)
        .values(status="CANCELED", updated_at=now)
    )

    _delete_firebase_user_best_effort(user.firebase_uid)
    await db.flush()


async def _owned_family_ids(db: AsyncSession, user_id: str) -> list[str]:
    result = await db.execute(select(Family.id).where(Family.owner_user_id == user_id))
    return list(result.scalars().all())


async def _member_family_ids(db: AsyncSession, user_id: str) -> list[str]:
    result = await db.execute(select(FamilyMember.family_id).where(FamilyMember.user_id == user_id))
    return list(result.scalars().all())


async def _soft_delete_family(db: AsyncSession, family_id: str, now) -> None:
    for model in (Baby, Bottle, FeedLog, DiaperLog, SleepLog):
        await db.execute(
            update(model)
            .where(model.family_id == family_id, model.deleted_at.is_(None))
            .values(deleted_at=now, updated_at=now)
        )
    await db.execute(
        update(FamilyMember)
        .where(FamilyMember.family_id == family_id, FamilyMember.deleted_at.is_(None))
        .values(deleted_at=now, updated_at=now, status="REMOVED")
    )
    await db.execute(
        update(Family)
        .where(Family.id == family_id, Family.deleted_at.is_(None))
        .values(deleted_at=now, updated_at=now)
    )


def _delete_firebase_user_best_effort(firebase_uid: str) -> None:
    try:
        revoke_refresh_tokens(firebase_uid)
        delete_user(firebase_uid)
    except Exception:
        logger.exception("Firebase Auth deletion failed", extra={"firebase_uid": firebase_uid})
