import datetime

from sqlalchemy import DateTime, ForeignKey, Index, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class DiaperLog(Base):
    """Diaper type values: WET | DIRTY | MIXED | DRY"""

    __tablename__ = "diaper_logs"
    __table_args__ = (Index("idx_diaper_logs_baby_changed", "baby_id", "changed_at"),)

    id: Mapped[str] = mapped_column(String, primary_key=True)
    family_id: Mapped[str] = mapped_column(String, ForeignKey("families.id"), nullable=False)
    baby_id: Mapped[str] = mapped_column(String, ForeignKey("babies.id"), nullable=False)
    diaper_type: Mapped[str] = mapped_column(String, nullable=False)
    changed_at: Mapped[datetime.datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    note: Mapped[str | None] = mapped_column(String, nullable=True)
    client_id: Mapped[str | None] = mapped_column(String, nullable=True)
    schema_version: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    created_at: Mapped[datetime.datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    updated_at: Mapped[datetime.datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    deleted_at: Mapped[datetime.datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
