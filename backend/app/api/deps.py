"""FastAPI dependency factories for auth and DB."""

from fastapi import Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app import repositories as repo
from app.core.errors import ForbiddenError
from app.core.security import CurrentUser, verify_firebase_token
from app.db.session import get_db


async def get_current_user(
    decoded: dict = Depends(verify_firebase_token),
    db: AsyncSession = Depends(get_db),
) -> CurrentUser:
    """
    Resolve Firebase token → internal User record.
    Never trusts client-supplied userId - always derived from token.
    """
    firebase_uid: str = decoded["uid"]
    email: str | None = decoded.get("email")

    user = await repo.user_repository.get_by_firebase_uid(db, firebase_uid)
    if user is None:
        # Auto-provision on first authenticated request
        from app.services.user_service import get_or_create

        user, _ = await get_or_create(db, firebase_uid, email)
        await db.commit()

    return CurrentUser(
        user_id=user.id,
        firebase_uid=firebase_uid,
        email=email,
        default_family_id=user.default_family_id,
    )


async def require_family_member(
    family_id: str,
    current_user: CurrentUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> tuple[CurrentUser, str]:
    """Verify the current user is an active member of the given family."""
    member = await repo.family_repository.get_member(db, family_id, current_user.user_id)
    if not member:
        raise ForbiddenError("Not a member of this family.")
    return current_user, family_id
