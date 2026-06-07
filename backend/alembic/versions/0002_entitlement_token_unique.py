"""Add unique purchase token hash constraint.

Revision ID: 0002
Revises: 0001
Create Date: 2026-06-07
"""

from typing import Sequence, Union

from alembic import op

revision: str = "0002"
down_revision: Union[str, None] = "0001"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_unique_constraint(
        "uq_entitlements_purchase_token_hash",
        "entitlements",
        ["purchase_token_hash"],
    )


def downgrade() -> None:
    op.drop_constraint(
        "uq_entitlements_purchase_token_hash",
        "entitlements",
        type_="unique",
    )
