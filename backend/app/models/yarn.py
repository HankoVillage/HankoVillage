from datetime import date, datetime

from sqlalchemy import Date, DateTime, ForeignKey, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class Yarn(Base):
    __tablename__ = "yarns"

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), index=True
    )
    label_image_key: Mapped[str | None] = mapped_column(String(255))
    name: Mapped[str] = mapped_column(String(50))
    color: Mapped[str] = mapped_column(String(30))
    purchase_date: Mapped[date | None] = mapped_column(Date)
    purchase_site: Mapped[str | None] = mapped_column(String(100))
    memo: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )