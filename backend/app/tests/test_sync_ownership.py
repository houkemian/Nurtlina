"""Ownership guards for FastAPI/Postgres sync writes."""

import datetime
from decimal import Decimal
from typing import Any

import pytest

from app.models.baby import Baby
from app.models.bottle import Bottle
from app.repositories.sync_repository import (
    upsert_baby,
    upsert_bottle,
    upsert_diaper_log,
    upsert_feed_log,
    upsert_sleep_log,
)

UTC = datetime.UTC


class FakeSession:
    def __init__(self, records: list[Any] | None = None) -> None:
        self.records: dict[tuple[type[Any], str], Any] = {}
        self.added: list[Any] = []
        self.flushed = False
        for record in records or []:
            self.records[(type(record), record.id)] = record

    async def get(self, model: type[Any], record_id: str) -> Any | None:
        return self.records.get((model, record_id))

    def add(self, record: Any) -> None:
        self.added.append(record)
        self.records[(type(record), record.id)] = record

    async def flush(self) -> None:
        self.flushed = True


@pytest.mark.asyncio
async def test_rejects_payload_family_mismatch() -> None:
    db = FakeSession()

    record_id, outcome = await upsert_baby(db, baby_data(family_id="family-2"), "family-1")

    assert record_id == "baby-1"
    assert outcome == "rejected"
    assert db.added == []
    assert not db.flushed


@pytest.mark.asyncio
async def test_rejects_existing_record_from_another_family() -> None:
    existing = Baby(
        id="baby-1",
        family_id="family-2",
        name="Other family baby",
        birth_date=None,
        avatar_color=None,
        client_id="android-test",
        schema_version=1,
        created_at=ts(0),
        updated_at=ts(0),
        deleted_at=None,
    )
    db = FakeSession([existing])

    _, outcome = await upsert_baby(db, baby_data(name="Cross-family overwrite"), "family-1")

    assert outcome == "rejected"
    assert existing.name == "Other family baby"
    assert not db.flushed


@pytest.mark.asyncio
async def test_rejects_bottle_when_baby_belongs_to_another_family() -> None:
    db = FakeSession([baby_record(family_id="family-2")])

    _, outcome = await upsert_bottle(db, bottle_data(), "family-1")

    assert outcome == "rejected"
    assert db.added == []
    assert not db.flushed


@pytest.mark.asyncio
async def test_rejects_feed_log_when_bottle_belongs_to_another_family() -> None:
    db = FakeSession(
        [
            baby_record(family_id="family-1"),
            bottle_record(family_id="family-2"),
        ]
    )

    _, outcome = await upsert_feed_log(db, feed_log_data(bottle_id="bottle-1"), "family-1")

    assert outcome == "rejected"
    assert db.added == []
    assert not db.flushed


@pytest.mark.asyncio
@pytest.mark.parametrize(
    ("upsert", "data"),
    [
        (upsert_diaper_log, lambda: diaper_log_data()),
        (upsert_sleep_log, lambda: sleep_log_data()),
    ],
)
async def test_rejects_child_logs_when_baby_is_not_in_family(upsert: Any, data: Any) -> None:
    db = FakeSession([baby_record(family_id="family-2")])

    _, outcome = await upsert(db, data(), "family-1")

    assert outcome == "rejected"
    assert db.added == []
    assert not db.flushed


@pytest.mark.asyncio
async def test_delete_change_soft_deletes_existing_same_family_bottle() -> None:
    deleted_at = ts(2)
    existing = bottle_record(family_id="family-1", updated_at=ts(1), deleted_at=None)
    db = FakeSession([baby_record(family_id="family-1"), existing])

    _, outcome = await upsert_bottle(
        db,
        bottle_data(updated_at=ts(2), deleted_at=deleted_at),
        "family-1",
    )

    assert outcome == "accepted"
    assert existing.deleted_at == deleted_at
    assert db.added == []
    assert db.flushed


def ts(minutes: int) -> datetime.datetime:
    return datetime.datetime(2026, 1, 1, 10, minutes, tzinfo=UTC)


def baby_record(
    *,
    record_id: str = "baby-1",
    family_id: str = "family-1",
) -> Baby:
    return Baby(
        id=record_id,
        family_id=family_id,
        name="Baby",
        birth_date=None,
        avatar_color=None,
        client_id="android-test",
        schema_version=1,
        created_at=ts(0),
        updated_at=ts(0),
        deleted_at=None,
    )


def bottle_record(
    *,
    record_id: str = "bottle-1",
    family_id: str = "family-1",
    baby_id: str = "baby-1",
    updated_at: datetime.datetime | None = None,
    deleted_at: datetime.datetime | None = None,
) -> Bottle:
    return Bottle(**bottle_data(record_id, family_id, baby_id, updated_at, deleted_at))


def baby_data(
    *,
    record_id: str = "baby-1",
    family_id: str = "family-1",
    name: str = "Baby",
    updated_at: datetime.datetime | None = None,
    deleted_at: datetime.datetime | None = None,
) -> dict[str, Any]:
    return {
        "id": record_id,
        "family_id": family_id,
        "name": name,
        "birth_date": None,
        "avatar_color": None,
        "client_id": "android-test",
        "schema_version": 1,
        "created_at": ts(0),
        "updated_at": updated_at or ts(1),
        "deleted_at": deleted_at,
    }


def bottle_data(
    record_id: str = "bottle-1",
    family_id: str = "family-1",
    baby_id: str = "baby-1",
    updated_at: datetime.datetime | None = None,
    deleted_at: datetime.datetime | None = None,
) -> dict[str, Any]:
    return {
        "id": record_id,
        "family_id": family_id,
        "baby_id": baby_id,
        "milk_type": "FORMULA",
        "amount_ml": Decimal("120.00"),
        "prepared_at": ts(0),
        "feeding_started_at": None,
        "refrigerated_at": None,
        "status": "NOT_STARTED",
        "guideline_region": "US",
        "rule_version": "default_v1",
        "expires_at": ts(2),
        "discarded_at": None,
        "fed_at": None,
        "note": None,
        "client_id": "android-test",
        "schema_version": 1,
        "created_at": ts(0),
        "updated_at": updated_at or ts(1),
        "deleted_at": deleted_at,
    }


def feed_log_data(
    *,
    record_id: str = "feed-1",
    family_id: str = "family-1",
    baby_id: str = "baby-1",
    bottle_id: str | None = None,
) -> dict[str, Any]:
    return {
        "id": record_id,
        "family_id": family_id,
        "baby_id": baby_id,
        "bottle_id": bottle_id,
        "feed_type": "FORMULA",
        "amount_ml": Decimal("120.00"),
        "started_at": ts(0),
        "ended_at": None,
        "note": None,
        "client_id": "android-test",
        "schema_version": 1,
        "created_at": ts(0),
        "updated_at": ts(1),
        "deleted_at": None,
    }


def diaper_log_data(
    *,
    record_id: str = "diaper-1",
    family_id: str = "family-1",
    baby_id: str = "baby-1",
) -> dict[str, Any]:
    return {
        "id": record_id,
        "family_id": family_id,
        "baby_id": baby_id,
        "diaper_type": "WET",
        "changed_at": ts(0),
        "note": None,
        "client_id": "android-test",
        "schema_version": 1,
        "created_at": ts(0),
        "updated_at": ts(1),
        "deleted_at": None,
    }


def sleep_log_data(
    *,
    record_id: str = "sleep-1",
    family_id: str = "family-1",
    baby_id: str = "baby-1",
) -> dict[str, Any]:
    return {
        "id": record_id,
        "family_id": family_id,
        "baby_id": baby_id,
        "started_at": ts(0),
        "ended_at": ts(1),
        "note": None,
        "client_id": "android-test",
        "schema_version": 1,
        "created_at": ts(0),
        "updated_at": ts(1),
        "deleted_at": None,
    }
