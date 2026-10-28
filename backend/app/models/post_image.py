from sqlalchemy import ForeignKey, String, text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class PostImage(Base):
    __tablename__ = "post_images"
    __table_args__ = (UniqueConstraint("post_id", "sort_order"),)

    id: Mapped[int] = mapped_column(primary_key=True)
    post_id: Mapped[int] = mapped_column(
        ForeignKey("posts.id", ondelete="CASCADE"), index=True
    )
    image_key: Mapped[str] = mapped_column(String(255))
    sort_order: Mapped[int] = mapped_column(server_default=text("0"))
