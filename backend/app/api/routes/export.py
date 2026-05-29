from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.core.security import CurrentUser
from app.db.session import get_db
from app.schemas.export import ExportRequest, ExportResponse
from app.services import export_service

router = APIRouter(prefix="/exports", tags=["Export"])


@router.post("", response_model=ExportResponse, status_code=202)
async def create_export(
    req: ExportRequest,
    current_user: CurrentUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> ExportResponse:
    """Enqueue a data export. Full implementation in V1.1."""
    return await export_service.create_export(db, current_user.user_id, req)


@router.get("/{export_id}", response_model=ExportResponse)
async def get_export(
    export_id: str,
    current_user: CurrentUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> ExportResponse:
    return await export_service.get_export(db, current_user.user_id, export_id)
