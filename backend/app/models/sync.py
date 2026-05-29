import datetime

from sqlalchemy import DateTime, ForeignKey, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class SyncCursor(Base):
    """Tracks the last successful sync timestamp per (user, family, client) triple."""

    __tablename__ = "sync_cursors"
    __table_args__ = (
        UniqueConstraint("user_id", "family_id", "client_id", name="uq_sync_cursor"),
    )

    id: Mapped[str] = mapped_column(String, primary_key=True)
    user_id: Mapped[str] = mapped_column(
        String, ForeignKey("users.id"), nullable=False
    )
    family_id: Mapped[str] = mapped_column(
        String, ForeignKey("families.id"), nullable=False
    )
    client_id: Mapped[str] = mapped_column(String, nullable=False)
    last_pull_at: Mapped[datetime.datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    last_push_at: Mapped[datetime.datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    created_at: Mapped[datetime.datetime] = mapped_column(
        DateTime(timezone=True), nullable=False
    )
    updated_at: Mapped[datetime.datetime] = mapped_column(
        DateTime(timezone=True), nullable=False
    )
