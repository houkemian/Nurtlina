"""Centralised time helpers. Always UTC. Never use datetime.now() elsewhere."""

import datetime


def utcnow() -> datetime.datetime:
    """Current UTC time as timezone-aware datetime."""
    return datetime.datetime.now(tz=datetime.timezone.utc)


def utcfromtimestamp(ts: float) -> datetime.datetime:
    return datetime.datetime.fromtimestamp(ts, tz=datetime.timezone.utc)


def ensure_utc(dt: datetime.datetime) -> datetime.datetime:
    """Attach UTC tzinfo if the datetime is naive."""
    if dt.tzinfo is None:
        return dt.replace(tzinfo=datetime.timezone.utc)
    return dt.astimezone(datetime.timezone.utc)
