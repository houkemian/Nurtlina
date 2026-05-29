"""Admin routes – placeholder for V1.1 internal dashboard.

These endpoints must be protected by an additional admin role check
before any real implementation is added.
"""

from fastapi import APIRouter

router = APIRouter(prefix="/admin", tags=["Admin"])


@router.get("/health")
async def admin_health() -> dict:
    """Placeholder – returns 200 so the route exists in OpenAPI docs."""
    return {"status": "not_implemented"}
