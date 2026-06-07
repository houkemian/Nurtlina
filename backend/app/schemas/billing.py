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


class GooglePlayRtdnMessage(ApiModel):
    package_name: str | None = None
    product_id: str | None = None
    purchase_token: str
    event_time_millis: str | None = None
    notification_type: int | None = None
    raw: dict | None = None
