from fastapi import APIRouter

from app.core.clock import utcnow
from app.core.config import settings

router = APIRouter(tags=["Health"])


@router.get("/health")
async def health() -> dict:
    return {
        "status": "ok",
        "version": settings.app_version,
        "time": utcnow().isoformat(),
    }
