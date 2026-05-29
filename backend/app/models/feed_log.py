import datetime
from decimal import Decimal

from sqlalchemy import DateTime, ForeignKey, Index, Integer, Numeric, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class FeedLog(Base):
    """Feed type values: FORMULA | BREAST_MILK | MIXED | NURSING | OTHER"""

    __tablename__ = "feed_logs"
    __table_args__ = (Index("idx_feed_logs_baby_started", "baby_id", "started_at"),)

    id: Mapped[str] = mapped_column(String, primary_key=True)
    family_id: Mapped[str] = mapped_column(
        String, ForeignKey("families.id"), nullable=False
    )
    baby_id: Mapped[str] = mapped_column(
        String, ForeignKey("babies.id"), nullable=False
    )
    bottle_id: Mapped[str | None] = mapped_column(
        String, ForeignKey("bottles.id"), nullable=True
    )
    feed_type: Mapped[str] = mapped_column(String, nullable=False)
    amount_ml: Mapped[Decimal | None] = mapped_column(Numeric(8, 2), nullable=True)
    started_at: Mapped[datetime.datetime] = mapped_column(
        DateTime(timezone=True), nullable=False
    )
    ended_at: Mapped[datetime.datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    note: Mapped[str | None] = mapped_column(String, nullable=True)
    client_id: Mapped[str | None] = mapped_column(String, nullable=True)
    schema_version: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    created_at: Mapped[datetime.datetime] = mapped_column(
        DateTime(timezone=True), nullable=False
    )
    updated_at: Mapped[datetime.datetime] = mapped_column(
        DateTime(timezone=True), nullable=False
    )
    deleted_at: Mapped[datetime.datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
