import base64
import json

from fastapi import APIRouter, Depends, Request
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.core.security import CurrentUser
from app.db.session import get_db
from app.schemas.billing import (
    EntitlementResponse,
    GooglePlayPurchaseRequest,
    GooglePlayRtdnMessage,
)
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
    request: Request,
    db: AsyncSession = Depends(get_db),
) -> None:
    """Process Google Play Real-Time Developer Notifications from Pub/Sub push."""
    body = await request.json()
    encoded = body.get("message", {}).get("data")
    if not encoded:
        return
    decoded = json.loads(base64.b64decode(encoded).decode())
    message = _rtdn_to_message(decoded)
    if message is None:
        return
    await billing_service.process_google_play_rtdn(db, message)


def _rtdn_to_message(decoded: dict) -> GooglePlayRtdnMessage | None:
    package_name = decoded.get("packageName")
    event_time = decoded.get("eventTimeMillis")
    notification = decoded.get("subscriptionNotification") or decoded.get(
        "oneTimeProductNotification"
    )
    if not notification:
        return None
    return GooglePlayRtdnMessage(
        package_name=package_name,
        product_id=notification.get("subscriptionId") or notification.get("sku"),
        purchase_token=notification["purchaseToken"],
        event_time_millis=event_time,
        notification_type=notification.get("notificationType"),
        raw=decoded,
    )
