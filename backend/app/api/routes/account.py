from fastapi import APIRouter, BackgroundTasks, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.core.security import CurrentUser
from app.db.session import get_db
from app.services import deletion_service

router = APIRouter(prefix="/account", tags=["Account"])


@router.post("/delete", status_code=202)
async def delete_account(
    background_tasks: BackgroundTasks,
    current_user: CurrentUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Initiate account deletion.
    Soft-deletes user data synchronously; heavy cleanup runs in background.
    Firebase Auth token revocation must be handled by the caller (Android) after
    receiving a successful response.
    """
    await deletion_service.delete_account(db, current_user.user_id)
    return {"status": "deletion_initiated"}
