import datetime

from app.schemas.common import ApiModel


class GooglePlayPurchaseRequest(ApiModel):
    package_name: str
    product_id: str
    purchase_token: str


class EntitlementResponse(ApiModel):
    is_pro: bool
    source: str | None = None
    plan: str | None = None
    status: str | None = None
    expires_at: datetime.datetime | None = None
    grace_period_until: datetime.datetime | None = None
    last_verified_at: datetime.datetime | None = None
