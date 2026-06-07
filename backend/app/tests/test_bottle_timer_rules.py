"""Unit tests for bottle timer expiry rules (Tech_Architecture.md §Guideline Source Handling).

These tests verify the default timer rules referenced in AGENTS.md:

  Formula, not started, room temperature:   2 hours from prepared_at
  Formula, after feeding starts:            1 hour from feeding_started_at
  Formula refrigerated (before feeding):   24 hours from prepared_at
  Breast milk, room temperature:            4 hours from expressed/prepared_at
  Breast milk refrigerated:                 4 days from expressed/prepared_at

No external dependencies required - pure datetime arithmetic.
"""

import datetime

UTC = datetime.UTC


def formula_room_temp_expiry(prepared_at: datetime.datetime) -> datetime.datetime:
    """Default rule: prepared formula at room temp expires 2 h after preparation."""
    return prepared_at + datetime.timedelta(hours=2)


def formula_feeding_started_expiry(feeding_started_at: datetime.datetime) -> datetime.datetime:
    """Formula after feeding starts: expires 1 h after feeding_started_at."""
    return feeding_started_at + datetime.timedelta(hours=1)


def formula_refrigerated_expiry(prepared_at: datetime.datetime) -> datetime.datetime:
    """Refrigerated formula (before feeding starts): expires 24 h after prepared_at."""
    return prepared_at + datetime.timedelta(hours=24)


def breast_milk_room_temp_expiry(expressed_at: datetime.datetime) -> datetime.datetime:
    """Fresh breast milk at room temp: expires 4 h after expression."""
    return expressed_at + datetime.timedelta(hours=4)


def breast_milk_refrigerated_expiry(expressed_at: datetime.datetime) -> datetime.datetime:
    """Refrigerated fresh breast milk: expires 4 days after expression."""
    return expressed_at + datetime.timedelta(days=4)


# ─── Tests ────────────────────────────────────────────────────────────────────


class TestFormulaRoomTemp:
    def test_expires_2_hours_after_prepared(self) -> None:
        prepared = datetime.datetime(2026, 1, 1, 10, 0, 0, tzinfo=UTC)
        expected = datetime.datetime(2026, 1, 1, 12, 0, 0, tzinfo=UTC)
        assert formula_room_temp_expiry(prepared) == expected

    def test_cross_day_boundary(self) -> None:
        prepared = datetime.datetime(2026, 1, 1, 23, 30, 0, tzinfo=UTC)
        expected = datetime.datetime(2026, 1, 2, 1, 30, 0, tzinfo=UTC)
        assert formula_room_temp_expiry(prepared) == expected

    def test_not_expired_before_2h(self) -> None:
        prepared = datetime.datetime(2026, 1, 1, 10, 0, 0, tzinfo=UTC)
        now = datetime.datetime(2026, 1, 1, 11, 59, 59, tzinfo=UTC)
        assert formula_room_temp_expiry(prepared) > now

    def test_expired_after_2h(self) -> None:
        prepared = datetime.datetime(2026, 1, 1, 10, 0, 0, tzinfo=UTC)
        now = datetime.datetime(2026, 1, 1, 12, 0, 1, tzinfo=UTC)
        assert formula_room_temp_expiry(prepared) < now


class TestFormulaFeedingStarted:
    def test_expires_1_hour_after_feeding_started(self) -> None:
        started = datetime.datetime(2026, 1, 1, 14, 0, 0, tzinfo=UTC)
        expected = datetime.datetime(2026, 1, 1, 15, 0, 0, tzinfo=UTC)
        assert formula_feeding_started_expiry(started) == expected

    def test_independent_of_prepared_time(self) -> None:
        # Even if bottle was prepared long ago, the 1 h clock starts at feeding
        feeding_started = datetime.datetime(2026, 1, 1, 15, 30, 0, tzinfo=UTC)
        expiry = formula_feeding_started_expiry(feeding_started)
        assert expiry == datetime.datetime(2026, 1, 1, 16, 30, 0, tzinfo=UTC)


class TestFormulaRefrigerated:
    def test_expires_24_hours_after_prepared(self) -> None:
        prepared = datetime.datetime(2026, 1, 1, 8, 0, 0, tzinfo=UTC)
        expected = datetime.datetime(2026, 1, 2, 8, 0, 0, tzinfo=UTC)
        assert formula_refrigerated_expiry(prepared) == expected

    def test_boundary_at_exactly_24h(self) -> None:
        prepared = datetime.datetime(2026, 1, 1, 0, 0, 0, tzinfo=UTC)
        now_at_24h = datetime.datetime(2026, 1, 2, 0, 0, 0, tzinfo=UTC)
        assert formula_refrigerated_expiry(prepared) == now_at_24h


class TestBreastMilkRoomTemp:
    def test_expires_4_hours_after_expressed(self) -> None:
        expressed = datetime.datetime(2026, 1, 1, 6, 0, 0, tzinfo=UTC)
        expected = datetime.datetime(2026, 1, 1, 10, 0, 0, tzinfo=UTC)
        assert breast_milk_room_temp_expiry(expressed) == expected


class TestBreastMilkRefrigerated:
    def test_expires_4_days_after_expressed(self) -> None:
        expressed = datetime.datetime(2026, 1, 1, 8, 0, 0, tzinfo=UTC)
        expected = datetime.datetime(2026, 1, 5, 8, 0, 0, tzinfo=UTC)
        assert breast_milk_refrigerated_expiry(expressed) == expected

    def test_cross_month_boundary(self) -> None:
        expressed = datetime.datetime(2026, 1, 30, 12, 0, 0, tzinfo=UTC)
        expected = datetime.datetime(2026, 2, 3, 12, 0, 0, tzinfo=UTC)
        assert breast_milk_refrigerated_expiry(expressed) == expected
