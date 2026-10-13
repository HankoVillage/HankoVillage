from datetime import datetime

from sqlalchemy import CheckConstraint, DateTime, ForeignKey, Index, String, Text, func, text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class Pattern(Base):
    __tablename__ = "patterns"
    __table_args__ = (
        CheckConstraint(
            "tag IN ('clothing', 'scarf', 'hat', 'bag', 'doll', 'etc')",
            name="tag_allowed",
        ),
        CheckConstraint(
            "status IN ('planned', 'in_progress', 'done')", name="status_allowed"
        ),
        Index(
            "ix_patterns_title_trgm",
            "title",
            postgresql_using="gin",
            postgresql_ops={"title": "gin_trgm_ops"},
        ),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), index=True
    )
    title: Mapped[str] = mapped_column(String(50))
    tag: Mapped[str] = mapped_column(String(10))
    status: Mapped[str] = mapped_column(String(15), server_default=text("'planned'"))
    description: Mapped[str | None] = mapped_column(Text)
    link_url: Mapped[str | None] = mapped_column(String(2048))
    finished_photo_key: Mapped[str | None] = mapped_column(String(255))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )
