from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.core.security import CurrentUser
from app.db.session import get_db
from app.schemas.user import MeInitResponse, MeResponse
from app.services import user_service

router = APIRouter(prefix="/me", tags=["Me"])


@router.post("/init", response_model=MeInitResponse)
async def init_me(
    current_user: CurrentUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> MeInitResponse:
    """Idempotent - ensures user + default family exist, returns their IDs."""
    return await user_service.init_me(db, current_user.firebase_uid, current_user.email)


@router.get("", response_model=MeResponse)
async def get_me(
    current_user: CurrentUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> MeResponse:
    return await user_service.get_me(db, current_user.user_id)
