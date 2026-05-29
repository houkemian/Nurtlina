"""Shared Pydantic base types and utilities."""

import datetime

from pydantic import BaseModel, ConfigDict


class ApiModel(BaseModel):
    model_config = ConfigDict(populate_by_name=True)


class ErrorDetail(ApiModel):
    code: str
    message: str
    request_id: str | None = None


class ErrorResponse(ApiModel):
    error: ErrorDetail


def to_utc(dt: datetime.datetime) -> datetime.datetime:
    """Ensure datetime is UTC-aware for serialization."""
    if dt.tzinfo is None:
        return dt.replace(tzinfo=datetime.timezone.utc)
    return dt.astimezone(datetime.timezone.utc)
