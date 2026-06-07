"""User and family provisioning (equivalent of /me/init and /me)."""

from sqlalchemy.ext.asyncio import AsyncSession

from app import repositories as repo
from app.core.clock import utcnow
from app.core.ids import new_id
from app.models.family import Family, FamilyMember
from app.models.user import User
from app.schemas.user import MeInitResponse, MeResponse


async def get_or_create(
    db: AsyncSession,
    firebase_uid: str,
    email: str | None,
    display_name: str | None = None,
) -> tuple[User, bool]:
    """Fetch existing user or create a new one. Returns (user, is_new)."""
    user = await repo.user_repository.get_by_firebase_uid(db, firebase_uid)
    if user:
        return user, False

    now = utcnow()
    user = User(
        id=new_id(),
        firebase_uid=firebase_uid,
        email=email,
        display_name=display_name,
        created_at=now,
        updated_at=now,
    )
    await repo.user_repository.create(db, user)
    return user, True


async def init_me(
    db: AsyncSession,
    firebase_uid: str,
    email: str | None,
) -> MeInitResponse:
    """Idempotent: ensure user + default family exist, return IDs."""
    user, is_new_user = await get_or_create(db, firebase_uid, email)

    if user.default_family_id:
        return MeInitResponse(
            user_id=user.id,
            default_family_id=user.default_family_id,
            is_new_user=False,
        )

    now = utcnow()
    family = Family(
        id=new_id(),
        owner_user_id=user.id,
        created_at=now,
        updated_at=now,
    )
    await repo.family_repository.create_family(db, family)

    member = FamilyMember(
        id=new_id(),
        family_id=family.id,
        user_id=user.id,
        role="OWNER",
        status="ACTIVE",
        created_at=now,
        updated_at=now,
    )
    await repo.family_repository.create_member(db, member)

    user.default_family_id = family.id
    user.updated_at = now
    await repo.user_repository.update(db, user)

    return MeInitResponse(
        user_id=user.id,
        default_family_id=family.id,
        is_new_user=is_new_user,
    )


async def get_me(db: AsyncSession, user_id: str) -> MeResponse:
    from app.core.errors import NotFoundError

    user = await repo.user_repository.get_by_id(db, user_id)
    if not user:
        raise NotFoundError("User not found.")
    return MeResponse(
        user_id=user.id,
        email=user.email,
        display_name=user.display_name,
        default_family_id=user.default_family_id,
        created_at=user.created_at,
    )
