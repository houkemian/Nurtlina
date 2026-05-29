from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user import User


async def get_by_firebase_uid(db: AsyncSession, firebase_uid: str) -> User | None:
    result = await db.execute(
        select(User).where(User.firebase_uid == firebase_uid, User.deleted_at.is_(None))
    )
    return result.scalar_one_or_none()


async def get_by_id(db: AsyncSession, user_id: str) -> User | None:
    result = await db.execute(
        select(User).where(User.id == user_id, User.deleted_at.is_(None))
    )
    return result.scalar_one_or_none()


async def create(db: AsyncSession, user: User) -> User:
    db.add(user)
    await db.flush()
    return user


async def update(db: AsyncSession, user: User) -> User:
    db.add(user)
    await db.flush()
    return user
