"""Remove bottles table (Bottle entity removed in v2.0).

Revision ID: 0003
Revises: 0002
Create Date: 2026-07-10

Drops the bottles table and its foreign key from feed_logs.
The feed_logs.bottle_id column is retained as a nullable legacy field.
"""

from typing import Sequence, Union

from alembic import op

revision: str = "0003"
down_revision: Union[str, None] = "0002"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Drop the FK constraint from feed_logs.bottle_id → bottles.id
    op.drop_constraint(
        "feed_logs_bottle_id_fkey",
        "feed_logs",
        type_="foreignkey",
    )
    # Drop indexes on bottles
    op.drop_index("idx_bottles_baby_prepared", table_name="bottles")
    op.drop_index("idx_bottles_family_updated", table_name="bottles")
    # Drop the bottles table
    op.drop_table("bottles")


def downgrade() -> None:
    # Re-create bottles table
    import sqlalchemy as sa

    op.create_table(
        "bottles",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("family_id", sa.String(), nullable=False),
        sa.Column("baby_id", sa.String(), nullable=False),
        sa.Column("milk_type", sa.String(), nullable=False),
        sa.Column("amount_ml", sa.Numeric(8, 2), nullable=True),
        sa.Column("prepared_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("feeding_started_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("refrigerated_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("status", sa.String(), nullable=False),
        sa.Column("guideline_region", sa.String(), nullable=False),
        sa.Column("rule_version", sa.String(), nullable=False),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("discarded_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("fed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("note", sa.String(), nullable=True),
        sa.Column("client_id", sa.String(), nullable=True),
        sa.Column("schema_version", sa.Integer(), nullable=False, server_default="1"),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("deleted_at", sa.DateTime(timezone=True), nullable=True),
        sa.ForeignKeyConstraint(["baby_id"], ["babies.id"]),
        sa.ForeignKeyConstraint(["family_id"], ["families.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("idx_bottles_baby_prepared", "bottles", ["baby_id", "prepared_at"])
    op.create_index("idx_bottles_family_updated", "bottles", ["family_id", "updated_at"])
    # Re-create FK from feed_logs.bottle_id → bottles.id
    op.create_foreign_key(
        "feed_logs_bottle_id_fkey",
        "feed_logs",
        "bottles",
        ["bottle_id"],
        ["id"],
    )
