import datetime

from app.schemas.common import ApiModel


class ExportRequest(ApiModel):
    family_id: str
    format: str = "CSV"  # "CSV" | "PDF"
    date_from: datetime.date | None = None
    date_to: datetime.date | None = None


class ExportResponse(ApiModel):
    export_id: str
    status: str  # "PENDING" | "PROCESSING" | "DONE" | "FAILED"
    download_url: str | None = None
    expires_at: datetime.datetime | None = None
    created_at: datetime.datetime
