from datetime import datetime

from sqlalchemy import DateTime, String, UniqueConstraint, func, text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class User(Base):
    __tablename__ = "users"
    __table_args__ = (UniqueConstraint("provider", "provider_id"),)

    id: Mapped[int] = mapped_column(primary_key=True)
    nickname: Mapped[str] = mapped_column(String(10), unique=True)
    provider: Mapped[str] = mapped_column(String(10))
    provider_id: Mapped[str] = mapped_column(String(64))
    profile_image_key: Mapped[str | None] = mapped_column(String(255))
    bio: Mapped[str | None] = mapped_column(String(30))
    notify_comment: Mapped[bool] = mapped_column(server_default=text("true"))
    notify_like: Mapped[bool] = mapped_column(server_default=text("true"))
    terms_agreed_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )