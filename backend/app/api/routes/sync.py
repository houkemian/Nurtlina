import datetime

from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.core.security import CurrentUser
from app.db.session import get_db
from app.schemas.sync import (
    BabySyncRequest,
    BottleSyncRequest,
    DiaperLogSyncRequest,
    FeedLogSyncRequest,
    SleepLogSyncRequest,
    SyncPullResponse,
    SyncPushResponse,
)
from app.services import sync_service

router = APIRouter(prefix="/sync", tags=["Sync"])


@router.post("/babies", response_model=SyncPushResponse)
async def push_babies(
    req: BabySyncRequest,
    current_user: CurrentUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> SyncPushResponse:
    return await sync_service.push_babies(db, req, current_user.user_id)


@router.post("/bottles", response_model=SyncPushResponse)
async def push_bottles(
    req: BottleSyncRequest,
    current_user: CurrentUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> SyncPushResponse:
    return await sync_service.push_bottles(db, req, current_user.user_id)


@router.post("/feed-logs", response_model=SyncPushResponse)
async def push_feed_logs(
    req: FeedLogSyncRequest,
    current_user: CurrentUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> SyncPushResponse:
    return await sync_service.push_feed_logs(db, req, current_user.user_id)


@router.post("/diaper-logs", response_model=SyncPushResponse)
async def push_diaper_logs(
    req: DiaperLogSyncRequest,
    current_user: CurrentUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> SyncPushResponse:
    return await sync_service.push_diaper_logs(db, req, current_user.user_id)


@router.post("/sleep-logs", response_model=SyncPushResponse)
async def push_sleep_logs(
    req: SleepLogSyncRequest,
    current_user: CurrentUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> SyncPushResponse:
    return await sync_service.push_sleep_logs(db, req, current_user.user_id)


@router.get("/changes", response_model=SyncPullResponse)
async def pull_changes(
    family_id: str = Query(...),
    client_id: str = Query(...),
    since: datetime.datetime = Query(
        default=datetime.datetime(2000, 1, 1, tzinfo=datetime.timezone.utc),
        description="ISO 8601 UTC timestamp; return records updated after this time.",
    ),
    current_user: CurrentUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> SyncPullResponse:
    return await sync_service.pull_changes(
        db, family_id, current_user.user_id, client_id, since
    )
