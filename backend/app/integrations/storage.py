"""Object storage helpers (Google Cloud Storage). MVP stub - not required at launch."""

import datetime
from typing import Any


class StorageNotConfiguredError(Exception):
    pass


def _get_bucket() -> Any:
    from app.core.config import settings

    if not settings.gcs_bucket_name:
        raise StorageNotConfiguredError("GCS_BUCKET_NAME is not configured.")
    try:
        from google.cloud import storage  # type: ignore[import]

        client = storage.Client()
        return client.bucket(settings.gcs_bucket_name)
    except ImportError as exc:
        raise StorageNotConfiguredError("google-cloud-storage is not installed.") from exc


def generate_signed_url(blob_name: str, expiry_seconds: int = 3600) -> str:
    """Generate a short-lived signed download URL for an export file."""
    bucket = _get_bucket()
    blob = bucket.blob(blob_name)
    return blob.generate_signed_url(
        expiration=datetime.timedelta(seconds=expiry_seconds),
        method="GET",
        version="v4",
    )


def upload_bytes(blob_name: str, data: bytes, content_type: str = "text/csv") -> str:
    """Upload bytes to GCS and return the blob name."""
    bucket = _get_bucket()
    blob = bucket.blob(blob_name)
    blob.upload_from_string(data, content_type=content_type)
    return blob_name
