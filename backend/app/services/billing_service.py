"""Google Play billing verification and entitlement management."""

import hashlib

from sqlalchemy.ext.asyncio import AsyncSession

from app import repositories as repo
from app.core.clock import utcnow
from app.core.errors import BillingError, NotFoundError
from app.core.ids import new_id
from app.integrations.google_play import (
    GooglePlayError,
    verify_one_time_purchase,
    verify_subscription_purchase,
)
from app.models.entitlement import Entitlement
from app.schemas.billing import EntitlementResponse, GooglePlayPurchaseRequest

# Lifetime product IDs (one-time purchases)
_LIFETIME_PRODUCT_IDS = {"pro_lifetime"}


def _hash_token(token: str) -> str:
    return hashlib.sha256(token.encode()).hexdigest()


def _derive_plan(product_id: str) -> str:
    if "yearly" in product_id:
        return "YEARLY"
    if "monthly" in product_id:
        return "MONTHLY"
    if "lifetime" in product_id:
        return "LIFETIME"
    return "UNKNOWN"


async def verify_and_save_purchase(
    db: AsyncSession,
    user_id: str,
    req: GooglePlayPurchaseRequest,
) -> EntitlementResponse:
    from app.core.config import settings

    if req.package_name != settings.google_play_package_name:
        raise BillingError("Invalid package name.")

    is_lifetime = req.product_id in _LIFETIME_PRODUCT_IDS
    now = utcnow()

    try:
        if is_lifetime:
            raw = await verify_one_time_purchase(
                package_name=req.package_name,
                product_id=req.product_id,
                purchase_token=req.purchase_token,
            )
            # purchaseState: 0=purchased, 2=pending
            if raw.get("purchaseState") != 0:
                raise BillingError("Purchase not in purchased state.")
            status = "ACTIVE"
            expires_at = None
        else:
            raw = await verify_subscription_purchase(
                package_name=req.package_name,
                subscription_id=req.product_id,
                purchase_token=req.purchase_token,
            )
            subscription_state = raw.get("subscriptionState", "")
            status = _map_subscription_state(subscription_state)
            expires_at = _parse_expiry(raw)

    except GooglePlayError as e:
        raise BillingError(str(e)) from e

    existing = await repo.entitlement_repository.get_active_by_user(db, user_id)

    if existing:
        existing.product_id = req.product_id
        existing.purchase_token_hash = _hash_token(req.purchase_token)
        existing.status = status
        existing.plan = _derive_plan(req.product_id)
        existing.expires_at = expires_at
        existing.last_verified_at = now
        existing.raw_provider_status = raw
        existing.updated_at = now
        entitlement = await repo.entitlement_repository.upsert(db, existing)
    else:
        entitlement = Entitlement(
            id=new_id(),
            user_id=user_id,
            source="GOOGLE_PLAY",
            product_id=req.product_id,
            purchase_token_hash=_hash_token(req.purchase_token),
            status=status,
            plan=_derive_plan(req.product_id),
            expires_at=expires_at,
            last_verified_at=now,
            raw_provider_status=raw,
            created_at=now,
            updated_at=now,
        )
        entitlement = await repo.entitlement_repository.upsert(db, entitlement)

    return _to_response(entitlement)


async def get_entitlement(db: AsyncSession, user_id: str) -> EntitlementResponse:
    entitlement = await repo.entitlement_repository.get_active_by_user(db, user_id)
    if not entitlement:
        return EntitlementResponse(is_pro=False)
    return _to_response(entitlement)


def _to_response(e: Entitlement) -> EntitlementResponse:
    now = utcnow()
    is_active = e.status == "ACTIVE" and (
        e.expires_at is None or e.expires_at > now
    )
    # Allow grace period
    if not is_active and e.grace_period_until and e.grace_period_until > now:
        is_active = True
    return EntitlementResponse(
        is_pro=is_active,
        source=e.source,
        plan=e.plan,
        status=e.status,
        expires_at=e.expires_at,
        grace_period_until=e.grace_period_until,
        last_verified_at=e.last_verified_at,
    )


def _map_subscription_state(state: str) -> str:
    active_states = {
        "SUBSCRIPTION_STATE_ACTIVE",
        "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
        "SUBSCRIPTION_STATE_PAUSED",  # still paid, just paused
    }
    return "ACTIVE" if state in active_states else "EXPIRED"


def _parse_expiry(raw: dict) -> object:
    import datetime

    items = raw.get("lineItems", [])
    if not items:
        return None
    expiry_str = items[0].get("expiryTime")
    if not expiry_str:
        return None
    try:
        return datetime.datetime.fromisoformat(expiry_str.replace("Z", "+00:00"))
    except ValueError:
        return None
