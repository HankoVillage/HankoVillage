from datetime import datetime

from sqlalchemy import CheckConstraint, DateTime, ForeignKey, String, func, text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class Project(Base):
    __tablename__ = "projects"
    __table_args__ = (
        CheckConstraint("current_row >= 0", name="current_row_non_negative"),
        CheckConstraint(
            "target_row IS NULL OR target_row > 0", name="target_row_positive"
        ),
        CheckConstraint(
            "needle_type IN ('knitting', 'crochet', 'both')",
            name="needle_type_allowed",
        ),
        CheckConstraint("status IN ('in_progress', 'done')", name="status_allowed"),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), index=True
    )
    name: Mapped[str] = mapped_column(String(50))
    yarn_id: Mapped[int | None] = mapped_column(
        ForeignKey("yarns.id", ondelete="SET NULL"), index=True
    )
    needle_type: Mapped[str] = mapped_column(
        String(10), server_default=text("'knitting'")
    )
    needle: Mapped[str | None] = mapped_column(String(50))
    status: Mapped[str] = mapped_column(
        String(15), server_default=text("'in_progress'")
    )
    current_row: Mapped[int] = mapped_column(server_default=text("0"))
    target_row: Mapped[int | None]
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )
