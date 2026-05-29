from fastapi import APIRouter

from app.api.routes import (
    account,
    admin,
    billing,
    export,
    health,
    me,
    sync,
)

# Public routes (no auth)
public_router = APIRouter()
public_router.include_router(health.router)

# Authenticated API routes under /api/v1
api_router = APIRouter(prefix="/api/v1")
api_router.include_router(me.router)
api_router.include_router(sync.router)
api_router.include_router(billing.router)
api_router.include_router(export.router)
api_router.include_router(account.router)
api_router.include_router(admin.router)
