from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.core.security import CurrentUser
from app.db.session import get_db
from app.schemas.billing import EntitlementResponse, GooglePlayPurchaseRequest
from app.services import billing_service

router = APIRouter(tags=["Billing"])


@router.post(
    "/billing/google-play/purchases",
    response_model=EntitlementResponse,
)
async def submit_purchase(
    req: GooglePlayPurchaseRequest,
    current_user: CurrentUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> EntitlementResponse:
    """Verify a Google Play purchase token server-side and save entitlement."""
    return await billing_service.verify_and_save_purchase(db, current_user.user_id, req)


@router.get("/entitlements/me", response_model=EntitlementResponse)
async def get_entitlement(
    current_user: CurrentUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> EntitlementResponse:
    """Return the current Pro entitlement status for the authenticated user."""
    return await billing_service.get_entitlement(db, current_user.user_id)


@router.post("/webhooks/google-play/rtdn", status_code=204)
async def rtdn_webhook(
    db: AsyncSession = Depends(get_db),
) -> None:
    """
    Google Play Real-Time Developer Notifications (Pub/Sub push).
    V1.1: verify Pub/Sub JWT, decode message, update entitlement.
    MVP: endpoint exists and returns 204 to avoid Pub/Sub retry storms.
    """
