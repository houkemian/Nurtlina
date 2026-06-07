"""Account and data deletion service (GDPR / Google Play Data Safety)."""

from sqlalchemy import delete, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.clock import utcnow
from app.models.baby import Baby
from app.models.bottle import Bottle
from app.models.diaper_log import DiaperLog
from app.models.family import Family, FamilyMember
from app.models.feed_log import FeedLog
from app.models.sleep_log import SleepLog
from app.models.sync import SyncCursor
from app.models.user import User


async def delete_account(db: AsyncSession, user_id: str) -> None:
    """
    Soft-delete all data owned by the user.
    If the user is the family owner, soft-deletes all family records.
    If a non-owner member, removes only their membership.
    Firebase Auth revocation is the caller's responsibility.
    """
    now = utcnow()

    user = await db.get(User, user_id)
    if not user:
        return

    # Find user's default family
    family_id = user.default_family_id

    if family_id:
        family = await db.get(Family, family_id)
        if family and family.owner_user_id == user_id:
            # Soft-delete entire family data
            for model in (Baby, Bottle, FeedLog, DiaperLog, SleepLog):
                await db.execute(
                    update(model)  # type: ignore[arg-type]
                    .where(model.family_id == family_id, model.deleted_at.is_(None))  # type: ignore[attr-defined]
                    .values(deleted_at=now, updated_at=now)
                )
            await db.execute(
                update(FamilyMember)
                .where(FamilyMember.family_id == family_id, FamilyMember.deleted_at.is_(None))
                .values(deleted_at=now, updated_at=now, status="REMOVED")
            )
            family.deleted_at = now
            family.updated_at = now
        else:
            # Non-owner: only remove from family
            await db.execute(
                update(FamilyMember)
                .where(
                    FamilyMember.family_id == family_id,
                    FamilyMember.user_id == user_id,
                )
                .values(deleted_at=now, updated_at=now, status="REMOVED")
            )

    # Soft-delete user record
    user.deleted_at = now
    user.updated_at = now
    # Anonymise PII fields
    user.email = None
    user.display_name = None

    # Delete sync cursors (not sensitive, clean up)
    await db.execute(delete(SyncCursor).where(SyncCursor.user_id == user_id))

    await db.flush()
