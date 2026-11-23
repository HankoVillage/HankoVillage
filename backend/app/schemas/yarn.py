from datetime import date, datetime
from typing import Optional
from pydantic import BaseModel, ConfigDict, Field


class YarnCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=50, description="실 이름 (1~50자 필수)")
    color: Optional[str] = Field(None, max_length=30, description="색상 (30자 이하 선택)")
    purchase_date: Optional[date] = Field(None, description="구매일 (YYYY-MM-DD)")
    purchase_site: Optional[str] = Field(None, max_length=100, description="구매처 (100자 이하)")
    memo: Optional[str] = Field(None, max_length=100, description="메모 (100자 이하)")


class YarnUpdate(BaseModel):
    name: Optional[str] = Field(None, min_length=1, max_length=50, description="실 이름 (null 전달 시 422)")
    color: Optional[str] = Field(None, max_length=30)
    purchase_date: Optional[date] = None
    purchase_site: Optional[str] = Field(None, max_length=100)
    memo: Optional[str] = Field(None, max_length=100)


class YarnImageResponse(BaseModel):
    image_url: Optional[str] = None
    thumbnail_url: Optional[str] = None


class YarnResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    user_id: int
    label_image_key: Optional[str] = None
    image_url: Optional[str] = None
    thumbnail_url: Optional[str] = None
    name: str
    color: Optional[str] = None
    purchase_date: Optional[date] = None
    purchase_site: Optional[str] = None
    memo: Optional[str] = None
    created_at: datetime

    def model_post_init(self, __context) -> None:
        if self.label_image_key:
            self.image_url = f"/yarns/{self.id}/image"
            self.thumbnail_url = f"/yarns/{self.id}/image?thumb=true"
        else:
            self.image_url = None
            self.thumbnail_url = None