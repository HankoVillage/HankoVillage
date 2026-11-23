from datetime import datetime
from typing import Literal, Optional
from pydantic import BaseModel, ConfigDict, Field

from app.photos import photo_urls

PatternTag = Literal["clothing", "scarf", "hat", "bag", "doll", "etc"]
PatternStatus = Literal["planned", "in_progress", "done"]


class PatternCreate(BaseModel):
    title: str = Field(..., min_length=1, max_length=50, description="도안 제목 (1~50자)")
    tag: PatternTag = Field(..., description="카테고리 태그 코드 (clothing/scarf/hat/bag/doll/etc)")
    status: PatternStatus = Field("planned", description="진행 상태 (기본값 planned)")
    link_url: Optional[str] = Field(None, max_length=2048, description="도안 링크 URL")
    yarn_info: Optional[str] = Field(None, max_length=50, description="사용 실 정보 (50자 이하)")


class PatternUpdate(BaseModel):
    title: Optional[str] = Field(None, min_length=1, max_length=50)
    tag: Optional[PatternTag] = None
    status: Optional[PatternStatus] = None
    link_url: Optional[str] = Field(None, max_length=2048)
    yarn_info: Optional[str] = Field(None, max_length=50)


class PatternResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    user_id: int
    title: str
    yarn_info: Optional[str] = None
    tag: str
    status: str
    link_url: Optional[str] = None
    finished_photo_key: Optional[str] = None
    image_url: Optional[str] = None
    thumbnail_url: Optional[str] = None
    created_at: datetime
    updated_at: datetime

    def model_post_init(self, __context) -> None:
        if self.finished_photo_key:
            urls = photo_urls(f"/patterns/{self.id}/image", self.finished_photo_key)
            self.image_url = urls.get("image_url")
            self.thumbnail_url = urls.get("thumbnail_url")
        else:
            self.image_url = None
            self.thumbnail_url = None