from typing import Annotated

from pydantic import field_validator
from pydantic_settings import BaseSettings, NoDecode, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    # ── App ──────────────────────────────────────────────────────────────────
    app_version: str = "1.0.0"
    debug: bool = False

    # ── Database ─────────────────────────────────────────────────────────────
    database_url: str  # postgresql+asyncpg://...
    database_sync_url: str  # postgresql+psycopg2://... (Alembic migrations)

    # Pool settings tuned for Cloud Run (many short-lived instances)
    db_pool_size: int = 5
    db_max_overflow: int = 10
    db_pool_timeout: int = 30

    # ── Firebase ─────────────────────────────────────────────────────────────
    firebase_project_id: str
    firebase_service_account_path: str

    # ── Google Play ──────────────────────────────────────────────────────────
    google_play_package_name: str = "com.nurtlina.app"
    google_play_service_account_path: str = ""

    # ── Object Storage (optional) ────────────────────────────────────────────
    gcs_bucket_name: str = ""
    gcs_export_expiry_seconds: int = 3600

    # ── CORS ─────────────────────────────────────────────────────────────────
    allowed_origins: Annotated[list[str], NoDecode] = ["*"]

    @field_validator("allowed_origins", mode="before")
    @classmethod
    def split_origins(cls, v: object) -> object:
        if isinstance(v, str):
            return [o.strip() for o in v.split(",")]
        return v


settings = Settings()  # type: ignore[call-arg]
