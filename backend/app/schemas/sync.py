"""Schemas for the sync push/pull API (section 17.7.3–17.7.4)."""

import datetime
from decimal import Decimal

from pydantic import Field

from app.schemas.common import ApiModel


# ─── Generic push envelope ────────────────────────────────────────────────────

class SyncPushResponse(ApiModel):
    server_time: datetime.datetime
    accepted: list[str]
    rejected: list[str]
    conflicts: list[str]


# ─── Pull response ────────────────────────────────────────────────────────────

class SyncPullResponse(ApiModel):
    server_time: datetime.datetime
    next_cursor: datetime.datetime | None = None
    has_more: bool = False
    babies: list["BabyChange"] = Field(default_factory=list)
    bottles: list["BottleChange"] = Field(default_factory=list)
    feed_logs: list["FeedLogChange"] = Field(default_factory=list)
    diaper_logs: list["DiaperLogChange"] = Field(default_factory=list)
    sleep_logs: list["SleepLogChange"] = Field(default_factory=list)


# ─── Change payload schemas ───────────────────────────────────────────────────

class BabyChange(ApiModel):
    id: str
    family_id: str
    name: str
    birth_date: datetime.date | None = None
    avatar_color: str | None = None
    client_id: str | None = None
    schema_version: int = 1
    created_at: datetime.datetime
    updated_at: datetime.datetime
    deleted_at: datetime.datetime | None = None


class BottleChange(ApiModel):
    id: str
    family_id: str
    baby_id: str
    milk_type: str
    amount_ml: Decimal | None = None
    prepared_at: datetime.datetime
    feeding_started_at: datetime.datetime | None = None
    refrigerated_at: datetime.datetime | None = None
    status: str
    guideline_region: str
    rule_version: str
    expires_at: datetime.datetime | None = None
    discarded_at: datetime.datetime | None = None
    fed_at: datetime.datetime | None = None
    note: str | None = None
    client_id: str | None = None
    schema_version: int = 1
    created_at: datetime.datetime
    updated_at: datetime.datetime
    deleted_at: datetime.datetime | None = None


class FeedLogChange(ApiModel):
    id: str
    family_id: str
    baby_id: str
    bottle_id: str | None = None
    feed_type: str
    amount_ml: Decimal | None = None
    started_at: datetime.datetime
    ended_at: datetime.datetime | None = None
    note: str | None = None
    client_id: str | None = None
    schema_version: int = 1
    created_at: datetime.datetime
    updated_at: datetime.datetime
    deleted_at: datetime.datetime | None = None


class DiaperLogChange(ApiModel):
    id: str
    family_id: str
    baby_id: str
    diaper_type: str
    changed_at: datetime.datetime
    note: str | None = None
    client_id: str | None = None
    schema_version: int = 1
    created_at: datetime.datetime
    updated_at: datetime.datetime
    deleted_at: datetime.datetime | None = None


class SleepLogChange(ApiModel):
    id: str
    family_id: str
    baby_id: str
    started_at: datetime.datetime
    ended_at: datetime.datetime | None = None
    note: str | None = None
    client_id: str | None = None
    schema_version: int = 1
    created_at: datetime.datetime
    updated_at: datetime.datetime
    deleted_at: datetime.datetime | None = None


# ─── Push request envelopes ───────────────────────────────────────────────────

class BabySyncRequest(ApiModel):
    family_id: str
    client_id: str
    changes: list[BabyChange]


class BottleSyncRequest(ApiModel):
    family_id: str
    client_id: str
    changes: list[BottleChange]


class FeedLogSyncRequest(ApiModel):
    family_id: str
    client_id: str
    changes: list[FeedLogChange]


class DiaperLogSyncRequest(ApiModel):
    family_id: str
    client_id: str
    changes: list[DiaperLogChange]


class SleepLogSyncRequest(ApiModel):
    family_id: str
    client_id: str
    changes: list[SleepLogChange]
