"""Ownership guards for FastAPI/Postgres sync writes (v2.0 — Bottle removed)."""

import datetime
from decimal import Decimal
from typing import Any

import pytest

from app.models.baby import Baby
from app.repositories.sync_repository import (
    upsert_baby,
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
async def test_rejects_feed_log_when_baby_belongs_to_another_family() -> None:
    db = FakeSession([baby_record(family_id="family-2")])

    _, outcome = await upsert_feed_log(db, feed_log_data(), "family-1")

    assert outcome == "rejected"
    assert db.added == []
    assert not db.flushed


@pytest.mark.asyncio
@pytest.mark.parametrize(
    ("upsert_fn", "data"),
    [
        (upsert_diaper_log, lambda: diaper_log_data()),
        (upsert_sleep_log, lambda: sleep_log_data()),
        (upsert_feed_log, lambda: feed_log_data()),
    ],
)
async def test_rejects_child_logs_when_baby_is_not_in_family(upsert_fn: Any, data: Any) -> None:
    db = FakeSession([baby_record(family_id="family-2")])

    _, outcome = await upsert_fn(db, data(), "family-1")

    assert outcome == "rejected"
    assert db.added == []
    assert not db.flushed


@pytest.mark.asyncio
async def test_accepts_feed_log_in_same_family() -> None:
    db = FakeSession([baby_record(family_id="family-1")])

    _, outcome = await upsert_feed_log(db, feed_log_data(), "family-1")

    assert outcome == "accepted"
    assert len(db.added) == 1
    assert db.flushed


@pytest.mark.asyncio
async def test_soft_delete_feed_log_same_family() -> None:
    deleted_at = ts(2)
    from app.models.feed_log import FeedLog as FeedLogModel
    existing = FeedLogModel(
        id="feed-1",
        family_id="family-1",
        baby_id="baby-1",
        bottle_id=None,
        feed_type="FORMULA",
        amount_ml=Decimal("120.00"),
        started_at=ts(0),
        ended_at=None,
        note=None,
        client_id="android-test",
        schema_version=1,
        created_at=ts(0),
        updated_at=ts(1),
        deleted_at=None,
    )
    db = FakeSession([baby_record(family_id="family-1"), existing])

    _, outcome = await upsert_feed_log(
        db, feed_log_data(updated_at=ts(2), deleted_at=deleted_at), "family-1"
    )

    assert outcome == "accepted"
    assert existing.deleted_at == deleted_at
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


def feed_log_data(
    *,
    record_id: str = "feed-1",
    family_id: str = "family-1",
    baby_id: str = "baby-1",
    bottle_id: str | None = None,
    updated_at: datetime.datetime | None = None,
    deleted_at: datetime.datetime | None = None,
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
        "updated_at": updated_at or ts(1),
        "deleted_at": deleted_at,
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
