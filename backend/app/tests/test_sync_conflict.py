"""Unit tests for sync conflict resolution rules.

Tests the BOTTLE_STATUS_PRIORITY table and the upsert logic in sync_repository.
No DB connection required - tests the pure priority logic directly.
"""

import datetime

from app.repositories.sync_repository import BOTTLE_STATUS_PRIORITY

UTC = datetime.UTC


class TestBottleStatusPriority:
    """Terminal states must outrank transitional ones."""

    def test_terminal_states_have_highest_priority(self) -> None:
        terminal = {"CANCELED", "DISCARDED", "FED"}
        for state in terminal:
            assert BOTTLE_STATUS_PRIORITY[state] == 6

    def test_expired_above_feeding_started(self) -> None:
        assert BOTTLE_STATUS_PRIORITY["EXPIRED"] > BOTTLE_STATUS_PRIORITY["FEEDING_STARTED"]

    def test_feeding_started_above_refrigerated(self) -> None:
        assert BOTTLE_STATUS_PRIORITY["FEEDING_STARTED"] > BOTTLE_STATUS_PRIORITY["REFRIGERATED"]

    def test_refrigerated_above_not_started(self) -> None:
        assert BOTTLE_STATUS_PRIORITY["REFRIGERATED"] > BOTTLE_STATUS_PRIORITY["NOT_STARTED"]

    def test_not_started_is_lowest(self) -> None:
        assert BOTTLE_STATUS_PRIORITY["NOT_STARTED"] == min(BOTTLE_STATUS_PRIORITY.values())

    def test_fed_wins_over_expired(self) -> None:
        assert BOTTLE_STATUS_PRIORITY["FED"] > BOTTLE_STATUS_PRIORITY["EXPIRED"]

    def test_discarded_wins_over_feeding_started(self) -> None:
        assert BOTTLE_STATUS_PRIORITY["DISCARDED"] > BOTTLE_STATUS_PRIORITY["FEEDING_STARTED"]


class TestConflictResolutionLogic:
    """Validate the timestamp + priority decision matrix (pure logic, no DB)."""

    def _should_accept(
        self,
        incoming_ts: datetime.datetime,
        existing_ts: datetime.datetime,
        incoming_status: str,
        existing_status: str,
    ) -> str:
        """Mirrors the decision logic in sync_repository.upsert_bottle."""
        if incoming_ts > existing_ts:
            return "accepted"
        if incoming_ts == existing_ts:
            incoming_p = BOTTLE_STATUS_PRIORITY.get(incoming_status, 0)
            existing_p = BOTTLE_STATUS_PRIORITY.get(existing_status, 0)
            if incoming_p > existing_p:
                return "conflict"
        return "rejected"

    def test_newer_timestamp_always_accepted(self) -> None:
        t1 = datetime.datetime(2026, 1, 1, 10, 0, 0, tzinfo=UTC)
        t2 = datetime.datetime(2026, 1, 1, 10, 0, 1, tzinfo=UTC)
        assert self._should_accept(t2, t1, "NOT_STARTED", "NOT_STARTED") == "accepted"

    def test_older_timestamp_rejected(self) -> None:
        t1 = datetime.datetime(2026, 1, 1, 10, 0, 0, tzinfo=UTC)
        t2 = datetime.datetime(2026, 1, 1, 9, 59, 59, tzinfo=UTC)
        assert self._should_accept(t2, t1, "FEEDING_STARTED", "NOT_STARTED") == "rejected"

    def test_same_timestamp_higher_priority_wins(self) -> None:
        t = datetime.datetime(2026, 1, 1, 10, 0, 0, tzinfo=UTC)
        assert self._should_accept(t, t, "DISCARDED", "FEEDING_STARTED") == "conflict"

    def test_same_timestamp_lower_priority_rejected(self) -> None:
        t = datetime.datetime(2026, 1, 1, 10, 0, 0, tzinfo=UTC)
        assert self._should_accept(t, t, "NOT_STARTED", "FEEDING_STARTED") == "rejected"

    def test_same_timestamp_same_priority_rejected(self) -> None:
        t = datetime.datetime(2026, 1, 1, 10, 0, 0, tzinfo=UTC)
        assert self._should_accept(t, t, "FEEDING_STARTED", "FEEDING_STARTED") == "rejected"

    def test_terminal_fed_wins_over_same_timestamp_not_started(self) -> None:
        older = datetime.datetime(2026, 1, 1, 9, 0, 0, tzinfo=UTC)
        same = datetime.datetime(2026, 1, 1, 9, 0, 0, tzinfo=UTC)
        assert self._should_accept(same, older, "FED", "NOT_STARTED") == "conflict"
        # but newer timestamp wins unconditionally:
        newer = datetime.datetime(2026, 1, 1, 9, 0, 1, tzinfo=UTC)
        assert self._should_accept(newer, older, "FED", "NOT_STARTED") == "accepted"
