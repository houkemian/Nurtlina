import datetime

from app.schemas.common import ApiModel


class MeInitResponse(ApiModel):
    user_id: str
    default_family_id: str
    is_new_user: bool


class MeResponse(ApiModel):
    user_id: str
    email: str | None
    display_name: str | None
    default_family_id: str | None
    created_at: datetime.datetime
