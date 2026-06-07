"""Export service - MVP returns a stub; full async export is V1.1."""

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.clock import utcnow
from app.core.ids import new_id
from app.schemas.export import ExportRequest, ExportResponse


async def create_export(
    db: AsyncSession,
    user_id: str,
    req: ExportRequest,
) -> ExportResponse:
    """
    MVP: returns a stub response.
    V1.1: enqueue a background task that generates CSV/PDF and uploads to GCS,
    then returns a signed URL via GET /exports/{exportId}.
    """
    export_id = new_id()
    now = utcnow()
    return ExportResponse(
        export_id=export_id,
        status="PENDING",
        download_url=None,
        expires_at=None,
        created_at=now,
    )


async def get_export(
    db: AsyncSession,
    user_id: str,
    export_id: str,
) -> ExportResponse:
    from app.core.errors import NotFoundError

    # MVP stub: export not found until full implementation
    raise NotFoundError("Export not found or not yet implemented.")
