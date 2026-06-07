from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.entitlement import Entitlement


async def get_active_by_user(db: AsyncSession, user_id: str) -> Entitlement | None:
    """Return the most recently updated active entitlement for the user."""
    result = await db.execute(
        select(Entitlement)
        .where(Entitlement.user_id == user_id, Entitlement.status == "ACTIVE")
        .order_by(Entitlement.updated_at.desc())
        .limit(1)
    )
    return result.scalar_one_or_none()


async def get_latest_by_user(db: AsyncSession, user_id: str) -> Entitlement | None:
    result = await db.execute(
        select(Entitlement)
        .where(Entitlement.user_id == user_id)
        .order_by(Entitlement.updated_at.desc())
        .limit(1)
    )
    return result.scalar_one_or_none()


async def get_by_purchase_token_hash(db: AsyncSession, token_hash: str) -> Entitlement | None:
    result = await db.execute(
        select(Entitlement).where(Entitlement.purchase_token_hash == token_hash).limit(1)
    )
    return result.scalar_one_or_none()


async def get_by_id(db: AsyncSession, entitlement_id: str) -> Entitlement | None:
    return await db.get(Entitlement, entitlement_id)


async def upsert(db: AsyncSession, entitlement: Entitlement) -> Entitlement:
    db.add(entitlement)
    await db.flush()
    return entitlement
