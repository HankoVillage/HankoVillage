from datetime import date, datetime
from typing import Optional
from pydantic import BaseModel, ConfigDict, Field

from app.photos import photo_urls


class YarnCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=50)
    color: Optional[str] = Field(None, max_length=30)
    purchase_date: Optional[date] = None
    purchase_site: Optional[str] = Field(None, max_length=100)
    memo: Optional[str] = Field(None, max_length=100)


class YarnUpdate(BaseModel):
    name: Optional[str] = Field(None, min_length=1, max_length=50)
    color: Optional[str] = Field(None, max_length=30)
    purchase_date: Optional[date] = None
    purchase_site: Optional[str] = Field(None, max_length=100)
    memo: Optional[str] = Field(None, max_length=100)


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
            urls = photo_urls(f"/yarns/{self.id}/image", self.label_image_key)
            if isinstance(urls, dict):
                self.image_url = urls.get("image_url")
                self.thumbnail_url = urls.get("thumbnail_url")
            else:
                self.image_url, self.thumbnail_url = urls
        else:
            self.image_url = None
            self.thumbnail_url = None