import datetime
from decimal import Decimal

from sqlalchemy import DateTime, ForeignKey, Index, Integer, Numeric, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class Bottle(Base):
    """Represents one prepared bottle with its full timer state.

    Status values (see AGENTS.md state machine):
      NOT_STARTED | FEEDING_STARTED | REFRIGERATED | EXPIRED | FED | DISCARDED | CANCELED
    Milk type values:
      FORMULA | BREAST_MILK | CUSTOM
    """

    __tablename__ = "bottles"
    __table_args__ = (
        Index("idx_bottles_family_updated", "family_id", "updated_at"),
        Index("idx_bottles_baby_prepared", "baby_id", "prepared_at"),
    )

    id: Mapped[str] = mapped_column(String, primary_key=True)
    family_id: Mapped[str] = mapped_column(String, ForeignKey("families.id"), nullable=False)
    baby_id: Mapped[str] = mapped_column(String, ForeignKey("babies.id"), nullable=False)
    milk_type: Mapped[str] = mapped_column(String, nullable=False)
    amount_ml: Mapped[Decimal | None] = mapped_column(Numeric(8, 2), nullable=True)
    prepared_at: Mapped[datetime.datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    feeding_started_at: Mapped[datetime.datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    refrigerated_at: Mapped[datetime.datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    status: Mapped[str] = mapped_column(String, nullable=False)
    guideline_region: Mapped[str] = mapped_column(String, nullable=False)
    rule_version: Mapped[str] = mapped_column(String, nullable=False)
    expires_at: Mapped[datetime.datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    discarded_at: Mapped[datetime.datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    fed_at: Mapped[datetime.datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    note: Mapped[str | None] = mapped_column(String, nullable=True)
    client_id: Mapped[str | None] = mapped_column(String, nullable=True)
    schema_version: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    created_at: Mapped[datetime.datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    updated_at: Mapped[datetime.datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    deleted_at: Mapped[datetime.datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
