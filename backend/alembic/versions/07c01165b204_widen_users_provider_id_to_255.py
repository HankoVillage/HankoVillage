"""widen users.provider_id to 255 characters

Revision ID: 07c01165b204
Revises: e240b078bdfe
Create Date: 2026-11-03 13:50:00

OpenID Connect allows sub values up to 255 ASCII characters,
and Kakao issues pairwise sub values whose length is not documented.
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '07c01165b204'
down_revision: Union[str, Sequence[str], None] = 'e240b078bdfe'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    op.alter_column('users', 'provider_id',
               existing_type=sa.String(length=64),
               type_=sa.String(length=255),
               existing_nullable=False)


def downgrade() -> None:
    """Downgrade schema."""
    # Fails if any provider_id is longer than 64 characters.
    op.alter_column('users', 'provider_id',
               existing_type=sa.String(length=255),
               type_=sa.String(length=64),
               existing_nullable=False)
