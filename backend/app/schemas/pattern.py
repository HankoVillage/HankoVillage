from datetime import datetime
from typing import Optional, Literal
from pydantic import BaseModel, Field, ConfigDict, model_validator

PatternTag = Literal["clothing", "scarf", "hat", "bag", "doll", "etc"]
PatternStatus = Literal["planned", "in_progress", "done"]


class PatternCreate(BaseModel):
    title: str = Field(..., min_length=1, max_length=50, description="도안 제목 (1~50자)")
    tag: PatternTag = Field("etc", description="카테고리 태그 코드")
    status: PatternStatus = Field("planned", description="진행 상태 (planned / in_progress / done)")
    link_url: Optional[str] = Field(None, max_length=2048, description="도안 링크 URL")
    finished_photo_key: Optional[str] = Field(None, max_length=255, description="완성작 사진 키")
    yarn_info: Optional[str] = Field(None, max_length=50, description="사용 실 정보 (50자 이하)")


class PatternUpdate(BaseModel):
    title: Optional[str] = Field(None, min_length=1, max_length=50)
    tag: Optional[PatternTag] = None
    status: Optional[PatternStatus] = None
    link_url: Optional[str] = Field(None, max_length=2048)
    finished_photo_key: Optional[str] = Field(None, max_length=255)
    yarn_info: Optional[str] = Field(None, max_length=50)


class PatternResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    user_id: int
    title: str
    tag: str
    status: str
    link_url: Optional[str] = None
    finished_photo_key: Optional[str] = None
    yarn_info: Optional[str] = None
    image_url: Optional[str] = None
    thumbnail_url: Optional[str] = None
    created_at: datetime
    updated_at: datetime

    @model_validator(mode="after")
    def populate_image_urls(self) -> "PatternResponse":
        if self.finished_photo_key:
            self.image_url = f"/patterns/{self.id}/image"
            self.thumbnail_url = f"/patterns/{self.id}/image?thumb=true"
        else:
            self.image_url = None
            self.thumbnail_url = None
        return self