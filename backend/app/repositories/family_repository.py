from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.family import Family, FamilyMember


async def get_family_by_id(db: AsyncSession, family_id: str) -> Family | None:
    result = await db.execute(
        select(Family).where(Family.id == family_id, Family.deleted_at.is_(None))
    )
    return result.scalar_one_or_none()


async def create_family(db: AsyncSession, family: Family) -> Family:
    db.add(family)
    await db.flush()
    return family


async def get_member(
    db: AsyncSession, family_id: str, user_id: str
) -> FamilyMember | None:
    result = await db.execute(
        select(FamilyMember).where(
            FamilyMember.family_id == family_id,
            FamilyMember.user_id == user_id,
            FamilyMember.deleted_at.is_(None),
            FamilyMember.status == "ACTIVE",
        )
    )
    return result.scalar_one_or_none()


async def create_member(db: AsyncSession, member: FamilyMember) -> FamilyMember:
    db.add(member)
    await db.flush()
    return member
