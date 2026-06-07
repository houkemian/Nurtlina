"""Immediate CSV/JSON export generation for MVP."""

import base64
import csv
import datetime
import io
import json
from decimal import Decimal
from typing import Any

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app import repositories as repo
from app.core.clock import utcnow
from app.core.errors import ForbiddenError, NotFoundError, ValidationError
from app.core.ids import new_id
from app.models.baby import Baby
from app.models.bottle import Bottle
from app.models.diaper_log import DiaperLog
from app.models.feed_log import FeedLog
from app.models.sleep_log import SleepLog
from app.schemas.export import ExportRequest, ExportResponse

_EXPORT_TTL = datetime.timedelta(hours=1)


async def create_export(db: AsyncSession, user_id: str, req: ExportRequest) -> ExportResponse:
    """Generate an immediate one-family export and return a short-lived data URL.

    MVP avoids storing export artifacts. The response is complete and scoped to a
    family the authenticated user belongs to.
    """
    member = await repo.family_repository.get_member(db, req.family_id, user_id)
    if not member:
        raise ForbiddenError("Not a member of this family.")

    fmt = req.format.upper()
    if fmt not in {"CSV", "JSON"}:
        raise ValidationError("Export format must be CSV or JSON.")

    records = await _load_records(db, req)
    if fmt == "JSON":
        payload = json.dumps(records, default=_json_default, ensure_ascii=False).encode()
        mime = "application/json"
    else:
        payload = _records_to_csv(records).encode()
        mime = "text/csv"

    now = utcnow()
    encoded = base64.b64encode(payload).decode()
    return ExportResponse(
        export_id=new_id(),
        status="DONE",
        download_url=f"data:{mime};base64,{encoded}",
        expires_at=now + _EXPORT_TTL,
        created_at=now,
    )


async def get_export(db: AsyncSession, user_id: str, export_id: str) -> ExportResponse:
    raise NotFoundError("Exports are generated immediately and are not stored in MVP.")


async def _load_records(db: AsyncSession, req: ExportRequest) -> dict[str, list[dict[str, Any]]]:
    return {
        "babies": [_model_dict(x) for x in await _all(db, Baby, req)],
        "bottles": [_model_dict(x) for x in await _all(db, Bottle, req)],
        "feed_logs": [_model_dict(x) for x in await _all(db, FeedLog, req)],
        "diaper_logs": [_model_dict(x) for x in await _all(db, DiaperLog, req)],
        "sleep_logs": [_model_dict(x) for x in await _all(db, SleepLog, req)],
    }


async def _all(db: AsyncSession, model: type[Any], req: ExportRequest) -> list[Any]:
    query = select(model).where(model.family_id == req.family_id, model.deleted_at.is_(None))
    time_field = _time_field(model)
    if req.date_from and time_field is not None:
        start = datetime.datetime.combine(req.date_from, datetime.time.min, tzinfo=datetime.UTC)
        query = query.where(time_field >= start)
    if req.date_to and time_field is not None:
        end = datetime.datetime.combine(req.date_to, datetime.time.max, tzinfo=datetime.UTC)
        query = query.where(time_field <= end)
    result = await db.execute(query.order_by(model.updated_at, model.id))
    return list(result.scalars().all())


def _time_field(model: type[Any]) -> Any | None:
    return {
        Baby: Baby.created_at,
        Bottle: Bottle.prepared_at,
        FeedLog: FeedLog.started_at,
        DiaperLog: DiaperLog.changed_at,
        SleepLog: SleepLog.started_at,
    }.get(model)


def _model_dict(model: Any) -> dict[str, Any]:
    return {column.name: getattr(model, column.name) for column in model.__table__.columns}


def _records_to_csv(records: dict[str, list[dict[str, Any]]]) -> str:
    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["entity", "field", "value", "record_id"])
    for entity, rows in records.items():
        for row in rows:
            record_id = row.get("id", "")
            for field, value in row.items():
                writer.writerow([entity, field, _json_default(value), record_id])
    return output.getvalue()


def _json_default(value: Any) -> Any:
    if isinstance(value, datetime.datetime | datetime.date):
        return value.isoformat()
    if isinstance(value, Decimal):
        return str(value)
    return value
